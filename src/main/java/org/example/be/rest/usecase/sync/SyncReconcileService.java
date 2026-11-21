package org.example.be.rest.usecase.sync;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class SyncReconcileService {
    private final JdbcTemplate jdbc;
    private final SyncMutations mutations;
    private final CategoryJpaRepository onlineCategories;
    private final EntityManager entityManager;

    @Transactional
    public Map<String, Object> reconcile(String userId, String deviceId, JsonNode payload) {
        if (!SyncDevice.isActive(jdbc, userId, deviceId)) throw new BusinessLogicException(ErrorCode.UNAUTHENTICATED);
        JsonNode workspace = payload.path("guestWorkspace");
        var categories = SyncInput.array(workspace.path("categories"), "guestWorkspace.categories", 0, 500);
        var buckets = SyncInput.array(workspace.path("buckets"), "guestWorkspace.buckets", 0, 100);
        var entries = SyncInput.array(workspace.path("entries"), "guestWorkspace.entries", 0, 20_000);
        var deletions = SyncInput.array(workspace.path("deletionMarkers"), "guestWorkspace.deletionMarkers", 0, 20_000);

        var assigned = new HashMap<String, String>();
        var merged = new LinkedHashMap<String, String>();
        int categoriesAdded = 0;
        for (int i = 0; i < categories.size(); i++) {
            JsonNode item = categories.get(i);
            String path = "guestWorkspace.categories[" + i + "]";
            String id = SyncInput.id(item.path("id"), path + ".id");
            if (assigned.containsKey(id)) throw SyncInput.invalid(path + ".id");
            String name = SyncInput.text(item.path("name"), path + ".name").trim();
            if (name.length() > 50) throw SyncInput.invalid(path + ".name");
            var online = onlineCategories.findByUserIdAndNameIgnoreCase(userId, name);
            if (online.isPresent()) {
                String onlineId = online.get().getId();
                assigned.put(id, onlineId);
                merged.put(id, onlineId);
            } else {
                if (mutations.deleted(userId, "category", id)) throw invalidReference(path + ".id");
                mutations.create(userId, "category", id, item, path);
                assigned.put(id, id);
                categoriesAdded++;
            }
        }

        int bucketsAdded = 0;
        for (int i = 0; i < buckets.size(); i++) {
            String path = "guestWorkspace.buckets[" + i + "]";
            ObjectNode item = ((ObjectNode) buckets.get(i)).deepCopy();
            String id = SyncInput.id(item.path("id"), path + ".id");
            JsonNode refs = item.path("categoryIds");
            if (!refs.isArray()) throw SyncInput.invalid(path + ".categoryIds");
            var target = item.putArray("categoryIds");
            var mapped = new java.util.LinkedHashSet<String>();
            for (JsonNode ref : refs) mapped.add(category(assigned, ref, path + ".categoryIds"));
            mapped.forEach(target::add);
            if (mutations.version(userId, "bucket", id) == null && !mutations.deleted(userId, "bucket", id)) {
                mutations.create(userId, "bucket", id, item, path);
                bucketsAdded++;
            }
        }

        int entriesAdded = 0;
        int entriesExisting = 0;
        int entriesDeleted = 0;
        for (int i = 0; i < entries.size(); i++) {
            String path = "guestWorkspace.entries[" + i + "]";
            ObjectNode item = ((ObjectNode) entries.get(i)).deepCopy();
            String id = SyncInput.id(item.path("id"), path + ".id");
            item.put("categoryId", category(assigned, item.path("categoryId"), path + ".categoryId"));
            if (mutations.deleted(userId, "entry", id)) {
                entriesDeleted++;
            } else if (mutations.version(userId, "entry", id) != null) {
                entriesExisting++;
            } else {
                mutations.create(userId, "entry", id, item, path);
                entriesAdded++;
            }
        }

        entityManager.flush();
        for (int i = 0; i < deletions.size(); i++) {
            JsonNode item = deletions.get(i);
            String path = "guestWorkspace.deletionMarkers[" + i + "]";
            String type = SyncInput.type(item.path("recordType"), path + ".recordType");
            String id = SyncInput.id(item.path("recordId"), path + ".recordId");
            LocalDateTime deletedAt = date(item.path("deletedAt"), path + ".deletedAt");
            Integer version = mutations.version(userId, type, id);
            if (version != null) {
                mutations.delete(userId, type, id, version);
                entityManager.flush();
            } else {
                mutations.writeMarker(userId, type, id, deletedAt);
            }
        }

        return Map.of("summary", Map.of("entriesAdded", entriesAdded, "entriesSkippedExisting", entriesExisting,
                "entriesSkippedDeleted", entriesDeleted, "categoriesAdded", categoriesAdded, "categoriesMerged", merged.size(),
                "bucketsAdded", bucketsAdded), "categoryIdMap", merged, "cursor", "0");
    }

    private static String category(Map<String, String> assigned, JsonNode ref, String path) {
        String id = SyncInput.id(ref, path);
        String mapped = assigned.get(id);
        if (mapped == null) throw invalidReference(path);
        return mapped;
    }

    private static BusinessLogicException invalidReference(String path) {
        return new BusinessLogicException(ErrorCode.GUEST_REFERENCE_INVALID,
                List.of(Map.of("field", path, "issue", "INVALID_REFERENCE", "message", "Category is missing from the upload.")));
    }

    private static LocalDateTime date(JsonNode node, String path) {
        try {
            LocalDateTime value = OffsetDateTime.parse(SyncInput.text(node, path)).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            if (value.isAfter(LocalDateTime.now(ZoneOffset.UTC))) throw SyncInput.invalid(path);
            return value;
        } catch (DateTimeParseException e) {
            throw SyncInput.invalid(path);
        }
    }
}
