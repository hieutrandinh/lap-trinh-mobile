package org.example.be.rest.usecase.repayment;

import org.example.be.rest.dto.res.repayment.ReversalRes;

public interface ApproveReversalUseCase {

    ReversalRes execute(String userId, String groupId, String reversalId);

}
