package org.example.be.rest.dto.res.entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.category.CategoryRefRes;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntryRes {

    String id;
    String type;
    String name;
    BigDecimal amount;
    String currency;
    LocalDateTime occurredAt;
    CategoryRefRes category;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    CategoryRefRes bucket;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    String note;
    boolean isShared;
    SharedRefRes shared;
    int version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<AlertRes> alerts;

}
