package org.example.be.rest.usecase.correction.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CorrectionRequestEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.repository.CorrectionParticipantJpaRepository;
import org.example.be.rest.repository.CorrectionRequestJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.correction.DetailCorrectionUseCase;
import org.example.be.rest.usecase.correction.support.CorrectionResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DetailCorrectionUseCaseImpl implements DetailCorrectionUseCase {

    CorrectionRequestJpaRepository correctionRequestJpaRepository;
    CorrectionParticipantJpaRepository correctionParticipantJpaRepository;
    GroupExpenseJpaRepository groupExpenseJpaRepository;
    GroupAccessService groupAccessService;
    CorrectionResAssembler correctionResAssembler;

    @Override
    @Transactional(readOnly = true)
    public CorrectionRequestRes execute(String userId, String groupId, String correctionId) {
        groupAccessService.requireAccess(groupId, userId);
        CorrectionRequestEntity request = correctionRequestJpaRepository.findById(correctionId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        groupExpenseJpaRepository.findByIdAndGroupId(request.getGroupExpenseId(), groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));

        return correctionResAssembler.fromEntity(groupId, request,
                correctionParticipantJpaRepository.findByCorrectionRequestId(correctionId));
    }

}
