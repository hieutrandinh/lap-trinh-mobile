package org.example.be.rest.usecase.bucket;

import org.example.be.rest.dto.req.bucket.CreateBucketReq;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.rest.usecase.CreationResult;

public interface CreateBucketUseCase {

    CreationResult<BucketRes> execute(String userId, CreateBucketReq req);

}
