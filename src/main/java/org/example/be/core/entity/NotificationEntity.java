package org.example.be.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblnotification")
@Getter
@Setter
@NoArgsConstructor
public class NotificationEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "notificationtype", nullable = false, length = 40)
    String notificationType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    String payload;

    @Column(name = "readat")
    LocalDateTime readAt;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "tbluserid", nullable = false, length = 36)
    String userId;

}
