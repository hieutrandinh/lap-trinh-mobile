package org.example.be.rest.dto.res.group;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupMemberRes {

    String userId;
    String displayName;
    String role;
    String status;
    LocalDateTime joinedAt;
    LocalDateTime leftAt;

}
