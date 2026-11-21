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
@Table(name = "tbldeletionmarker")
@Getter
@Setter
@NoArgsConstructor
public class DeletionMarkerEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "recordtype", nullable = false, length = 20)
    String recordType;

    @Column(name = "recordid", nullable = false, length = 36)
    String recordId;

    @Column(name = "deletedat", nullable = false)
    LocalDateTime deletedAt;

    @Column(name = "expiresat", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "tbluserid", nullable = false, length = 36)
    String userId;

}
