package org.example.be.rest.usecase.repayment.impl;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.entity.RepaymentReversalEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.repayment.ReversalRes;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.repository.RepaymentReversalJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.repayment.ApproveReversalUseCase;
import org.example.be.rest.usecase.repayment.support.RepaymentResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approving restores balances to before the confirmation simply by flipping the repayment
 * to 'reversed' - vGroupBalance only counts status='confirmed' repayments.
 * No notification is sent here: 12.2's notification type list has reversal_requested but
 * no reversal_approved/rejected, so the outcome isn't pushed as its own notification.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApproveReversalUseCaseImpl implements ApproveReversalUseCase {

    RepaymentReversalJpaRepository repaymentReversalJpaRepository;
    RepaymentJpaRepository repaymentJpaRepository;
    GroupAccessService groupAccessService;
    RepaymentResAssembler repaymentResAssembler;

    @Override
    @Transactional
    public ReversalRes execute(String userId, String groupId, String reversalId) {
        groupAccessService.requireAccess(groupId, userId);
        RepaymentReversalEntity reversal = repaymentReversalJpaRepository.findById(reversalId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        RepaymentEntity repayment = repaymentJpaRepository.findByIdAndGroupId(reversal.getRepaymentId(), groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (!reversal.getApproverUserId().equals(userId)) {
            throw new BusinessLogicException(ErrorCode.NOT_REVERSAL_APPROVER);
        }
        if (!"pending".equals(reversal.getStatus())) {
            throw new BusinessLogicException(ErrorCode.INVALID_REVERSAL_STATE);
        }

        LocalDateTime now = LocalDateTime.now();
        reversal.setStatus("approved");
        reversal.setRespondedAt(now);
        repaymentReversalJpaRepository.save(reversal);

        repayment.setStatus("reversed");
        repaymentJpaRepository.save(repayment);

        return repaymentResAssembler.toReversalRes(reversal);
    }

}
