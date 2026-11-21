package org.example.be.rest.dto.req.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginReq {

    @NotBlank
    @Email
    String email;

    @NotBlank
    @Size(min = 1, max = 72)
    String password;

    @NotBlank
    @Pattern(regexp = "ios|android|web")
    String platform;

}
