package org.example.be.rest.dto.res.entry;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SharedRefRes {

    String groupId;
    String groupExpenseId;
    String groupName;
    String groupCategory;
    BigDecimal totalAmount;
    boolean canOpenGroup;
    String accessMode;
    boolean canDelete;

}
