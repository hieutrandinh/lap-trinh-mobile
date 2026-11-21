package org.example.be.rest.usecase.entry.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.EntryDetailView;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.JpaPagination;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.req.entry.EntrySearchReq;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.dto.res.entry.EntryListRes;
import org.example.be.rest.dto.res.entry.TotalsRes;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.usecase.entry.SearchEntryUseCase;
import org.example.be.rest.usecase.entry.support.EntryQueryService;
import org.example.be.rest.usecase.entry.support.EntrySpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchEntryUseCaseImpl implements SearchEntryUseCase {

    private static final Set<String> ALLOWED_SORTS = Set.of("occurredAt", "-occurredAt", "amount", "-amount");

    EntryDetailViewRepository entryDetailViewRepository;
    EntryQueryService entryQueryService;

    @Override
    @Transactional(readOnly = true)
    public EntryListRes execute(String userId, EntrySearchReq req) {
        if (StringUtils.hasText(req.getMonth()) && (req.getFrom() != null || req.getTo() != null)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        LocalDateTime from = null;
        LocalDateTime to = null;
        if (StringUtils.hasText(req.getMonth())) {
            YearMonth month = parseMonth(req.getMonth());
            from = MonthUtil.startOf(month);
            to = MonthUtil.startOfNext(month);
        } else {
            if (req.getFrom() != null) {
                from = req.getFrom().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            if (req.getTo() != null) {
                to = req.getTo().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            if (from != null && to != null && from.isAfter(to)) {
                throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
            }
        }

        String sort = StringUtils.hasText(req.getSort()) ? req.getSort() : "-occurredAt";
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        int page = Math.max(req.getPage(), 1);
        int pageSize = req.getPageSize() < 1 || req.getPageSize() > 100 ? 20 : req.getPageSize();

        Specification<EntryDetailView> spec = EntrySpecifications.build(userId, req.getType(), req.getCategoryId(),
                req.getBucketId(), from, to, req.getQ(), req.getIsShared(), req.getGroupId());

        Sort sortSpec = toSort(sort);
        Page<EntryDetailView> result = entryDetailViewRepository.findAll(spec,
                JpaPagination.request(page, pageSize, sortSpec));

        List<EntryDetailView> all = entryDetailViewRepository.findAll(spec);
        BigDecimal income = sumByType(all, "income");
        BigDecimal expense = sumByType(all, "expense");

        return EntryListRes.builder()
                .data(entryQueryService.toRes(result.getContent()))
                .pagination(PaginationRes.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(result.getTotalElements())
                        .totalPages(result.getTotalPages())
                        .build())
                .totals(TotalsRes.builder().income(income).expense(expense).currency("USD").build())
                .build();
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private Sort toSort(String sort) {
        boolean desc = sort.startsWith("-");
        String property = desc ? sort.substring(1) : sort;
        return Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, property);
    }

    private BigDecimal sumByType(List<EntryDetailView> entries, String type) {
        return entries.stream()
                .filter(e -> type.equals(e.getEntryType()))
                .map(EntryDetailView::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
