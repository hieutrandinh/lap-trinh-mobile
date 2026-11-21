package org.example.be.rest.usecase.category.impl;

import org.example.be.rest.usecase.category.DeleteCategoryUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.BucketCategoryEntity;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.DeletionMarkerEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.category.CategoryRefRes;
import org.example.be.rest.dto.res.category.DeleteCategoryRes;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.DeletionMarkerJpaRepository;
import org.example.be.rest.usecase.category.support.CategoryReassignmentService;
import org.example.be.rest.usecase.category.support.CategoryQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeleteCategoryUseCaseImpl implements DeleteCategoryUseCase {

    private static final long DELETION_MARKER_TTL_DAYS = 30;

    CategoryJpaRepository categoryJpaRepository;
    BucketCategoryJpaRepository bucketCategoryJpaRepository;
    DeletionMarkerJpaRepository deletionMarkerJpaRepository;
    CategoryReassignmentService categoryReassignmentService;
    CategoryQueryService categoryQuery;

    @Override
    @Transactional
    public DeleteCategoryRes execute(String userId, String categoryId, int version, String replacementCategoryId) {
        CategoryEntity category = categoryJpaRepository.findForUpdate(categoryId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (category.isSystem()) {
            throw new BusinessLogicException(ErrorCode.SYSTEM_CATEGORY_READ_ONLY);
        }
        if (category.getVersion() != version) {
            throw new BusinessLogicException(ErrorCode.VERSION_CONFLICT, Map.of("current", categoryQuery.toRes(category)));
        }

        CategoryEntity target = categoryReassignmentService.resolveReplacement(userId, categoryId,
                replacementCategoryId);

        LocalDateTime now = LocalDateTime.now();
        long movedExpenseCount = categoryReassignmentService.reassignEntries(userId, categoryId, target, now);

        List<String> affectedBucketIds = bucketCategoryJpaRepository.findByCategoryId(categoryId).stream()
                .map(BucketCategoryEntity::getBucketId)
                .toList();
        bucketCategoryJpaRepository.deleteByCategoryId(categoryId);

        categoryJpaRepository.delete(category);

        DeletionMarkerEntity marker = new DeletionMarkerEntity();
        marker.setId(UUID.randomUUID().toString());
        marker.setRecordType("category");
        marker.setRecordId(categoryId);
        marker.setDeletedAt(now);
        marker.setExpiresAt(now.plusDays(DELETION_MARKER_TTL_DAYS));
        marker.setUserId(userId);
        deletionMarkerJpaRepository.save(marker);

        return DeleteCategoryRes.builder()
                .deletedCategoryId(categoryId)
                .movedExpenseCount(movedExpenseCount)
                .movedTo(CategoryRefRes.builder().id(target.getId()).name(target.getName()).build())
                .affectedBucketIds(affectedBucketIds)
                .build();
    }

}
