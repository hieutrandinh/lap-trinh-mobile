package org.example.be.rest.dto.req.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateGroupReq {

    @NotBlank
    @Size(min = 1, max = 100)
    String name;

    @Size(max = 500)
    String description;

}
