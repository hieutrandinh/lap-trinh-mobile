package org.example.be.rest.usecase.expense;

import org.example.be.rest.dto.res.expense.GroupExpenseRes;

public interface DetailGroupExpenseUseCase {

    GroupExpenseRes execute(String userId, String groupId, String expenseId);

}
