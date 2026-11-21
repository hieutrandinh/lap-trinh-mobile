package org.example.be.rest.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.res.report.OverviewRes;
import org.example.be.rest.dto.res.report.SpendingByCategoryRes;
import org.example.be.rest.usecase.report.ReportOverviewUseCase;
import org.example.be.rest.usecase.report.ReportSpendingByCategoryUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {

    ReportOverviewUseCase reportOverviewUseCase;
    ReportSpendingByCategoryUseCase reportSpendingByCategoryUseCase;

    @GetMapping("/overview")
    public ApiRes<OverviewRes> overview(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(required = false) String month) {
        return ApiRes.success(HttpStatus.OK.value(), reportOverviewUseCase.execute(jwt.getSubject(), month));
    }

    @GetMapping("/spending-by-category")
    public ApiRes<SpendingByCategoryRes> spendingByCategory(@AuthenticationPrincipal Jwt jwt,
                                                              @RequestParam String month,
                                                              @RequestParam(required = false) String compareMonth) {
        return ApiRes.success(HttpStatus.OK.value(),
                reportSpendingByCategoryUseCase.execute(jwt.getSubject(), month, compareMonth));
    }

}
