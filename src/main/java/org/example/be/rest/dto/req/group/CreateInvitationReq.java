package org.example.be.rest.dto.req.group;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateInvitationReq {

    @Min(1)
    @Max(720)
    Integer expiresInHours;

    @Min(1)
    @Max(100)
    Integer maxUses;

}
