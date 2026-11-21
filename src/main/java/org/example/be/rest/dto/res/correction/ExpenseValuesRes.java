package org.example.be.rest.dto.res.correction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class ExpenseValuesRes {

    String title;
    BigDecimal totalAmount;
    LocalDateTime occurredAt;
    String category;
    UserRefRes payer;
    String splitMethod;

}
