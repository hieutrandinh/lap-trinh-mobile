package org.example.be.rest.usecase.expense.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.EntryEntity;
import org.example.be.core.entity.ExpenseSplitEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.rest.dto.req.expense.CreateGroupExpenseReq;
import org.example.be.rest.dto.res.expense.GroupExpenseRes;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.ExpenseSplitJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.entry.support.EntryBucketAssignmentService;
import org.example.be.rest.usecase.expense.CreateGroupExpenseUseCase;
import org.example.be.rest.usecase.expense.support.ExpenseMembershipValidator;
import org.example.be.rest.usecase.expense.support.ExpenseResAssembler;
import org.example.be.rest.usecase.expense.support.PersonalCategoryResolver;
import org.example.be.rest.usecase.expense.support.SplitCalculator;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateGroupExpenseUseCaseImpl implements CreateGroupExpenseUseCase {

    GroupExpenseJpaRepository groupExpenseJpaRepository;
    ExpenseSplitJpaRepository expenseSplitJpaRepository;
    EntryJpaRepository entryJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    GroupActivityLogService groupActivityLogService;
    ExpenseMembershipValidator expenseMembershipValidator;
    SplitCalculator splitCalculator;
    PersonalCategoryResolver personalCategoryResolver;
    EntryBucketAssignmentService entryBucketAssignmentService;
    ExpenseResAssembler expenseResAssembler;

    @Override
    @Transactional
    public GroupExpenseRes execute(String userId, String groupId, CreateGroupExpenseReq req) {
        var access = groupAccessService.requireAccess(groupId, userId);
        groupAccessService.requireWritable(access.accessMode());

        expenseMembershipValidator.validate(groupId, req.getPayerUserId(), req.getParticipants());
        var computations = splitCalculator.compute(req.getTotalAmount(), req.getSplitMethod(), req.getParticipants());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime occurredAt = req.getOccurredAt() != null
                ? req.getOccurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                : now;
        String category = req.getCategory().trim();

        GroupExpenseEntity expense = new GroupExpenseEntity();
        expense.setId(UUID.randomUUID().toString());
        expense.setTitle(req.getTitle().trim());
        expense.setTotalAmount(req.getTotalAmount());
        expense.setOccurredAt(occurredAt);
        expense.setCategory(category);
        expense.setSplitMethod(req.getSplitMethod());
        expense.setNote(req.getNote());
        expense.setStatus("active");
        expense.setVersion(1);
        expense.setCreatedAt(now);
        expense.setUpdatedAt(now);
        expense.setGroupId(groupId);
        expense.setPayerUserId(req.getPayerUserId());
        expense.setCreatorUserId(userId);
        groupExpenseJpaRepository.save(expense);

        List<ExpenseSplitEntity> splits = new ArrayList<>();
        for (var c : computations) {
            ExpenseSplitEntity split = new ExpenseSplitEntity();
            split.setId(UUID.randomUUID().toString());
            split.setAmount(c.amount());
            split.setRoundingAdjustment(c.roundingAdjustment());
            split.setGroupExpenseId(expense.getId());
            split.setUserId(c.userId());
            expenseSplitJpaRepository.save(split);
            splits.add(split);

            CategoryEntity personalCategory = personalCategoryResolver.resolve(c.userId(), category);
            EntryEntity entry = new EntryEntity();
            entry.setId(UUID.randomUUID().toString());
            entry.setVersion(1);
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            entry.setUserId(c.userId());
            entry.setCategoryId(personalCategory.getId());
            entry.setBucketId(entryBucketAssignmentService.resolveBucketIdSilently(personalCategory.getId()));
            entry.setExpenseSplitId(split.getId());
            entryJpaRepository.save(entry);
        }

        groupActivityLogService.log(groupId, "expense_created", "expense", expense.getId(), "{}", userId);
        notifyParticipantsAndPayer(expense, computations.stream().map(SplitCalculator.SplitComputation::userId).toList(),
                now);

        return expenseResAssembler.toRes(expense, splits, userId);
    }

    private void notifyParticipantsAndPayer(GroupExpenseEntity expense, List<String> participantIds,
                                             LocalDateTime now) {
        java.util.Set<String> recipients = new java.util.LinkedHashSet<>(participantIds);
        recipients.add(expense.getPayerUserId());
        String payload = "{\"groupId\":\"" + expense.getGroupId() + "\",\"expenseId\":\"" + expense.getId() + "\"}";
        for (String recipient : recipients) {
            notificationService.notifyGroup(recipient, "expense_added", payload);
        }
    }

}
