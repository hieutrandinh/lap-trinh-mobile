package org.example.be.rest.usecase.repayment;

import org.example.be.rest.dto.req.repayment.CreateReversalReq;
import org.example.be.rest.dto.res.repayment.ReversalRes;

public interface CreateReversalUseCase {

    ReversalRes execute(String userId, String groupId, String repaymentId, CreateReversalReq req);

}
