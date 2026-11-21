package org.example.be.rest.usecase.expense;

import org.example.be.rest.dto.req.expense.SplitPreviewReq;
import org.example.be.rest.dto.res.expense.SplitPreviewRes;

public interface SplitPreviewUseCase {

    SplitPreviewRes execute(String userId, String groupId, SplitPreviewReq req);

}
