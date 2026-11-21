package org.example.be.rest.usecase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.ApiRes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;

@Service
public class IdempotencyService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ObjectMapper fingerprintJson = JsonMapper.builder().enable(JsonNodeFeature.WRITE_PROPERTIES_SORTED).build();

    public IdempotencyService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional
    public ApiRes<?> execute(String userId, String key, String operation, Object request, Supplier<ApiRes<?>> action) {
        String fingerprint = reserve(userId, key, operation, request);
        var saved = jdbc.queryForObject("""
                SELECT status, response FROM api_idempotency WHERE user_id = ? AND request_key = ?
                """, (row, index) -> new Cached(row.getObject(1, Integer.class), row.getString(2)), userId, key);
        if (saved.status() != null) {
            return ApiRes.success(saved.status(), json.readTree(saved.response()));
        }
        ApiRes<?> result = action.get();
        jdbc.update("""
                UPDATE api_idempotency SET status = ?, response = ?, expires_at = clock_timestamp() + INTERVAL '24 hours'
                WHERE user_id = ? AND request_key = ? AND fingerprint = ?
                """, result.getStatus(), json.writeValueAsString(result.getData()), userId, key, fingerprint);
        return result;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public String reserve(String userId, String key, String operation, Object request) {
        if (!StringUtils.hasText(key) || key.length() > 64) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        String fingerprint = fingerprint(operation, request);
        jdbc.update("""
                DELETE FROM api_idempotency WHERE (user_id, request_key) IN (
                    SELECT user_id, request_key FROM api_idempotency
                    WHERE expires_at <= clock_timestamp() LIMIT 100 FOR UPDATE SKIP LOCKED
                )
                """);
        jdbc.update("""
                INSERT INTO api_idempotency (user_id, request_key, fingerprint, expires_at)
                VALUES (?, ?, ?, clock_timestamp() + INTERVAL '24 hours')
                ON CONFLICT (user_id, request_key) DO UPDATE
                SET fingerprint = EXCLUDED.fingerprint, expires_at = EXCLUDED.expires_at, status = NULL, response = NULL
                WHERE api_idempotency.expires_at <= clock_timestamp()
                """, userId, key, fingerprint);
        String saved = jdbc.queryForObject("""
                SELECT fingerprint FROM api_idempotency
                WHERE user_id = ? AND request_key = ? FOR UPDATE
                """, String.class, userId, key);
        if (!fingerprint.equals(saved)) {
            throw new BusinessLogicException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        return fingerprint;
    }

    private String fingerprint(String operation, Object request) {
        try {
            byte[] bytes = fingerprintJson.writeValueAsBytes(List.of(operation, fingerprintJson.valueToTree(request)));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private record Cached(Integer status, String response) {}
}
