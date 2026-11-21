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
@Table(name = "tblrepayment")
@Getter
@Setter
@NoArgsConstructor
public class RepaymentEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(name = "paidat", nullable = false)
    LocalDateTime paidAt;

    @Column(name = "note", length = 500)
    String note;

    @Column(name = "status", nullable = false, length = 12)
    String status;

    @Column(name = "respondedat")
    LocalDateTime respondedAt;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "tblgroupid", nullable = false, length = 36)
    String groupId;

    @Column(name = "fromuserid", nullable = false, length = 36)
    String fromUserId;

    @Column(name = "touserid", nullable = false, length = 36)
    String toUserId;

}
