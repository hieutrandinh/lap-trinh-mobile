package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class RepaymentSearchApiTest extends AbstractApiTest {

    @Test
    void searchFiltersAndReversalRejectFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");

        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(), alice.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null, "");

        request(201, "POST", "/groups/" + groupId + "/expenses", alice.device(), alice.token(), "exp-1", """
                {"title":"Dinner","totalAmount":"40.00","category":"Food","payerUserId":"%s",
                 "splitMethod":"equal","participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId(), bob.userId()));

        String confirmedId = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "rp-confirmed", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"10.00\"}").path("id")
                .asText();
        request(200, "POST", "/groups/" + groupId + "/repayments/" + confirmedId + "/confirm", alice.device(),
                alice.token(), null, "");

        String pendingId = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "rp-pending", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"5.00\"}").path("id").asText();

        JsonNode pendingOnly = request(200, "GET", "/groups/" + groupId + "/repayments?status=pending",
                alice.device(), alice.token(), null, null);
        assertThat(pendingOnly.path("data")).hasSize(1);
        assertThat(pendingOnly.path("data").get(0).path("id").asText()).isEqualTo(pendingId);

        JsonNode both = request(200, "GET", "/groups/" + groupId + "/repayments?status=pending,confirmed",
                alice.device(), alice.token(), null, null);
        assertThat(both.path("data")).hasSize(2);

        JsonNode involvingBob = request(200, "GET", "/groups/" + groupId + "/repayments?involvingMe=true",
                bob.device(), bob.token(), null, null);
        assertThat(involvingBob.path("data")).hasSize(2);

        JsonNode badStatus = request(422, "GET", "/groups/" + groupId + "/repayments?status=not-a-status",
                alice.device(), alice.token(), null, null);
        assertThat(badStatus.at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");

        // Reversal reject: the approver declines, the repayment stays confirmed.
        String reversalId = request(201, "POST",
                "/groups/" + groupId + "/repayments/" + confirmedId + "/reversals", bob.device(), bob.token(), null,
                "{\"reason\":\"Changed my mind\"}").path("id").asText();

        JsonNode wrongRejecter = request(403, "POST", "/groups/" + groupId + "/reversals/" + reversalId + "/reject",
                bob.device(), bob.token(), null, "{}");
        assertThat(wrongRejecter.at("/error/code").asText()).isEqualTo("NOT_REVERSAL_APPROVER");

        JsonNode rejected = request(200, "POST", "/groups/" + groupId + "/reversals/" + reversalId + "/reject",
                alice.device(), alice.token(), null, "{\"note\":\"Already settled, keep it confirmed\"}");
        assertThat(rejected.path("status").asText()).isEqualTo("rejected");
        assertThat(rejected.path("rejectionNote").asText()).isEqualTo("Already settled, keep it confirmed");

        JsonNode stillConfirmed = request(200, "GET", "/groups/" + groupId + "/repayments/" + confirmedId,
                alice.device(), alice.token(), null, null);
        assertThat(stillConfirmed.path("status").asText()).isEqualTo("confirmed");

        JsonNode alreadyHandled = request(409, "POST", "/groups/" + groupId + "/reversals/" + reversalId + "/reject",
                alice.device(), alice.token(), null, "{}");
        assertThat(alreadyHandled.at("/error/code").asText()).isEqualTo("INVALID_REVERSAL_STATE");
    }

}
