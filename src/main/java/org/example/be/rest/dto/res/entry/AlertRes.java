package org.example.be.rest.dto.res.entry;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertRes {

    String bucketId;
    int threshold;

}
