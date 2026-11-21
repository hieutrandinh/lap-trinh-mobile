package org.example.be.rest.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.usecase.IdempotencyService;
import org.example.be.rest.usecase.sync.SyncDevice;
import org.example.be.rest.usecase.sync.SyncPullService;
import org.example.be.rest.usecase.sync.SyncPushService;
import org.example.be.rest.usecase.sync.SyncReconcileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {
    private final IdempotencyService idempotency;
    private final SyncReconcileService reconcile;
    private final SyncPushService push;
    private final SyncPullService pull;

    @PostMapping("/reconcile")
    public ApiRes<?> reconcile(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Device-Id") String deviceId,
                               @RequestHeader("Idempotency-Key") String key, @RequestBody JsonNode request) {
        SyncDevice.requireMatchingHeader(jwt, deviceId);
        return idempotency.execute(jwt.getSubject(), key, "sync/reconcile/" + deviceId, request,
                () -> ApiRes.success(200, reconcile.reconcile(jwt.getSubject(), deviceId, request)));
    }

    @PostMapping("/push")
    public ApiRes<Map<String, Object>> push(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Device-Id") String deviceId,
                                           @RequestHeader("Idempotency-Key") String key, @RequestBody JsonNode request) {
        SyncDevice.requireMatchingHeader(jwt, deviceId);
        return ApiRes.success(200, push.push(jwt.getSubject(), deviceId, key, request));
    }

    @GetMapping("/changes")
    public ApiRes<Map<String, Object>> changes(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Device-Id") String deviceId,
                                              @RequestParam(defaultValue = "0") String cursor,
                                              @RequestParam(defaultValue = "500") int limit) {
        SyncDevice.requireMatchingHeader(jwt, deviceId);
        return ApiRes.success(200, pull.pull(jwt.getSubject(), deviceId, cursor, limit));
    }
}
