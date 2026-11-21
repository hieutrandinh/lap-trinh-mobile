package org.example.be.rest.dto.res.correction;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.common.PaginationRes;

@Data
@Builder
public class CorrectionListRes {

    OriginalExpenseRes original;
    List<CorrectionRequestRes> data;
    PaginationRes pagination;

}
