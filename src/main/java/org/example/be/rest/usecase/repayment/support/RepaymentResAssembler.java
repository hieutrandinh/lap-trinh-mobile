package org.example.be.rest.usecase.repayment.support;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.entity.RepaymentReversalEntity;
import org.example.be.core.entity.UserEntity;
import org.example.be.rest.dto.res.group.UserRefRes;
import org.example.be.rest.dto.res.repayment.RepaymentRes;
import org.example.be.rest.dto.res.repayment.ReversalRes;
import org.example.be.rest.repository.RepaymentReversalJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RepaymentResAssembler {

    UserJpaRepository userJpaRepository;
    RepaymentReversalJpaRepository repaymentReversalJpaRepository;

    public UserRefRes toRef(String userId) {
        return userJpaRepository.findById(userId)
                .map(u -> UserRefRes.builder().userId(u.getId()).displayName(u.getDisplayName()).build())
                .orElse(UserRefRes.builder().userId(userId).displayName(null).build());
    }

    public RepaymentRes toRes(RepaymentEntity repayment) {
        ReversalRes pendingReversal = repaymentReversalJpaRepository
                .findByRepaymentIdAndStatus(repayment.getId(), "pending")
                .map(this::toReversalRes)
                .orElse(null);

        return RepaymentRes.builder()
                .id(repayment.getId())
                .groupId(repayment.getGroupId())
                .from(toRef(repayment.getFromUserId()))
                .to(toRef(repayment.getToUserId()))
                .amount(repayment.getAmount())
                .currency("USD")
                .paidAt(repayment.getPaidAt())
                .note(repayment.getNote())
                .status(repayment.getStatus())
                .respondedAt(repayment.getRespondedAt())
                .pendingReversal(pendingReversal)
                .createdAt(repayment.getCreatedAt())
                .build();
    }

    public ReversalRes toReversalRes(RepaymentReversalEntity reversal) {
        return ReversalRes.builder()
                .id(reversal.getId())
                .repaymentId(reversal.getRepaymentId())
                .reason(reversal.getReason())
                .status(reversal.getStatus())
                .rejectionNote(reversal.getRejectionNote())
                .requester(toRef(reversal.getRequesterUserId()))
                .approver(toRef(reversal.getApproverUserId()))
                .respondedAt(reversal.getRespondedAt())
                .createdAt(reversal.getCreatedAt())
                .build();
    }

}
