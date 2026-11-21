package org.example.be.rest.usecase.correction.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CorrectionParticipantEntity;
import org.example.be.core.entity.CorrectionRequestEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.correction.CreateCorrectionReq;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.repository.CorrectionParticipantJpaRepository;
import org.example.be.rest.repository.CorrectionRequestJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.correction.CreateCorrectionUseCase;
import org.example.be.rest.usecase.correction.support.CorrectionApplyService;
import org.example.be.rest.usecase.correction.support.CorrectionPlan;
import org.example.be.rest.usecase.correction.support.CorrectionPlanner;
import org.example.be.rest.usecase.correction.support.CorrectionResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateCorrectionUseCaseImpl implements CreateCorrectionUseCase {

    GroupExpenseJpaRepository groupExpenseJpaRepository;
    CorrectionRequestJpaRepository correctionRequestJpaRepository;
    CorrectionParticipantJpaRepository correctionParticipantJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    GroupActivityLogService groupActivityLogService;
    CorrectionPlanner correctionPlanner;
    CorrectionApplyService correctionApplyService;
    CorrectionResAssembler correctionResAssembler;

    @Override
    @Transactional
    public CorrectionRequestRes execute(String userId, String groupId, String expenseId, CreateCorrectionReq req) {
        var access = groupAccessService.requireAccess(groupId, userId);
        groupAccessService.requireWritable(access.accessMode());

        GroupExpenseEntity expense = groupExpenseJpaRepository.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (!expense.getCreatorUserId().equals(userId)) {
            throw new BusinessLogicException(ErrorCode.NOT_EXPENSE_CREATOR);
        }
        if ("cancelled".equals(expense.getStatus())) {
            throw new BusinessLogicException(ErrorCode.EXPENSE_CANCELLED);
        }
        if (expense.getVersion() != req.getBaseVersion()) {
            throw new BusinessLogicException(ErrorCode.VERSION_CONFLICT, Map.of("current", expense.getVersion()));
        }
        correctionRequestJpaRepository.findByGroupExpenseIdAndStatus(expenseId, "pending").ifPresent(pending -> {
            throw new BusinessLogicException(ErrorCode.CORRECTION_ALREADY_PENDING,
                    Map.of("correctionId", pending.getId()));
        });

        boolean cancellation = "cancellation".equals(req.getType());
        if (!cancellation && req.getProposed() == null) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        if (cancellation && req.getProposed() != null) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        if (!cancellation && !StringUtils.hasText(req.getReason())) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        CorrectionPlan plan = correctionPlanner.plan(expense, req);

        LocalDateTime now = LocalDateTime.now();
        CorrectionRequestEntity request = new CorrectionRequestEntity();
        request.setId(UUID.randomUUID().toString());
        request.setRequestType(req.getType());
        request.setReason(req.getReason());
        request.setStatus("pending");
        request.setBaseVersion(req.getBaseVersion());
        request.setPreviousTitle(plan.previousTitle());
        request.setPreviousTotalAmount(plan.previousTotalAmount());
        request.setPreviousOccurredAt(plan.previousOccurredAt());
        request.setPreviousCategory(plan.previousCategory());
        request.setPreviousSplitMethod(plan.previousSplitMethod());
        request.setPreviousPayerUserId(plan.previousPayerUserId());
        if (!cancellation) {
            request.setProposedTitle(plan.proposedTitle());
            request.setProposedTotalAmount(plan.proposedTotalAmount());
            request.setProposedOccurredAt(plan.proposedOccurredAt());
            request.setProposedCategory(plan.proposedCategory());
            request.setProposedSplitMethod(plan.proposedSplitMethod());
            request.setProposedPayerUserId(plan.proposedPayerUserId());
        }
        request.setCreatedAt(now);
        request.setGroupExpenseId(expenseId);
        request.setRequesterUserId(userId);
        correctionRequestJpaRepository.save(request);

        List<CorrectionParticipantEntity> participants = buildParticipants(request, plan, userId, now);
        correctionParticipantJpaRepository.saveAll(participants);

        boolean anyPending = participants.stream().anyMatch(p -> "pending".equals(p.getApprovalStatus()));
        if (!anyPending) {
            correctionApplyService.apply(request, participants, expense);
            correctionRequestJpaRepository.save(request);
        } else {
            for (CorrectionParticipantEntity cp : participants) {
                if (cp.getApprovalStatus() != null) {
                    notify(cp.getUserId(), expense, request, now);
                }
            }
            groupActivityLogService.log(groupId, "correction_requested", "expense", expenseId, "{}", userId);
        }

        return correctionResAssembler.fromEntity(groupId, request, participants);
    }

    private List<CorrectionParticipantEntity> buildParticipants(CorrectionRequestEntity request, CorrectionPlan plan,
                                                                  String requesterUserId, LocalDateTime now) {
        Set<String> allUsers = new LinkedHashSet<>();
        allUsers.addAll(plan.previousSplits().keySet());
        allUsers.addAll(plan.proposedSplits().keySet());
        allUsers.addAll(plan.affectedUserIds());

        return allUsers.stream().map(u -> {
            CorrectionParticipantEntity cp = new CorrectionParticipantEntity();
            cp.setId(UUID.randomUUID().toString());
            cp.setPreviousAmount(plan.previousSplits().get(u));
            cp.setProposedAmount(plan.proposedSplits().get(u));
            cp.setCorrectionRequestId(request.getId());
            cp.setUserId(u);
            if (plan.affectedUserIds().contains(u)) {
                boolean isRequester = u.equals(requesterUserId);
                cp.setApprovalStatus(isRequester ? "approved" : "pending");
                cp.setRespondedAt(isRequester ? now : null);
            }
            return cp;
        }).toList();
    }

    private void notify(String userId, GroupExpenseEntity expense, CorrectionRequestEntity request,
                         LocalDateTime now) {
        notificationService.notifyGroup(userId, "correction_requested",
                "{\"groupId\":\"" + expense.getGroupId() + "\",\"expenseId\":\"" + expense.getId()
                        + "\",\"correctionId\":\"" + request.getId() + "\"}");
    }

}
