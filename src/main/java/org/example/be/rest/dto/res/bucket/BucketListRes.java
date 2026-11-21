package org.example.be.rest.dto.res.bucket;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BucketListRes {

    String month;
    List<BucketRes> data;

}
