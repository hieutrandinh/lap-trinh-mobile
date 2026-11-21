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
import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.repository.CorrectionParticipantJpaRepository;
import org.example.be.rest.repository.CorrectionRequestJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.correction.RejectCorrectionUseCase;
import org.example.be.rest.usecase.correction.support.CorrectionResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RejectCorrectionUseCaseImpl implements RejectCorrectionUseCase {

    CorrectionRequestJpaRepository correctionRequestJpaRepository;
    CorrectionParticipantJpaRepository correctionParticipantJpaRepository;
    GroupExpenseJpaRepository groupExpenseJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    GroupActivityLogService groupActivityLogService;
    CorrectionResAssembler correctionResAssembler;

    @Override
    @Transactional
    public CorrectionRequestRes execute(String userId, String groupId, String correctionId, NoteReq req) {
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
        mine.setApprovalStatus("rejected");
        mine.setRejectionNote(req.getNote());
        mine.setRespondedAt(now);
        correctionParticipantJpaRepository.save(mine);

        for (CorrectionParticipantEntity cp : participants) {
            if (cp != mine && "pending".equals(cp.getApprovalStatus())) {
                cp.setApprovalStatus("no_response");
                correctionParticipantJpaRepository.save(cp);
            }
        }

        request.setStatus("rejected");
        request.setClosedAt(now);
        correctionRequestJpaRepository.save(request);

        for (CorrectionParticipantEntity cp : participants) {
            notify(cp.getUserId(), expense, request, now);
        }
        groupActivityLogService.log(groupId, "correction_rejected", "expense", expense.getId(), "{}", userId);

        return correctionResAssembler.fromEntity(groupId, request, participants);
    }

    private void notify(String userId, GroupExpenseEntity expense, CorrectionRequestEntity request,
                         LocalDateTime now) {
        notificationService.notifyGroup(userId, "correction_rejected",
                "{\"groupId\":\"" + expense.getGroupId() + "\",\"expenseId\":\"" + expense.getId()
                        + "\",\"correctionId\":\"" + request.getId() + "\"}");
    }

}
