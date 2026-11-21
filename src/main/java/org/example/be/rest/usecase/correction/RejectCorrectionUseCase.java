package org.example.be.rest.usecase.correction;

import org.example.be.rest.dto.req.repayment.NoteReq;
import org.example.be.rest.dto.res.correction.CorrectionRequestRes;

public interface RejectCorrectionUseCase {

    CorrectionRequestRes execute(String userId, String groupId, String correctionId, NoteReq req);

}
