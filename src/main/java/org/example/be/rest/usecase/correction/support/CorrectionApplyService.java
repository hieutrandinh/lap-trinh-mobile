package org.example.be.rest.usecase.correction.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.CorrectionParticipantEntity;
import org.example.be.core.entity.CorrectionRequestEntity;
import org.example.be.core.entity.DeletionMarkerEntity;
import org.example.be.core.entity.EntryEntity;
import org.example.be.core.entity.ExpenseSplitEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.rest.repository.DeletionMarkerJpaRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.ExpenseSplitJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.entry.support.EntryBucketAssignmentService;
import org.example.be.rest.usecase.expense.support.PersonalCategoryResolver;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.springframework.stereotype.Service;

/**
 * Runs when a correction request gets its last required approval (11.4), whether that
 * happens immediately on create (the requester was the only affected participant) or
 * later via the approve endpoint.
 *
 * Cancellation deliberately leaves tblExpenseSplit rows untouched (only the personal
 * shared entries are deleted): the DB's deferred trgSplitTotal/trgExpenseTotal triggers
 * re-check sum(splits) = totalAmount at commit, and since totalAmount isn't changed for
 * a cancellation, deleting the splits would make that sum mismatch and abort the
 * transaction. A real correction instead rebuilds the whole split set to match the new
 * totalAmount, which keeps the trigger's invariant satisfied by construction.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CorrectionApplyService {

    private static final long DELETION_MARKER_TTL_DAYS = 30;

    GroupExpenseJpaRepository groupExpenseJpaRepository;
    ExpenseSplitJpaRepository expenseSplitJpaRepository;
    EntryJpaRepository entryJpaRepository;
    DeletionMarkerJpaRepository deletionMarkerJpaRepository;
    NotificationService notificationService;
    PersonalCategoryResolver personalCategoryResolver;
    EntryBucketAssignmentService entryBucketAssignmentService;
    GroupActivityLogService groupActivityLogService;

    public void apply(CorrectionRequestEntity request, List<CorrectionParticipantEntity> participants,
                       GroupExpenseEntity expense) {
        LocalDateTime now = LocalDateTime.now();
        boolean cancellation = "cancellation".equals(request.getRequestType());

        if (cancellation) {
            expense.setStatus("cancelled");
            for (CorrectionParticipantEntity cp : participants) {
                if (cp.getPreviousAmount() != null) {
                    deleteSharedEntry(expense.getId(), cp.getUserId());
                }
            }
        } else {
            applyFieldChanges(request, expense);
            for (CorrectionParticipantEntity cp : participants) {
                applyParticipantChange(expense, cp, now);
            }
            expense.setVersion(expense.getVersion() + 1);
        }
        expense.setUpdatedAt(now);
        groupExpenseJpaRepository.save(expense);

        request.setStatus("completed");
        request.setClosedAt(now);

        for (CorrectionParticipantEntity cp : participants) {
            notify(cp.getUserId(), "correction_completed", expense.getGroupId(), expense.getId(), request.getId(),
                    now);
        }
        groupActivityLogService.log(expense.getGroupId(), "correction_completed", "expense", expense.getId(), "{}",
                request.getRequesterUserId());
    }

    private void applyFieldChanges(CorrectionRequestEntity request, GroupExpenseEntity expense) {
        if (request.getProposedTitle() != null) {
            expense.setTitle(request.getProposedTitle());
        }
        if (request.getProposedTotalAmount() != null) {
            expense.setTotalAmount(request.getProposedTotalAmount());
        }
        if (request.getProposedOccurredAt() != null) {
            expense.setOccurredAt(request.getProposedOccurredAt());
        }
        if (request.getProposedCategory() != null) {
            expense.setCategory(request.getProposedCategory());
        }
        if (request.getProposedPayerUserId() != null) {
            expense.setPayerUserId(request.getProposedPayerUserId());
        }
        if (request.getProposedSplitMethod() != null) {
            expense.setSplitMethod(request.getProposedSplitMethod());
        }
    }

    private void applyParticipantChange(GroupExpenseEntity expense, CorrectionParticipantEntity cp,
                                         LocalDateTime now) {
        boolean hadBefore = cp.getPreviousAmount() != null;
        boolean hasAfter = cp.getProposedAmount() != null;

        if (hadBefore && !hasAfter) {
            expenseSplitJpaRepository.findByGroupExpenseIdAndUserId(expense.getId(), cp.getUserId())
                    .ifPresent(expenseSplitJpaRepository::delete);
            deleteSharedEntry(expense.getId(), cp.getUserId());
            return;
        }
        if (!hadBefore && hasAfter) {
            ExpenseSplitEntity split = new ExpenseSplitEntity();
            split.setId(UUID.randomUUID().toString());
            split.setAmount(cp.getProposedAmount());
            split.setRoundingAdjustment(BigDecimal.ZERO.setScale(2));
            split.setGroupExpenseId(expense.getId());
            split.setUserId(cp.getUserId());
            expenseSplitJpaRepository.save(split);

            CategoryEntity category = personalCategoryResolver.resolve(cp.getUserId(), expense.getCategory());
            EntryEntity entry = new EntryEntity();
            entry.setId(UUID.randomUUID().toString());
            entry.setVersion(1);
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            entry.setUserId(cp.getUserId());
            entry.setCategoryId(category.getId());
            entry.setBucketId(entryBucketAssignmentService.resolveBucketIdSilently(category.getId()));
            entry.setExpenseSplitId(split.getId());
            entryJpaRepository.save(entry);
            return;
        }
        if (hadBefore && hasAfter && cp.getPreviousAmount().compareTo(cp.getProposedAmount()) != 0) {
            expenseSplitJpaRepository.findByGroupExpenseIdAndUserId(expense.getId(), cp.getUserId())
                    .ifPresent(split -> {
                        split.setAmount(cp.getProposedAmount());
                        expenseSplitJpaRepository.save(split);
                        // Keep the personal category the participant already set (11.4 rule); just bump version.
                        entryJpaRepository.findByExpenseSplitId(split.getId()).ifPresent(entry -> {
                            entry.setVersion(entry.getVersion() + 1);
                            entry.setUpdatedAt(now);
                            entryJpaRepository.save(entry);
                        });
                    });
        }
    }

    private void deleteSharedEntry(String expenseId, String userId) {
        expenseSplitJpaRepository.findByGroupExpenseIdAndUserId(expenseId, userId)
                .flatMap(split -> entryJpaRepository.findByExpenseSplitId(split.getId()))
                .ifPresent(entry -> {
                    entryJpaRepository.delete(entry);
                    DeletionMarkerEntity marker = new DeletionMarkerEntity();
                    marker.setId(UUID.randomUUID().toString());
                    marker.setRecordType("entry");
                    marker.setRecordId(entry.getId());
                    marker.setDeletedAt(LocalDateTime.now());
                    marker.setExpiresAt(LocalDateTime.now().plusDays(DELETION_MARKER_TTL_DAYS));
                    marker.setUserId(userId);
                    deletionMarkerJpaRepository.save(marker);
                });
    }

    private void notify(String userId, String type, String groupId, String expenseId, String correctionId,
                         LocalDateTime now) {
        notificationService.notifyGroup(userId, type, "{\"groupId\":\"" + groupId + "\",\"expenseId\":\"" + expenseId
                + "\",\"correctionId\":\"" + correctionId + "\"}");
    }

}
