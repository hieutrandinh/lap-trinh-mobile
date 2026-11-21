package org.example.be.rest.usecase.repayment.impl;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.repayment.RepaymentRes;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.example.be.rest.usecase.repayment.ConfirmRepaymentUseCase;
import org.example.be.rest.usecase.repayment.support.RepaymentResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfirmRepaymentUseCaseImpl implements ConfirmRepaymentUseCase {

    RepaymentJpaRepository repaymentJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    GroupActivityLogService groupActivityLogService;
    RepaymentResAssembler repaymentResAssembler;

    @Override
    @Transactional
    public RepaymentRes execute(String userId, String groupId, String repaymentId) {
        groupAccessService.requireAccess(groupId, userId);
        RepaymentEntity repayment = repaymentJpaRepository.findByIdAndGroupId(repaymentId, groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (!repayment.getToUserId().equals(userId)) {
            throw new BusinessLogicException(ErrorCode.NOT_REPAYMENT_RECEIVER);
        }
        if (!"pending".equals(repayment.getStatus())) {
            throw new BusinessLogicException(ErrorCode.INVALID_REPAYMENT_STATE,
                    Map.of("currentStatus", repayment.getStatus()));
        }

        LocalDateTime now = LocalDateTime.now();
        repayment.setStatus("confirmed");
        repayment.setRespondedAt(now);
        repaymentJpaRepository.save(repayment);

        notificationService.notifyGroup(repayment.getFromUserId(), "repayment_confirmed",
                "{\"groupId\":\"" + groupId + "\",\"repaymentId\":\"" + repaymentId + "\"}");
        groupActivityLogService.log(groupId, "repayment_confirmed", "repayment", repaymentId, "{}", userId);

        return repaymentResAssembler.toRes(repayment);
    }

}
