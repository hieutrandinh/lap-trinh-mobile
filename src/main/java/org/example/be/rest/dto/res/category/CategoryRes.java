package org.example.be.rest.dto.res.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.example.be.core.entity.CategoryEntity;

@Data
@Builder
public class CategoryRes {

    String id;
    String name;
    // Putting @JsonProperty on the field only renamed the field-sourced property and left
    // Jackson's getter-derived "system" property alongside it, so both appeared in the JSON.
    // Annotating the getter itself is the one Jackson actually uses to serialize this value.
    @Getter(onMethod_ = {@JsonProperty("isSystem")})
    boolean isSystem;
    long expenseCount;
    List<String> bucketIds;
    int version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static CategoryRes from(CategoryEntity category, long expenseCount, List<String> bucketIds) {
        return CategoryRes.builder()
                .id(category.getId())
                .name(category.getName())
                .isSystem(category.isSystem())
                .expenseCount(expenseCount)
                .bucketIds(bucketIds)
                .version(category.getVersion())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

}
