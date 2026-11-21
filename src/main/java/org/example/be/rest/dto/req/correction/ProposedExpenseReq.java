package org.example.be.rest.dto.req.correction;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;
import org.example.be.rest.dto.req.expense.ParticipantReq;

@Data
public class ProposedExpenseReq {

    @Size(min = 1, max = 100)
    String title;

    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal totalAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime occurredAt;

    @Size(min = 1, max = 50)
    String category;

    String payerUserId;

    @Pattern(regexp = "equal|exact")
    String splitMethod;

    @Size(max = 50)
    List<ParticipantReq> participants;

}
