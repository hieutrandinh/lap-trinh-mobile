package org.example.be.rest.usecase.category;

import org.example.be.rest.dto.res.category.CategoryListRes;

public interface SearchCategoryUseCase {

    CategoryListRes execute(String userId, String q, String sort);

}
