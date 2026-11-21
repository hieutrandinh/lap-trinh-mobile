package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.category.CreateCategoryReq;
import org.example.be.rest.dto.req.category.RenameCategoryReq;
import org.example.be.rest.dto.res.category.CategoryListRes;
import org.example.be.rest.dto.res.category.CategoryRes;
import org.example.be.rest.dto.res.category.DeleteCategoryRes;
import org.example.be.rest.dto.res.category.RenameCategoryRes;
import org.example.be.rest.usecase.category.CreateCategoryUseCase;
import org.example.be.rest.usecase.category.DeleteCategoryUseCase;
import org.example.be.rest.usecase.category.SearchCategoryUseCase;
import org.example.be.rest.usecase.category.UpdateCategoryUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryController {

    CreateCategoryUseCase createCategoryUseCase;
    SearchCategoryUseCase searchCategoryUseCase;
    UpdateCategoryUseCase updateCategoryUseCase;
    DeleteCategoryUseCase deleteCategoryUseCase;

    @GetMapping
    public ApiRes<CategoryListRes> search(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) String sort) {
        return ApiRes.success(HttpStatus.OK.value(), searchCategoryUseCase.execute(jwt.getSubject(), q, sort));
    }

    @PostMapping
    public ApiRes<CategoryRes> create(@AuthenticationPrincipal Jwt jwt,
                                       @RequestBody @Valid CreateCategoryReq req) {
        var result = createCategoryUseCase.execute(jwt.getSubject(), req);
        return ApiRes.success(result.alreadyExisted() ? HttpStatus.OK.value() : HttpStatus.CREATED.value(), result.record());
    }

    @PatchMapping("/{categoryId}")
    public ApiRes<RenameCategoryRes> update(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable String categoryId,
                                             @RequestBody @Valid RenameCategoryReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                updateCategoryUseCase.execute(jwt.getSubject(), categoryId, req));
    }

    @DeleteMapping("/{categoryId}")
    public ApiRes<DeleteCategoryRes> delete(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable String categoryId,
                                             @RequestParam int version,
                                             @RequestParam(required = false) String replacementCategoryId) {
        return ApiRes.success(HttpStatus.OK.value(),
                deleteCategoryUseCase.execute(jwt.getSubject(), categoryId, version, replacementCategoryId));
    }

}
