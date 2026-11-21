package org.example.be.rest.usecase.sync;

import java.util.ArrayList;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class SyncPushService {
    private final SyncPushTransactions transactions;

    public Map<String, Object> push(String userId, String deviceId, String key, JsonNode body) {
        var items = SyncInput.array(body.path("changes"), "changes", 1, 500);
        var changes = new ArrayList<SyncPushTransactions.Change>(items.size());
        for (int i = 0; i < items.size(); i++) changes.add(parse(items.get(i), "changes[" + i + "]"));
        var job = transactions.open(userId, deviceId, key, body);
        var results = new ArrayList<JsonNode>(items.size());
        for (int i = 0; i < changes.size(); i++) {
            var change = changes.get(i);
            try {
                results.add(transactions.apply(userId, deviceId, job, i, change));
            } catch (BusinessLogicException failure) {
                if (failure.getErrorCode() == ErrorCode.UNAUTHENTICATED) throw failure;
                results.add(transactions.reject(userId, job, i, change, failure));
            }
        }
        return Map.of("results", results, "cursor", job.cursor());
    }

    private static SyncPushTransactions.Change parse(JsonNode item, String path) {
        String id = SyncInput.id(item.path("recordId"), path + ".recordId");
        String type = SyncInput.type(item.path("recordType"), path + ".recordType");
        String operation = SyncInput.text(item.path("operation"), path + ".operation");
        if (!"upsert".equals(operation) && !"delete".equals(operation)) throw SyncInput.invalid(path + ".operation");
        JsonNode value = item.path("baseVersion");
        if (!value.isNull() && (!value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() < 1)) {
            throw SyncInput.invalid(path + ".baseVersion");
        }
        JsonNode data = item.path("data");
        if ("upsert".equals(operation) && !data.isObject()) throw SyncInput.invalid(path + ".data");
        return new SyncPushTransactions.Change(id, type, operation, value.isNull() ? null : value.asInt(), data);
    }
}
