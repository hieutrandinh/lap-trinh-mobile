package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.req.group.UpdateGroupReq;
import org.example.be.rest.dto.res.group.GroupRes;

public interface UpdateGroupUseCase {

    GroupRes execute(String userId, String groupId, UpdateGroupReq req);

}
