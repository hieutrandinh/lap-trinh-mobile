package org.example.be.rest.usecase.category.support;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.BucketCategoryEntity;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.rest.dto.res.category.CategoryRes;
import org.example.be.rest.repository.BucketCategoryJpaRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryQueryService {

    EntryJpaRepository entryJpaRepository;
    BucketCategoryJpaRepository bucketCategoryJpaRepository;

    public long expenseCount(String userId, String categoryId) {
        return entryJpaRepository.countByUserIdAndCategoryId(userId, categoryId);
    }

    public List<String> bucketIds(String categoryId) {
        return bucketCategoryJpaRepository.findByCategoryId(categoryId).stream()
                .map(BucketCategoryEntity::getBucketId)
                .toList();
    }

    public CategoryRes toRes(CategoryEntity category) {
        return CategoryRes.from(category, expenseCount(category.getUserId(), category.getId()), bucketIds(category.getId()));
    }

}
