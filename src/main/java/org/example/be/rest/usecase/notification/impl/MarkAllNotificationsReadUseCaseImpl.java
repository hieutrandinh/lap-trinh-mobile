package org.example.be.rest.usecase.notification.impl;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.rest.dto.res.notification.MarkAllReadRes;
import org.example.be.rest.repository.NotificationJpaRepository;
import org.example.be.rest.usecase.notification.MarkAllNotificationsReadUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MarkAllNotificationsReadUseCaseImpl implements MarkAllNotificationsReadUseCase {

    NotificationJpaRepository notificationJpaRepository;

    @Override
    @Transactional
    public MarkAllReadRes execute(String userId) {
        int updated = notificationJpaRepository.markAllRead(userId, LocalDateTime.now());
        return MarkAllReadRes.builder().updated(updated).build();
    }

}
