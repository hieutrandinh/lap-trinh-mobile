package org.example.be.rest.usecase.auth;

import org.example.be.rest.dto.req.auth.RefreshTokenReq;
import org.example.be.rest.dto.res.auth.RefreshRes;

public interface RefreshTokenUseCase {

    RefreshRes execute(String deviceId, RefreshTokenReq req);

}
