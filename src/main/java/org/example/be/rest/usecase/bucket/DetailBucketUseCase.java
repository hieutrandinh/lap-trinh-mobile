package org.example.be.rest.usecase.bucket;

import org.example.be.rest.dto.res.bucket.BucketRes;

public interface DetailBucketUseCase {

    BucketRes execute(String userId, String bucketId, String month);

}
