package org.example.be.rest.dto.res.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaginationRes {

    int page;
    int pageSize;
    long totalItems;
    int totalPages;

}
