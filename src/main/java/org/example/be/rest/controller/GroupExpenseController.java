package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.usecase.IdempotencyService;
import org.example.be.rest.dto.req.expense.CreateGroupExpenseReq;
import org.example.be.rest.dto.req.expense.GroupExpenseSearchReq;
import org.example.be.rest.dto.req.expense.SplitPreviewReq;
import org.example.be.rest.dto.res.expense.GroupExpenseListRes;
import org.example.be.rest.dto.res.expense.GroupExpenseRes;
import org.example.be.rest.dto.res.expense.SplitPreviewRes;
import org.example.be.rest.usecase.expense.CreateGroupExpenseUseCase;
import org.example.be.rest.usecase.expense.DetailGroupExpenseUseCase;
import org.example.be.rest.usecase.expense.SearchGroupExpenseUseCase;
import org.example.be.rest.usecase.expense.SplitPreviewUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/expenses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupExpenseController {

    SplitPreviewUseCase splitPreviewUseCase;
    CreateGroupExpenseUseCase createGroupExpenseUseCase;
    SearchGroupExpenseUseCase searchGroupExpenseUseCase;
    DetailGroupExpenseUseCase detailGroupExpenseUseCase;
    IdempotencyService idempotency;

    @PostMapping("/split-preview")
    public ApiRes<SplitPreviewRes> splitPreview(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                                 @RequestBody @Valid SplitPreviewReq req) {
        return ApiRes.success(HttpStatus.OK.value(), splitPreviewUseCase.execute(jwt.getSubject(), groupId, req));
    }

    @PostMapping
    public ApiRes<?> create(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                           @RequestHeader("Idempotency-Key") String idempotencyKey,
                                           @RequestBody @Valid CreateGroupExpenseReq req) {
        return idempotency.execute(jwt.getSubject(), idempotencyKey, "expenses/" + groupId, req,
                () -> ApiRes.success(HttpStatus.CREATED.value(), createGroupExpenseUseCase.execute(jwt.getSubject(), groupId, req)));
    }

    @GetMapping
    public ApiRes<GroupExpenseListRes> search(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                               @ModelAttribute GroupExpenseSearchReq req) {
        return ApiRes.success(HttpStatus.OK.value(), searchGroupExpenseUseCase.execute(jwt.getSubject(), groupId, req));
    }

    @GetMapping("/{expenseId}")
    public ApiRes<GroupExpenseRes> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                           @PathVariable String expenseId) {
        return ApiRes.success(HttpStatus.OK.value(),
                detailGroupExpenseUseCase.execute(jwt.getSubject(), groupId, expenseId));
    }

}
