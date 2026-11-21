package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.res.group.GroupRes;

public interface AcceptInvitationUseCase {

    GroupRes execute(String userId, String code);

}
