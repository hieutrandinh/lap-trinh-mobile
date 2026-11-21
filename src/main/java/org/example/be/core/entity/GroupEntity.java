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
@Table(name = "tblgroup")
@Getter
@Setter
@NoArgsConstructor
public class GroupEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "description", length = 500)
    String description;

    @Column(name = "status", nullable = false, length = 10)
    String status;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "closedat")
    LocalDateTime closedAt;

    @Column(name = "creatoruserid", nullable = false, length = 36)
    String creatorUserId;

}
