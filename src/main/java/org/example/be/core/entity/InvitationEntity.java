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
@Table(name = "tblinvitation")
@Getter
@Setter
@NoArgsConstructor
public class InvitationEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "code", nullable = false, length = 12)
    String code;

    @Column(name = "maxuses")
    Integer maxUses;

    @Column(name = "usecount", nullable = false)
    int useCount;

    @Column(name = "expiresat", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "status", nullable = false, length = 10)
    String status;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "tblgroupid", nullable = false, length = 36)
    String groupId;

    @Column(name = "creatoruserid", nullable = false, length = 36)
    String creatorUserId;

}
