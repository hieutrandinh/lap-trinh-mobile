package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.res.repayment.ReversalRes;
import org.example.be.rest.usecase.repayment.ApproveReversalUseCase;
import org.example.be.rest.usecase.repayment.RejectReversalUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/reversals/{reversalId}")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReversalController {

    ApproveReversalUseCase approveReversalUseCase;
    RejectReversalUseCase rejectReversalUseCase;

    @PostMapping("/approve")
    public ApiRes<ReversalRes> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                        @PathVariable String reversalId) {
        return ApiRes.success(HttpStatus.OK.value(),
                approveReversalUseCase.execute(jwt.getSubject(), groupId, reversalId));
    }

    @PostMapping("/reject")
    public ApiRes<ReversalRes> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                       @PathVariable String reversalId,
                                       @RequestBody(required = false) @Valid NoteReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                rejectReversalUseCase.execute(jwt.getSubject(), groupId, reversalId, req != null ? req : new NoteReq()));
    }

}
