package org.example.be.rest.usecase.category.impl;

import org.example.be.rest.usecase.category.SearchCategoryUseCase;

import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.rest.dto.res.category.CategoryListRes;
import org.example.be.rest.dto.res.category.CategoryRes;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.usecase.category.support.CategoryQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchCategoryUseCaseImpl implements SearchCategoryUseCase {

    CategoryJpaRepository categoryJpaRepository;
    CategoryQueryService categoryQueryService;

    @Override
    @Transactional(readOnly = true)
    public CategoryListRes execute(String userId, String q, String sort) {
        List<CategoryEntity> categories = StringUtils.hasText(q)
                ? categoryJpaRepository.findByUserIdAndNameContainingIgnoreCaseOrderByNameAsc(userId, q.trim())
                : categoryJpaRepository.findByUserIdOrderByNameAsc(userId);

        List<CategoryRes> data = categories.stream()
                .map(c -> CategoryRes.from(c, categoryQueryService.expenseCount(userId, c.getId()),
                        categoryQueryService.bucketIds(c.getId())))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        if ("-expenseCount".equals(sort)) {
            data.sort(Comparator.comparingLong(CategoryRes::getExpenseCount).reversed());
        }

        return CategoryListRes.builder().data(data).build();
    }

}
