package org.example.be.rest.usecase.repayment.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.rest.dto.res.repayment.BalancesRes;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.repayment.GetBalancesUseCase;
import org.example.be.rest.usecase.repayment.support.BalanceCalculatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetBalancesUseCaseImpl implements GetBalancesUseCase {

    GroupAccessService groupAccessService;
    BalanceCalculatorService balanceCalculatorService;

    @Override
    @Transactional(readOnly = true)
    public BalancesRes execute(String userId, String groupId) {
        groupAccessService.requireAccess(groupId, userId);
        return balanceCalculatorService.computeGroupBalances(groupId);
    }

}
