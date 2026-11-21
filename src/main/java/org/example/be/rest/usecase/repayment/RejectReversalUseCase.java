package org.example.be.rest.usecase.repayment;

import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.res.repayment.ReversalRes;

public interface RejectReversalUseCase {

    ReversalRes execute(String userId, String groupId, String reversalId, NoteReq req);

}
