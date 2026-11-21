package org.example.be.rest.usecase.correction.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CorrectionParticipantEntity;
import org.example.be.core.entity.CorrectionRequestEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.repository.CorrectionParticipantJpaRepository;
import org.example.be.rest.repository.CorrectionRequestJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.correction.WithdrawCorrectionUseCase;
import org.example.be.rest.usecase.correction.support.CorrectionResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WithdrawCorrectionUseCaseImpl implements WithdrawCorrectionUseCase {

    CorrectionRequestJpaRepository correctionRequestJpaRepository;
    CorrectionParticipantJpaRepository correctionParticipantJpaRepository;
    GroupExpenseJpaRepository groupExpenseJpaRepository;
    GroupAccessService groupAccessService;
    CorrectionResAssembler correctionResAssembler;

    @Override
    @Transactional
    public CorrectionRequestRes execute(String userId, String groupId, String correctionId) {
        groupAccessService.requireAccess(groupId, userId);
        CorrectionRequestEntity request = correctionRequestJpaRepository.findById(correctionId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        GroupExpenseEntity expense = groupExpenseJpaRepository
                .findByIdAndGroupId(request.getGroupExpenseId(), groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));

        if (!expense.getCreatorUserId().equals(userId)) {
            throw new BusinessLogicException(ErrorCode.NOT_EXPENSE_CREATOR);
        }
        if (!"pending".equals(request.getStatus())) {
            throw new BusinessLogicException(ErrorCode.CORRECTION_NOT_PENDING,
                    Map.of("currentStatus", request.getStatus()));
        }

        List<CorrectionParticipantEntity> participants = correctionParticipantJpaRepository
                .findByCorrectionRequestId(correctionId);
        for (CorrectionParticipantEntity cp : participants) {
            if ("pending".equals(cp.getApprovalStatus())) {
                cp.setApprovalStatus("no_response");
                correctionParticipantJpaRepository.save(cp);
            }
        }

        request.setStatus("withdrawn");
        request.setClosedAt(LocalDateTime.now());
        correctionRequestJpaRepository.save(request);

        return correctionResAssembler.fromEntity(groupId, request, participants);
    }

}
