package org.example.be.rest.dto.res.group;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberRes {

    String userId;
    String displayName;
    String role;
    String status;
    LocalDateTime joinedAt;
    LocalDateTime leftAt;

}
