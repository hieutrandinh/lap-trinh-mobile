package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class BucketApiTest extends AbstractApiTest {

    @Test
    void previewAndCreateIncludeExistingSpendingAndSyncDeleteUnassignsIt() throws Exception {
        Account me = registerUser("bucket-sync");
        String categoryId = UUID.randomUUID().toString();
        String entryId = UUID.randomUUID().toString();
        String bucketId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null, """
                {"id":"%s","name":"Existing spending"}
                """.formatted(categoryId));
        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Lunch","amount":"30.00","categoryId":"%s"}
                """.formatted(entryId, categoryId));

        JsonNode preview = request(200, "GET",
                "/buckets/preview?categoryIds=" + categoryId + "&monthlyTarget=20.00",
                me.device(), me.token(), null, null);
        assertThat(preview.path("spent").asText()).isEqualTo("30.00");
        assertThat(preview.path("remaining").asText()).isEqualTo("-10.00");
        assertThat(preview.path("entryCount").asInt()).isOne();
        assertThat(preview.path("progressPercent").asDouble()).isEqualTo(150.0);

        JsonNode bucket = request(201, "POST", "/buckets", me.device(), me.token(), null, """
                {"id":"%s","name":"Lunch budget","monthlyTarget":"20.00","categoryIds":["%s"]}
                """.formatted(bucketId, categoryId));
        assertThat(bucket.path("spent").asText()).isEqualTo("30.00");
        JsonNode entry = request(200, "GET", "/entries", me.device(), me.token(), null, null).at("/data/0");
        assertThat(entry.path("id").asText()).isEqualTo(entryId);
        assertThat(entry.at("/bucket/id").asText()).isEqualTo(bucketId);
        assertThat(entry.path("version").asInt()).isEqualTo(2);

        String cursor = request(200, "GET", "/sync/changes", me.device(), me.token(), null, null)
                .path("nextCursor").asText();
        JsonNode removed = request(200, "POST", "/sync/push", me.device(), me.token(), "delete-budget", """
                {"changes":[{"recordType":"bucket","recordId":"%s","operation":"delete","baseVersion":1}]}
                """.formatted(bucketId));
        assertThat(removed.at("/results/0/status").asText()).isEqualTo("applied");
        request(404, "GET", "/buckets/" + bucketId, me.device(), me.token(), null, null);
        JsonNode changes = request(200, "GET", "/sync/changes?cursor=" + cursor, me.device(), me.token(), null, null);
        assertThat(changes.at("/deletions/0/recordId").asText()).isEqualTo(bucketId);
        assertThat(changes.at("/deletions/0/recordType").asText()).isEqualTo("bucket");
        assertThat(changes.at("/upserts/entries/0/id").asText()).isEqualTo(entryId);
        assertThat(changes.at("/upserts/entries/0/version").asInt()).isEqualTo(3);
        assertThat(changes.at("/upserts/entries/0/bucket").isNull()).isTrue();
    }

    @Test
    void createMatchPreviewAndUpdateFollowTheApiContract() throws Exception {
        Account me = registerUser("bucket");

        String categoryA = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + categoryA + "\",\"name\":\"Groceries\"}");
        String categoryB = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + categoryB + "\",\"name\":\"Streaming\"}");

        // No bucket matches yet -> suggestion "none".
        JsonNode noMatch = request(200, "GET", "/buckets/matching?categoryId=" + categoryA, me.device(), me.token(),
                null, null);
        assertThat(noMatch.path("suggestion").asText()).isEqualTo("none");
        assertThat(noMatch.path("matchCount").asInt()).isZero();

        JsonNode preview = request(200, "GET",
                "/buckets/preview?categoryIds=" + categoryA + "&monthlyTarget=100.00", me.device(), me.token(),
                null, null);
        assertThat(preview.path("spent").asText()).isEqualTo("0.00");
        assertThat(preview.path("remaining").asText()).isEqualTo("100.00");

        String bucketId = UUID.randomUUID().toString();
        JsonNode created = request(201, "POST", "/buckets", me.device(), me.token(), null, """
                {"id":"%s","name":"Grocery budget","monthlyTarget":"100.00","categoryIds":["%s"]}
                """.formatted(bucketId, categoryA));
        assertThat(created.path("status").asText()).isEqualTo("under");
        assertThat(created.path("categories")).hasSize(1);

        JsonNode oneMatch = request(200, "GET", "/buckets/matching?categoryId=" + categoryA, me.device(), me.token(),
                null, null);
        assertThat(oneMatch.path("suggestion").asText()).isEqualTo("auto");
        assertThat(oneMatch.path("buckets").get(0).path("id").asText()).isEqualTo(bucketId);

        JsonNode detail = request(200, "GET", "/buckets/" + bucketId, me.device(), me.token(), null, null);
        assertThat(detail.path("name").asText()).isEqualTo("Grocery budget");

        String expenseId = UUID.randomUUID().toString();
        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Subscription","amount":"12.00","categoryId":"%s"}
                """.formatted(expenseId, categoryB));

        // Replace categoryIds with B only -> A removed, B added.
        JsonNode updated = request(200, "PATCH", "/buckets/" + bucketId, me.device(), me.token(), null, """
                {"version":1,"categoryIds":["%s"]}
                """.formatted(categoryB));
        assertThat(updated.path("version").asInt()).isEqualTo(2);
        assertThat(updated.path("categories")).hasSize(1);
        assertThat(updated.at("/categories/0/id").asText()).isEqualTo(categoryB);
        assertThat(updated.path("spent").asText()).isEqualTo("12.00");
        JsonNode assigned = request(200, "GET", "/entries?bucketId=" + bucketId, me.device(),
                me.token(), null, null);
        assertThat(assigned.path("data")).hasSize(1);
        assertThat(assigned.at("/data/0/id").asText()).isEqualTo(expenseId);

        JsonNode staleUpdate = request(409, "PATCH", "/buckets/" + bucketId, me.device(), me.token(), null,
                """
                {"version":1,"name":"Stale name"}
                """);
        assertThat(staleUpdate.at("/error/code").asText()).isEqualTo("VERSION_CONFLICT");
        assertThat(staleUpdate.at("/error/current/version").asInt()).isEqualTo(2);

        JsonNode noLongerMatches = request(200, "GET", "/buckets/matching?categoryId=" + categoryA, me.device(),
                me.token(), null, null);
        assertThat(noLongerMatches.path("suggestion").asText()).isEqualTo("none");

        JsonNode listed = request(200, "GET", "/buckets", me.device(), me.token(), null, null);
        assertThat(listed.path("data")).hasSize(1);
    }

    @Test
    void createAndUpdateRejectNullCategoryIdsWithoutChangingBuckets() throws Exception {
        Account me = registerUser("bucket-validation");
        String bucketId = UUID.randomUUID().toString();
        String createBody = """
                {"id":"%s","name":"Budget","monthlyTarget":"20.00","categoryIds":[null]}
                """.formatted(bucketId);

        JsonNode invalidCreate = request(422, "POST", "/buckets", me.device(), me.token(), null, createBody);
        assertThat(invalidCreate.at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");
        request(201, "POST", "/buckets", me.device(), me.token(), null, createBody.replace("[null]", "[]"));

        String path = "/buckets/" + bucketId;
        JsonNode invalidUpdate = request(422, "PATCH", path, me.device(), me.token(), null, """
                {"version":1,"name":"Changed","categoryIds":[null]}
                """);
        assertThat(invalidUpdate.at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");
        JsonNode unchanged = request(200, "GET", path, me.device(), me.token(), null, null);
        assertThat(unchanged.path("name").asText()).isEqualTo("Budget");
        assertThat(unchanged.path("version").asInt()).isEqualTo(1);
    }

    @Test
    void anotherAccountCannotReadOrUpdatePrivateBuckets() throws Exception {
        Account owner = registerUser("bucket-owner");
        Account other = registerUser("bucket-other");
        String categoryId = UUID.randomUUID().toString();
        String bucketId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", owner.device(), owner.token(), null,
                """
                {"id":"%s","name":"Groceries"}
                """.formatted(categoryId));
        request(201, "POST", "/buckets", owner.device(), owner.token(), null, """
                {"id":"%s","name":"Private bucket","monthlyTarget":"100.00","categoryIds":["%s"]}
                """.formatted(bucketId, categoryId));

        JsonNode list = request(200, "GET", "/buckets", other.device(), other.token(), null, null);
        assertThat(list.path("data")).isEmpty();
        String path = "/buckets/" + bucketId;
        assertThat(request(404, "GET", path, other.device(), other.token(), null, null)
                .at("/error/code").asText()).isEqualTo("NOT_FOUND");
        assertThat(request(404, "PATCH", path, other.device(), other.token(), null,
                """
                {"version":1,"name":"Unauthorized change"}
                """).at("/error/code").asText()).isEqualTo("NOT_FOUND");

        JsonNode detail = request(200, "GET", path, owner.device(), owner.token(), null, null);
        assertThat(detail.path("name").asText()).isEqualTo("Private bucket");
        assertThat(detail.path("version").asInt()).isEqualTo(1);
    }

}
