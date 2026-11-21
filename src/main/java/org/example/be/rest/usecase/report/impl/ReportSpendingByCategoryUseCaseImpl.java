package org.example.be.rest.usecase.report.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.res.category.CategoryRefRes;
import org.example.be.rest.dto.res.report.CategorySpendingItemRes;
import org.example.be.rest.dto.res.report.SpendingByCategoryRes;
import org.example.be.rest.repository.CategoryAmountProjection;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.usecase.report.ReportSpendingByCategoryUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportSpendingByCategoryUseCaseImpl implements ReportSpendingByCategoryUseCase {

    EntryDetailViewRepository entryDetailViewRepository;
    CategoryJpaRepository categoryJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public SpendingByCategoryRes execute(String userId, String monthParam, String compareMonthParam) {
        if (!StringUtils.hasText(monthParam)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        YearMonth month = parseMonth(monthParam);
        Map<String, BigDecimal> byCategory = sumByCategory(userId, month);
        BigDecimal total = byCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasCompare = StringUtils.hasText(compareMonthParam);
        YearMonth compareMonth = hasCompare ? parseMonth(compareMonthParam) : null;
        Map<String, BigDecimal> compareByCategory = hasCompare ? sumByCategory(userId, compareMonth) : Map.of();
        BigDecimal compareTotal = hasCompare
                ? compareByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;

        Map<String, CategoryEntity> categories = categoryJpaRepository.findAllById(byCategory.keySet()).stream()
                .collect(java.util.stream.Collectors.toMap(CategoryEntity::getId, c -> c));

        var items = byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> toItem(categories.get(e.getKey()), e.getValue(), total, hasCompare, compareByCategory))
                .toList();

        BigDecimal changeAmount = hasCompare ? total.subtract(compareTotal) : null;
        Double changePercent = hasCompare && compareTotal.signum() != 0
                ? changeAmount.multiply(BigDecimal.valueOf(100)).divide(compareTotal, 4, RoundingMode.HALF_UP)
                    .doubleValue()
                : null;

        return SpendingByCategoryRes.builder()
                .month(month.toString())
                .compareMonth(hasCompare ? compareMonth.toString() : null)
                .total(total)
                .compareTotal(compareTotal)
                .changeAmount(changeAmount)
                .changePercent(changePercent)
                .items(items)
                .build();
    }

    private CategorySpendingItemRes toItem(CategoryEntity category, BigDecimal amount, BigDecimal total,
                                            boolean hasCompare, Map<String, BigDecimal> compareByCategory) {
        CategoryRefRes ref = category == null ? null
                : CategoryRefRes.builder().id(category.getId()).name(category.getName()).build();
        double percent = total.signum() == 0 ? 0.0
                : amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).doubleValue();
        BigDecimal compareAmount = null;
        BigDecimal changeAmount = null;
        if (hasCompare) {
            compareAmount = compareByCategory.getOrDefault(category == null ? null : category.getId(),
                    BigDecimal.ZERO);
            changeAmount = amount.subtract(compareAmount);
        }
        return CategorySpendingItemRes.builder()
                .category(ref)
                .amount(amount)
                .percent(percent)
                .compareAmount(compareAmount)
                .changeAmount(changeAmount)
                .build();
    }

    private Map<String, BigDecimal> sumByCategory(String userId, YearMonth month) {
        LocalDateTime from = MonthUtil.startOf(month);
        LocalDateTime to = MonthUtil.startOfNext(month);
        Map<String, BigDecimal> result = new HashMap<>();
        for (CategoryAmountProjection row : entryDetailViewRepository.sumExpenseByCategory(userId, from, to)) {
            result.put(row.getCategoryId(), row.getAmount());
        }
        return result;
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
    }

}
