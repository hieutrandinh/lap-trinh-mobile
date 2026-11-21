package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.notification.UpdateNotificationSettingsReq;
import org.example.be.rest.dto.res.auth.MeRes;
import org.example.be.rest.dto.res.auth.NotificationSettingsRes;
import org.example.be.rest.usecase.me.GetMeUseCase;
import org.example.be.rest.usecase.notification.UpdateNotificationSettingsUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeController {

    GetMeUseCase getMeUseCase;
    UpdateNotificationSettingsUseCase updateNotificationSettingsUseCase;

    @GetMapping
    public ApiRes<MeRes> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiRes.success(HttpStatus.OK.value(), getMeUseCase.execute(jwt.getSubject()));
    }

    @PatchMapping("/notification-settings")
    public ApiRes<NotificationSettingsRes> updateNotificationSettings(
            @AuthenticationPrincipal Jwt jwt, @RequestBody @Valid UpdateNotificationSettingsReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                updateNotificationSettingsUseCase.execute(jwt.getSubject(), req));
    }

}
