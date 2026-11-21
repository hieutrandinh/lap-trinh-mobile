package org.example.be.rest.usecase.expense.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.ExpenseSplitEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.expense.GroupExpenseSearchReq;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.dto.res.expense.GroupExpenseListRes;
import org.example.be.rest.repository.ExpenseSplitJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.expense.SearchGroupExpenseUseCase;
import org.example.be.rest.usecase.expense.support.ExpenseResAssembler;
import org.example.be.rest.usecase.expense.support.ExpenseSpecifications;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * accessMode = snapshot should only show what was in the leave-time snapshot (9.3),
 * but 8.10 doesn't capture expenses yet (see LeaveGroupUseCaseImpl), so this always
 * reads live data regardless of accessMode.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchGroupExpenseUseCaseImpl implements SearchGroupExpenseUseCase {

    private static final Set<String> ALLOWED_SORTS = Set.of("occurredAt", "-occurredAt", "totalAmount",
            "-totalAmount");

    GroupExpenseJpaRepository groupExpenseJpaRepository;
    ExpenseSplitJpaRepository expenseSplitJpaRepository;
    GroupAccessService groupAccessService;
    ExpenseResAssembler expenseResAssembler;

    @Override
    @Transactional(readOnly = true)
    public GroupExpenseListRes execute(String userId, String groupId, GroupExpenseSearchReq req) {
        groupAccessService.requireAccess(groupId, userId);

        String sort = StringUtils.hasText(req.getSort()) ? req.getSort() : "-occurredAt";
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        LocalDateTime from = req.getFrom() != null
                ? req.getFrom().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime() : null;
        LocalDateTime to = req.getTo() != null
                ? req.getTo().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime() : null;
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        List<String> idsIn = null;
        if (Boolean.TRUE.equals(req.getHasPendingCorrection())) {
            idsIn = List.of(); // no correction module yet: pendingCorrectionId is always null
        } else if (StringUtils.hasText(req.getParticipantUserId())) {
            idsIn = expenseSplitJpaRepository.findByUserId(req.getParticipantUserId()).stream()
                    .map(ExpenseSplitEntity::getGroupExpenseId)
                    .toList();
        }

        Specification<GroupExpenseEntity> spec = ExpenseSpecifications.build(groupId, req.getCategory(),
                req.getPayerUserId(), req.getStatus(), from, to, idsIn);

        int page = Math.max(req.getPage(), 1);
        int pageSize = req.getPageSize() < 1 || req.getPageSize() > 100 ? 20 : req.getPageSize();
        Page<GroupExpenseEntity> result = groupExpenseJpaRepository.findAll(spec,
                PageRequest.of(page - 1, pageSize, toSort(sort)));

        var data = result.getContent().stream()
                .map(expense -> expenseResAssembler.toRes(expense,
                        expenseSplitJpaRepository.findByGroupExpenseId(expense.getId()), userId))
                .toList();

        return GroupExpenseListRes.builder()
                .data(data)
                .pagination(PaginationRes.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(result.getTotalElements())
                        .totalPages(result.getTotalPages())
                        .build())
                .build();
    }

    private Sort toSort(String sort) {
        boolean desc = sort.startsWith("-");
        String property = desc ? sort.substring(1) : sort;
        return Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, property);
    }

}
