package org.example.be.rest.dto.req.activity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ActivitySearchReq {

    String category;
    String type;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime from;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime to;

    int page = 1;
    int pageSize = 20;

}
