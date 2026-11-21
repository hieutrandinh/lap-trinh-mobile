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
@Table(name = "tblcorrectionrequest")
@Getter
@Setter
@NoArgsConstructor
public class CorrectionRequestEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "requesttype", nullable = false, length = 12)
    String requestType;

    @Column(name = "reason", length = 500)
    String reason;

    @Column(name = "status", nullable = false, length = 10)
    String status;

    @Column(name = "baseversion", nullable = false)
    int baseVersion;

    @Column(name = "previoustitle", nullable = false, length = 100)
    String previousTitle;

    @Column(name = "proposedtitle", length = 100)
    String proposedTitle;

    @Column(name = "previoustotalamount", nullable = false, precision = 12, scale = 2)
    BigDecimal previousTotalAmount;

    @Column(name = "proposedtotalamount", precision = 12, scale = 2)
    BigDecimal proposedTotalAmount;

    @Column(name = "previousoccurredat", nullable = false)
    LocalDateTime previousOccurredAt;

    @Column(name = "proposedoccurredat")
    LocalDateTime proposedOccurredAt;

    @Column(name = "previouscategory", nullable = false, length = 50)
    String previousCategory;

    @Column(name = "proposedcategory", length = 50)
    String proposedCategory;

    @Column(name = "previoussplitmethod", nullable = false, length = 10)
    String previousSplitMethod;

    @Column(name = "proposedsplitmethod", length = 10)
    String proposedSplitMethod;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "closedat")
    LocalDateTime closedAt;

    @Column(name = "tblgroupexpenseid", nullable = false, length = 36)
    String groupExpenseId;

    @Column(name = "requesteruserid", nullable = false, length = 36)
    String requesterUserId;

    @Column(name = "previouspayeruserid", nullable = false, length = 36)
    String previousPayerUserId;

    @Column(name = "proposedpayeruserid", length = 36)
    String proposedPayerUserId;

}
