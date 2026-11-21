package org.example.be.rest.dto.res.bucket;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BucketPreviewRes {

    String month;
    String currency;
    BigDecimal spent;
    BigDecimal remaining;
    double progressPercent;
    long entryCount;

}
