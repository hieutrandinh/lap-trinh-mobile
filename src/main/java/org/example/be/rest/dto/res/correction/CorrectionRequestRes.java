package org.example.be.rest.dto.res.correction;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class CorrectionRequestRes {

    String id;
    String groupExpenseId;
    String type;
    UserRefRes requestedBy;
    String reason;
    String status;
    ExpenseValuesRes previous;
    ExpenseValuesRes proposed;
    List<ShareRes> shares;
    List<CorrectionBalanceEffectRes> balanceEffects;
    List<ApprovalRes> approvals;
    LocalDateTime createdAt;
    LocalDateTime closedAt;

}
