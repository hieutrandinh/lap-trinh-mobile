package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class CategoryBucketLimitsApiTest extends AbstractApiTest {

    @Test
    void bucketCreationRejectsAnUnknownCategoryAndEnforcesTheLimit() throws Exception {
        Account me = registerUser("bklimit");

        JsonNode categoryNotFound = request(422, "POST", "/buckets", me.device(), me.token(), null, """
                {"id":"%s","name":"Ghost budget","monthlyTarget":"10.00","categoryIds":["%s"]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        assertThat(categoryNotFound.at("/error/code").asText()).isEqualTo("CATEGORY_NOT_FOUND");

        // 5 default categories already exist (Food, Transport, Shopping, Bills, Salary) plus
        // Uncategorized, which doesn't count toward the limit either way; fill up to 100 buckets.
        for (int i = 0; i < 100; i++) {
            request(201, "POST", "/buckets", me.device(), me.token(), null, """
                    {"id":"%s","name":"Budget %s","monthlyTarget":"1.00"}
                    """.formatted(UUID.randomUUID(), i));
        }

        JsonNode overLimit = request(422, "POST", "/buckets", me.device(), me.token(), null, """
                {"id":"%s","name":"One too many","monthlyTarget":"1.00"}
                """.formatted(UUID.randomUUID()));
        assertThat(overLimit.at("/error/code").asText()).isEqualTo("BUCKET_LIMIT_REACHED");
    }

    @Test
    void categoryCreationEnforcesTheLimit() throws Exception {
        Account me = registerUser("catlimit");

        // 6 categories already exist after registration (5 defaults + Uncategorized); fill to 500.
        for (int i = 0; i < 494; i++) {
            request(201, "POST", "/categories", me.device(), me.token(), null, """
                    {"id":"%s","name":"Category %s"}
                    """.formatted(UUID.randomUUID(), i));
        }

        JsonNode overLimit = request(422, "POST", "/categories", me.device(), me.token(), null, """
                {"id":"%s","name":"One too many"}
                """.formatted(UUID.randomUUID()));
        assertThat(overLimit.at("/error/code").asText()).isEqualTo("CATEGORY_LIMIT_REACHED");
    }

    @Test
    void deleteAndRenameWithInvalidReplacementCategoryAreRejected() throws Exception {
        Account me = registerUser("replace");
        String categoryId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + categoryId + "\",\"name\":\"Removable\"}");
        String otherCategoryId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + otherCategoryId + "\",\"name\":\"Replacement target\"}");

        JsonNode selfReplacement = request(422, "DELETE",
                "/categories/" + categoryId + "?version=1&replacementCategoryId=" + categoryId, me.device(),
                me.token(), null, null);
        assertThat(selfReplacement.at("/error/code").asText()).isEqualTo("REPLACEMENT_INVALID");

        JsonNode unknownReplacement = request(422, "DELETE",
                "/categories/" + categoryId + "?version=1&replacementCategoryId=" + UUID.randomUUID(), me.device(),
                me.token(), null, null);
        assertThat(unknownReplacement.at("/error/code").asText()).isEqualTo("REPLACEMENT_INVALID");

        // expenseHandling = reassign with an explicit replacement moves existing entries there.
        String entryId = UUID.randomUUID().toString();
        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Something","amount":"9.00","categoryId":"%s"}
                """.formatted(entryId, categoryId));

        JsonNode reassigned = request(200, "PATCH", "/categories/" + categoryId, me.device(), me.token(), null, """
                {"version":1,"name":"Removable renamed","expenseHandling":"reassign","replacementCategoryId":"%s"}
                """.formatted(otherCategoryId));
        assertThat(reassigned.path("movedExpenseCount").asInt()).isEqualTo(1);
        assertThat(reassigned.at("/movedTo/id").asText()).isEqualTo(otherCategoryId);

        JsonNode entryAfter = request(200, "GET", "/entries", me.device(), me.token(), null, null);
        assertThat(entryAfter.path("data")).anySatisfy(e -> {
            if (e.path("id").asText().equals(entryId)) {
                assertThat(e.at("/category/id").asText()).isEqualTo(otherCategoryId);
            }
        });
    }

}
