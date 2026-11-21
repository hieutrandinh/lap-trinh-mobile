package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class CorrectionEdgeCasesApiTest extends AbstractApiTest {

    @Test
    void addAndRemoveParticipantsFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);

        // Alice-only expense; adding Bob changes both shares, so both must approve.
        String expenseId = createExpense(alice, groupId, "Snack", "30.00", "[{\"userId\":\"" + alice.userId()
                + "\"}]", "add-1");
        JsonNode pending = request(201, "POST", "/groups/" + groupId + "/expenses/" + expenseId + "/corrections",
                alice.device(), alice.token(), "add-corr-1", """
                {"type":"correction","reason":"Bob joined for this one","baseVersion":1,
                 "proposed":{"participants":[{"userId":"%s"},{"userId":"%s"}]}}
                """.formatted(alice.userId(), bob.userId()));
        assertThat(pending.path("status").asText()).isEqualTo("pending");
        assertThat(pending.path("shares")).anySatisfy(s -> {
            if (s.at("/user/userId").asText().equals(bob.userId())) {
                assertThat(s.path("previousAmount").isNull()).isTrue();
                assertThat(s.path("proposedAmount").asText()).isEqualTo("15.00");
            }
        });

        JsonNode completed = request(200, "POST",
                "/groups/" + groupId + "/corrections/" + pending.path("id").asText() + "/approve", bob.device(),
                bob.token(), null, "");
        assertThat(completed.path("status").asText()).isEqualTo("completed");

        JsonNode afterAdd = request(200, "GET", "/groups/" + groupId + "/expenses/" + expenseId, alice.device(),
                alice.token(), null, null);
        assertThat(afterAdd.path("splits")).hasSize(2);
    }

    @Test
    void changingOnlyThePayerRequiresBothOldAndNewPayerApproval() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);

        String expenseId = createExpense(alice, groupId, "Dinner", "40.00",
                "[{\"userId\":\"" + alice.userId() + "\"},{\"userId\":\"" + bob.userId() + "\"}]", "payer-1");

        JsonNode pending = request(201, "POST", "/groups/" + groupId + "/expenses/" + expenseId + "/corrections",
                alice.device(), alice.token(), "payer-corr-1", """
                {"type":"correction","reason":"Bob actually paid","baseVersion":1,
                 "proposed":{"payerUserId":"%s"}}
                """.formatted(bob.userId()));
        assertThat(pending.path("status").asText()).isEqualTo("pending");
        assertThat(pending.path("approvals")).hasSize(2);

        JsonNode completed = request(200, "POST",
                "/groups/" + groupId + "/corrections/" + pending.path("id").asText() + "/approve", bob.device(),
                bob.token(), null, "");
        assertThat(completed.path("status").asText()).isEqualTo("completed");

        JsonNode detail = request(200, "GET", "/groups/" + groupId + "/expenses/" + expenseId, alice.device(),
                alice.token(), null, null);
        assertThat(detail.at("/payer/userId").asText()).isEqualTo(bob.userId());
    }

    @Test
    void nonMoneyChangeStillRequiresEveryParticipantToApprove() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);

        String expenseId = createExpense(alice, groupId, "Dinner", "40.00",
                "[{\"userId\":\"" + alice.userId() + "\"},{\"userId\":\"" + bob.userId() + "\"}]", "title-1");

        JsonNode pending = request(201, "POST", "/groups/" + groupId + "/expenses/" + expenseId + "/corrections",
                alice.device(), alice.token(), "title-corr-1", """
                {"type":"correction","reason":"Typo","baseVersion":1,"proposed":{"title":"Dinner (fixed)"}}
                """);
        assertThat(pending.path("status").asText()).isEqualTo("pending");
        assertThat(pending.path("approvals")).hasSize(2);
        assertThat(pending.path("approvals")).anySatisfy(a -> {
            if (a.at("/user/userId").asText().equals(bob.userId())) {
                assertThat(a.path("status").asText()).isEqualTo("pending");
            }
        });

        // A second request while the first is still pending is rejected regardless of its content.
        JsonNode secondRequest = request(409, "POST",
                "/groups/" + groupId + "/expenses/" + expenseId + "/corrections", alice.device(), alice.token(),
                "nochange-1", """
                {"type":"correction","reason":"Nothing changed","baseVersion":1,"proposed":{"title":"Dinner"}}
                """);
        assertThat(secondRequest.at("/error/code").asText()).isEqualTo("CORRECTION_ALREADY_PENDING");
    }

    @Test
    void noChangesIsRejectedWhenNothingIsPending() throws Exception {
        Account alice = registerUser("alice");
        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Solo\"}").path("id").asText();
        String expenseId = createExpense(alice, groupId, "Coffee", "4.00",
                "[{\"userId\":\"" + alice.userId() + "\"}]", "nochange-solo");

        JsonNode noChanges = request(422, "POST", "/groups/" + groupId + "/expenses/" + expenseId + "/corrections",
                alice.device(), alice.token(), "nochange-solo-1", """
                {"type":"correction","reason":"Same value","baseVersion":1,"proposed":{"title":"Coffee"}}
                """);
        assertThat(noChanges.at("/error/code").asText()).isEqualTo("NO_CHANGES");
    }

    @Test
    void formerMemberInvolvedBlocksCorrectionsTouchingThem() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = joinGroup(alice, bob);

        String expenseId = createExpense(alice, groupId, "Dinner", "40.00",
                "[{\"userId\":\"" + alice.userId() + "\"},{\"userId\":\"" + bob.userId() + "\"}]", "leave-1");

        // Settle Bob's share so he's allowed to leave the group.
        String repaymentId = request(201, "POST", "/groups/" + groupId + "/repayments", bob.device(), bob.token(),
                "settle-1", "{\"toUserId\":\"" + alice.userId() + "\",\"amount\":\"20.00\"}").path("id").asText();
        request(200, "POST", "/groups/" + groupId + "/repayments/" + repaymentId + "/confirm", alice.device(),
                alice.token(), null, "");
        request(200, "POST", "/groups/" + groupId + "/leave", bob.device(), bob.token(), null, "");

        JsonNode blocked = request(409, "POST", "/groups/" + groupId + "/expenses/" + expenseId + "/corrections",
                alice.device(), alice.token(), "leave-corr-1", """
                {"type":"correction","reason":"Bob left","baseVersion":1,
                 "proposed":{"participants":[{"userId":"%s"}]}}
                """.formatted(alice.userId()));
        assertThat(blocked.at("/error/code").asText()).isEqualTo("FORMER_MEMBER_INVOLVED");
    }

    private String joinGroup(Account owner, Account other) throws Exception {
        String groupId = request(201, "POST", "/groups", owner.device(), owner.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", owner.device(), owner.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", other.device(), other.token(), null, "");
        return groupId;
    }

    private String createExpense(Account payer, String groupId, String title, String totalAmount,
                                  String participantsJson, String idempotencyKey) throws Exception {
        String body = """
                {"title":"%s","totalAmount":"%s","category":"General","payerUserId":"%s",
                 "splitMethod":"equal","participants":%s}
                """.formatted(title, totalAmount, payer.userId(), participantsJson);
        return request(201, "POST", "/groups/" + groupId + "/expenses", payer.device(), payer.token(),
                idempotencyKey, body).path("id").asText();
    }

}
