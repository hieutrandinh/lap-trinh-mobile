package org.example.be.rest.dto.req.repayment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReversalReq {

    @NotBlank
    @Size(min = 1, max = 500)
    String reason;

}
