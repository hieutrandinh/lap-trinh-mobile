package org.example.be.rest.usecase.expense.support;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.example.be.core.entity.CorrectionRequestEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.springframework.data.jpa.domain.Specification;

public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<GroupExpenseEntity> build(String groupId, String category, String payerUserId,
                                                            String status, LocalDateTime from, LocalDateTime to,
                                                            List<String> idsIn, Boolean hasPendingCorrection) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("groupId"), groupId));
            if (category != null) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }
            if (payerUserId != null) {
                predicates.add(cb.equal(root.get("payerUserId"), payerUserId));
            }
            if (status != null && !"all".equals(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), to));
            }
            if (idsIn != null) {
                predicates.add(idsIn.isEmpty() ? cb.disjunction() : root.get("id").in(idsIn));
            }
            if (hasPendingCorrection != null) {
                var pending = query.subquery(Integer.class);
                var correction = pending.from(CorrectionRequestEntity.class);
                pending.select(cb.literal(1)).where(cb.equal(correction.get("groupExpenseId"), root.get("id")),
                        cb.equal(correction.get("status"), "pending"));
                var exists = cb.exists(pending);
                predicates.add(hasPendingCorrection ? exists : cb.not(exists));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
