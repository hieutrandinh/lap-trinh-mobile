package org.example.be.rest.usecase.bucket.impl;

import org.example.be.rest.usecase.bucket.MatchingBucketUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.BucketCategoryEntity;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.res.bucket.BucketMatchingRes;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MatchingBucketUseCaseImpl implements MatchingBucketUseCase {

    CategoryJpaRepository categoryJpaRepository;
    BucketCategoryJpaRepository bucketCategoryJpaRepository;
    SpendingBucketJpaRepository spendingBucketJpaRepository;
    BucketQueryService bucketQueryService;

    @Override
    @Transactional(readOnly = true)
    public BucketMatchingRes execute(String userId, String categoryId) {
        categoryJpaRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.CATEGORY_NOT_FOUND));

        List<String> bucketIds = bucketCategoryJpaRepository.findByCategoryId(categoryId).stream()
                .map(BucketCategoryEntity::getBucketId)
                .toList();

        List<SpendingBucketEntity> buckets = spendingBucketJpaRepository.findAllById(bucketIds).stream()
                .filter(b -> b.getUserId().equals(userId))
                .toList();

        YearMonth month = YearMonth.now(bucketQueryService.zoneOf(userId));
        LocalDateTime from = MonthUtil.startOf(month);
        LocalDateTime to = MonthUtil.startOfNext(month);

        List<BucketRes> bucketResList = buckets.stream()
                .map(bucket -> {
                    BigDecimal spent = bucketQueryService.spent(userId, bucket.getId(), from, to);
                    return BucketRes.of(bucket, bucketQueryService.categoryRefs(bucket.getId()), month.toString(),
                            spent);
                })
                .toList();

        String suggestion = bucketResList.isEmpty() ? "none" : bucketResList.size() == 1 ? "auto" : "choose";
        return BucketMatchingRes.builder()
                .matchCount(bucketResList.size())
                .buckets(bucketResList)
                .suggestion(suggestion)
                .build();
    }

}
