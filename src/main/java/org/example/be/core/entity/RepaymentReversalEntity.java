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
@Table(name = "tblrepaymentreversal")
@Getter
@Setter
@NoArgsConstructor
public class RepaymentReversalEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "reason", nullable = false, length = 500)
    String reason;

    @Column(name = "status", nullable = false, length = 10)
    String status;

    @Column(name = "rejectionnote", length = 500)
    String rejectionNote;

    @Column(name = "respondedat")
    LocalDateTime respondedAt;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "tblrepaymentid", nullable = false, length = 36)
    String repaymentId;

    @Column(name = "requesteruserid", nullable = false, length = 36)
    String requesterUserId;

    @Column(name = "approveruserid", nullable = false, length = 36)
    String approverUserId;

}
