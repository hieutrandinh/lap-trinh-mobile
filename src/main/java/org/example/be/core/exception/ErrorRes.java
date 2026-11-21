package org.example.be.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorRes {

    Detail error;

    public static ErrorRes of(String code, String message) {
        return of(code, message, null);
    }

    public static ErrorRes of(String code, String message, Object details) {
        var detail = Detail.builder().code(code).message(message).requestId("req_" + UUID.randomUUID());
        if ("VERSION_CONFLICT".equals(code) && details instanceof Map<?, ?> values) {
            Object current = values.get("current");
            detail.current(current instanceof Number ? Map.of("version", current) : current);
            details = null;
        }
        List<?> items = details instanceof List<?> list ? list : details == null ? List.of() : List.of(details);
        return ErrorRes.builder().error(detail.details(items).build()).build();
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Detail {
        String code;
        String message;
        List<?> details;
        Object current;
        String requestId;
    }

}
