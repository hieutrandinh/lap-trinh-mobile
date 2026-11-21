package org.example.be.rest.dto.res.group;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupRes {

    String id;
    String name;
    String description;
    String status;
    String accessMode;
    String myRole;
    BigDecimal myBalance;
    String currency;
    int memberCount;
    LocalDateTime snapshotCapturedAt;
    LocalDateTime closedAt;
    LocalDateTime createdAt;

    // Detail-only (8.3); null/absent in list items (8.2) and write responses (8.1/8.4).
    List<GroupMemberRes> members;
    PendingActionsRes pendingActions;
    List<String> expenseCategories;

}
