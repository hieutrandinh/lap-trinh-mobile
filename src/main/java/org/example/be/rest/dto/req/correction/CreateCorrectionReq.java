package org.example.be.rest.dto.req.correction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCorrectionReq {

    @NotBlank
    @Pattern(regexp = "correction|cancellation")
    String type;

    @Size(max = 500)
    String reason;

    @NotNull
    Integer baseVersion;

    ProposedExpenseReq proposed;

}
