package org.example.be.rest.dto.res.group;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.common.PaginationRes;

@Data
@Builder
public class InvitationListRes {

    List<InvitationRes> data;
    PaginationRes pagination;

}
