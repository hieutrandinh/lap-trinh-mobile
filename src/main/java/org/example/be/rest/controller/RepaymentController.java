package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.usecase.IdempotencyService;
import org.example.be.rest.dto.req.repayment.CreateRepaymentReq;
import org.example.be.rest.dto.req.repayment.CreateReversalReq;
import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.req.repayment.RepaymentSearchReq;
import org.example.be.rest.dto.res.repayment.RepaymentListRes;
import org.example.be.rest.dto.res.repayment.RepaymentRes;
import org.example.be.rest.dto.res.repayment.ReversalRes;
import org.example.be.rest.usecase.repayment.CancelRepaymentUseCase;
import org.example.be.rest.usecase.repayment.ConfirmRepaymentUseCase;
import org.example.be.rest.usecase.repayment.CreateRepaymentUseCase;
import org.example.be.rest.usecase.repayment.CreateReversalUseCase;
import org.example.be.rest.usecase.repayment.DetailRepaymentUseCase;
import org.example.be.rest.usecase.repayment.RejectRepaymentUseCase;
import org.example.be.rest.usecase.repayment.SearchRepaymentUseCase;
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
@RequestMapping("/groups/{groupId}/repayments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RepaymentController {

    CreateRepaymentUseCase createRepaymentUseCase;
    SearchRepaymentUseCase searchRepaymentUseCase;
    DetailRepaymentUseCase detailRepaymentUseCase;
    ConfirmRepaymentUseCase confirmRepaymentUseCase;
    RejectRepaymentUseCase rejectRepaymentUseCase;
    CancelRepaymentUseCase cancelRepaymentUseCase;
    CreateReversalUseCase createReversalUseCase;
    IdempotencyService idempotency;

    @PostMapping
    public ApiRes<?> create(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                        @RequestHeader("Idempotency-Key") String idempotencyKey,
                                        @RequestBody @Valid CreateRepaymentReq req) {
        return idempotency.execute(jwt.getSubject(), idempotencyKey, "repayments/" + groupId, req,
                () -> ApiRes.success(HttpStatus.CREATED.value(), createRepaymentUseCase.execute(jwt.getSubject(), groupId, req)));
    }

    @GetMapping
    public ApiRes<RepaymentListRes> search(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                            @ModelAttribute RepaymentSearchReq req) {
        return ApiRes.success(HttpStatus.OK.value(), searchRepaymentUseCase.execute(jwt.getSubject(), groupId, req));
    }

    @GetMapping("/{repaymentId}")
    public ApiRes<RepaymentRes> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                        @PathVariable String repaymentId) {
        return ApiRes.success(HttpStatus.OK.value(),
                detailRepaymentUseCase.execute(jwt.getSubject(), groupId, repaymentId));
    }

    @PostMapping("/{repaymentId}/confirm")
    public ApiRes<RepaymentRes> confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                         @PathVariable String repaymentId) {
        return ApiRes.success(HttpStatus.OK.value(),
                confirmRepaymentUseCase.execute(jwt.getSubject(), groupId, repaymentId));
    }

    @PostMapping("/{repaymentId}/reject")
    public ApiRes<RepaymentRes> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                        @PathVariable String repaymentId,
                                        @RequestBody(required = false) @Valid NoteReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                rejectRepaymentUseCase.execute(jwt.getSubject(), groupId, repaymentId, req != null ? req : new NoteReq()));
    }

    @PostMapping("/{repaymentId}/cancel")
    public ApiRes<RepaymentRes> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                        @PathVariable String repaymentId) {
        return ApiRes.success(HttpStatus.OK.value(),
                cancelRepaymentUseCase.execute(jwt.getSubject(), groupId, repaymentId));
    }

    @PostMapping("/{repaymentId}/reversals")
    public ApiRes<ReversalRes> createReversal(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                               @PathVariable String repaymentId,
                                               @RequestBody @Valid CreateReversalReq req) {
        return ApiRes.success(HttpStatus.CREATED.value(),
                createReversalUseCase.execute(jwt.getSubject(), groupId, repaymentId, req));
    }

}
