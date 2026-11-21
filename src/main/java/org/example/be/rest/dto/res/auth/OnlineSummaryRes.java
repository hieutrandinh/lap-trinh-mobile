package org.example.be.rest.dto.res.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnlineSummaryRes {

    long entryCount;
    long categoryCount;
    long bucketCount;

}
