package org.example.be.rest.usecase.group;

import org.example.be.rest.dto.req.group.TransferOwnershipReq;
import org.example.be.rest.dto.res.group.GroupRes;

public interface TransferOwnershipUseCase {

    GroupRes execute(String userId, String groupId, TransferOwnershipReq req);

}
