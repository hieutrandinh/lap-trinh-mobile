package org.example.be.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Synchronize;

/**
 * Read-only mapping of the vEntryDetail view (V1__init.sql section 4.2):
 * unions personal entries and the caller's shared-expense splits so month-based
 * amount/category/bucket queries don't need to special-case either source.
 */
@Entity
@Table(name = "ventrydetail")
@Immutable
@Synchronize({"tblentry", "tblpersonalentry", "tblexpensesplit", "tblgroupexpense"})
@Getter
@NoArgsConstructor
public class EntryDetailView {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "tbluserid", length = 36)
    String userId;

    @Column(name = "tblcategoryid", length = 36)
    String categoryId;

    @Column(name = "tblspendingbucketid", length = 36)
    String bucketId;

    @Column(name = "entrytype", length = 10)
    String entryType;

    @Column(name = "name", length = 100)
    String name;

    @Column(name = "amount", precision = 12, scale = 2)
    BigDecimal amount;

    @Column(name = "occurredat")
    LocalDateTime occurredAt;

    @Column(name = "isshared")
    boolean shared;

    @Column(name = "tblgroupid", length = 36)
    String groupId;

    @Column(name = "note", length = 500)
    String note;

    @Column(name = "version")
    int version;

    @Column(name = "createdat")
    LocalDateTime createdAt;

    @Column(name = "updatedat")
    LocalDateTime updatedAt;

    @Column(name = "tblgroupexpenseid", length = 36)
    String groupExpenseId;

}
