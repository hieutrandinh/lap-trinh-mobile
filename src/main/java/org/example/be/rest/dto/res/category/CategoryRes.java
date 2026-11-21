package org.example.be.rest.dto.res.category;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.core.entity.CategoryEntity;

@Data
@Builder
public class CategoryRes {

    String id;
    String name;
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
