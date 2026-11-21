package org.example.be.rest.dto.req.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameCategoryReq {

    @NotNull
    Integer version;

    @NotBlank
    @Size(min = 1, max = 50)
    String name;

    String expenseHandling = "keep";

    String replacementCategoryId;

}
