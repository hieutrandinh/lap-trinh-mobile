package org.example.be.rest.usecase.bucket;

import org.example.be.rest.dto.res.bucket.BucketListRes;

public interface SearchBucketUseCase {

    BucketListRes execute(String userId, String month);

}
