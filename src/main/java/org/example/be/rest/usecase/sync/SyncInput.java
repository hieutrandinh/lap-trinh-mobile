package org.example.be.rest.usecase.sync;

import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
class SyncInput {
    private static final Set<String> TYPES = Set.of("entry", "category", "bucket");
    private final ObjectMapper json;
    private final Validator validator;

    static List<JsonNode> array(JsonNode value, String path, int min, int max) {
        if (!value.isArray() || value.size() < min) throw invalid(path);
        if (value.size() > max) throw new BusinessLogicException(ErrorCode.PAYLOAD_TOO_LARGE);
        var items = new ArrayList<JsonNode>(value.size());
        for (JsonNode item : value) {
            if (!item.isObject()) throw invalid(path + "[" + items.size() + "]");
            items.add(item);
        }
        return items;
    }

    static String text(JsonNode value, String path) {
        if (!value.isString() || value.asText().isBlank()) throw invalid(path);
        return value.asText();
    }

    static String id(JsonNode value, String path) {
        String id = text(value, path);
        if (!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            throw invalid(path);
        }
        return id;
    }

    static String type(JsonNode value, String path) {
        String type = text(value, path);
        if (!TYPES.contains(type)) throw invalid(path);
        return type;
    }

    ObjectNode withId(JsonNode node, String id) {
        ObjectNode copy = ((ObjectNode) node).deepCopy();
        copy.put("id", id);
        return copy;
    }

    <T> T bind(JsonNode node, Class<T> type, String path) {
        T request;
        try {
            request = json.treeToValue(node, type);
        } catch (JacksonException e) {
            throw invalid(path);
        }
        var violations = validator.validate(request).stream().map(violation -> Map.of(
                "field", path + "." + violation.getPropertyPath(), "issue", "INVALID_FORMAT",
                "message", violation.getMessage())).toList();
        if (!violations.isEmpty()) throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR, violations);
        return request;
    }

    static BusinessLogicException invalid(String path) {
        return new BusinessLogicException(ErrorCode.VALIDATION_ERROR,
                List.of(Map.of("field", path, "issue", "INVALID_FORMAT", "message", "Invalid value.")));
    }
}
