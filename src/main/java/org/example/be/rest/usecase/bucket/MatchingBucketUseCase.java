package org.example.be.rest.usecase.bucket;

import org.example.be.rest.dto.res.bucket.BucketMatchingRes;

public interface MatchingBucketUseCase {

    BucketMatchingRes execute(String userId, String categoryId);

}
