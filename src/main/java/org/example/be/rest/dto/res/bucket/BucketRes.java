package org.example.be.rest.dto.res.bucket;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.rest.dto.res.category.CategoryRefRes;

@Data
@Builder
public class BucketRes {

    String id;
    String name;
    BigDecimal monthlyTarget;
    String currency;
    List<CategoryRefRes> categories;
    String month;
    BigDecimal spent;
    BigDecimal remaining;
    double progressPercent;
    String status;
    int version;

    public static BucketRes of(SpendingBucketEntity bucket, List<CategoryRefRes> categories, String month,
                                BigDecimal spent) {
        BigDecimal remaining = bucket.getMonthlyTarget().subtract(spent);
        double progressPercent = bucket.getMonthlyTarget().signum() == 0 ? 0.0
                : spent.multiply(BigDecimal.valueOf(100))
                    .divide(bucket.getMonthlyTarget(), 1, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        String status = progressPercent >= 100 ? "over" : progressPercent >= 80 ? "near" : "under";
        return BucketRes.builder()
                .id(bucket.getId())
                .name(bucket.getName())
                .monthlyTarget(bucket.getMonthlyTarget())
                .currency("USD")
                .categories(categories)
                .month(month)
                .spent(spent)
                .remaining(remaining)
                .progressPercent(progressPercent)
                .status(status)
                .version(bucket.getVersion())
                .build();
    }

}
