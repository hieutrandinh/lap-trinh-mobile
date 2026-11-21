package org.example.be.rest.usecase.repayment;

import org.example.be.rest.dto.req.repayment.RepaymentSearchReq;
import org.example.be.rest.dto.res.repayment.RepaymentListRes;

public interface SearchRepaymentUseCase {

    RepaymentListRes execute(String userId, String groupId, RepaymentSearchReq req);

}
