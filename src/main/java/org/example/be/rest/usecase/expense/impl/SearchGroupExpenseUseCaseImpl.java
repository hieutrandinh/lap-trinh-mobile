package org.example.be.rest.usecase.expense.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.ExpenseSplitEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.JpaPagination;
import org.example.be.rest.dto.req.expense.GroupExpenseSearchReq;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.dto.res.expense.GroupExpenseListRes;
import org.example.be.rest.dto.res.expense.GroupExpenseRes;
import org.example.be.rest.repository.ExpenseSplitJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.expense.SearchGroupExpenseUseCase;
import org.example.be.rest.usecase.expense.support.ExpenseResAssembler;
import org.example.be.rest.usecase.expense.support.ExpenseSpecifications;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    GroupSnapshot groupSnapshot;

    @Override
    @Transactional(readOnly = true)
    public GroupExpenseListRes execute(String userId, String groupId, GroupExpenseSearchReq req) {
        var access = groupAccessService.requireAccess(groupId, userId);

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

        int page = Math.max(req.getPage(), 1);
        int pageSize = req.getPageSize() < 1 || req.getPageSize() > 100 ? 20 : req.getPageSize();
        PageRequest paging = JpaPagination.request(page, pageSize, toSort(sort));
        Page<GroupExpenseRes> result = groupSnapshot.appliesTo(access)
                ? searchSnapshot(groupSnapshot.read(access).expenses(), req, from, to, paging, sort)
                : searchLive(userId, groupId, req, from, to, paging);

        return GroupExpenseListRes.builder()
                .data(result.getContent())
                .pagination(PaginationRes.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(result.getTotalElements())
                        .totalPages(result.getTotalPages())
                        .build())
                .build();
    }

    private Page<GroupExpenseRes> searchLive(String userId, String groupId, GroupExpenseSearchReq req,
                                             LocalDateTime from, LocalDateTime to, PageRequest paging) {
        List<String> idsIn = null;
        if (StringUtils.hasText(req.getParticipantUserId())) {
            idsIn = expenseSplitJpaRepository.findByUserId(req.getParticipantUserId()).stream()
                    .map(ExpenseSplitEntity::getGroupExpenseId)
                    .toList();
        }
        Specification<GroupExpenseEntity> spec = ExpenseSpecifications.build(groupId, req.getCategory(),
                req.getPayerUserId(), req.getStatus(), from, to, idsIn, req.getHasPendingCorrection());
        return groupExpenseJpaRepository.findAll(spec, paging)
                .map(expense -> expenseResAssembler.toRes(expense,
                        expenseSplitJpaRepository.findByGroupExpenseId(expense.getId()), userId));
    }

    private Page<GroupExpenseRes> searchSnapshot(List<GroupExpenseRes> expenses, GroupExpenseSearchReq req,
                                                 LocalDateTime from, LocalDateTime to, PageRequest paging, String sort) {
        Comparator<GroupExpenseRes> order = sort.endsWith("totalAmount")
                ? Comparator.comparing(GroupExpenseRes::getTotalAmount)
                : Comparator.comparing(GroupExpenseRes::getOccurredAt);
        if (sort.startsWith("-")) {
            order = order.reversed();
        }
        var filtered = expenses.stream().filter(expense -> matches(expense, req, from, to)).toList();
        var data = filtered.stream()
                .sorted(order.thenComparing(GroupExpenseRes::getId))
                .skip(paging.getOffset())
                .limit(paging.getPageSize())
                .map(expense -> {
                    expense.setAccessMode(null);
                    expense.setMyEntry(null);
                    expense.setCorrections(null);
                    expense.setRelatedRepayments(null);
                    return expense;
                })
                .toList();
        return new PageImpl<>(data, paging, filtered.size());
    }

    private boolean matches(GroupExpenseRes expense, GroupExpenseSearchReq req, LocalDateTime from, LocalDateTime to) {
        return (req.getCategory() == null || req.getCategory().equalsIgnoreCase(expense.getCategory()))
                && (req.getPayerUserId() == null || req.getPayerUserId().equals(expense.getPayer().getUserId()))
                && (req.getStatus() == null || "all".equals(req.getStatus()) || req.getStatus().equals(expense.getStatus()))
                && (from == null || !expense.getOccurredAt().isBefore(from))
                && (to == null || expense.getOccurredAt().isBefore(to))
                && (req.getHasPendingCorrection() == null
                    || req.getHasPendingCorrection() == (expense.getPendingCorrectionId() != null))
                && (!StringUtils.hasText(req.getParticipantUserId()) || expense.getSplits().stream()
                    .anyMatch(split -> req.getParticipantUserId().equals(split.getUser().getUserId())));
    }

    private Sort toSort(String sort) {
        boolean desc = sort.startsWith("-");
        String property = desc ? sort.substring(1) : sort;
        return Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, property);
    }

}
