package org.example.be.rest.usecase.repayment;

import org.example.be.rest.dto.res.repayment.RepaymentRes;

public interface DetailRepaymentUseCase {

    RepaymentRes execute(String userId, String groupId, String repaymentId);

}
