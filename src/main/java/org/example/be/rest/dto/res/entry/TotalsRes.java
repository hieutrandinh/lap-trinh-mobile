package org.example.be.rest.dto.res.entry;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TotalsRes {

    BigDecimal income;
    BigDecimal expense;
    String currency;

}
