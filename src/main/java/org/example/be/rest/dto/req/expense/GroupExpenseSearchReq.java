package org.example.be.rest.dto.req.expense;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class GroupExpenseSearchReq {

    String category;
    String payerUserId;
    String participantUserId;
    String status = "active";

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime from;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime to;

    Boolean hasPendingCorrection;
    String sort = "-occurredAt";
    int page = 1;
    int pageSize = 20;

}
