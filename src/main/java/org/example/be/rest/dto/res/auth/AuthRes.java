package org.example.be.rest.dto.res.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.example.be.core.entity.UserEntity;

@Data
@Builder
public class AuthRes {

    UserRes user;
    String accessToken;
    long accessTokenExpiresIn;
    String refreshToken;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    OnlineSummaryRes onlineSummary;

    public static AuthRes of(UserEntity user, String accessToken, long accessTokenExpiresIn, String refreshToken) {
        return AuthRes.builder()
                .user(UserRes.from(user))
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshToken(refreshToken)
                .build();
    }

}
