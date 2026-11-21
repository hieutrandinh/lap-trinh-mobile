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
@Table(name = "tblentry")
@Getter
@Setter
@NoArgsConstructor
public class EntryEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "note", length = 500)
    String note;

    @Column(name = "version", nullable = false)
    int version;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updatedat", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "tbluserid", nullable = false, length = 36)
    String userId;

    @Column(name = "tblcategoryid", nullable = false, length = 36)
    String categoryId;

    @Column(name = "tblspendingbucketid", length = 36)
    String bucketId;

    @Column(name = "tblexpensesplitid", length = 36)
    String expenseSplitId;

}
