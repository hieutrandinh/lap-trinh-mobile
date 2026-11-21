package org.example.be.rest.usecase.expense;

import org.example.be.rest.dto.req.expense.CreateGroupExpenseReq;
import org.example.be.rest.dto.res.expense.GroupExpenseRes;

public interface CreateGroupExpenseUseCase {

    GroupExpenseRes execute(String userId, String groupId, CreateGroupExpenseReq req);

}
