package org.example.be.rest.dto.res.report;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpendingByCategoryRes {

    String month;
    String compareMonth;
    BigDecimal total;
    BigDecimal compareTotal;
    BigDecimal changeAmount;
    Double changePercent;
    List<CategorySpendingItemRes> items;

}
