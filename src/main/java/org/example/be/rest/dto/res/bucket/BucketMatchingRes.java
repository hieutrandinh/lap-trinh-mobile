package org.example.be.rest.dto.res.bucket;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BucketMatchingRes {

    int matchCount;
    List<BucketRes> buckets;
    String suggestion;

}
