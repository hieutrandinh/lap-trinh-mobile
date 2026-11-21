package org.example.be.rest.dto.res.group;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvitationRes {

    String id;
    String code;
    String link;
    LocalDateTime expiresAt;
    Integer maxUses;
    int useCount;
    String status;
    UserRefRes createdBy;
    LocalDateTime createdAt;

}
