package org.example.be.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Read-only mapping of vGroupBalance (V1__init.sql section 4.1). Already correct with
 * zero rows of group expense/repayment data - it folds to 0.00 for every member until the
 * shared-expense/repayment modules (section 9-10) exist.
 */
@Entity
@Table(name = "vgroupbalance")
@IdClass(GroupBalanceId.class)
@Immutable
@Getter
@NoArgsConstructor
public class GroupBalanceView {

    @Id
    @Column(name = "tblgroupid", length = 36)
    String groupId;

    @Id
    @Column(name = "tbluserid", length = 36)
    String userId;

    @Column(name = "balance", precision = 12, scale = 2)
    BigDecimal balance;

}
