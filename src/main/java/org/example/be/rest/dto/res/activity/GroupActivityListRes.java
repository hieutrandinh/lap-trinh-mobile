package org.example.be.rest.dto.res.activity;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.common.PaginationRes;

@Data
@Builder
public class GroupActivityListRes {

    List<GroupActivityRes> data;
    PaginationRes pagination;

}
