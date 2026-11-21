package org.example.be.rest.dto.req.bucket;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CreateBucketReq {

    @NotBlank
    String id;

    @NotBlank
    @Size(min = 1, max = 50)
    String name;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal monthlyTarget;

    @Size(max = 50)
    List<String> categoryIds = List.of();

}
