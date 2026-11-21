package org.example.be.rest.dto.res.correction;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class ApprovalRes {

    UserRefRes user;
    String status;
    LocalDateTime respondedAt;
    String rejectionNote;

}
