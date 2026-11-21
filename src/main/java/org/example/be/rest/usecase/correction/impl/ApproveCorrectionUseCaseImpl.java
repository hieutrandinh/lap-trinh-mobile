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
import org.example.be.rest.usecase.correction.ApproveCorrectionUseCase;
import org.example.be.rest.usecase.correction.support.CorrectionApplyService;
import org.example.be.rest.usecase.correction.support.CorrectionResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApproveCorrectionUseCaseImpl implements ApproveCorrectionUseCase {

    CorrectionRequestJpaRepository correctionRequestJpaRepository;
    CorrectionParticipantJpaRepository correctionParticipantJpaRepository;
    GroupExpenseJpaRepository groupExpenseJpaRepository;
    GroupAccessService groupAccessService;
    CorrectionApplyService correctionApplyService;
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

        List<CorrectionParticipantEntity> participants = correctionParticipantJpaRepository
                .findByCorrectionRequestId(correctionId);
        CorrectionParticipantEntity mine = participants.stream()
                .filter(p -> p.getUserId().equals(userId) && p.getApprovalStatus() != null)
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_REQUIRED_APPROVER));

        if (!"pending".equals(request.getStatus())) {
            throw new BusinessLogicException(ErrorCode.CORRECTION_NOT_PENDING,
                    Map.of("currentStatus", request.getStatus()));
        }
        if (!"pending".equals(mine.getApprovalStatus())) {
            throw new BusinessLogicException(ErrorCode.ALREADY_RESPONDED);
        }

        LocalDateTime now = LocalDateTime.now();
        mine.setApprovalStatus("approved");
        mine.setRespondedAt(now);
        correctionParticipantJpaRepository.save(mine);

        boolean anyPending = participants.stream().anyMatch(p -> "pending".equals(p.getApprovalStatus()));
        if (!anyPending) {
            correctionApplyService.apply(request, participants, expense);
            correctionRequestJpaRepository.save(request);
        }

        return correctionResAssembler.fromEntity(groupId, request, participants);
    }

}
