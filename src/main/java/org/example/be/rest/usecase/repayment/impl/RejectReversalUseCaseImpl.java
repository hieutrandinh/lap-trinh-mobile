package org.example.be.rest.usecase.repayment.impl;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentReversalEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.res.repayment.ReversalRes;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.repository.RepaymentReversalJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.repayment.RejectReversalUseCase;
import org.example.be.rest.usecase.repayment.support.RepaymentResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RejectReversalUseCaseImpl implements RejectReversalUseCase {

    RepaymentReversalJpaRepository repaymentReversalJpaRepository;
    RepaymentJpaRepository repaymentJpaRepository;
    GroupAccessService groupAccessService;
    RepaymentResAssembler repaymentResAssembler;

    @Override
    @Transactional
    public ReversalRes execute(String userId, String groupId, String reversalId, NoteReq req) {
        groupAccessService.requireAccess(groupId, userId);
        RepaymentReversalEntity reversal = repaymentReversalJpaRepository.findById(reversalId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        repaymentJpaRepository.findByIdAndGroupId(reversal.getRepaymentId(), groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (!reversal.getApproverUserId().equals(userId)) {
            throw new BusinessLogicException(ErrorCode.NOT_REVERSAL_APPROVER);
        }
        if (!"pending".equals(reversal.getStatus())) {
            throw new BusinessLogicException(ErrorCode.INVALID_REVERSAL_STATE);
        }

        reversal.setStatus("rejected");
        reversal.setRejectionNote(req.getNote());
        reversal.setRespondedAt(LocalDateTime.now());
        repaymentReversalJpaRepository.save(reversal);

        return repaymentResAssembler.toReversalRes(reversal);
    }

}
