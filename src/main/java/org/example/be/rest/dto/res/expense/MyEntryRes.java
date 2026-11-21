package org.example.be.rest.dto.res.expense;

import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.category.CategoryRefRes;

@Data
@Builder
public class MyEntryRes {

    String entryId;
    CategoryRefRes category;
    int version;

}
