package org.example.be.rest.usecase.notification.impl;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.NotificationEntity;
import org.example.be.rest.repository.NotificationJpaRepository;
import org.example.be.rest.usecase.notification.MarkNotificationReadUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent like the rest of this contract's "mark done" actions: re-reading an already-read notification is a no-op, not an error. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MarkNotificationReadUseCaseImpl implements MarkNotificationReadUseCase {

    NotificationJpaRepository notificationJpaRepository;

    @Override
    @Transactional
    public void execute(String userId, String notificationId) {
        notificationJpaRepository.findByIdAndUserId(notificationId, userId).ifPresent(n -> {
            if (n.getReadAt() == null) {
                n.setReadAt(LocalDateTime.now());
                notificationJpaRepository.save(n);
            }
        });
    }

}
