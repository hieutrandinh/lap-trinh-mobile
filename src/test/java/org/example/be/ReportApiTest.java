package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ReportApiTest extends AbstractApiTest {

    @Test
    void overviewAndSpendingByCategoryFollowTheApiContract() throws Exception {
        Account me = registerUser("report");
        String month = "2026-10";

        String foodId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + foodId + "\",\"name\":\"Snacks\"}");
        String transportId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + transportId + "\",\"name\":\"Taxi\"}");

        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"income","name":"Salary","amount":"2000.00","categoryId":"%s",
                 "occurredAt":"2026-10-01T00:00:00Z"}
                """.formatted(UUID.randomUUID(), foodId));
        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Groceries","amount":"60.00","categoryId":"%s",
                 "occurredAt":"2026-10-01T00:00:00Z"}
                """.formatted(UUID.randomUUID(), foodId));
        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Bus","amount":"15.00","categoryId":"%s",
                 "occurredAt":"2026-10-31T23:59:59Z"}
                """.formatted(UUID.randomUUID(), transportId));
        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Previous groceries","amount":"50.00","categoryId":"%s",
                 "occurredAt":"2026-09-30T23:59:59Z"}
                """.formatted(UUID.randomUUID(), foodId));
        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Next bus","amount":"1.00","categoryId":"%s",
                 "occurredAt":"2026-11-01T00:00:00Z"}
                """.formatted(UUID.randomUUID(), transportId));

        JsonNode overview = request(200, "GET", "/reports/overview?month=" + month, me.device(), me.token(), null,
                null);
        assertThat(overview.path("currency").asText()).isEqualTo("USD");
        assertThat(overview.path("monthlyIncome").asText()).isEqualTo("2000.00");
        assertThat(overview.path("monthlySpending").asText()).isEqualTo("75.00");
        assertThat(overview.at("/groupSummary/youOwe").asText()).isEqualTo("0.00");
        assertThat(overview.path("buckets")).isEmpty();

        JsonNode byCategory = request(200, "GET", "/reports/spending-by-category?month=" + month, me.device(),
                me.token(), null, null);
        assertThat(byCategory.path("total").asText()).isEqualTo("75.00");
        assertThat(byCategory.path("items").get(0).path("amount").asText()).isEqualTo("60.00");
        assertThat(byCategory.path("compareTotal").isNull()).isTrue();

        String reportPath = "/reports/spending-by-category?month=" + month;
        JsonNode comparison = request(200, "GET", reportPath + "&compareMonth=2026-09",
                me.device(), me.token(), null, null);
        assertThat(comparison.path("compareTotal").asText()).isEqualTo("50.00");
        assertThat(comparison.path("changeAmount").asText()).isEqualTo("25.00");
        assertThat(comparison.path("changePercent").asDouble()).isEqualTo(50.0);
        assertThat(comparison.at("/items/0/compareAmount").asText()).isEqualTo("50.00");
        assertThat(comparison.at("/items/0/changeAmount").asText()).isEqualTo("10.00");
        assertThat(comparison.at("/items/1/compareAmount").asText()).isEqualTo("0.00");

        JsonNode emptyComparison = request(200, "GET", reportPath + "&compareMonth=2026-08",
                me.device(), me.token(), null, null);
        assertThat(emptyComparison.path("compareTotal").asText()).isEqualTo("0.00");
        assertThat(emptyComparison.path("changePercent").isNull()).isTrue();
        assertThat(request(422, "GET", reportPath + "&compareMonth=2026-13", me.device(), me.token(), null, null)
                .at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");

        JsonNode missingMonth = request(422, "GET", "/reports/spending-by-category", me.device(), me.token(), null,
                null);
        assertThat(missingMonth.at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");
    }

}
