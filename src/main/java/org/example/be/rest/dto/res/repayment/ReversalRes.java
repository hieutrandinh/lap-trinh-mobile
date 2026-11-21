package org.example.be.rest.dto.res.repayment;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReversalRes {

    String id;
    String repaymentId;
    String reason;
    String status;
    String rejectionNote;
    UserRefRes requester;
    UserRefRes approver;
    LocalDateTime respondedAt;
    LocalDateTime createdAt;

}
