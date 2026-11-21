package org.example.be.rest.usecase.category.impl;

import org.example.be.rest.usecase.category.UpdateCategoryUseCase;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.category.RenameCategoryReq;
import org.example.be.rest.dto.res.category.CategoryRefRes;
import org.example.be.rest.dto.res.category.CategoryRes;
import org.example.be.rest.dto.res.category.RenameCategoryRes;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.usecase.category.support.CategoryQueryService;
import org.example.be.rest.usecase.category.support.CategoryReassignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateCategoryUseCaseImpl implements UpdateCategoryUseCase {

    CategoryJpaRepository categoryJpaRepository;
    CategoryQueryService categoryQueryService;
    CategoryReassignmentService categoryReassignmentService;

    @Override
    @Transactional
    public RenameCategoryRes execute(String userId, String categoryId, RenameCategoryReq req) {
        CategoryEntity category = categoryJpaRepository.findForUpdate(categoryId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (category.isSystem()) {
            throw new BusinessLogicException(ErrorCode.SYSTEM_CATEGORY_READ_ONLY);
        }
        if (category.getVersion() != req.getVersion()) {
            throw new BusinessLogicException(ErrorCode.VERSION_CONFLICT, Map.of("current", categoryQueryService.toRes(category)));
        }

        String newName = req.getName().trim();
        categoryJpaRepository.findByUserIdAndNameIgnoreCase(userId, newName).ifPresent(existing -> {
            if (!existing.getId().equals(categoryId)) {
                throw new BusinessLogicException(ErrorCode.CATEGORY_NAME_EXISTS,
                        Map.of("existing", Map.of("id", existing.getId(), "name", existing.getName())));
            }
        });

        String expenseHandling = StringUtils.hasText(req.getExpenseHandling()) ? req.getExpenseHandling() : "keep";
        boolean reassign = "reassign".equals(expenseHandling);
        if (!reassign && StringUtils.hasText(req.getReplacementCategoryId())) {
            throw new BusinessLogicException(ErrorCode.REPLACEMENT_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        category.setName(newName);
        category.setVersion(category.getVersion() + 1);
        category.setUpdatedAt(now);
        categoryJpaRepository.save(category);

        long movedExpenseCount = 0;
        CategoryRefRes movedTo = null;
        if (reassign) {
            CategoryEntity target = categoryReassignmentService.resolveReplacement(userId, categoryId,
                    req.getReplacementCategoryId());
            movedExpenseCount = categoryReassignmentService.reassignEntries(userId, categoryId, target, now);
            movedTo = CategoryRefRes.builder().id(target.getId()).name(target.getName()).build();
        }

        CategoryRes categoryRes = CategoryRes.from(category, categoryQueryService.expenseCount(userId, categoryId),
                categoryQueryService.bucketIds(categoryId));
        return RenameCategoryRes.builder()
                .category(categoryRes)
                .movedExpenseCount(movedExpenseCount)
                .movedTo(movedTo)
                .build();
    }

}
