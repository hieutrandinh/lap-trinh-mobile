package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.res.group.InvitationListRes;

public interface SearchInvitationUseCase {

    InvitationListRes execute(String userId, String groupId, String status, int page, int pageSize);

}
