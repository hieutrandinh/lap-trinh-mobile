package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.req.group.CreateInvitationReq;
import org.example.be.rest.dto.res.group.InvitationRes;

public interface CreateInvitationUseCase {

    InvitationRes execute(String userId, String groupId, CreateInvitationReq req);

}
