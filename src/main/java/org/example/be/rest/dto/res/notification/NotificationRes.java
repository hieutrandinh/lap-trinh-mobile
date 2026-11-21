package org.example.be.rest.dto.res.notification;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRes {

    String id;
    String type;
    String title;
    String body;
    Map<String, Object> payload;
    LocalDateTime readAt;
    LocalDateTime createdAt;

}
