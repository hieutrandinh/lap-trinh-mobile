package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.usecase.IdempotencyService;
import org.example.be.rest.dto.req.correction.CorrectionSearchReq;
import org.example.be.rest.dto.req.correction.CreateCorrectionReq;
import org.example.be.rest.dto.res.correction.CorrectionListRes;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.usecase.correction.CreateCorrectionUseCase;
import org.example.be.rest.usecase.correction.PreviewCorrectionUseCase;
import org.example.be.rest.usecase.correction.SearchCorrectionUseCase;
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
@RequestMapping("/groups/{groupId}/expenses/{expenseId}/corrections")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseCorrectionController {

    CreateCorrectionUseCase createCorrectionUseCase;
    PreviewCorrectionUseCase previewCorrectionUseCase;
    SearchCorrectionUseCase searchCorrectionUseCase;
    IdempotencyService idempotency;

    @PostMapping
    public ApiRes<?> create(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                                @PathVariable String expenseId,
                                                @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                @RequestBody @Valid CreateCorrectionReq req) {
        return idempotency.execute(jwt.getSubject(), idempotencyKey, "corrections/" + groupId + "/" + expenseId, req,
                () -> ApiRes.success(HttpStatus.CREATED.value(),
                        createCorrectionUseCase.execute(jwt.getSubject(), groupId, expenseId, req)));
    }

    @PostMapping("/preview")
    public ApiRes<CorrectionRequestRes> preview(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                                 @PathVariable String expenseId,
                                                 @RequestBody @Valid CreateCorrectionReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                previewCorrectionUseCase.execute(jwt.getSubject(), groupId, expenseId, req));
    }

    @GetMapping
    public ApiRes<CorrectionListRes> search(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                             @PathVariable String expenseId,
                                             @ModelAttribute CorrectionSearchReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                searchCorrectionUseCase.execute(jwt.getSubject(), groupId, expenseId, req));
    }

}
