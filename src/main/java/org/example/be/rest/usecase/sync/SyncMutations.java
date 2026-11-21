package org.example.be.rest.usecase.sync;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.JsonNullable;
import org.example.be.rest.dto.req.bucket.CreateBucketReq;
import org.example.be.rest.dto.req.bucket.EditBucketReq;
import org.example.be.rest.dto.req.category.CreateCategoryReq;
import org.example.be.rest.dto.req.category.RenameCategoryReq;
import org.example.be.rest.dto.req.entry.CreateEntryReq;
import org.example.be.rest.dto.req.entry.UpdateEntryReq;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.DeletionMarkerJpaRepository;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.usecase.bucket.CreateBucketUseCase;
import org.example.be.rest.usecase.bucket.UpdateBucketUseCase;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.example.be.rest.usecase.category.CreateCategoryUseCase;
import org.example.be.rest.usecase.category.DeleteCategoryUseCase;
import org.example.be.rest.usecase.category.UpdateCategoryUseCase;
import org.example.be.rest.usecase.entry.CreateEntryUseCase;
import org.example.be.rest.usecase.entry.DeleteEntryUseCase;
import org.example.be.rest.usecase.entry.UpdateEntryUseCase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
class SyncMutations {
    private final JdbcTemplate jdbc;
    private final SyncInput input;
    private final CategoryJpaRepository categories;
    private final SpendingBucketJpaRepository buckets;
    private final EntryDetailViewRepository entryViews;
    private final DeletionMarkerJpaRepository markers;
    private final BucketQueryService bucketQuery;
    private final CreateBucketUseCase createBucket;
    private final UpdateBucketUseCase updateBucket;
    private final CreateCategoryUseCase createCategory;
    private final UpdateCategoryUseCase updateCategory;
    private final DeleteCategoryUseCase deleteCategory;
    private final CreateEntryUseCase createEntry;
    private final UpdateEntryUseCase updateEntry;
    private final DeleteEntryUseCase deleteEntry;

    boolean deleted(String userId, String type, String id) {
        return markers.existsByUserIdAndRecordTypeAndRecordId(userId, type, id);
    }

    Integer version(String userId, String type, String id) {
        String table = switch (type) {
            case "entry" -> "tblEntry";
            case "category" -> "tblCategory";
            case "bucket" -> "tblSpendingBucket";
            default -> throw new IllegalArgumentException("Unsupported record type");
        };
        return jdbc.query("SELECT version FROM " + table + " WHERE tblUserid = ? AND id = ? FOR UPDATE",
                (row, index) -> row.getInt(1), userId, id).stream().findFirst().orElse(null);
    }

    int create(String userId, String type, String id, JsonNode data, String path) {
        var payload = input.withId(data, id);
        return switch (type) {
            case "entry" -> createEntry.execute(userId, input.bind(payload, CreateEntryReq.class, path))
                    .getEntry().getVersion();
            case "category" -> createCategory.execute(userId, input.bind(payload, CreateCategoryReq.class, path)).record().getVersion();
            case "bucket" -> createBucket.execute(userId, input.bind(payload, CreateBucketReq.class, path)).record().getVersion();
            default -> throw new IllegalArgumentException("Unsupported record type");
        };
    }

    boolean same(String userId, String type, String id, JsonNode data, String path) {
        var payload = input.withId(data, id);
        return switch (type) {
            case "category" -> {
                var req = input.bind(payload, CreateCategoryReq.class, path);
                yield categories.findByIdAndUserId(id, userId).orElseThrow().getName().equals(req.getName().trim());
            }
            case "bucket" -> {
                var req = input.bind(payload, CreateBucketReq.class, path);
                var bucket = buckets.findByIdAndUserId(id, userId).orElseThrow();
                yield bucket.getName().equals(req.getName().trim()) && bucket.getMonthlyTarget().compareTo(req.getMonthlyTarget()) == 0
                        && new HashSet<>(bucketQuery.categoryIds(id)).equals(new HashSet<>(bucketQuery.validateCategoryIds(userId, req.getCategoryIds())));
            }
            case "entry" -> {
                var req = input.bind(payload, CreateEntryReq.class, path);
                var entry = entryViews.findById(id).orElseThrow();
                yield !entry.isShared() && req.getCategoryId().equals(entry.getCategoryId()) && sameEntryValues(req, entry);
            }
            default -> throw new IllegalArgumentException("Unsupported record type");
        };
    }

    void update(String userId, String type, String id, int version, JsonNode data, String path) {
        var payload = input.withId(data, id);
        switch (type) {
            case "category" -> {
                var request = input.bind(payload, CreateCategoryReq.class, path);
                var edit = new RenameCategoryReq();
                edit.setVersion(version);
                edit.setName(request.getName());
                updateCategory.execute(userId, id, edit);
            }
            case "bucket" -> {
                var request = input.bind(payload, CreateBucketReq.class, path);
                var edit = new EditBucketReq();
                edit.setVersion(version);
                edit.setName(request.getName());
                edit.setMonthlyTarget(request.getMonthlyTarget());
                edit.setCategoryIds(request.getCategoryIds());
                updateBucket.execute(userId, id, edit);
            }
            case "entry" -> {
                var request = input.bind(payload, CreateEntryReq.class, path);
                var entry = entryViews.findById(id).orElseThrow();
                var edit = new UpdateEntryReq();
                edit.setVersion(version);
                edit.setCategoryId(request.getCategoryId());
                if (entry.isShared()) {
                    if (!sameEntryValues(request, entry)) throw new BusinessLogicException(ErrorCode.SHARED_ENTRY_READ_ONLY);
                } else {
                    edit.setName(request.getName());
                    edit.setType(request.getType());
                    edit.setAmount(request.getAmount());
                    edit.setOccurredAt(request.getOccurredAt());
                    edit.setBucketId(request.getBucketId());
                    edit.setNote(JsonNullable.of(request.getNote()));
                }
                updateEntry.execute(userId, id, edit);
            }
            default -> throw new IllegalArgumentException("Unsupported record type");
        }
    }

    void delete(String userId, String type, String id, int version) {
        switch (type) {
            case "entry" -> deleteEntry.execute(userId, id, version);
            case "category" -> deleteCategory.execute(userId, id, version, null);
            case "bucket" -> {
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                jdbc.update("""
                        UPDATE tblEntry SET tblSpendingBucketid = NULL, version = version + 1, updatedAt = ?
                        WHERE tblUserid = ? AND tblSpendingBucketid = ?
                        """, now, userId, id);
                jdbc.update("DELETE FROM tblSpendingBucket WHERE tblUserid = ? AND id = ?", userId, id);
                writeMarker(userId, type, id, now);
            }
            default -> throw new IllegalArgumentException("Unsupported record type");
        }
    }

    void writeMarker(String userId, String type, String id, LocalDateTime deletedAt) {
        jdbc.update("""
                INSERT INTO tblDeletionMarker (id, tblUserid, recordType, recordId, deletedAt, expiresAt)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (tblUserid, recordType, recordId) DO NOTHING
                """, UUID.randomUUID().toString(), userId, type, id, deletedAt,
                LocalDateTime.now(ZoneOffset.UTC).plusDays(30));
    }

    private static boolean sameEntryValues(CreateEntryReq req, org.example.be.core.entity.EntryDetailView entry) {
        return req.getName().trim().equals(entry.getName()) && req.getType().equals(entry.getEntryType())
                && req.getAmount().compareTo(entry.getAmount()) == 0 && Objects.equals(req.getNote(), entry.getNote())
                && (req.getOccurredAt() == null || req.getOccurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                    .truncatedTo(java.time.temporal.ChronoUnit.MICROS).equals(entry.getOccurredAt()))
                && (!req.getBucketId().isPresent() || Objects.equals(req.getBucketId().get(), entry.getBucketId()));
    }
}
