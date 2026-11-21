package org.example.be.rest.dto.req.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenReq {

    @NotBlank
    String refreshToken;

}
