package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.req.group.GroupSearchReq;
import org.example.be.rest.dto.res.group.GroupListRes;

public interface SearchGroupUseCase {

    GroupListRes execute(String userId, GroupSearchReq req);

}
