package org.example.be.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblexpensesplit")
@Getter
@Setter
@NoArgsConstructor
public class ExpenseSplitEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(name = "roundingadjustment", nullable = false, precision = 12, scale = 2)
    BigDecimal roundingAdjustment;

    @Column(name = "tblgroupexpenseid", nullable = false, length = 36)
    String groupExpenseId;

    @Column(name = "tbluserid", nullable = false, length = 36)
    String userId;

}
