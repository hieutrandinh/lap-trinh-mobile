package org.example.be.rest.dto.res.report;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.category.CategoryRefRes;

@Data
@Builder
public class CategorySpendingItemRes {

    CategoryRefRes category;
    BigDecimal amount;
    double percent;
    BigDecimal compareAmount;
    BigDecimal changeAmount;

}
