package org.example.be.rest.usecase.correction.support;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.ExpenseSplitEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.correction.CreateCorrectionReq;
import org.example.be.rest.dto.req.correction.ProposedExpenseReq;
import org.example.be.rest.repository.ExpenseSplitJpaRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.usecase.expense.support.ExpenseMembershipValidator;
import org.example.be.rest.usecase.expense.support.SplitCalculator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CorrectionPlanner {

    ExpenseSplitJpaRepository expenseSplitJpaRepository;
    GroupMemberJpaRepository groupMemberJpaRepository;
    ExpenseMembershipValidator expenseMembershipValidator;
    SplitCalculator splitCalculator;

    public CorrectionPlan plan(GroupExpenseEntity expense, CreateCorrectionReq req) {
        boolean cancellation = "cancellation".equals(req.getType());
        ProposedExpenseReq p = req.getProposed();

        String proposedTitle = p != null && p.getTitle() != null ? p.getTitle().trim() : expense.getTitle();
        var proposedTotalAmount = p != null && p.getTotalAmount() != null ? p.getTotalAmount()
                : expense.getTotalAmount();
        LocalDateTime proposedOccurredAt = p != null && p.getOccurredAt() != null
                ? p.getOccurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                : expense.getOccurredAt();
        String proposedCategory = p != null && p.getCategory() != null ? p.getCategory().trim()
                : expense.getCategory();
        String proposedPayerUserId = p != null && p.getPayerUserId() != null ? p.getPayerUserId()
                : expense.getPayerUserId();
        String proposedSplitMethod = p != null && p.getSplitMethod() != null ? p.getSplitMethod()
                : expense.getSplitMethod();

        List<ExpenseSplitEntity> currentSplits = expenseSplitJpaRepository.findByGroupExpenseId(expense.getId());
        Map<String, java.math.BigDecimal> previousSplits = currentSplits.stream()
                .collect(Collectors.toMap(ExpenseSplitEntity::getUserId, ExpenseSplitEntity::getAmount));

        boolean moneyFieldsProvided = !cancellation && p != null
                && (p.getTotalAmount() != null || p.getSplitMethod() != null || p.getParticipants() != null);

        Map<String, java.math.BigDecimal> proposedSplits;
        if (cancellation) {
            proposedSplits = Map.of();
        } else if (moneyFieldsProvided) {
            if (p.getParticipants() == null || p.getParticipants().isEmpty()) {
                throw new BusinessLogicException(ErrorCode.PARTICIPANTS_REQUIRED);
            }
            expenseMembershipValidator.validate(expense.getGroupId(), proposedPayerUserId, p.getParticipants());
            var computations = splitCalculator.compute(proposedTotalAmount, proposedSplitMethod,
                    p.getParticipants());
            proposedSplits = computations.stream()
                    .collect(Collectors.toMap(SplitCalculator.SplitComputation::userId,
                            SplitCalculator.SplitComputation::amount));
        } else {
            proposedSplits = previousSplits;
        }

        Set<String> allUserIds = new LinkedHashSet<>();
        allUserIds.addAll(previousSplits.keySet());
        allUserIds.addAll(proposedSplits.keySet());

        boolean payerChanged = !proposedPayerUserId.equals(expense.getPayerUserId());
        Set<String> moneyAffected = allUserIds.stream()
                .filter(u -> !Objects.equals(previousSplits.get(u), proposedSplits.get(u)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> affected = new LinkedHashSet<>();
        if (cancellation) {
            affected.addAll(allUserIds);
            affected.add(expense.getPayerUserId());
        } else if (!moneyAffected.isEmpty() || payerChanged) {
            affected.addAll(moneyAffected);
            if (payerChanged) {
                affected.add(expense.getPayerUserId());
                affected.add(proposedPayerUserId);
            }
        } else {
            // Only non-money fields changed: everybody involved must still sign off (11.1 design assumption).
            affected.addAll(allUserIds);
            affected.add(expense.getPayerUserId());
        }

        for (String userId : affected) {
            boolean active = groupMemberJpaRepository.findByGroupIdAndUserId(expense.getGroupId(), userId)
                    .map(m -> "active".equals(m.getStatus()))
                    .orElse(false);
            if (!active) {
                throw new BusinessLogicException(ErrorCode.FORMER_MEMBER_INVOLVED);
            }
        }

        boolean hasChanges = cancellation
                || !moneyAffected.isEmpty()
                || payerChanged
                || !proposedTitle.equals(expense.getTitle())
                || !proposedOccurredAt.equals(expense.getOccurredAt())
                || !proposedCategory.equalsIgnoreCase(expense.getCategory())
                || !proposedSplitMethod.equals(expense.getSplitMethod());
        if (!hasChanges) {
            throw new BusinessLogicException(ErrorCode.NO_CHANGES);
        }

        return new CorrectionPlan(
                expense.getTitle(), proposedTitle,
                expense.getTotalAmount(), proposedTotalAmount,
                expense.getOccurredAt(), proposedOccurredAt,
                expense.getCategory(), proposedCategory,
                expense.getPayerUserId(), proposedPayerUserId,
                expense.getSplitMethod(), proposedSplitMethod,
                previousSplits, proposedSplits,
                affected, cancellation, true);
    }

}
