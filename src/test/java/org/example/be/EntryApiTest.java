package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class EntryApiTest extends AbstractApiTest {

    @Test
    void createUpdateDeleteAndFilterFollowTheApiContract() throws Exception {
        Account me = registerUser("entry");

        String categoryId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + categoryId + "\",\"name\":\"Pets\"}");

        String incomeId = UUID.randomUUID().toString();
        JsonNode income = request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"income","name":"Salary","amount":"1000.00","categoryId":"%s"}
                """.formatted(incomeId, categoryId));
        assertThat(income.path("bucket").isNull() || income.path("bucket").isMissingNode()).isTrue();

        String bucketId = UUID.randomUUID().toString();
        request(201, "POST", "/buckets", me.device(), me.token(), null, """
                {"id":"%s","name":"Food budget","monthlyTarget":"200.00","categoryIds":["%s"]}
                """.formatted(bucketId, categoryId));

        String expenseId = UUID.randomUUID().toString();
        JsonNode expense = request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Lunch","amount":"12.50","categoryId":"%s"}
                """.formatted(expenseId, categoryId));
        assertThat(expense.at("/bucket/id").asText()).isEqualTo(bucketId);
        assertThat(expense.path("version").asInt()).isEqualTo(1);

        // A second bucket tracking the same category makes auto-assignment ambiguous.
        String bucket2Id = UUID.randomUUID().toString();
        request(201, "POST", "/buckets", me.device(), me.token(), null, """
                {"id":"%s","name":"Also food","monthlyTarget":"50.00","categoryIds":["%s"]}
                """.formatted(bucket2Id, categoryId));
        String ambiguousId = UUID.randomUUID().toString();
        JsonNode ambiguous = request(422, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Snack","amount":"3.00","categoryId":"%s"}
                """.formatted(ambiguousId, categoryId));
        assertThat(ambiguous.at("/error/code").asText()).isEqualTo("BUCKET_SELECTION_REQUIRED");

        JsonNode updated = request(200, "PATCH", "/entries/" + expenseId, me.device(), me.token(), null, """
                {"version":1,"name":"Lunch out","amount":"15.00"}
                """);
        assertThat(updated.path("name").asText()).isEqualTo("Lunch out");
        assertThat(updated.path("version").asInt()).isEqualTo(2);

        JsonNode staleUpdate = request(409, "PATCH", "/entries/" + expenseId, me.device(), me.token(), null,
                "{\"version\":1,\"name\":\"stale\"}");
        assertThat(staleUpdate.at("/error/code").asText()).isEqualTo("VERSION_CONFLICT");

        JsonNode listExpenses = request(200, "GET", "/entries?type=expense", me.device(), me.token(), null, null);
        assertThat(listExpenses.path("data")).hasSize(1);
        assertThat(listExpenses.at("/totals/expense").asText()).isEqualTo("15.00");
        assertThat(listExpenses.at("/totals/income").asText()).isEqualTo("0.00");

        JsonNode q = request(200, "GET", "/entries?q=Lunch", me.device(), me.token(), null, null);
        assertThat(q.path("data")).hasSize(1);

        request(204, "DELETE", "/entries/" + expenseId + "?version=2", me.device(), me.token(), null, null);
        JsonNode afterDelete = request(200, "GET", "/entries?type=expense", me.device(), me.token(), null, null);
        assertThat(afterDelete.path("data")).isEmpty();

        request(404, "DELETE", "/entries/" + expenseId + "?version=2", me.device(), me.token(), null, null);
    }

}
