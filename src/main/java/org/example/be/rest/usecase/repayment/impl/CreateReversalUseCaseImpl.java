package org.example.be.rest.usecase.repayment.impl;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.entity.RepaymentReversalEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.repayment.CreateReversalReq;
import org.example.be.rest.dto.res.repayment.ReversalRes;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.repository.RepaymentReversalJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.example.be.rest.usecase.repayment.CreateReversalUseCase;
import org.example.be.rest.usecase.repayment.support.RepaymentResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateReversalUseCaseImpl implements CreateReversalUseCase {

    RepaymentJpaRepository repaymentJpaRepository;
    RepaymentReversalJpaRepository repaymentReversalJpaRepository;
    GroupMemberJpaRepository groupMemberJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    RepaymentResAssembler repaymentResAssembler;

    @Override
    @Transactional
    public ReversalRes execute(String userId, String groupId, String repaymentId, CreateReversalReq req) {
        groupAccessService.requireAccess(groupId, userId);
        RepaymentEntity repayment = repaymentJpaRepository.findByIdAndGroupId(repaymentId, groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));

        if (!userId.equals(repayment.getFromUserId()) && !userId.equals(repayment.getToUserId())) {
            throw new BusinessLogicException(ErrorCode.FORBIDDEN);
        }
        if (!"confirmed".equals(repayment.getStatus())) {
            throw new BusinessLogicException(ErrorCode.INVALID_REPAYMENT_STATE,
                    java.util.Map.of("currentStatus", repayment.getStatus()));
        }
        if (repaymentReversalJpaRepository.findByRepaymentIdAndStatus(repaymentId, "pending").isPresent()) {
            throw new BusinessLogicException(ErrorCode.REVERSAL_ALREADY_PENDING);
        }
        if (!isActiveMember(groupId, repayment.getFromUserId()) || !isActiveMember(groupId, repayment.getToUserId())) {
            throw new BusinessLogicException(ErrorCode.FORMER_MEMBER_INVOLVED);
        }

        String approverUserId = userId.equals(repayment.getFromUserId()) ? repayment.getToUserId()
                : repayment.getFromUserId();

        LocalDateTime now = LocalDateTime.now();
        RepaymentReversalEntity reversal = new RepaymentReversalEntity();
        reversal.setId(UUID.randomUUID().toString());
        reversal.setReason(req.getReason().trim());
        reversal.setStatus("pending");
        reversal.setCreatedAt(now);
        reversal.setRepaymentId(repaymentId);
        reversal.setRequesterUserId(userId);
        reversal.setApproverUserId(approverUserId);
        repaymentReversalJpaRepository.save(reversal);

        notificationService.notifyGroup(approverUserId, "reversal_requested",
                "{\"groupId\":\"" + groupId + "\",\"repaymentId\":\"" + repaymentId + "\",\"reversalId\":\""
                        + reversal.getId() + "\"}");

        return repaymentResAssembler.toReversalRes(reversal);
    }

    private boolean isActiveMember(String groupId, String userId) {
        return groupMemberJpaRepository.findByGroupIdAndUserId(groupId, userId)
                .map(m -> "active".equals(m.getStatus()))
                .orElse(false);
    }

}
