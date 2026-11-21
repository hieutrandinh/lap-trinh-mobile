package org.example.be.rest.usecase.category;

import org.example.be.rest.dto.req.category.RenameCategoryReq;
import org.example.be.rest.dto.res.category.RenameCategoryRes;

public interface UpdateCategoryUseCase {

    RenameCategoryRes execute(String userId, String categoryId, RenameCategoryReq req);

}
