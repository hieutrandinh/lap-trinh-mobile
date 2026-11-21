package org.example.be.rest.usecase.report;

import org.example.be.rest.dto.res.report.SpendingByCategoryRes;

public interface ReportSpendingByCategoryUseCase {

    SpendingByCategoryRes execute(String userId, String month, String compareMonth);

}
