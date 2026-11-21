package org.example.be.rest.usecase.expense.support;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.springframework.stereotype.Service;

/** 9.2: each participant's shared entry uses their own category with the same name as the group's category. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonalCategoryResolver {

    CategoryJpaRepository categoryJpaRepository;

    public CategoryEntity resolve(String userId, String categoryName) {
        return categoryJpaRepository.findByUserIdAndNameIgnoreCase(userId, categoryName)
                .orElseGet(() -> create(userId, categoryName));
    }

    private CategoryEntity create(String userId, String categoryName) {
        LocalDateTime now = LocalDateTime.now();
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID().toString());
        category.setName(categoryName);
        category.setSystem(false);
        category.setVersion(1);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        category.setUserId(userId);
        return categoryJpaRepository.save(category);
    }

}
