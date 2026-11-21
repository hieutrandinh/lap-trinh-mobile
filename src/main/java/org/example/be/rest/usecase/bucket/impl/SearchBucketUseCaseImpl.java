package org.example.be.rest.usecase.bucket.impl;

import org.example.be.rest.usecase.bucket.SearchBucketUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.res.bucket.BucketListRes;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchBucketUseCaseImpl implements SearchBucketUseCase {

    SpendingBucketJpaRepository spendingBucketJpaRepository;
    BucketQueryService bucketQueryService;

    @Override
    @Transactional(readOnly = true)
    public BucketListRes execute(String userId, String monthParam) {
        YearMonth month = MonthUtil.resolve(monthParam, bucketQueryService.zoneOf(userId).getId());
        LocalDateTime from = MonthUtil.startOf(month);
        LocalDateTime to = MonthUtil.startOfNext(month);

        var data = spendingBucketJpaRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(bucket -> toRes(userId, bucket, month, from, to))
                .toList();

        return BucketListRes.builder().month(month.toString()).data(data).build();
    }

    private BucketRes toRes(String userId, SpendingBucketEntity bucket, YearMonth month,
                             LocalDateTime from, LocalDateTime to) {
        BigDecimal spent = bucketQueryService.spent(userId, bucket.getId(), from, to);
        return BucketRes.of(bucket, bucketQueryService.categoryRefs(bucket.getId()), month.toString(), spent);
    }

}
