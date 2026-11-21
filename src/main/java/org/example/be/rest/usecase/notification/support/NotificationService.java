package org.example.be.rest.usecase.notification.support;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.NotificationEntity;
import org.example.be.core.entity.UserEntity;
import org.example.be.rest.repository.NotificationJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.springframework.stereotype.Service;

/**
 * Central point for creating notifications so 12.4's settings actually take effect:
 * groupAlerts=false suppresses every group notification, bucketAlerts=false suppresses
 * bucket_80/bucket_exceeded. tblBucketAlert dedup rows are written by the caller
 * regardless (12.4: turning alerts back on must not resend missed ones).
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

    NotificationJpaRepository notificationJpaRepository;
    UserJpaRepository userJpaRepository;

    public void notifyGroup(String userId, String type, String payloadJson) {
        if (userJpaRepository.findById(userId).map(UserEntity::isNotifyGroupAlerts).orElse(true)) {
            save(userId, type, payloadJson);
        }
    }

    public void notifyBucketAlert(String userId, String type, String payloadJson) {
        if (userJpaRepository.findById(userId).map(UserEntity::isNotifyBucketAlerts).orElse(true)) {
            save(userId, type, payloadJson);
        }
    }

    private void save(String userId, String type, String payloadJson) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setNotificationType(type);
        notification.setPayload(payloadJson);
        notification.setCreatedAt(LocalDateTime.now());
        notificationJpaRepository.save(notification);
    }

}
