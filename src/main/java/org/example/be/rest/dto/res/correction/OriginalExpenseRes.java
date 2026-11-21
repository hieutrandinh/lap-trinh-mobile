package org.example.be.rest.dto.res.correction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.expense.SplitRes;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class OriginalExpenseRes {

    String title;
    BigDecimal totalAmount;
    LocalDateTime occurredAt;
    String category;
    UserRefRes payer;
    String splitMethod;
    List<SplitRes> splits;

}
