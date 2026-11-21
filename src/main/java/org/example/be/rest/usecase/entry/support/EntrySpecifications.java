package org.example.be.rest.usecase.entry.support;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.example.be.core.entity.EntryDetailView;
import org.springframework.data.jpa.domain.Specification;

public final class EntrySpecifications {

    private EntrySpecifications() {
    }

    public static Specification<EntryDetailView> build(String userId, String type, String categoryId,
                                                         String bucketId, LocalDateTime from, LocalDateTime to,
                                                         String q, Boolean isShared, String groupId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            if (type != null) {
                predicates.add(cb.equal(root.get("entryType"), type));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if ("none".equals(bucketId)) {
                predicates.add(cb.isNull(root.get("bucketId")));
            } else if (bucketId != null) {
                predicates.add(cb.equal(root.get("bucketId"), bucketId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), to));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("note"), "")), like)));
            }
            if (isShared != null) {
                predicates.add(cb.equal(root.get("shared"), isShared));
            }
            if (groupId != null) {
                predicates.add(cb.equal(root.get("groupId"), groupId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
