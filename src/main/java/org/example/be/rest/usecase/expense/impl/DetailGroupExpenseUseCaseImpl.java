package org.example.be.rest.usecase.expense.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.EntryEntity;
import org.example.be.core.entity.ExpenseSplitEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.category.CategoryRefRes;
import org.example.be.rest.dto.res.expense.GroupExpenseRes;
import org.example.be.rest.dto.res.expense.MyEntryRes;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.ExpenseSplitJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.expense.DetailGroupExpenseUseCase;
import org.example.be.rest.usecase.expense.support.ExpenseResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DetailGroupExpenseUseCaseImpl implements DetailGroupExpenseUseCase {

    GroupExpenseJpaRepository groupExpenseJpaRepository;
    ExpenseSplitJpaRepository expenseSplitJpaRepository;
    EntryJpaRepository entryJpaRepository;
    CategoryJpaRepository categoryJpaRepository;
    GroupAccessService groupAccessService;
    ExpenseResAssembler expenseResAssembler;
    GroupSnapshot groupSnapshot;

    @Override
    @Transactional(readOnly = true)
    public GroupExpenseRes execute(String userId, String groupId, String expenseId) {
        var access = groupAccessService.requireAccess(groupId, userId);
        if (groupSnapshot.appliesTo(access)) {
            GroupExpenseRes snapshot = groupSnapshot.read(access).expenses().stream()
                    .filter(expense -> expenseId.equals(expense.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
            snapshot.setAccessMode(access.accessMode());
            return snapshot;
        }
        GroupExpenseEntity expense = groupExpenseJpaRepository.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));

        var splits = expenseSplitJpaRepository.findByGroupExpenseId(expenseId);
        GroupExpenseRes res = expenseResAssembler.toRes(expense, splits, userId);
        res.setAccessMode(access.accessMode());
        res.setCorrections(java.util.List.of());
        res.setRelatedRepayments(java.util.List.of());
        res.setMyEntry(buildMyEntry(expenseId, userId));
        return res;
    }

    private MyEntryRes buildMyEntry(String expenseId, String userId) {
        ExpenseSplitEntity mySplit = expenseSplitJpaRepository.findByGroupExpenseIdAndUserId(expenseId, userId)
                .orElse(null);
        if (mySplit == null) {
            return null;
        }
        EntryEntity entry = entryJpaRepository.findByExpenseSplitId(mySplit.getId()).orElse(null);
        if (entry == null) {
            return null;
        }
        CategoryEntity category = categoryJpaRepository.findById(entry.getCategoryId()).orElse(null);
        CategoryRefRes categoryRef = category == null ? null
                : CategoryRefRes.builder().id(category.getId()).name(category.getName()).build();
        return MyEntryRes.builder().entryId(entry.getId()).category(categoryRef).version(entry.getVersion()).build();
    }

}
