package org.example.be.rest.usecase.auth;

import org.example.be.rest.dto.req.auth.LogoutReq;

public interface LogoutUseCase {

    void execute(String userId, String deviceId, LogoutReq req);

}
