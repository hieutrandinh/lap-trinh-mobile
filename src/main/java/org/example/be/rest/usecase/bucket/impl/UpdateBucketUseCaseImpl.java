package org.example.be.rest.usecase.bucket.impl;

import org.example.be.rest.usecase.bucket.UpdateBucketUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.BucketCategoryEntity;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.req.bucket.EditBucketReq;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateBucketUseCaseImpl implements UpdateBucketUseCase {

    SpendingBucketJpaRepository spendingBucketJpaRepository;
    BucketCategoryJpaRepository bucketCategoryJpaRepository;
    EntryJpaRepository entryJpaRepository;
    EntryDetailViewRepository entryDetailViewRepository;
    BucketQueryService bucketQueryService;

    @Override
    @Transactional
    public BucketRes execute(String userId, String bucketId, EditBucketReq req) {
        SpendingBucketEntity bucket = spendingBucketJpaRepository.findForUpdate(bucketId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (bucket.getVersion() != req.getVersion()) {
            throw new BusinessLogicException(ErrorCode.VERSION_CONFLICT, Map.of("current", bucketQueryService.currentMonth(bucket)));
        }

        LocalDateTime now = LocalDateTime.now();
        if (req.getName() != null) {
            bucket.setName(req.getName().trim());
        }
        if (req.getMonthlyTarget() != null) {
            bucket.setMonthlyTarget(req.getMonthlyTarget());
        }

        YearMonth month = YearMonth.now(bucketQueryService.zoneOf(userId));
        LocalDateTime from = MonthUtil.startOf(month);
        LocalDateTime to = MonthUtil.startOfNext(month);

        if (req.getCategoryIds() != null) {
            applyCategoryChanges(userId, bucketId, req.getCategoryIds(), now, from, to);
        }

        bucket.setVersion(bucket.getVersion() + 1);
        bucket.setUpdatedAt(now);
        spendingBucketJpaRepository.save(bucket);

        BigDecimal spent = bucketQueryService.spent(userId, bucketId, from, to);
        return BucketRes.of(bucket, bucketQueryService.categoryRefs(bucketId), month.toString(), spent);
    }

    private void applyCategoryChanges(String userId, String bucketId, List<String> newCategoryIds,
                                       LocalDateTime now, LocalDateTime from, LocalDateTime to) {
        List<String> validated = bucketQueryService.validateCategoryIds(userId, newCategoryIds);
        Set<String> newSet = new HashSet<>(validated);
        Set<String> existingSet = new HashSet<>(bucketQueryService.categoryIds(bucketId));

        List<String> toRemove = existingSet.stream().filter(id -> !newSet.contains(id)).toList();
        List<String> toAdd = newSet.stream().filter(id -> !existingSet.contains(id)).toList();

        if (!toRemove.isEmpty()) {
            bucketCategoryJpaRepository.deleteByBucketIdAndCategoryIdIn(bucketId, toRemove);
            entryJpaRepository.clearBucketForCategories(userId, bucketId, toRemove, now);
        }
        if (!toAdd.isEmpty()) {
            bucketCategoryJpaRepository.saveAllAndFlush(toAdd.stream()
                    .map(categoryId -> new BucketCategoryEntity(bucketId, categoryId))
                    .toList());
            List<String> entryIds = entryDetailViewRepository.findUnbucketedExpenseIds(userId, toAdd, from, to);
            if (!entryIds.isEmpty()) {
                entryJpaRepository.assignBucket(entryIds, bucketId, now);
            }
        }
    }

}
