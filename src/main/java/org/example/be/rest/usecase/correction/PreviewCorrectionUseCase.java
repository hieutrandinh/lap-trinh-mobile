package org.example.be.rest.usecase.correction;

import org.example.be.rest.dto.req.correction.CreateCorrectionReq;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;

public interface PreviewCorrectionUseCase {

    CorrectionRequestRes execute(String userId, String groupId, String expenseId, CreateCorrectionReq req);

}
