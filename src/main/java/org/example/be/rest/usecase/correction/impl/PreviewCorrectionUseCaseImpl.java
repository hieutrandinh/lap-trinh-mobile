package org.example.be.rest.usecase.correction.impl;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.correction.CreateCorrectionReq;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.repository.CorrectionRequestJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.correction.PreviewCorrectionUseCase;
import org.example.be.rest.usecase.correction.support.CorrectionPlan;
import org.example.be.rest.usecase.correction.support.CorrectionPlanner;
import org.example.be.rest.usecase.correction.support.CorrectionResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PreviewCorrectionUseCaseImpl implements PreviewCorrectionUseCase {

    GroupExpenseJpaRepository groupExpenseJpaRepository;
    CorrectionRequestJpaRepository correctionRequestJpaRepository;
    GroupAccessService groupAccessService;
    CorrectionPlanner correctionPlanner;
    CorrectionResAssembler correctionResAssembler;

    @Override
    @Transactional(readOnly = true)
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

        CorrectionPlan plan = correctionPlanner.plan(expense, req);
        return correctionResAssembler.fromPlan(expense, groupId, userId, req.getType(), req.getReason(), plan,
                null, "preview", null);
    }

}
