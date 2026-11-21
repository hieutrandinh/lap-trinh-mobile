package org.example.be.rest.usecase.report;

import org.example.be.rest.dto.res.report.OverviewRes;

public interface ReportOverviewUseCase {

    OverviewRes execute(String userId, String month);

}
