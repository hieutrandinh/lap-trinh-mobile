package org.example.be.rest.usecase.sync;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.usecase.IdempotencyService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
class SyncPushTransactions {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final IdempotencyService idempotency;
    private final SyncMutations mutations;

    @Transactional
    public Job open(String userId, String deviceId, String key, JsonNode body) {
        if (!SyncDevice.isActive(jdbc, userId, deviceId)) throw new BusinessLogicException(ErrorCode.UNAUTHENTICATED);
        String fingerprint = idempotency.reserve(userId, key, "sync/push/" + deviceId, body);
        jdbc.update("""
                DELETE FROM sync_push_job WHERE user_id = ? AND request_key = ? AND expires_at <= clock_timestamp()
                """, userId, key);
        jdbc.update("""
                DELETE FROM sync_push_job WHERE id IN (
                    SELECT id FROM sync_push_job WHERE expires_at <= clock_timestamp() LIMIT 100 FOR UPDATE SKIP LOCKED
                )
                """);
        jdbc.update("""
                INSERT INTO sync_push_job(id, user_id, request_key, fingerprint, expires_at, cursor)
                SELECT ?, a.user_id, a.request_key, a.fingerprint, a.expires_at, COALESCE(d.sync_cursor, '0')
                FROM api_idempotency a JOIN tblDevice d ON d.tblUserid = a.user_id
                WHERE d.id = ? AND d.signedOutAt IS NULL AND a.user_id = ? AND a.request_key = ?
                ON CONFLICT (user_id, request_key) DO NOTHING
                """, UUID.randomUUID(), deviceId, userId, key);
        return jdbc.query("""
                SELECT id, cursor FROM sync_push_job WHERE user_id = ? AND request_key = ? AND fingerprint = ?
                """, (row, index) -> new Job(row.getObject(1, UUID.class), row.getString(2)), userId, key, fingerprint)
                .stream().findFirst().orElseThrow(() -> new BusinessLogicException(ErrorCode.UNAUTHENTICATED));
    }

    @Transactional
    public JsonNode apply(String userId, String deviceId, Job job, int position, Change change) {
        if (!SyncDevice.isActive(jdbc, userId, deviceId)) throw new BusinessLogicException(ErrorCode.UNAUTHENTICATED);
        JsonNode saved = lockAndRead(job, position);
        if (saved != null) return saved;
        Result result = process(userId, change, "changes[" + position + "].data");
        return save(job, position, result);
    }

    @Transactional
    public JsonNode reject(String userId, Job job, int position, Change change, BusinessLogicException failure) {
        JsonNode saved = lockAndRead(job, position);
        if (saved != null) return saved;
        Object details = failure.getDetails();
        Object errors = details instanceof List<?> ? details : details == null ? List.of() : List.of(details);
        Integer currentVersion = mutations.version(userId, change.type(), change.id());
        var result = new Result(change.id(), "rejected_invalid", currentVersion,
                Map.of("code", failure.getErrorCode().name(), "details", errors));
        return save(job, position, result);
    }

    private Result process(String userId, Change change, String path) {
        String id = change.id();
        String type = change.type();
        if (mutations.deleted(userId, type, id)) return Result.status(id, "discarded_deleted", null);

        Integer version = mutations.version(userId, type, id);
        if (change.baseVersion() == null) {
            if ("delete".equals(change.operation())) throw SyncInput.invalid(path);
            if (version == null) {
                int created = mutations.create(userId, type, id, change.data(), path);
                return Result.status(id, "applied", created);
            }
            boolean identical = mutations.same(userId, type, id, change.data(), path);
            return Result.status(id, identical ? "applied" : "discarded_conflict", version);
        }
        if (version == null || !Objects.equals(version, change.baseVersion())) {
            return Result.status(id, "discarded_conflict", version);
        }
        if ("delete".equals(change.operation())) {
            mutations.delete(userId, type, id, version);
            return Result.status(id, "applied", null);
        }
        mutations.update(userId, type, id, version, change.data(), path);
        return Result.status(id, "applied", version + 1);
    }

    private JsonNode lockAndRead(Job job, int position) {
        jdbc.queryForObject("SELECT id FROM sync_push_job WHERE id = ? FOR UPDATE", UUID.class, job.id());
        return jdbc.query("SELECT response FROM sync_push_result WHERE job_id = ? AND position = ?",
                (row, index) -> json.readTree(row.getString(1)), job.id(), position).stream().findFirst().orElse(null);
    }

    private JsonNode save(Job job, int position, Result result) {
        JsonNode value = json.valueToTree(result);
        jdbc.update("INSERT INTO sync_push_result (job_id, position, response) VALUES (?, ?, ?)",
                job.id(), position, json.writeValueAsString(value));
        return value;
    }

    record Job(UUID id, String cursor) {}
    record Change(String id, String type, String operation, Integer baseVersion, JsonNode data) {}
    private record Result(String recordId, String status, Integer version,
                          @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                          Map<String, Object> error) {
        static Result status(String id, String status, Integer version) {
            return new Result(id, status, version, null);
        }
    }
}
