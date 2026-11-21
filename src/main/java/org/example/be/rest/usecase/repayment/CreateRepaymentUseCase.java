package org.example.be.rest.usecase.repayment;

import org.example.be.rest.dto.req.repayment.CreateRepaymentReq;
import org.example.be.rest.dto.res.repayment.RepaymentRes;

public interface CreateRepaymentUseCase {

    RepaymentRes execute(String userId, String groupId, CreateRepaymentReq req);

}
