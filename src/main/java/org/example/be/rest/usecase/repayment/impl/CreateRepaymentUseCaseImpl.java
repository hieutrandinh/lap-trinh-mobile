package org.example.be.rest.usecase.repayment.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.repayment.CreateRepaymentReq;
import org.example.be.rest.dto.res.repayment.RepaymentRes;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.example.be.rest.usecase.repayment.CreateRepaymentUseCase;
import org.example.be.rest.usecase.repayment.support.BalanceCalculatorService;
import org.example.be.rest.usecase.repayment.support.RepaymentResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateRepaymentUseCaseImpl implements CreateRepaymentUseCase {

    RepaymentJpaRepository repaymentJpaRepository;
    GroupMemberJpaRepository groupMemberJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    BalanceCalculatorService balanceCalculatorService;
    RepaymentResAssembler repaymentResAssembler;

    @Override
    @Transactional
    public RepaymentRes execute(String userId, String groupId, CreateRepaymentReq req) {
        var access = groupAccessService.requireAccess(groupId, userId);
        groupAccessService.requireWritable(access.accessMode());

        if (req.getToUserId().equals(userId)) {
            throw new BusinessLogicException(ErrorCode.CANNOT_REPAY_SELF);
        }
        boolean toActive = groupMemberJpaRepository.findByGroupIdAndUserId(groupId, req.getToUserId())
                .map(m -> "active".equals(m.getStatus()))
                .orElse(false);
        if (!toActive) {
            throw new BusinessLogicException(ErrorCode.NOT_ACTIVE_MEMBER);
        }

        BigDecimal maxAmount = balanceCalculatorService.remainingDebt(groupId, userId);
        if (req.getAmount().compareTo(maxAmount) > 0) {
            throw new BusinessLogicException(ErrorCode.REPAYMENT_EXCEEDS_DEBT, Map.of("maxAmount", maxAmount));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paidAt = req.getPaidAt() != null
                ? req.getPaidAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                : now;
        if (paidAt.isAfter(now)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        RepaymentEntity repayment = new RepaymentEntity();
        repayment.setId(UUID.randomUUID().toString());
        repayment.setAmount(req.getAmount());
        repayment.setPaidAt(paidAt);
        repayment.setNote(req.getNote());
        repayment.setStatus("pending");
        repayment.setCreatedAt(now);
        repayment.setGroupId(groupId);
        repayment.setFromUserId(userId);
        repayment.setToUserId(req.getToUserId());
        repaymentJpaRepository.save(repayment);

        notificationService.notifyGroup(req.getToUserId(), "repayment_pending",
                "{\"groupId\":\"" + groupId + "\",\"repaymentId\":\"" + repayment.getId() + "\"}");

        return repaymentResAssembler.toRes(repayment);
    }

}
