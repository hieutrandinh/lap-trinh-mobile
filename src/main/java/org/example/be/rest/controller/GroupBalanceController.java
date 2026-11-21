package org.example.be.rest.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.res.repayment.BalancesRes;
import org.example.be.rest.usecase.repayment.GetBalancesUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/balances")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupBalanceController {

    GetBalancesUseCase getBalancesUseCase;

    @GetMapping
    public ApiRes<BalancesRes> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId) {
        return ApiRes.success(HttpStatus.OK.value(), getBalancesUseCase.execute(jwt.getSubject(), groupId));
    }

}
