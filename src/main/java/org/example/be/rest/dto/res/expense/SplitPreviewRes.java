package org.example.be.rest.dto.res.expense;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SplitPreviewRes {

    BigDecimal totalAmount;
    List<SplitRes> splits;
    BigDecimal roundingAdjustmentTotal;
    List<BalanceEffectRes> balanceEffects;

}
