package org.example.be.rest.usecase.me;

import org.example.be.rest.dto.res.auth.MeRes;

public interface GetMeUseCase {

    MeRes execute(String userId);

}
