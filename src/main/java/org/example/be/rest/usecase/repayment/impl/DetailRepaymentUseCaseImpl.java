package org.example.be.rest.usecase.repayment.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.repayment.RepaymentRes;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.repayment.DetailRepaymentUseCase;
import org.example.be.rest.usecase.repayment.support.RepaymentResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DetailRepaymentUseCaseImpl implements DetailRepaymentUseCase {

    RepaymentJpaRepository repaymentJpaRepository;
    GroupAccessService groupAccessService;
    RepaymentResAssembler repaymentResAssembler;

    @Override
    @Transactional(readOnly = true)
    public RepaymentRes execute(String userId, String groupId, String repaymentId) {
        groupAccessService.requireAccess(groupId, userId);
        RepaymentEntity repayment = repaymentJpaRepository.findByIdAndGroupId(repaymentId, groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        return repaymentResAssembler.toRes(repayment);
    }

}
