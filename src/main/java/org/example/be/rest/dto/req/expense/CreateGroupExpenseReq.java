package org.example.be.rest.dto.req.expense;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CreateGroupExpenseReq {

    @NotBlank
    @Size(min = 1, max = 100)
    String title;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal totalAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime occurredAt;

    @NotBlank
    @Size(min = 1, max = 50)
    String category;

    @NotBlank
    String payerUserId;

    @NotBlank
    @Pattern(regexp = "equal|exact")
    String splitMethod;

    @Size(min = 1, max = 50)
    List<ParticipantReq> participants;

    @Size(max = 500)
    String note;

}
