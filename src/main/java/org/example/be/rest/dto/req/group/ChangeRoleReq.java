package org.example.be.rest.dto.req.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangeRoleReq {

    @NotBlank
    @Pattern(regexp = "admin|member")
    String role;

}
