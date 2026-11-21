package org.example.be.rest.usecase.correction.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CorrectionRequestEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.correction.CorrectionSearchReq;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.dto.res.correction.CorrectionListRes;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.dto.res.correction.OriginalExpenseRes;
import org.example.be.rest.dto.res.expense.SplitRes;
import org.example.be.rest.repository.CorrectionParticipantJpaRepository;
import org.example.be.rest.repository.CorrectionRequestJpaRepository;
import org.example.be.rest.repository.ExpenseSplitJpaRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.usecase.correction.SearchCorrectionUseCase;
import org.example.be.rest.usecase.correction.support.CorrectionResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchCorrectionUseCaseImpl implements SearchCorrectionUseCase {

    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "completed", "rejected", "withdrawn");

    GroupExpenseJpaRepository groupExpenseJpaRepository;
    CorrectionRequestJpaRepository correctionRequestJpaRepository;
    CorrectionParticipantJpaRepository correctionParticipantJpaRepository;
    ExpenseSplitJpaRepository expenseSplitJpaRepository;
    GroupAccessService groupAccessService;
    CorrectionResAssembler correctionResAssembler;

    @Override
    @Transactional(readOnly = true)
    public CorrectionListRes execute(String userId, String groupId, String expenseId, CorrectionSearchReq req) {
        groupAccessService.requireAccess(groupId, userId);
        GroupExpenseEntity expense = groupExpenseJpaRepository.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));

        boolean desc = "-createdAt".equals(req.getSort());
        if (!desc && req.getSort() != null && !"createdAt".equals(req.getSort())) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        List<String> statuses = StringUtils.hasText(req.getStatus())
                ? List.of(req.getStatus().split(",")).stream().map(String::trim).toList() : null;
        if (statuses != null && !ALLOWED_STATUSES.containsAll(statuses)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        List<CorrectionRequestEntity> all = correctionRequestJpaRepository.findByGroupExpenseId(expenseId).stream()
                .sorted(Comparator.comparing(CorrectionRequestEntity::getCreatedAt))
                .toList();

        OriginalExpenseRes original = buildOriginal(expense, all);

        List<CorrectionRequestEntity> filtered = all.stream()
                .filter(r -> statuses == null || statuses.contains(r.getStatus()))
                .toList();
        if (desc) {
            filtered = new java.util.ArrayList<>(filtered);
            java.util.Collections.reverse(filtered);
        }

        int page = Math.max(req.getPage(), 1);
        int pageSize = req.getPageSize() < 1 || req.getPageSize() > 100 ? 20 : req.getPageSize();
        int from = Math.min((page - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());

        List<CorrectionRequestRes> data = filtered.subList(from, to).stream()
                .map(r -> correctionResAssembler.fromEntity(groupId, r,
                        correctionParticipantJpaRepository.findByCorrectionRequestId(r.getId())))
                .toList();

        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(filtered.size() / (double) pageSize);
        return CorrectionListRes.builder()
                .original(original)
                .data(data)
                .pagination(PaginationRes.builder()
                        .page(page).pageSize(pageSize).totalItems(filtered.size()).totalPages(totalPages)
                        .build())
                .build();
    }

    /** The earliest correction's "previous" snapshot is the expense as originally created; with no
     * corrections yet, the live expense/splits are themselves still the original. */
    private OriginalExpenseRes buildOriginal(GroupExpenseEntity expense, List<CorrectionRequestEntity> history) {
        if (history.isEmpty()) {
            List<SplitRes> splits = expenseSplitJpaRepository.findByGroupExpenseId(expense.getId()).stream()
                    .map(s -> SplitRes.builder()
                            .user(correctionResAssembler.toRef(s.getUserId()))
                            .amount(s.getAmount())
                            .roundingAdjustment(s.getRoundingAdjustment())
                            .build())
                    .toList();
            return OriginalExpenseRes.builder()
                    .title(expense.getTitle())
                    .totalAmount(expense.getTotalAmount())
                    .occurredAt(expense.getOccurredAt())
                    .category(expense.getCategory())
                    .payer(correctionResAssembler.toRef(expense.getPayerUserId()))
                    .splitMethod(expense.getSplitMethod())
                    .splits(splits)
                    .build();
        }

        CorrectionRequestEntity first = history.get(0);
        List<SplitRes> splits = correctionParticipantJpaRepository.findByCorrectionRequestId(first.getId()).stream()
                .filter(p -> p.getPreviousAmount() != null)
                .map(p -> SplitRes.builder()
                        .user(correctionResAssembler.toRef(p.getUserId()))
                        .amount(p.getPreviousAmount())
                        .roundingAdjustment(java.math.BigDecimal.ZERO.setScale(2))
                        .build())
                .toList();
        return OriginalExpenseRes.builder()
                .title(first.getPreviousTitle())
                .totalAmount(first.getPreviousTotalAmount())
                .occurredAt(first.getPreviousOccurredAt())
                .category(first.getPreviousCategory())
                .payer(correctionResAssembler.toRef(first.getPreviousPayerUserId()))
                .splitMethod(first.getPreviousSplitMethod())
                .splits(splits)
                .build();
    }

}
