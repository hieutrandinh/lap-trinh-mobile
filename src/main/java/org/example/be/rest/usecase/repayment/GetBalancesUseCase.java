package org.example.be.rest.usecase.repayment;

import org.example.be.rest.dto.res.repayment.BalancesRes;

public interface GetBalancesUseCase {

    BalancesRes execute(String userId, String groupId);

}
