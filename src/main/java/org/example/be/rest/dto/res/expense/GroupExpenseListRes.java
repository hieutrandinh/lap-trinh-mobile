package org.example.be.rest.dto.res.expense;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.common.PaginationRes;

@Data
@Builder
public class GroupExpenseListRes {

    List<GroupExpenseRes> data;
    PaginationRes pagination;

}
