package org.example.be.rest.dto.req.repayment;

import lombok.Data;

@Data
public class RepaymentSearchReq {

    String status;
    Boolean involvingMe;
    String sort = "-createdAt";
    int page = 1;
    int pageSize = 20;

}
