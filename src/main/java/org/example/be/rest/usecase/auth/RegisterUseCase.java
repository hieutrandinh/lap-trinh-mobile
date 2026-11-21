package org.example.be.rest.usecase.auth;

import org.example.be.rest.dto.req.auth.RegisterReq;
import org.example.be.rest.dto.res.auth.AuthRes;

public interface RegisterUseCase {

    AuthRes execute(String deviceId, RegisterReq req);

}
