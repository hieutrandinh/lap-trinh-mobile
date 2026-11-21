package org.example.be.rest.dto.res.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import org.example.be.rest.dto.res.category.CategoryRefRes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyEntryRes {

    String entryId;
    CategoryRefRes category;
    int version;

}
