package org.example.be.rest.dto.res.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupExpenseRes {

    String id;
    String groupId;
    String title;
    BigDecimal totalAmount;
    String currency;
    LocalDateTime occurredAt;
    String category;
    UserRefRes payer;
    UserRefRes createdBy;
    String splitMethod;
    List<SplitRes> splits;
    String note;
    String status;
    String pendingCorrectionId;
    BigDecimal myShare;
    int version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Detail-only (9.4); null/absent in list items (9.3) and the create response (9.2).
    String accessMode;
    List<Object> corrections;
    MyEntryRes myEntry;
    List<Object> relatedRepayments;

}
