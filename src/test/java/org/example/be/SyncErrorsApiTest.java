package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class SyncErrorsApiTest extends AbstractApiTest {

    @Test
    void pushRejectsMoreThanFiveHundredChanges() throws Exception {
        Account me = registerUser("sync-oversized");
        var changes = json.createArrayNode();
        for (int i = 0; i < 501; i++) {
            changes.addObject().put("recordType", "category").put("recordId", UUID.randomUUID().toString())
                    .put("operation", "upsert").putNull("baseVersion").putObject("data").put("name", "Category " + i);
        }
        String body = json.createObjectNode().set("changes", changes).toString();
        assertThat(request(413, "POST", "/sync/push", me.device(), me.token(), "oversized-push", body)
                .at("/error/code").asText()).isEqualTo("PAYLOAD_TOO_LARGE");
    }

    @Test
    void reconcileRejectsAnEntryWhoseCategoryIsMissingFromTheUpload() throws Exception {
        Account me = registerUser("syncref");

        String entryId = UUID.randomUUID().toString();
        String unknownCategoryId = UUID.randomUUID().toString();
        JsonNode result = request(422, "POST", "/sync/reconcile", me.device(), me.token(), "bad-ref-1", """
                {"guestWorkspace":{"categories":[],"buckets":[],"entries":[
                  {"id":"%s","type":"expense","name":"Mystery","amount":"1.00","occurredAt":"2026-10-01T00:00:00Z",
                   "categoryId":"%s","bucketId":null,"note":null}],"deletionMarkers":[]}}
                """.formatted(entryId, unknownCategoryId));
        assertThat(result.at("/error/code").asText()).isEqualTo("GUEST_REFERENCE_INVALID");
        assertThat(result.at("/error/details/0/field").asText()).contains("categoryId");
    }

    @Test
    void anOldCursorIsRejectedAsExpired() throws Exception {
        Account me = registerUser("syncexpired");

        long fortyDaysAgo = Instant.now().minusSeconds(40L * 24 * 3600).getEpochSecond();
        String expiredCursor = "0:0:" + fortyDaysAgo;

        JsonNode result = request(410, "GET", "/sync/changes?cursor=" + expiredCursor, me.device(), me.token(),
                null, null);
        assertThat(result.at("/error/code").asText()).isEqualTo("CURSOR_EXPIRED");
    }

    @Test
    void malformedCursorIsAValidationError() throws Exception {
        Account me = registerUser("syncmalformed");

        JsonNode result = request(422, "GET", "/sync/changes?cursor=not-a-cursor", me.device(), me.token(), null,
                null);
        assertThat(result.at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void deviceHeaderMustMatchTheTokensDevice() throws Exception {
        Account me = registerUser("syncdevice");
        String otherDevice = UUID.randomUUID().toString();

        JsonNode result = request(401, "GET", "/sync/changes", otherDevice, me.token(), null, null);
        assertThat(result.at("/error/code").asText()).isEqualTo("UNAUTHENTICATED");
    }

}
