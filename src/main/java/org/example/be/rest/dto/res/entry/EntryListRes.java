package org.example.be.rest.dto.res.entry;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.common.PaginationRes;

@Data
@Builder
public class EntryListRes {

    List<EntryRes> data;
    PaginationRes pagination;
    TotalsRes totals;

}
