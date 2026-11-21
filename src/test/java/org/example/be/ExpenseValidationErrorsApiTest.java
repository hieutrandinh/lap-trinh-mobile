package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ExpenseValidationErrorsApiTest extends AbstractApiTest {

    @Test
    void createExpenseValidationErrorsFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        Account outsider = registerUser("outsider");

        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(), alice.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null, "");

        String route = "/groups/" + groupId + "/expenses";

        // participants omitted entirely (not just empty) is the only way to reach the business-rule
        // check instead of bean validation's own @Size(min = 1).
        JsonNode noParticipants = request(422, "POST", route, alice.device(), alice.token(), "no-participants", """
                {"title":"Dinner","totalAmount":"40.00","category":"Food","payerUserId":"%s","splitMethod":"equal"}
                """.formatted(alice.userId()));
        assertThat(noParticipants.at("/error/code").asText()).isEqualTo("PARTICIPANTS_REQUIRED");

        JsonNode notMember = request(422, "POST", route, alice.device(), alice.token(), "not-member", """
                {"title":"Dinner","totalAmount":"40.00","category":"Food","payerUserId":"%s","splitMethod":"equal",
                 "participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId(), outsider.userId()));
        assertThat(notMember.at("/error/code").asText()).isEqualTo("NOT_ACTIVE_MEMBER");

        JsonNode duplicate = request(422, "POST", route, alice.device(), alice.token(), "duplicate", """
                {"title":"Dinner","totalAmount":"40.00","category":"Food","payerUserId":"%s","splitMethod":"equal",
                 "participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId(), alice.userId()));
        assertThat(duplicate.at("/error/code").asText()).isEqualTo("DUPLICATE_PARTICIPANT");

        JsonNode mismatch = request(422, "POST", route, alice.device(), alice.token(), "mismatch", """
                {"title":"Dinner","totalAmount":"40.00","category":"Food","payerUserId":"%s","splitMethod":"exact",
                 "participants":[{"userId":"%s","amount":"20.00"},{"userId":"%s","amount":"15.00"}]}
                """.formatted(alice.userId(), alice.userId(), bob.userId()));
        assertThat(mismatch.at("/error/code").asText()).isEqualTo("SPLIT_TOTAL_MISMATCH");
        assertThat(mismatch.at("/error/details/0/sum").asText()).isEqualTo("35.00");

        JsonNode amountNotAllowed = request(422, "POST", route, alice.device(), alice.token(), "amount-not-allowed",
                """
                {"title":"Dinner","totalAmount":"40.00","category":"Food","payerUserId":"%s","splitMethod":"equal",
                 "participants":[{"userId":"%s","amount":"20.00"},{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId(), bob.userId()));
        assertThat(amountNotAllowed.at("/error/code").asText()).isEqualTo("SPLIT_AMOUNT_NOT_ALLOWED");

        // Sanity check: a valid request on the same route still succeeds after all the rejected ones.
        JsonNode ok = request(201, "POST", route, alice.device(), alice.token(), "valid-one", """
                {"title":"Dinner","totalAmount":"40.00","category":"Food","payerUserId":"%s","splitMethod":"equal",
                 "participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId(), bob.userId()));
        assertThat(ok.path("status").asText()).isEqualTo("active");
    }

}
