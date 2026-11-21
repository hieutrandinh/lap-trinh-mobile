package org.example.be.rest.usecase.category;

import org.example.be.rest.dto.res.category.DeleteCategoryRes;

public interface DeleteCategoryUseCase {

    DeleteCategoryRes execute(String userId, String categoryId, int version, String replacementCategoryId);

}
