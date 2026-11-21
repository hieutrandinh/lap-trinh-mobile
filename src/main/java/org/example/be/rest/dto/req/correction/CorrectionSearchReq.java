package org.example.be.rest.dto.req.correction;

import lombok.Data;

@Data
public class CorrectionSearchReq {

    String status;
    String sort = "createdAt";
    int page = 1;
    int pageSize = 20;

}
