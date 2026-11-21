package org.example.be.rest.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.notification.NotificationSearchReq;
import org.example.be.rest.dto.res.notification.MarkAllReadRes;
import org.example.be.rest.dto.res.notification.NotificationListRes;
import org.example.be.rest.usecase.notification.MarkAllNotificationsReadUseCase;
import org.example.be.rest.usecase.notification.MarkNotificationReadUseCase;
import org.example.be.rest.usecase.notification.SearchNotificationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    SearchNotificationUseCase searchNotificationUseCase;
    MarkNotificationReadUseCase markNotificationReadUseCase;
    MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;

    @GetMapping
    public ApiRes<NotificationListRes> search(@AuthenticationPrincipal Jwt jwt,
                                               @ModelAttribute NotificationSearchReq req) {
        return ApiRes.success(HttpStatus.OK.value(), searchNotificationUseCase.execute(jwt.getSubject(), req));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable String notificationId) {
        markNotificationReadUseCase.execute(jwt.getSubject(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ApiRes<MarkAllReadRes> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return ApiRes.success(HttpStatus.OK.value(), markAllNotificationsReadUseCase.execute(jwt.getSubject()));
    }

}
