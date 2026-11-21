package org.example.be.rest.dto.req.entry;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import org.example.be.core.util.JsonNullable;

@Data
public class CreateEntryReq {

    @NotBlank
    String id;

    @NotBlank
    @Pattern(regexp = "income|expense")
    String type;

    @NotBlank
    @Size(min = 1, max = 100)
    String name;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @DecimalMax("999999999.99")
    BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime occurredAt;

    @NotBlank
    String categoryId;

    JsonNullable<String> bucketId = JsonNullable.undefined();

    @Size(max = 500)
    String note;

}
