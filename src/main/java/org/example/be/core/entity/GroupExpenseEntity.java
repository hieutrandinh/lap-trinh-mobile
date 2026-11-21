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
@Table(name = "tblgroupexpense")
@Getter
@Setter
@NoArgsConstructor
public class GroupExpenseEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "title", nullable = false, length = 100)
    String title;

    @Column(name = "totalamount", nullable = false, precision = 12, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "occurredat", nullable = false)
    LocalDateTime occurredAt;

    @Column(name = "category", nullable = false, length = 50)
    String category;

    @Column(name = "splitmethod", nullable = false, length = 10)
    String splitMethod;

    @Column(name = "note", length = 500)
    String note;

    @Column(name = "status", nullable = false, length = 10)
    String status;

    @Column(name = "version", nullable = false)
    int version;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updatedat", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "tblgroupid", nullable = false, length = 36)
    String groupId;

    @Column(name = "payeruserid", nullable = false, length = 36)
    String payerUserId;

    @Column(name = "creatoruserid", nullable = false, length = 36)
    String creatorUserId;

}
