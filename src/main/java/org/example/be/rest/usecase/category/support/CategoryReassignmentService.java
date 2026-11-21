package org.example.be.rest.usecase.category.support;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryReassignmentService {

    CategoryJpaRepository categoryJpaRepository;
    EntryJpaRepository entryJpaRepository;
    BucketCategoryJpaRepository bucketCategoryJpaRepository;

    public CategoryEntity resolveReplacement(String userId, String categoryId, String replacementCategoryId) {
        if (!StringUtils.hasText(replacementCategoryId)) {
            return categoryJpaRepository.findByUserIdAndIsSystemTrue(userId)
                    .orElseThrow(() -> new BusinessLogicException(ErrorCode.REPLACEMENT_INVALID));
        }
        if (replacementCategoryId.equals(categoryId)) {
            throw new BusinessLogicException(ErrorCode.REPLACEMENT_INVALID);
        }
        return categoryJpaRepository.findByIdAndUserId(replacementCategoryId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.REPLACEMENT_INVALID));
    }

    /**
     * Moves every entry from categoryId to target. Entries whose current bucket does not
     * capture the target category lose that bucket first (6.3 rule, reused for renaming/deleting).
     */
    public long reassignEntries(String userId, String categoryId, CategoryEntity target, LocalDateTime now) {
        List<String> bucketsInUse = entryJpaRepository.findDistinctBucketIdsByUserIdAndCategoryId(userId, categoryId);
        for (String bucketId : bucketsInUse) {
            if (!bucketCategoryJpaRepository.existsByBucketIdAndCategoryId(bucketId, target.getId())) {
                entryJpaRepository.clearBucketForCategoryAndBucket(userId, categoryId, bucketId, now);
            }
        }
        return entryJpaRepository.reassignCategory(userId, categoryId, target.getId(), now);
    }

}
