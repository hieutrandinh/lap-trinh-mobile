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
@Table(name = "tblgroupactivity")
@Getter
@Setter
@NoArgsConstructor
public class GroupActivityEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "activitytype", nullable = false, length = 40)
    String activityType;

    @Column(name = "targettype", length = 30)
    String targetType;

    @Column(name = "targetid", length = 36)
    String targetId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    String payload;

    @Column(name = "createdat", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "tblgroupid", nullable = false, length = 36)
    String groupId;

    @Column(name = "actoruserid", length = 36)
    String actorUserId;

}
