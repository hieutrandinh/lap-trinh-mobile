package org.example.be.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblcorrectionparticipant")
@Getter
@Setter
@NoArgsConstructor
public class CorrectionParticipantEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "previousamount", precision = 12, scale = 2)
    BigDecimal previousAmount;

    @Column(name = "proposedamount", precision = 12, scale = 2)
    BigDecimal proposedAmount;

    @Column(name = "approvalstatus", length = 12)
    String approvalStatus;

    @Column(name = "respondedat")
    LocalDateTime respondedAt;

    @Column(name = "rejectionnote", length = 500)
    String rejectionNote;

    @Column(name = "tblcorrectionrequestid", nullable = false, length = 36)
    String correctionRequestId;

    @Column(name = "tbluserid", nullable = false, length = 36)
    String userId;

}
