package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.req.group.CreateGroupReq;
import org.example.be.rest.dto.res.group.GroupRes;

public interface CreateGroupUseCase {

    GroupRes execute(String userId, CreateGroupReq req);

}
