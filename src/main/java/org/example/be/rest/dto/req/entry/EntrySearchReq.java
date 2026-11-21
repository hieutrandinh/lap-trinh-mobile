package org.example.be.rest.dto.req.entry;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class EntrySearchReq {

    String type;
    String categoryId;
    String bucketId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime from;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime to;

    String month;
    String q;
    Boolean isShared;
    String groupId;
    String sort = "-occurredAt";
    int page = 1;
    int pageSize = 20;

}
