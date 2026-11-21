package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.res.group.GroupRes;

public interface CloseGroupUseCase {

    GroupRes execute(String userId, String groupId);

}
