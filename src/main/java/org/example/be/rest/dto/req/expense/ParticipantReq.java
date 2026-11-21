package org.example.be.rest.dto.req.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ParticipantReq {

    @NotBlank
    String userId;

    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal amount;

}
