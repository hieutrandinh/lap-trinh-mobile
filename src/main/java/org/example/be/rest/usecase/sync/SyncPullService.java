package org.example.be.rest.usecase.sync;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.category.CategoryRes;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.example.be.rest.usecase.entry.support.EntryQueryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncPullService {
    private final JdbcTemplate jdbc;
    private final CategoryJpaRepository categories;
    private final SpendingBucketJpaRepository buckets;
    private final EntryJpaRepository entries;
    private final EntryDetailViewRepository entryViews;
    private final EntryQueryService entryQuery;
    private final BucketCategoryJpaRepository bucketCategories;
    private final BucketQueryService bucketQuery;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> pull(String userId, String deviceId, String cursor, int limit) {
        Cursor position = Cursor.parse(cursor);
        if (limit < 1 || limit > 1000) throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        if (!SyncDevice.isActive(jdbc, userId, deviceId)) throw new BusinessLogicException(ErrorCode.UNAUTHENTICATED);

        // The snapshot horizon prevents a late-committing transaction from being skipped by a later cursor.
        List<Change> changes = jdbc.query("""
                SELECT change_tx::text, sequence, c.record_type, c.record_id, m.deletedAt
                FROM personal_sync_change c
                LEFT JOIN tblDeletionMarker m ON m.tblUserid = c.user_id
                    AND m.recordType = c.record_type AND m.recordId = c.record_id
                WHERE c.user_id = ? AND (c.change_tx, c.sequence) > (?::xid8, ?)
                    AND c.change_tx < pg_snapshot_xmin(pg_current_snapshot())
                    AND (m.expiresAt IS NULL OR m.expiresAt > timezone('UTC', statement_timestamp()))
                ORDER BY c.change_tx, c.sequence LIMIT ?
                """, (row, index) -> new Change(row.getString(1), row.getLong(2), row.getString(3), row.getString(4),
                        row.getObject(5, LocalDateTime.class)), userId, position.transaction(), position.sequence(), limit + 1);
        boolean hasMore = changes.size() > limit;
        List<Change> page = changes.subList(0, Math.min(changes.size(), limit));
        var entryRows = entryViews.findAllById(ids(page, "entry")).stream()
                .filter(row -> userId.equals(row.getUserId())).toList();
        var categoryRows = categories.findAllById(ids(page, "category")).stream()
                .filter(row -> userId.equals(row.getUserId()))
                .map(row -> CategoryRes.from(row, entries.countByUserIdAndCategoryId(userId, row.getId()),
                        bucketCategories.findByCategoryId(row.getId()).stream().map(link -> link.getBucketId()).toList()))
                .toList();
        var bucketRows = buckets.findAllById(ids(page, "bucket")).stream()
                .filter(row -> userId.equals(row.getUserId()))
                .map(row -> new Bucket(row.getId(), row.getName(), row.getMonthlyTarget(), "USD",
                        bucketQuery.categoryIds(row.getId()), bucketQuery.categoryRefs(row.getId()), row.getVersion()))
                .toList();
        var deletions = new ArrayList<Map<String, Object>>();
        for (Change change : page) {
            if (change.deletedAt() != null) {
                deletions.add(Map.of("recordType", change.type(), "recordId", change.id(), "deletedAt", change.deletedAt()));
            }
        }
        String next = page.isEmpty() ? cursor : Cursor.from(page.getLast());
        if (!hasMore) jdbc.update("""
                UPDATE tblDevice SET lastSyncAt = ?, sync_cursor = ? WHERE id = ? AND tblUserid = ? AND signedOutAt IS NULL
                """, LocalDateTime.now(ZoneOffset.UTC), next, deviceId, userId);
        return Map.of("upserts", Map.of("entries", entryQuery.toRes(entryRows), "categories", categoryRows,
                "buckets", bucketRows), "deletions", deletions, "nextCursor", next, "hasMore", hasMore);
    }

    private static List<String> ids(List<Change> page, String type) {
        return page.stream().filter(change -> change.deletedAt() == null && type.equals(change.type()))
                .map(Change::id).toList();
    }

    private record Change(String transaction, long sequence, String type, String id, LocalDateTime deletedAt) {}

    private record Bucket(String id, String name, BigDecimal monthlyTarget, String currency, List<String> categoryIds,
                          List<org.example.be.rest.dto.res.category.CategoryRefRes> categories, int version) {}

    private record Cursor(String transaction, long sequence) {
        static Cursor parse(String raw) {
            if ("0".equals(raw)) return new Cursor("0", 0);
            try {
                if (!raw.matches("[0-9]{1,20}:[0-9]{1,19}:[0-9]{1,12}")) throw new IllegalArgumentException();
                String[] parts = raw.split(":");
                Long.parseUnsignedLong(parts[0]);
                long sequence = Long.parseLong(parts[1]);
                Instant issued = Instant.ofEpochSecond(Long.parseLong(parts[2]));
                if (issued.isBefore(Instant.now().minus(30, ChronoUnit.DAYS))) {
                    throw new BusinessLogicException(ErrorCode.CURSOR_EXPIRED);
                }
                return new Cursor(parts[0], sequence);
            } catch (IllegalArgumentException invalid) {
                throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
            }
        }

        static String from(Change change) {
            return change.transaction() + ":" + change.sequence() + ":" + Instant.now().getEpochSecond();
        }
    }
}
