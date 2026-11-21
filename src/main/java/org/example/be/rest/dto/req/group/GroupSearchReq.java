package org.example.be.rest.dto.req.group;

import lombok.Data;

@Data
public class GroupSearchReq {

    String accessMode;
    String q;
    String sort = "-createdAt";
    int page = 1;
    int pageSize = 20;

}
