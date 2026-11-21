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
@Table(name = "tblcategory")
@Getter
@Setter
@NoArgsConstructor
public class CategoryEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "name", nullable = false, length = 50)
    String name;

    @Column(name = "issystem", nullable = false)
    boolean isSystem;

    @Column(name = "version", nullable = false)
    int version;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updatedat", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "tbluserid", nullable = false, length = 36)
    String userId;

}
