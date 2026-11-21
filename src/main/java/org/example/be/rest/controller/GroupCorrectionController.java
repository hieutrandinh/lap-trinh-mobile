package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;
import org.example.be.rest.usecase.correction.ApproveCorrectionUseCase;
import org.example.be.rest.usecase.correction.DetailCorrectionUseCase;
import org.example.be.rest.usecase.correction.RejectCorrectionUseCase;
import org.example.be.rest.usecase.correction.WithdrawCorrectionUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/corrections/{correctionId}")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupCorrectionController {

    DetailCorrectionUseCase detailCorrectionUseCase;
    ApproveCorrectionUseCase approveCorrectionUseCase;
    RejectCorrectionUseCase rejectCorrectionUseCase;
    WithdrawCorrectionUseCase withdrawCorrectionUseCase;

    @GetMapping
    public ApiRes<CorrectionRequestRes> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                                @PathVariable String correctionId) {
        return ApiRes.success(HttpStatus.OK.value(),
                detailCorrectionUseCase.execute(jwt.getSubject(), groupId, correctionId));
    }

    @PostMapping("/approve")
    public ApiRes<CorrectionRequestRes> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                                 @PathVariable String correctionId) {
        return ApiRes.success(HttpStatus.OK.value(),
                approveCorrectionUseCase.execute(jwt.getSubject(), groupId, correctionId));
    }

    @PostMapping("/reject")
    public ApiRes<CorrectionRequestRes> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                                @PathVariable String correctionId,
                                                @RequestBody(required = false) @Valid NoteReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                rejectCorrectionUseCase.execute(jwt.getSubject(), groupId, correctionId,
                        req != null ? req : new NoteReq()));
    }

    @PostMapping("/withdraw")
    public ApiRes<CorrectionRequestRes> withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                                  @PathVariable String correctionId) {
        return ApiRes.success(HttpStatus.OK.value(),
                withdrawCorrectionUseCase.execute(jwt.getSubject(), groupId, correctionId));
    }

}
