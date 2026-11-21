package org.example.be.rest.usecase.correction;

import org.example.be.rest.dto.req.correction.CorrectionSearchReq;
import org.example.be.rest.dto.res.correction.CorrectionListRes;

public interface SearchCorrectionUseCase {

    CorrectionListRes execute(String userId, String groupId, String expenseId, CorrectionSearchReq req);

}
