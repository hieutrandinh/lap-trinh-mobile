package org.example.be.rest.usecase.bucket;

import java.math.BigDecimal;
import java.util.List;
import org.example.be.rest.dto.res.bucket.BucketPreviewRes;

public interface PreviewBucketUseCase {

    BucketPreviewRes execute(String userId, List<String> categoryIds, BigDecimal monthlyTarget, String bucketId);

}
