package org.example.be.rest.usecase.correction;

import org.example.be.rest.dto.res.correction.CorrectionRequestRes;

public interface WithdrawCorrectionUseCase {

    CorrectionRequestRes execute(String userId, String groupId, String correctionId);

}
