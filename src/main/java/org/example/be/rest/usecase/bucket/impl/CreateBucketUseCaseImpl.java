package org.example.be.rest.usecase.bucket.impl;

import org.example.be.rest.usecase.bucket.CreateBucketUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.HashSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.BucketCategoryEntity;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.req.bucket.CreateBucketReq;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.repository.DeletionMarkerJpaRepository;
import org.example.be.rest.usecase.CreationResult;
import org.example.be.rest.usecase.PersonalRecordLocks;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateBucketUseCaseImpl implements CreateBucketUseCase {

    private static final int MAX_BUCKETS_PER_USER = 100;

    SpendingBucketJpaRepository spendingBucketJpaRepository;
    BucketCategoryJpaRepository bucketCategoryJpaRepository;
    EntryJpaRepository entryJpaRepository;
    EntryDetailViewRepository entryDetailViewRepository;
    BucketQueryService bucketQueryService;
    DeletionMarkerJpaRepository markers;
    PersonalRecordLocks locks;

    @Override
    @Transactional
    public CreationResult<BucketRes> execute(String userId, CreateBucketReq req) {
        locks.lockCreation("bucket", req.getId());
        var byId = spendingBucketJpaRepository.findById(req.getId());
        if (byId.isPresent()) {
            SpendingBucketEntity existing = byId.get();
            if (userId.equals(existing.getUserId()) && existing.getName().equals(req.getName().trim())
                    && existing.getMonthlyTarget().compareTo(req.getMonthlyTarget()) == 0
                    && new HashSet<>(bucketQueryService.categoryIds(existing.getId()))
                        .equals(new HashSet<>(bucketQueryService.validateCategoryIds(userId, req.getCategoryIds())))) {
                return new CreationResult<>(bucketQueryService.currentMonth(existing), true);
            }
            throw new BusinessLogicException(ErrorCode.ID_ALREADY_EXISTS);
        }
        if (markers.existsByUserIdAndRecordTypeAndRecordId(userId, "bucket", req.getId())) {
            throw new BusinessLogicException(ErrorCode.RECORD_DELETED);
        }
        if (spendingBucketJpaRepository.countByUserId(userId) >= MAX_BUCKETS_PER_USER) {
            throw new BusinessLogicException(ErrorCode.BUCKET_LIMIT_REACHED);
        }

        List<String> categoryIds = bucketQueryService.validateCategoryIds(userId, req.getCategoryIds());

        LocalDateTime now = LocalDateTime.now();
        SpendingBucketEntity bucket = new SpendingBucketEntity();
        bucket.setId(req.getId());
        bucket.setName(req.getName().trim());
        bucket.setMonthlyTarget(req.getMonthlyTarget());
        bucket.setVersion(1);
        bucket.setCreatedAt(now);
        bucket.setUpdatedAt(now);
        bucket.setUserId(userId);
        spendingBucketJpaRepository.save(bucket);

        if (!categoryIds.isEmpty()) {
            bucketCategoryJpaRepository.saveAllAndFlush(categoryIds.stream()
                    .map(categoryId -> new BucketCategoryEntity(bucket.getId(), categoryId))
                    .toList());
        }

        YearMonth month = YearMonth.now(bucketQueryService.zoneOf(userId));
        LocalDateTime from = MonthUtil.startOf(month);
        LocalDateTime to = MonthUtil.startOfNext(month);
        if (!categoryIds.isEmpty()) {
            List<String> entryIds = entryDetailViewRepository.findUnbucketedExpenseIds(userId, categoryIds, from, to);
            if (!entryIds.isEmpty()) {
                entryJpaRepository.assignBucket(entryIds, bucket.getId(), now);
            }
        }

        BigDecimal spent = bucketQueryService.spent(userId, bucket.getId(), from, to);
        return new CreationResult<>(BucketRes.of(bucket, bucketQueryService.categoryRefs(bucket.getId()), month.toString(), spent), false);
    }

}
