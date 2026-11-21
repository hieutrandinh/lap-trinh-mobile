package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BeApplicationTests {

    @Test
    void contextLoads() {
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void syncImportPushRetryAndDeleteFollowTheApiContract() throws Exception {
        String device = UUID.randomUUID().toString();
        JsonNode registration = request(201, "POST", "/auth/register", device, null, null, """
                {"email":"sync-%s@example.test","password":"TestPassword123","displayName":"Sync Test","platform":"web"}
                """.formatted(device));
        String token = registration.path("accessToken").asText();
        assertThat(token).isNotBlank();
        assertThat(registration.has("data")).isFalse();
        assertThat(registration.at("/user/createdAt").asText()).endsWith("Z");
        assertThat(json.readTree(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1])).path("did").asText())
                .isEqualTo(device);
        JsonNode report = request(200, "GET", "/reports/spending-by-category?month=2026-10", device, token, null, null);
        assertThat(report.path("total").asText()).isEqualTo("0.00");
        assertThat(report.path("compareTotal").isNull()).isTrue();

        String guestCategory = UUID.randomUUID().toString();
        String entry = UUID.randomUUID().toString();
        String reconcile = """
                {"guestWorkspace":{"categories":[{"id":"%s","name":"food"}],"buckets":[],"entries":[
                  {"id":"%s","type":"expense","name":"Coffee","amount":"4.50","occurredAt":"2026-10-30T08:10:00+07:00",
                   "categoryId":"%s","bucketId":null,"note":null}],"deletionMarkers":[]}}
                """.formatted(guestCategory, entry, guestCategory);
        JsonNode merged = request(200, "POST", "/sync/reconcile", device, token, "merge-1", reconcile);
        String category = merged.path("categoryIdMap").path(guestCategory).asText();
        assertThat(category).isNotBlank().isNotEqualTo(guestCategory);
        assertThat(merged.at("/summary/entriesAdded").asInt()).isOne();
        assertThat(request(200, "POST", "/sync/reconcile", device, token, "merge-1", reconcile)).isEqualTo(merged);

        JsonNode firstPage = request(200, "GET", "/sync/changes?limit=1", device, token, null, null);
        assertThat(firstPage.path("hasMore").asBoolean()).isTrue();
        JsonNode pulled = request(200, "GET", "/sync/changes?cursor=" + firstPage.path("nextCursor").asText(), device, token, null, null);
        assertThat(pulled.at("/upserts/entries/0/amount").asText()).isEqualTo("4.50");
        assertThat(pulled.at("/upserts/entries/0/occurredAt").asText()).isEqualTo("2026-10-30T01:10:00Z");
        String cursor = pulled.path("nextCursor").asText();
        String push = """
                {"changes":[
                  {"recordType":"entry","recordId":"%s","operation":"upsert","baseVersion":1,"data":{"name":"invalid"}},
                  {"recordType":"entry","recordId":"%s","operation":"upsert","baseVersion":1,"data":{
                    "type":"expense","name":"Coffee","amount":"5.00","occurredAt":"2026-10-30T01:10:00Z",
                    "categoryId":"%s","bucketId":null,"note":null}}]}
                """.formatted(entry, entry, category);
        JsonNode pushed = request(200, "POST", "/sync/push", device, token, "push-1", push);
        assertThat(pushed.at("/results/0/status").asText()).isEqualTo("rejected_invalid");
        assertThat(pushed.at("/results/1/version").asInt()).isEqualTo(2);
        assertThat(request(200, "POST", "/sync/push", device, token, "push-1", push)).isEqualTo(pushed);
        assertThat(request(409, "POST", "/sync/push", device, token, "push-1", push.replace("5.00", "6.00"))
                .at("/error/code").asText()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        JsonNode updated = request(200, "GET", "/sync/changes?cursor=" + cursor, device, token, null, null);
        assertThat(updated.at("/upserts/entries/0/amount").asText()).isEqualTo("5.00");

        request(204, "DELETE", "/entries/" + entry + "?version=2", device, token, null, null);
        JsonNode deletion = request(200, "GET", "/sync/changes?cursor=" + updated.path("nextCursor").asText(), device, token, null, null);
        assertThat(deletion.at("/deletions/0/recordId").asText()).isEqualTo(entry);

        String bucket = """
                {"id":"%s","name":"Food budget","monthlyTarget":"50.00","categoryIds":["%s"]}
                """.formatted(UUID.randomUUID(), category);
        JsonNode created = request(201, "POST", "/buckets", device, token, null, bucket);
        assertThat(created.at("/categories/0/id").asText()).isEqualTo(category);
        assertThat(request(200, "POST", "/buckets", device, token, null, bucket)).isEqualTo(created);
        assertThat(request(409, "POST", "/buckets", device, token, null, bucket.replace("50.00", "60.00"))
                .at("/error/code").asText()).isEqualTo("ID_ALREADY_EXISTS");
    }

    @Test
    void securityAndValidationReturnStructuredErrors() throws Exception {
        JsonNode error = request(401, "GET", "/me", null, null, null, null).path("error");
        assertThat(error.path("code").asText()).isEqualTo("UNAUTHENTICATED");
        assertThat(error.path("requestId").asText()).startsWith("req_");
        JsonNode invalid = request(422, "POST", "/auth/login", UUID.randomUUID().toString(), null, null, "{}");
        assertThat(invalid.at("/error/details/0/field").asText()).isNotBlank();
    }

    @Test
    void repeatedInvalidLoginsAreRateLimited() throws Exception {
        String device = UUID.randomUUID().toString();
        String body = """
                {"email":"missing-%s@example.test","password":"TestPassword123","platform":"web"}
                """.formatted(device);
        for (int i = 0; i < 5; i++) {
            assertThat(request(401, "POST", "/auth/login", device, null, null, body).at("/error/code").asText())
                    .isEqualTo("INVALID_CREDENTIALS");
        }
        assertThat(request(429, "POST", "/auth/login", device, null, null, body).at("/error/code").asText())
                .isEqualTo("TOO_MANY_LOGIN_ATTEMPTS");
    }

    @Test
    void sharedExpenseReplayKeepsOneRecordAndProtectsTheSharedEntry() throws Exception {
        String device = UUID.randomUUID().toString();
        JsonNode account = request(201, "POST", "/auth/register", device, null, null, """
                {"email":"group-%s@example.test","password":"TestPassword123","displayName":"Group Test","platform":"web"}
                """.formatted(device));
        String token = account.path("accessToken").asText();
        String user = account.at("/user/id").asText();
        String group = request(201, "POST", "/groups", device, token, null, "{\"name\":\"Travel\"}").path("id").asText();
        String route = "/groups/" + group + "/expenses";
        String body = """
                {"title":"Dinner","totalAmount":"10.00","category":"Food","payerUserId":"%s",
                 "splitMethod":"equal","participants":[{"userId":"%s"}]}
                """.formatted(user, user);
        JsonNode created = request(201, "POST", route, device, token, "expense-1", body);
        assertThat(request(201, "POST", route, device, token, "expense-1", body)).isEqualTo(created);
        assertThat(request(409, "POST", route, device, token, "expense-1", body.replace("10.00", "11.00"))
                .at("/error/code").asText()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(request(200, "GET", route, device, token, null, null).path("data").size()).isOne();
        JsonNode entry = request(200, "GET", "/entries", device, token, null, null).at("/data/0");
        assertThat(entry.at("/shared/groupName").asText()).isEqualTo("Travel");
        assertThat(entry.at("/shared/totalAmount").asText()).isEqualTo("10.00");
        assertThat(request(409, "DELETE", "/entries/" + entry.path("id").asText() + "?version=1", device, token, null, null)
                .at("/error/code").asText()).isEqualTo("SHARED_ENTRY_GROUP_IN_VIEW");
    }

    private JsonNode request(int status, String method, String path, String device, String token, String key, String body)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (device != null) builder.header("X-Device-Id", device);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (key != null) builder.header("Idempotency-Key", key);
        if (body != null) builder.header("Content-Type", "application/json");
        var response = http.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).withFailMessage("%s %s -> %s: %s", method, path, response.statusCode(), response.body())
                .isEqualTo(status);
        return status == 204 ? null : json.readTree(response.body());
    }

}
