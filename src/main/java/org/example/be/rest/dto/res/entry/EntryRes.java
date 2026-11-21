package org.example.be.rest.dto.res.entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
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
    // Named sharedFlag (not isShared) because Jackson derives "shared" as the getter-based
    // property name for isShared() by stripping "is" - the exact same implicit name as the
    // "shared" field below, and that collision made Jackson drop this value from the JSON
    // entirely. The disjoint field name avoids the collision; @JsonProperty then renames the
    // wire field to match the api-contract's isShared.
    @Getter(onMethod_ = {@JsonProperty("isShared")})
    boolean sharedFlag;
    SharedRefRes shared;
    int version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<AlertRes> alerts;

}
