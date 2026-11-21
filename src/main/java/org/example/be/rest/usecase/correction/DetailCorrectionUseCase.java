package org.example.be.rest.usecase.correction;

import org.example.be.rest.dto.res.correction.CorrectionRequestRes;

public interface DetailCorrectionUseCase {

    CorrectionRequestRes execute(String userId, String groupId, String correctionId);

}
