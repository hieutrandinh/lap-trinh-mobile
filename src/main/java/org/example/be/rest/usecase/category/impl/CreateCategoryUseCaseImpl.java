package org.example.be.rest.usecase.category.impl;

import org.example.be.rest.usecase.category.CreateCategoryUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.category.CreateCategoryReq;
import org.example.be.rest.dto.res.category.CategoryRes;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.DeletionMarkerJpaRepository;
import org.example.be.rest.usecase.CreationResult;
import org.example.be.rest.usecase.PersonalRecordLocks;
import org.example.be.rest.usecase.category.support.CategoryQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateCategoryUseCaseImpl implements CreateCategoryUseCase {

    private static final int MAX_CATEGORIES_PER_USER = 500;

    CategoryJpaRepository categoryJpaRepository;
    DeletionMarkerJpaRepository markers;
    PersonalRecordLocks locks;
    CategoryQueryService categoryQuery;

    @Override
    @Transactional
    public CreationResult<CategoryRes> execute(String userId, CreateCategoryReq req) {
        locks.lockCreation("category", req.getId());
        String name = req.getName().trim();
        var byId = categoryJpaRepository.findById(req.getId());
        if (byId.isPresent()) {
            CategoryEntity existing = byId.get();
            if (userId.equals(existing.getUserId()) && name.equals(existing.getName())) {
                return new CreationResult<>(categoryQuery.toRes(existing), true);
            }
            throw new BusinessLogicException(ErrorCode.ID_ALREADY_EXISTS);
        }
        if (markers.existsByUserIdAndRecordTypeAndRecordId(userId, "category", req.getId())) {
            throw new BusinessLogicException(ErrorCode.RECORD_DELETED);
        }
        categoryJpaRepository.findByUserIdAndNameIgnoreCase(userId, name).ifPresent(existing -> {
            throw new BusinessLogicException(ErrorCode.CATEGORY_NAME_EXISTS,
                    Map.of("existing", Map.of("id", existing.getId(), "name", existing.getName())));
        });
        if (categoryJpaRepository.countByUserId(userId) >= MAX_CATEGORIES_PER_USER) {
            throw new BusinessLogicException(ErrorCode.CATEGORY_LIMIT_REACHED);
        }

        LocalDateTime now = LocalDateTime.now();
        CategoryEntity category = new CategoryEntity();
        category.setId(req.getId());
        category.setName(name);
        category.setSystem(false);
        category.setVersion(1);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        category.setUserId(userId);
        categoryJpaRepository.save(category);

        return new CreationResult<>(CategoryRes.from(category, 0, List.of()), false);
    }

}
