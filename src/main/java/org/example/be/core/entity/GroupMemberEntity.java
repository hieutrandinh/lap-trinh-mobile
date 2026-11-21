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
@Table(name = "tblgroupmember")
@Getter
@Setter
@NoArgsConstructor
public class GroupMemberEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "role", nullable = false, length = 10)
    String role;

    @Column(name = "status", nullable = false, length = 10)
    String status;

    @Column(name = "joinedat", nullable = false)
    LocalDateTime joinedAt;

    @Column(name = "leftat")
    LocalDateTime leftAt;

    @Column(name = "hiddenat")
    LocalDateTime hiddenAt;

    @Column(name = "retainedgroupname", length = 100)
    String retainedGroupName;

    @Column(name = "snapshotcapturedat")
    LocalDateTime snapshotCapturedAt;

    @Column(name = "snapshotdata", columnDefinition = "text")
    String snapshotData;

    @Column(name = "tblgroupid", nullable = false, length = 36)
    String groupId;

    @Column(name = "tbluserid", nullable = false, length = 36)
    String userId;

}
