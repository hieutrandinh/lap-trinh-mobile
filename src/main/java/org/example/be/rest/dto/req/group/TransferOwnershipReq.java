package org.example.be.rest.dto.req.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferOwnershipReq {

    @NotBlank
    String newOwnerUserId;

}
