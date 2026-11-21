package org.example.be.rest.dto.req.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class SplitPreviewReq {

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal totalAmount;

    @NotBlank
    @Pattern(regexp = "equal|exact")
    String splitMethod;

    @NotBlank
    String payerUserId;

    @Size(min = 1, max = 50)
    List<ParticipantReq> participants;

}
