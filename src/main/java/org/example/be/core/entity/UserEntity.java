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
@Table(name = "tbluser")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "email", nullable = false, length = 255)
    String email;

    @Column(name = "passwordhash", nullable = false, length = 255)
    String passwordHash;

    @Column(name = "displayname", nullable = false, length = 50)
    String displayName;

    @Column(name = "timezone", nullable = false, length = 64)
    String timezone;

    @Column(name = "notifybucketalerts", nullable = false)
    boolean notifyBucketAlerts;

    @Column(name = "notifygroupalerts", nullable = false)
    boolean notifyGroupAlerts;

    @Column(name = "createdat", nullable = false, updatable = false)
    LocalDateTime createdAt;

}
