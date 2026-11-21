package org.example.be.rest.dto.res.auth;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.example.be.core.entity.UserEntity;

@Data
@Builder
public class UserRes {

    String id;
    String email;
    String displayName;
    String timezone;
    LocalDateTime createdAt;

    public static UserRes from(UserEntity user) {
        return UserRes.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .timezone(user.getTimezone())
                .createdAt(user.getCreatedAt())
                .build();
    }

}
