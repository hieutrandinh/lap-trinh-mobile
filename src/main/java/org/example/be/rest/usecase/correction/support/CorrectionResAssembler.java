package org.example.be.rest.usecase.correction.support;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CorrectionParticipantEntity;
import org.example.be.core.entity.CorrectionRequestEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.rest.dto.res.correction.ApprovalRes;
import org.example.be.rest.dto.res.correction.CorrectionBalanceEffectRes;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.dto.res.correction.ExpenseValuesRes;
import org.example.be.rest.dto.res.correction.ShareRes;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.repayment.support.BalanceCalculatorService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CorrectionResAssembler {

    UserJpaRepository userJpaRepository;
    BalanceCalculatorService balanceCalculatorService;

    public org.example.be.rest.dto.res.group.UserRefRes toRef(String userId) {
        return userJpaRepository.findById(userId)
                .map(u -> org.example.be.rest.dto.res.group.UserRefRes.builder().userId(u.getId())
                        .displayName(u.getDisplayName()).build())
                .orElse(org.example.be.rest.dto.res.group.UserRefRes.builder().userId(userId).displayName(null)
                        .build());
    }

    /** 11.1 preview / create response, built straight from the computed plan (no persisted rows yet). */
    public CorrectionRequestRes fromPlan(GroupExpenseEntity expense, String groupId, String requesterUserId,
                                          String type, String reason, CorrectionPlan plan, String idOrNull,
                                          String statusOverride, java.time.LocalDateTime createdAtOrNull) {
        List<ShareRes> shares = buildShares(plan);
        List<CorrectionBalanceEffectRes> balanceEffects = buildBalanceEffects(groupId, plan);
        List<ApprovalRes> approvals = plan.affectedUserIds().stream()
                .map(u -> ApprovalRes.builder()
                        .user(toRef(u))
                        .status(u.equals(requesterUserId) ? "approved" : "pending")
                        .respondedAt(u.equals(requesterUserId) ? createdAtOrNull : null)
                        .rejectionNote(null)
                        .build())
                .toList();

        return CorrectionRequestRes.builder()
                .id(idOrNull)
                .groupExpenseId(expense.getId())
                .type(type)
                .requestedBy(toRef(requesterUserId))
                .reason(reason)
                .status(statusOverride)
                .previous(previousValues(plan))
                .proposed(proposedValues(plan))
                .shares(shares)
                .balanceEffects(balanceEffects)
                .approvals(approvals)
                .createdAt(createdAtOrNull)
                .closedAt(null)
                .build();
    }

    /** 11.3/11.4/11.5/11.6 - rebuilds the plan from the persisted request + participant rows. */
    public CorrectionRequestRes fromEntity(String groupId, CorrectionRequestEntity request,
                                            List<CorrectionParticipantEntity> participants) {
        CorrectionPlan plan = toPlan(request, participants);
        List<ShareRes> shares = buildShares(plan);
        List<CorrectionBalanceEffectRes> balanceEffects = "pending".equals(request.getStatus())
                ? buildBalanceEffects(groupId, plan) : List.of();

        List<ApprovalRes> approvals = participants.stream()
                .filter(cp -> cp.getApprovalStatus() != null)
                .map(cp -> ApprovalRes.builder()
                        .user(toRef(cp.getUserId()))
                        .status(cp.getApprovalStatus())
                        .respondedAt(cp.getRespondedAt())
                        .rejectionNote(cp.getRejectionNote())
                        .build())
                .toList();

        return CorrectionRequestRes.builder()
                .id(request.getId())
                .groupExpenseId(request.getGroupExpenseId())
                .type(request.getRequestType())
                .requestedBy(toRef(request.getRequesterUserId()))
                .reason(request.getReason())
                .status(request.getStatus())
                .previous(previousValues(plan))
                .proposed(proposedValues(plan))
                .shares(shares)
                .balanceEffects(balanceEffects)
                .approvals(approvals)
                .createdAt(request.getCreatedAt())
                .closedAt(request.getClosedAt())
                .build();
    }

    private CorrectionPlan toPlan(CorrectionRequestEntity r, List<CorrectionParticipantEntity> participants) {
        java.util.Map<String, BigDecimal> previousSplits = new java.util.LinkedHashMap<>();
        java.util.Map<String, BigDecimal> proposedSplits = new java.util.LinkedHashMap<>();
        for (CorrectionParticipantEntity cp : participants) {
            if (cp.getPreviousAmount() != null) {
                previousSplits.put(cp.getUserId(), cp.getPreviousAmount());
            }
            if (cp.getProposedAmount() != null) {
                proposedSplits.put(cp.getUserId(), cp.getProposedAmount());
            }
        }
        Set<String> affected = new LinkedHashSet<>();
        for (CorrectionParticipantEntity cp : participants) {
            if (cp.getApprovalStatus() != null) {
                affected.add(cp.getUserId());
            }
        }
        boolean cancellation = "cancellation".equals(r.getRequestType());
        return new CorrectionPlan(
                r.getPreviousTitle(), r.getProposedTitle() != null ? r.getProposedTitle() : r.getPreviousTitle(),
                r.getPreviousTotalAmount(),
                r.getProposedTotalAmount() != null ? r.getProposedTotalAmount() : r.getPreviousTotalAmount(),
                r.getPreviousOccurredAt(),
                r.getProposedOccurredAt() != null ? r.getProposedOccurredAt() : r.getPreviousOccurredAt(),
                r.getPreviousCategory(),
                r.getProposedCategory() != null ? r.getProposedCategory() : r.getPreviousCategory(),
                r.getPreviousPayerUserId(),
                r.getProposedPayerUserId() != null ? r.getProposedPayerUserId() : r.getPreviousPayerUserId(),
                r.getPreviousSplitMethod(),
                r.getProposedSplitMethod() != null ? r.getProposedSplitMethod() : r.getPreviousSplitMethod(),
                previousSplits, proposedSplits, affected, cancellation, true);
    }

    private ExpenseValuesRes previousValues(CorrectionPlan plan) {
        return ExpenseValuesRes.builder()
                .title(plan.previousTitle())
                .totalAmount(plan.previousTotalAmount())
                .occurredAt(plan.previousOccurredAt())
                .category(plan.previousCategory())
                .payer(toRef(plan.previousPayerUserId()))
                .splitMethod(plan.previousSplitMethod())
                .build();
    }

    private ExpenseValuesRes proposedValues(CorrectionPlan plan) {
        return ExpenseValuesRes.builder()
                .title(plan.proposedTitle())
                .totalAmount(plan.proposedTotalAmount())
                .occurredAt(plan.proposedOccurredAt())
                .category(plan.proposedCategory())
                .payer(toRef(plan.proposedPayerUserId()))
                .splitMethod(plan.proposedSplitMethod())
                .build();
    }

    private List<ShareRes> buildShares(CorrectionPlan plan) {
        Set<String> allUserIds = new LinkedHashSet<>();
        allUserIds.addAll(plan.previousSplits().keySet());
        allUserIds.addAll(plan.proposedSplits().keySet());
        return allUserIds.stream()
                .map(u -> ShareRes.builder()
                        .user(toRef(u))
                        .previousAmount(plan.previousSplits().get(u))
                        .proposedAmount(plan.proposedSplits().get(u))
                        .build())
                .toList();
    }

    private List<CorrectionBalanceEffectRes> buildBalanceEffects(String groupId, CorrectionPlan plan) {
        if (groupId == null) {
            return List.of();
        }
        Set<String> involved = new LinkedHashSet<>(plan.affectedUserIds());
        involved.add(plan.previousPayerUserId());
        involved.add(plan.proposedPayerUserId());

        return involved.stream()
                .map(u -> {
                    BigDecimal current = balanceCalculatorService.balanceOf(groupId, u);
                    BigDecimal payerDelta = (u.equals(plan.proposedPayerUserId()) ? plan.proposedTotalAmount()
                            : BigDecimal.ZERO)
                            .subtract(u.equals(plan.previousPayerUserId()) ? plan.previousTotalAmount()
                                    : BigDecimal.ZERO);
                    BigDecimal shareDelta = plan.previousSplits().getOrDefault(u, BigDecimal.ZERO)
                            .subtract(plan.proposedSplits().getOrDefault(u, BigDecimal.ZERO));
                    BigDecimal projected = current.add(payerDelta).add(shareDelta);
                    return CorrectionBalanceEffectRes.builder()
                            .user(toRef(u))
                            .currentBalance(current)
                            .projectedBalance(projected)
                            .build();
                })
                .toList();
    }

}
