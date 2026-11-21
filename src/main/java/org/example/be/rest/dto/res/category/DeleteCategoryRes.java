package org.example.be.rest.dto.res.category;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteCategoryRes {

    String deletedCategoryId;
    long movedExpenseCount;
    CategoryRefRes movedTo;
    List<String> affectedBucketIds;

}
