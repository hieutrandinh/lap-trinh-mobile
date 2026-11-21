package org.example.be.rest.usecase.auth;

import org.example.be.rest.dto.req.auth.LoginReq;
import org.example.be.rest.dto.res.auth.AuthRes;

public interface LoginUseCase {

    AuthRes execute(String deviceId, LoginReq req);

}
