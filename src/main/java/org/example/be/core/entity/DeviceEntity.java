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
@Table(name = "tbldevice")
@Getter
@Setter
@NoArgsConstructor
public class DeviceEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "platform", nullable = false, length = 20)
    String platform;

    @Column(name = "lastsyncat")
    LocalDateTime lastSyncAt;

    @Column(name = "sync_cursor", columnDefinition = "text")
    String syncCursor;

    @Column(name = "signedoutat")
    LocalDateTime signedOutAt;

    @Column(name = "refreshtokenhash", length = 255)
    String refreshTokenHash;

    @Column(name = "refreshtokenexpiresat")
    LocalDateTime refreshTokenExpiresAt;

    @Column(name = "tbluserid", nullable = false, length = 36)
    String userId;

}
