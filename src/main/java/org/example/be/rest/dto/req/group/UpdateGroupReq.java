package org.example.be.rest.dto.req.group;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.be.core.util.JsonNullable;

@Data
public class UpdateGroupReq {

    @Size(min = 1, max = 100)
    String name;

    JsonNullable<String> description = JsonNullable.undefined();

}
