package org.example.be.rest.usecase.expense;

import org.example.be.rest.dto.req.expense.GroupExpenseSearchReq;
import org.example.be.rest.dto.res.expense.GroupExpenseListRes;

public interface SearchGroupExpenseUseCase {

    GroupExpenseListRes execute(String userId, String groupId, GroupExpenseSearchReq req);

}
