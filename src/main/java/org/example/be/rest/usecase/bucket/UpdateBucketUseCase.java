package org.example.be.rest.usecase.bucket;

import org.example.be.rest.dto.req.bucket.EditBucketReq;
import org.example.be.rest.dto.res.bucket.BucketRes;

public interface UpdateBucketUseCase {

    BucketRes execute(String userId, String bucketId, EditBucketReq req);

}
