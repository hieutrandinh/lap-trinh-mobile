package org.example.be.rest.usecase.category;

import org.example.be.rest.dto.req.category.CreateCategoryReq;
import org.example.be.rest.dto.res.category.CategoryRes;
import org.example.be.rest.usecase.CreationResult;

public interface CreateCategoryUseCase {

    CreationResult<CategoryRes> execute(String userId, CreateCategoryReq req);

}
