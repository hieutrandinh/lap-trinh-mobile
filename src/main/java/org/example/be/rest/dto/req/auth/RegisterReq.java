package org.example.be.rest.dto.req.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterReq {

    @NotBlank
    @Email
    @Size(max = 254)
    String email;

    @NotBlank
    @Size(min = 8, max = 72)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least 1 letter and 1 digit")
    String password;

    @NotBlank
    @Size(min = 1, max = 50)
    String displayName;

    String timezone;

    @NotBlank
    @Pattern(regexp = "ios|android|web")
    String platform;

}
