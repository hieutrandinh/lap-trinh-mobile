package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class RepaymentPermissionErrorsApiTest extends AbstractApiTest {

    @Test
    void toUserIdMustBeAnActiveMember() throws Exception {
        Account alice = registerUser("alice");
        Account outsider = registerUser("outsider");
        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();

        JsonNode notMember = request(422, "POST", "/groups/" + groupId + "/repayments", alice.device(),
                alice.token(), "not-member-1",
                "{\"toUserId\":\"" + outsider.userId() + "\",\"amount\":\"5.00\"}");
        assertThat(notMember.at("/error/code").asText()).isEqualTo("NOT_ACTIVE_MEMBER");
    }

    @Test
    void onlyTheReceiverCanConfirmOrRejectAndOnlyTheSenderCanCancel() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);
        createExpense(alice, bob, groupId, "40.00", "perm-exp-1");

        String repaymentId = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "perm-rp-1", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"10.00\"}").path("id").asText();

        JsonNode senderCannotConfirm = request(403, "POST",
                "/groups/" + groupId + "/repayments/" + repaymentId + "/confirm", bob.device(), bob.token(), null,
                "");
        assertThat(senderCannotConfirm.at("/error/code").asText()).isEqualTo("NOT_REPAYMENT_RECEIVER");

        JsonNode senderCannotReject = request(403, "POST",
                "/groups/" + groupId + "/repayments/" + repaymentId + "/reject", bob.device(), bob.token(), null,
                "{}");
        assertThat(senderCannotReject.at("/error/code").asText()).isEqualTo("NOT_REPAYMENT_RECEIVER");

        JsonNode receiverCannotCancel = request(403, "POST",
                "/groups/" + groupId + "/repayments/" + repaymentId + "/cancel", alice.device(), alice.token(),
                null, "");
        assertThat(receiverCannotCancel.at("/error/code").asText()).isEqualTo("NOT_REPAYMENT_SENDER");

        JsonNode confirmed = request(200, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/confirm",
                alice.device(), alice.token(), null, "");
        assertThat(confirmed.path("status").asText()).isEqualTo("confirmed");

        JsonNode rejectAfterConfirm = request(409, "POST",
                "/groups/" + groupId + "/repayments/" + repaymentId + "/reject", alice.device(), alice.token(),
                null, "{}");
        assertThat(rejectAfterConfirm.at("/error/code").asText()).isEqualTo("INVALID_REPAYMENT_STATE");
        assertThat(rejectAfterConfirm.at("/error/details/0/currentStatus").asText()).isEqualTo("confirmed");

        JsonNode cancelAfterConfirm = request(409, "POST",
                "/groups/" + groupId + "/repayments/" + repaymentId + "/cancel", bob.device(), bob.token(), null,
                "");
        assertThat(cancelAfterConfirm.at("/error/code").asText()).isEqualTo("INVALID_REPAYMENT_STATE");
    }

    @Test
    void cancellingTwiceIsRejected() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);
        createExpense(alice, bob, groupId, "20.00", "perm-exp-2");

        String repaymentId = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "perm-rp-2", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"5.00\"}").path("id").asText();
        request(200, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/cancel", bob.device(),
                bob.token(), null, "");

        JsonNode twice = request(409, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/cancel",
                bob.device(), bob.token(), null, "");
        assertThat(twice.at("/error/code").asText()).isEqualTo("INVALID_REPAYMENT_STATE");
    }

    private String joinGroup(Account owner, Account other) throws Exception {
        String groupId = request(201, "POST", "/groups", owner.device(), owner.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", owner.device(), owner.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", other.device(), other.token(), null, "");
        return groupId;
    }

    private void createExpense(Account payer, Account other, String groupId, String totalAmount,
                                String idempotencyKey) throws Exception {
        request(201, "POST", "/groups/" + groupId + "/expenses", payer.device(), payer.token(), idempotencyKey,
                """
                {"title":"Dinner","totalAmount":"%s","category":"Food","payerUserId":"%s","splitMethod":"equal",
                 "participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(totalAmount, payer.userId(), payer.userId(), other.userId()));
    }

}
