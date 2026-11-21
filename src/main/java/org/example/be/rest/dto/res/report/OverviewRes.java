package org.example.be.rest.dto.res.report;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.bucket.BucketRes;

@Data
@Builder
public class OverviewRes {

    String month;
    String currency;
    BigDecimal monthlyIncome;
    BigDecimal monthlySpending;
    List<BucketRes> buckets;
    GroupSummaryRes groupSummary;

}
