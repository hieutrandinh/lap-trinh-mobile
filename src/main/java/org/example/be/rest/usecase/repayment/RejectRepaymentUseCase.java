package org.example.be.rest.usecase.repayment;

import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.res.repayment.RepaymentRes;

public interface RejectRepaymentUseCase {

    RepaymentRes execute(String userId, String groupId, String repaymentId, NoteReq req);

}
