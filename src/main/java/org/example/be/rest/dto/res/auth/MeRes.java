package org.example.be.rest.dto.res.auth;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.example.be.core.entity.UserEntity;

@Data
@Builder
public class MeRes {

    String id;
    String email;
    String displayName;
    String timezone;
    LocalDateTime createdAt;
    NotificationSettingsRes notificationSettings;

    public static MeRes from(UserEntity user) {
        return MeRes.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .timezone(user.getTimezone())
                .createdAt(user.getCreatedAt())
                .notificationSettings(NotificationSettingsRes.builder()
                        .bucketAlerts(user.isNotifyBucketAlerts())
                        .groupAlerts(user.isNotifyGroupAlerts())
                        .build())
                .build();
    }

}
