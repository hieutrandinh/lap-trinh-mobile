package org.example.be.rest.dto.res.repayment;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepaymentRes {

    String id;
    String groupId;
    UserRefRes from;
    UserRefRes to;
    BigDecimal amount;
    String currency;
    LocalDateTime paidAt;
    String note;
    String status;
    LocalDateTime respondedAt;
    ReversalRes pendingReversal;
    LocalDateTime createdAt;

}
