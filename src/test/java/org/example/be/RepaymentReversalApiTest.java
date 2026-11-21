package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class RepaymentReversalApiTest extends AbstractApiTest {

    @Test
    void balancesConfirmRejectCancelAndReversalFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");

        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(), alice.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null, "");

        String expenseBody = """
                {"title":"Dinner","totalAmount":"100.00","category":"Food","payerUserId":"%s",
                 "splitMethod":"equal","participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId(), bob.userId());
        request(201, "POST", "/groups/" + groupId + "/expenses", alice.device(), alice.token(), "exp-1",
                expenseBody);

        JsonNode balances = request(200, "GET", "/groups/" + groupId + "/balances", alice.device(), alice.token(),
                null, null);
        var byUser = balances.path("members");
        assertThat(byUser).anySatisfy(m -> {
            assertThat(m.at("/user/userId").asText()).isEqualTo(alice.userId());
            assertThat(m.path("balance").asText()).isEqualTo("50.00");
            assertThat(m.path("position").asText()).isEqualTo("receives");
        });
        assertThat(balances.path("suggestedRepayments").get(0).at("/from/userId").asText())
                .isEqualTo(bob.userId());
        assertThat(balances.path("suggestedRepayments").get(0).path("amount").asText()).isEqualTo("50.00");

        JsonNode creditorSummary = request(200, "GET", "/reports/overview", alice.device(),
                alice.token(), null, null).path("groupSummary");
        assertThat(creditorSummary.path("youAreOwed").asText()).isEqualTo("50.00");
        assertThat(creditorSummary.path("youOwe").asText()).isEqualTo("0.00");
        assertThat(creditorSummary.at("/groups/0/groupId").asText()).isEqualTo(groupId);
        assertThat(creditorSummary.at("/groups/0/name").asText()).isEqualTo("Trip");
        assertThat(creditorSummary.at("/groups/0/balance").asText()).isEqualTo("50.00");

        JsonNode debtorSummary = request(200, "GET", "/reports/overview", bob.device(),
                bob.token(), null, null).path("groupSummary");
        assertThat(debtorSummary.path("youOwe").asText()).isEqualTo("50.00");
        assertThat(debtorSummary.path("youAreOwed").asText()).isEqualTo("0.00");
        assertThat(debtorSummary.at("/groups/0/balance").asText()).isEqualTo("-50.00");

        JsonNode tooMuch = request(422, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "rp-toomuch", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"999.00\"}");
        assertThat(tooMuch.at("/error/code").asText()).isEqualTo("REPAYMENT_EXCEEDS_DEBT");

        JsonNode selfRepay = request(422, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "rp-self", "{\"toUserId\":\"" + bob.userId() + "\",\"amount\":\"10.00\"}");
        assertThat(selfRepay.at("/error/code").asText()).isEqualTo("CANNOT_REPAY_SELF");

        JsonNode repayment = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "rp-1", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"50.00\",\"note\":\"bank transfer\"}");
        String repaymentId = repayment.path("id").asText();
        assertThat(repayment.path("status").asText()).isEqualTo("pending");

        JsonNode confirmed = request(200, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/confirm",
                alice.device(), alice.token(), null, "");
        assertThat(confirmed.path("status").asText()).isEqualTo("confirmed");

        JsonNode settled = request(200, "GET", "/groups/" + groupId + "/balances", bob.device(), bob.token(), null,
                null);
        assertThat(settled.path("members")).allSatisfy(m -> assertThat(m.path("balance").asText()).isEqualTo("0.00"));

        JsonNode wrongConfirmer = request(409, "POST",
                "/groups/" + groupId + "/repayments/" + repaymentId + "/confirm", alice.device(), alice.token(),
                null, "");
        assertThat(wrongConfirmer.at("/error/code").asText()).isEqualTo("INVALID_REPAYMENT_STATE");

        // Reversal: Bob (sender) requests it, Alice (receiver) approves -> balances restored.
        JsonNode reversal = request(201, "POST",
                "/groups/" + groupId + "/repayments/" + repaymentId + "/reversals", bob.device(), bob.token(), null,
                "{\"reason\":\"Paid by mistake\"}");
        String reversalId = reversal.path("id").asText();
        assertThat(reversal.path("status").asText()).isEqualTo("pending");

        JsonNode approverOnly = request(403, "POST", "/groups/" + groupId + "/reversals/" + reversalId + "/approve",
                bob.device(), bob.token(), null, "");
        assertThat(approverOnly.at("/error/code").asText()).isEqualTo("NOT_REVERSAL_APPROVER");

        JsonNode approved = request(200, "POST", "/groups/" + groupId + "/reversals/" + reversalId + "/approve",
                alice.device(), alice.token(), null, "");
        assertThat(approved.path("status").asText()).isEqualTo("approved");

        JsonNode detail = request(200, "GET", "/groups/" + groupId + "/repayments/" + repaymentId, alice.device(),
                alice.token(), null, null);
        assertThat(detail.path("status").asText()).isEqualTo("reversed");

        JsonNode restored = request(200, "GET", "/groups/" + groupId + "/balances", bob.device(), bob.token(), null,
                null);
        assertThat(restored.path("members")).anySatisfy(m -> {
            if (m.at("/user/userId").asText().equals(bob.userId())) {
                assertThat(m.path("balance").asText()).isEqualTo("-50.00");
            }
        });

        // Reject path on a fresh repayment.
        JsonNode second = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "rp-2", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"20.00\"}");
        JsonNode rejected = request(200, "POST",
                "/groups/" + groupId + "/repayments/" + second.path("id").asText() + "/reject", alice.device(),
                alice.token(), null, "{\"note\":\"Not received\"}");
        assertThat(rejected.path("status").asText()).isEqualTo("rejected");

        // Cancel path on a fresh repayment.
        JsonNode third = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "rp-3", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"15.00\"}");
        JsonNode cancelled = request(200, "POST",
                "/groups/" + groupId + "/repayments/" + third.path("id").asText() + "/cancel", bob.device(),
                bob.token(), null, "");
        assertThat(cancelled.path("status").asText()).isEqualTo("cancelled");

        JsonNode list = request(200, "GET", "/groups/" + groupId + "/repayments?status=rejected,cancelled,reversed",
                alice.device(), alice.token(), null, null);
        assertThat(list.path("data")).hasSize(3);
    }

}
