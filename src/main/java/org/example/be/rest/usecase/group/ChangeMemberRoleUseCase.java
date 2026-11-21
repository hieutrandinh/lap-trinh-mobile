package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.req.group.ChangeRoleReq;
import org.example.be.rest.dto.res.group.GroupMemberRes;

public interface ChangeMemberRoleUseCase {

    GroupMemberRes execute(String userId, String groupId, String targetUserId, ChangeRoleReq req);

}
