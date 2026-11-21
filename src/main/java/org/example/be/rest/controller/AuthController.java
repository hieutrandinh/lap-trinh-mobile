package org.example.be.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.auth.LoginReq;
import org.example.be.rest.dto.req.auth.LogoutReq;
import org.example.be.rest.dto.req.auth.RefreshTokenReq;
import org.example.be.rest.dto.req.auth.RegisterReq;
import org.example.be.rest.dto.res.auth.AuthRes;
import org.example.be.rest.dto.res.auth.RefreshRes;
import org.example.be.rest.usecase.auth.LoginUseCase;
import org.example.be.rest.usecase.auth.LogoutUseCase;
import org.example.be.rest.usecase.auth.RefreshTokenUseCase;
import org.example.be.rest.usecase.auth.RegisterUseCase;
import org.example.be.rest.usecase.auth.support.AuthThrottle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    RegisterUseCase registerUseCase;
    LoginUseCase loginUseCase;
    RefreshTokenUseCase refreshTokenUseCase;
    LogoutUseCase logoutUseCase;
    AuthThrottle throttle;

    @PostMapping("/register")
    public ApiRes<AuthRes> register(@RequestHeader("X-Device-Id") String deviceId,
                                     @RequestBody @Valid RegisterReq req, HttpServletRequest request) {
        validateDevice(deviceId);
        return ApiRes.success(HttpStatus.CREATED.value(),
                throttle.register(request.getRemoteAddr(), () -> registerUseCase.execute(deviceId, req)));
    }

    @PostMapping("/login")
    public ApiRes<AuthRes> login(@RequestHeader("X-Device-Id") String deviceId,
                                  @RequestBody @Valid LoginReq req, HttpServletRequest request) {
        validateDevice(deviceId);
        return ApiRes.success(HttpStatus.OK.value(),
                throttle.login(request.getRemoteAddr(), req.getEmail(), () -> loginUseCase.execute(deviceId, req)).response());
    }

    @PostMapping("/refresh")
    public ApiRes<RefreshRes> refresh(@RequestHeader("X-Device-Id") String deviceId,
                                       @RequestBody @Valid RefreshTokenReq req) {
        validateDevice(deviceId);
        return ApiRes.success(HttpStatus.OK.value(), refreshTokenUseCase.execute(deviceId, req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt,
                                        @RequestHeader("X-Device-Id") String deviceId,
                                        @RequestBody @Valid LogoutReq req) {
        validateDevice(deviceId);
        logoutUseCase.execute(jwt.getSubject(), deviceId, req);
        return ResponseEntity.noContent().build();
    }

    private static void validateDevice(String deviceId) {
        if (!deviceId.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
    }

}
