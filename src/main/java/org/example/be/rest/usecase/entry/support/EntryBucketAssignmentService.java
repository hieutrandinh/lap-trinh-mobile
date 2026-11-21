package org.example.be.rest.usecase.entry.support;

import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.BucketCategoryEntity;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.JsonNullable;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EntryBucketAssignmentService {

    CategoryJpaRepository categoryJpaRepository;
    BucketCategoryJpaRepository bucketCategoryJpaRepository;
    SpendingBucketJpaRepository spendingBucketJpaRepository;

    public CategoryEntity requireCategory(String userId, String categoryId) {
        return categoryJpaRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    /**
     * Resolves the bucketId to persist for an expense entry (4.2/4.3 rules):
     * explicit id -> must capture the category; explicit null -> no bucket;
     * omitted -> auto-assign when exactly one bucket captures the category.
     */
    public String resolveExpenseBucketId(String userId, String categoryId, JsonNullable<String> bucketIdField) {
        if (bucketIdField.isPresent()) {
            String bucketId = bucketIdField.get();
            if (bucketId == null) {
                return null;
            }
            SpendingBucketEntity bucket = spendingBucketJpaRepository.findByIdAndUserId(bucketId, userId)
                    .orElseThrow(() -> new BusinessLogicException(ErrorCode.BUCKET_CATEGORY_MISMATCH));
            if (!bucketCategoryJpaRepository.existsByBucketIdAndCategoryId(bucket.getId(), categoryId)) {
                throw new BusinessLogicException(ErrorCode.BUCKET_CATEGORY_MISMATCH);
            }
            return bucket.getId();
        }

        List<String> matches = bucketCategoryJpaRepository.findByCategoryId(categoryId).stream()
                .map(BucketCategoryEntity::getBucketId)
                .toList();
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        List<Map<String, String>> buckets = spendingBucketJpaRepository.findAllById(matches).stream()
                .map(b -> Map.of("id", b.getId(), "name", b.getName()))
                .toList();
        throw new BusinessLogicException(ErrorCode.BUCKET_SELECTION_REQUIRED, Map.of("buckets", buckets));
    }

    /**
     * Shared-expense entries (9.2) assign automatically without user interaction:
     * exactly one matching bucket -> use it, anything else (0 or several) -> no bucket, no error.
     */
    public String resolveBucketIdSilently(String categoryId) {
        List<String> matches = bucketCategoryJpaRepository.findByCategoryId(categoryId).stream()
                .map(BucketCategoryEntity::getBucketId)
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

}
