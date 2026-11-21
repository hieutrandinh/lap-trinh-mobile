package org.example.be.rest.usecase.bucket.impl;

import org.example.be.rest.usecase.bucket.DetailBucketUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DetailBucketUseCaseImpl implements DetailBucketUseCase {

    SpendingBucketJpaRepository spendingBucketJpaRepository;
    BucketQueryService bucketQueryService;

    @Override
    @Transactional(readOnly = true)
    public BucketRes execute(String userId, String bucketId, String monthParam) {
        SpendingBucketEntity bucket = spendingBucketJpaRepository.findByIdAndUserId(bucketId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));

        YearMonth month = MonthUtil.resolve(monthParam, bucketQueryService.zoneOf(userId).getId());
        LocalDateTime from = MonthUtil.startOf(month);
        LocalDateTime to = MonthUtil.startOfNext(month);
        BigDecimal spent = bucketQueryService.spent(userId, bucketId, from, to);

        return BucketRes.of(bucket, bucketQueryService.categoryRefs(bucketId), month.toString(), spent);
    }

}
