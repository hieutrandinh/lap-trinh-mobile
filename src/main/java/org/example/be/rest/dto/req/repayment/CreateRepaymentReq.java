package org.example.be.rest.dto.req.repayment;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class CreateRepaymentReq {

    @NotBlank
    String toUserId;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime paidAt;

    @Size(max = 500)
    String note;

}
