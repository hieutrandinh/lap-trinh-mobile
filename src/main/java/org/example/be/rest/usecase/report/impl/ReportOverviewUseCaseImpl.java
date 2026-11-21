package org.example.be.rest.usecase.report.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.rest.dto.res.report.GroupSummaryRes;
import org.example.be.rest.dto.res.report.OverviewRes;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.repository.GroupBalanceViewRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.example.be.rest.usecase.report.ReportOverviewUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportOverviewUseCaseImpl implements ReportOverviewUseCase {

    UserJpaRepository userJpaRepository;
    EntryDetailViewRepository entryDetailViewRepository;
    SpendingBucketJpaRepository spendingBucketJpaRepository;
    BucketQueryService bucketQueryService;
    GroupBalanceViewRepository groupBalanceViewRepository;

    @Override
    @Transactional(readOnly = true)
    public OverviewRes execute(String userId, String monthParam) {
        String timezone = userJpaRepository.findById(userId).map(u -> u.getTimezone()).orElse("UTC");
        YearMonth month = MonthUtil.resolve(monthParam, timezone);
        LocalDateTime from = MonthUtil.startOf(month);
        LocalDateTime to = MonthUtil.startOfNext(month);

        BigDecimal income = entryDetailViewRepository.sumByType(userId, "income", from, to);
        BigDecimal spending = entryDetailViewRepository.sumByType(userId, "expense", from, to);

        var buckets = spendingBucketJpaRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(bucket -> toBucketRes(userId, bucket, month, from, to))
                .toList();
        var groupBalances = groupBalanceViewRepository.findVisibleActiveGroupBalances(userId);
        BigDecimal youOwe = BigDecimal.ZERO;
        BigDecimal youAreOwed = BigDecimal.ZERO;
        for (var group : groupBalances) {
            if (group.getBalance().signum() < 0) {
                youOwe = youOwe.subtract(group.getBalance());
            } else {
                youAreOwed = youAreOwed.add(group.getBalance());
            }
        }

        return OverviewRes.builder()
                .month(month.toString())
                .currency("USD")
                .monthlyIncome(income)
                .monthlySpending(spending)
                .buckets(buckets)
                .groupSummary(GroupSummaryRes.builder()
                        .youOwe(youOwe)
                        .youAreOwed(youAreOwed)
                        .groups(groupBalances)
                        .build())
                .build();
    }

    private BucketRes toBucketRes(String userId, SpendingBucketEntity bucket, YearMonth month,
                                   LocalDateTime from, LocalDateTime to) {
        BigDecimal spent = bucketQueryService.spent(userId, bucket.getId(), from, to);
        return BucketRes.of(bucket, bucketQueryService.categoryRefs(bucket.getId()), month.toString(), spent);
    }

}
