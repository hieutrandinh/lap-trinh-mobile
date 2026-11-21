package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class GroupSettlementErrorsApiTest extends AbstractApiTest {

    @Test
    void balanceNotSettledBlocksLeaving() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);
        createExpense(alice, groupId, "40.00", "bal-leave-1");

        JsonNode blocked = request(409, "POST", "/groups/" + groupId + "/leave", bob.device(), bob.token(), null,
                "");
        assertThat(blocked.at("/error/code").asText()).isEqualTo("BALANCE_NOT_SETTLED");
        assertThat(blocked.at("/error/details/0/balance").asText()).isEqualTo("-20.00");
    }

    @Test
    void groupNotSettledBlocksClosing() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);
        createExpense(alice, groupId, "40.00", "bal-close-1");

        JsonNode blocked = request(409, "POST", "/groups/" + groupId + "/close", alice.device(), alice.token(),
                null, "");
        assertThat(blocked.at("/error/code").asText()).isEqualTo("GROUP_NOT_SETTLED");
        assertThat(blocked.at("/error/details/0/members")).isNotEmpty();
    }

    @Test
    void pendingActionsBlockLeavingAndClosing() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);
        createExpense(alice, groupId, "40.00", "pending-1");

        // Settle the balance first (a pending, unconfirmed repayment wouldn't - it hits
        // BALANCE_NOT_SETTLED before this check even runs), then leave a reversal request open
        // on the now-confirmed repayment so a pending action still exists with balance = 0.
        String repaymentId = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "settle-rp-1", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"20.00\"}").path("id")
                .asText();
        request(200, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/confirm", alice.device(),
                alice.token(), null, "");
        request(201, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/reversals", bob.device(),
                bob.token(), null, "{\"reason\":\"Need to double-check this\"}");

        JsonNode leaveBlocked = request(409, "POST", "/groups/" + groupId + "/leave", bob.device(), bob.token(),
                null, "");
        assertThat(leaveBlocked.at("/error/code").asText()).isEqualTo("PENDING_ACTIONS_EXIST");

        JsonNode closeBlocked = request(409, "POST", "/groups/" + groupId + "/close", alice.device(), alice.token(),
                null, "");
        assertThat(closeBlocked.at("/error/code").asText()).isEqualTo("PENDING_ACTIONS_EXIST");
    }

    private String joinGroup(Account owner, Account other) throws Exception {
        String groupId = request(201, "POST", "/groups", owner.device(), owner.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", owner.device(), owner.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", other.device(), other.token(), null, "");
        return groupId;
    }

    private void createExpense(Account payer, String groupId, String totalAmount, String idempotencyKey)
            throws Exception {
        JsonNode group = request(200, "GET", "/groups/" + groupId, payer.device(), payer.token(), null, null);
        String otherUserId = null;
        for (JsonNode m : group.path("members")) {
            if (!m.path("userId").asText().equals(payer.userId())) {
                otherUserId = m.path("userId").asText();
            }
        }
        request(201, "POST", "/groups/" + groupId + "/expenses", payer.device(), payer.token(), idempotencyKey,
                """
                {"title":"Dinner","totalAmount":"%s","category":"Food","payerUserId":"%s","splitMethod":"equal",
                 "participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(totalAmount, payer.userId(), payer.userId(), otherUserId));
    }

}
