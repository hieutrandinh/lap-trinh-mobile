package org.example.be.rest.usecase.bucket.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.BucketCategoryEntity;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.res.category.CategoryRefRes;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BucketQueryService {

    BucketCategoryJpaRepository bucketCategoryJpaRepository;
    CategoryJpaRepository categoryJpaRepository;
    EntryDetailViewRepository entryDetailViewRepository;
    UserJpaRepository userJpaRepository;

    /**
     * Validates categoryIds belong to the user and contain no duplicates; returns them deduplicated.
     */
    public List<String> validateCategoryIds(String userId, List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        if (categoryIds.stream().anyMatch(Objects::isNull) || Set.copyOf(categoryIds).size() != categoryIds.size()) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        List<CategoryEntity> found = categoryJpaRepository.findAllById(categoryIds).stream()
                .filter(c -> c.getUserId().equals(userId))
                .toList();
        if (found.size() != categoryIds.size()) {
            throw new BusinessLogicException(ErrorCode.CATEGORY_NOT_FOUND, Map.of("field", "categoryIds"));
        }
        return found.stream().map(CategoryEntity::getId).toList();
    }

    public List<String> categoryIds(String bucketId) {
        return bucketCategoryJpaRepository.findByBucketId(bucketId).stream()
                .map(BucketCategoryEntity::getCategoryId)
                .toList();
    }

    public List<CategoryRefRes> categoryRefs(String bucketId) {
        List<String> ids = categoryIds(bucketId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return categoryJpaRepository.findAllById(ids).stream()
                .map(c -> CategoryRefRes.builder().id(c.getId()).name(c.getName()).build())
                .toList();
    }

    public BigDecimal spent(String userId, String bucketId, LocalDateTime from, LocalDateTime to) {
        return entryDetailViewRepository.sumBucketSpent(userId, bucketId, from, to);
    }

    public ZoneId zoneOf(String userId) {
        return MonthUtil.safeZone(userJpaRepository.findById(userId).map(u -> u.getTimezone()).orElse("UTC"));
    }

    public BucketRes currentMonth(SpendingBucketEntity bucket) {
        YearMonth month = YearMonth.now(zoneOf(bucket.getUserId()));
        return BucketRes.of(bucket, categoryRefs(bucket.getId()), month.toString(),
                spent(bucket.getUserId(), bucket.getId(), MonthUtil.startOf(month), MonthUtil.startOfNext(month)));
    }

}
