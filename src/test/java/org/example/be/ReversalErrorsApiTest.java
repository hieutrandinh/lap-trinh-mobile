package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ReversalErrorsApiTest extends AbstractApiTest {

    @Test
    void reversalRequiresAConfirmedRepaymentAndOnlySenderOrReceiver() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        Account carol = registerUser("carol");
        String groupId = joinGroup(alice, bob, carol);
        createExpense(alice, groupId, "60.00", "rev-exp-1", alice.userId(), bob.userId(), carol.userId());

        String pendingRepaymentId = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(),
                bob.token(), "rev-rp-1", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"20.00\"}")
                .path("id").asText();

        JsonNode notConfirmed = request(409, "POST",
                "/groups/" + groupId + "/repayments/" + pendingRepaymentId + "/reversals", bob.device(),
                bob.token(), null, "{\"reason\":\"Too early\"}");
        assertThat(notConfirmed.at("/error/code").asText()).isEqualTo("INVALID_REPAYMENT_STATE");

        request(200, "POST", "/groups/" + groupId + "/repayments/" + pendingRepaymentId + "/confirm",
                alice.device(), alice.token(), null, "");

        JsonNode outsider = request(403, "POST",
                "/groups/" + groupId + "/repayments/" + pendingRepaymentId + "/reversals", carol.device(),
                carol.token(), null, "{\"reason\":\"Not my business\"}");
        assertThat(outsider.at("/error/code").asText()).isEqualTo("FORBIDDEN");

        JsonNode firstReversal = request(201, "POST",
                "/groups/" + groupId + "/repayments/" + pendingRepaymentId + "/reversals", bob.device(),
                bob.token(), null, "{\"reason\":\"Need to review\"}");
        assertThat(firstReversal.path("status").asText()).isEqualTo("pending");

        JsonNode alreadyPending = request(409, "POST",
                "/groups/" + groupId + "/repayments/" + pendingRepaymentId + "/reversals", alice.device(),
                alice.token(), null, "{\"reason\":\"Me too\"}");
        assertThat(alreadyPending.at("/error/code").asText()).isEqualTo("REVERSAL_ALREADY_PENDING");
    }

    @Test
    void formerMemberInvolvedBlocksReversalOnceSenderOrReceiverLeft() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob, null);
        createExpense(alice, groupId, "40.00", "rev-exp-2", alice.userId(), bob.userId(), null);

        String repaymentId = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "rev-rp-2", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"20.00\"}").path("id").asText();
        request(200, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/confirm", alice.device(),
                alice.token(), null, "");

        // Bob's balance is now 0 (debt settled by the confirmed repayment), so he can leave.
        request(200, "POST", "/groups/" + groupId + "/leave", bob.device(), bob.token(), null, "");

        JsonNode blocked = request(409, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/reversals",
                alice.device(), alice.token(), null, "{\"reason\":\"Bob left though\"}");
        assertThat(blocked.at("/error/code").asText()).isEqualTo("FORMER_MEMBER_INVOLVED");
    }

    private String joinGroup(Account owner, Account member, Account optionalThird) throws Exception {
        String groupId = request(201, "POST", "/groups", owner.device(), owner.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        acceptInvite(owner, groupId, member);
        if (optionalThird != null) {
            acceptInvite(owner, groupId, optionalThird);
        }
        return groupId;
    }

    private void acceptInvite(Account owner, String groupId, Account joiner) throws Exception {
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", owner.device(), owner.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", joiner.device(), joiner.token(), null, "");
    }

    private void createExpense(Account payer, String groupId, String totalAmount, String idempotencyKey,
                                String... participantUserIds) throws Exception {
        StringBuilder participants = new StringBuilder("[");
        for (int i = 0; i < participantUserIds.length; i++) {
            if (participantUserIds[i] == null) {
                continue;
            }
            if (participants.length() > 1) {
                participants.append(',');
            }
            participants.append("{\"userId\":\"").append(participantUserIds[i]).append("\"}");
        }
        participants.append(']');
        request(201, "POST", "/groups/" + groupId + "/expenses", payer.device(), payer.token(), idempotencyKey,
                """
                {"title":"Dinner","totalAmount":"%s","category":"Food","payerUserId":"%s","splitMethod":"equal",
                 "participants":%s}
                """.formatted(totalAmount, payer.userId(), participants));
    }

}
