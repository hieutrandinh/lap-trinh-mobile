package org.example.be.rest.usecase.repayment.impl;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.res.repayment.RepaymentRes;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.example.be.rest.usecase.repayment.RejectRepaymentUseCase;
import org.example.be.rest.usecase.repayment.support.RepaymentResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * tblRepayment has no dedicated rejection-note column, so req.note is only carried in
 * the notification payload - it is not persisted on the repayment itself.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RejectRepaymentUseCaseImpl implements RejectRepaymentUseCase {

    RepaymentJpaRepository repaymentJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    RepaymentResAssembler repaymentResAssembler;

    @Override
    @Transactional
    public RepaymentRes execute(String userId, String groupId, String repaymentId, NoteReq req) {
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
        repayment.setStatus("rejected");
        repayment.setRespondedAt(now);
        repaymentJpaRepository.save(repayment);

        String note = StringUtils.hasText(req.getNote()) ? req.getNote().replace("\"", "'") : null;
        String payload = "{\"groupId\":\"" + groupId + "\",\"repaymentId\":\"" + repaymentId + "\""
                + (note != null ? ",\"note\":\"" + note + "\"" : "") + "}";
        notificationService.notifyGroup(repayment.getFromUserId(), "repayment_rejected", payload);

        return repaymentResAssembler.toRes(repayment);
    }

}
