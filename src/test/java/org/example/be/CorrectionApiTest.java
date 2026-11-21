package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class CorrectionApiTest extends AbstractApiTest {

    @Test
    void previewCreateApproveRejectAndWithdrawFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        Account carol = registerUser("carol");

        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(), alice.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null, "");
        request(200, "POST", "/invitations/" + code + "/accept", carol.device(), carol.token(), null, "");

        // E1: Alice is the only participant, so her own approval is enough to auto-complete.
        String soloExpenseId = createExpense(alice, groupId, "Solo coffee", "30.00",
                "[{\"userId\":\"" + alice.userId() + "\"}]", "solo-1");

        JsonNode preview = request(200, "POST",
                "/groups/" + groupId + "/expenses/" + soloExpenseId + "/corrections/preview", alice.device(),
                alice.token(), null, """
                {"type":"correction","baseVersion":1,"proposed":{"totalAmount":"40.00",
                 "participants":[{"userId":"%s"}]}}
                """.formatted(alice.userId()));
        assertThat(preview.path("id").isNull()).isTrue();
        assertThat(preview.path("status").asText()).isEqualTo("preview");
        assertThat(preview.path("approvals").get(0).path("status").asText()).isEqualTo("approved");

        JsonNode completed = request(201, "POST",
                "/groups/" + groupId + "/expenses/" + soloExpenseId + "/corrections", alice.device(), alice.token(),
                "corr-solo-1", """
                {"type":"correction","reason":"Forgot tax","baseVersion":1,
                 "proposed":{"totalAmount":"40.00","participants":[{"userId":"%s"}]}}
                """.formatted(alice.userId()));
        assertThat(completed.path("status").asText()).isEqualTo("completed");
        JsonNode soloAfter = request(200, "GET", "/groups/" + groupId + "/expenses/" + soloExpenseId, alice.device(),
                alice.token(), null, null);
        assertThat(soloAfter.path("totalAmount").asText()).isEqualTo("40.00");
        assertThat(soloAfter.path("version").asInt()).isEqualTo(2);

        String soloCorrections = "/groups/" + groupId + "/expenses/" + soloExpenseId + "/corrections";
        String cancellation = """
                {"type":"cancellation","baseVersion":2,"reason":"Duplicate expense"}
                """;
        JsonNode cancelled = request(201, "POST", soloCorrections, alice.device(), alice.token(),
                "cancel-solo", cancellation);
        assertThat(cancelled.path("status").asText()).isEqualTo("completed");
        assertThat(request(409, "POST", soloCorrections + "/preview", alice.device(), alice.token(), null,
                cancellation).at("/error/code").asText()).isEqualTo("EXPENSE_CANCELLED");
        assertThat(request(409, "POST", soloCorrections, alice.device(), alice.token(), "cancel-solo-again",
                cancellation).at("/error/code").asText()).isEqualTo("EXPENSE_CANCELLED");

        // E2: shared with Bob, so the correction needs his approval before it applies.
        String sharedExpenseId = createExpense(alice, groupId, "Dinner", "100.00",
                "[{\"userId\":\"" + alice.userId() + "\"},{\"userId\":\"" + bob.userId() + "\"}]", "shared-1");

        JsonNode notCreator = request(403, "POST",
                "/groups/" + groupId + "/expenses/" + sharedExpenseId + "/corrections", bob.device(), bob.token(),
                "corr-bob-1", correctionBody(alice.userId(), bob.userId(), "120.00"));
        assertThat(notCreator.at("/error/code").asText()).isEqualTo("NOT_EXPENSE_CREATOR");

        JsonNode pending = request(201, "POST",
                "/groups/" + groupId + "/expenses/" + sharedExpenseId + "/corrections", alice.device(),
                alice.token(), "corr-shared-1", correctionBody(alice.userId(), bob.userId(), "120.00"));
        String correctionId = pending.path("id").asText();
        assertThat(pending.path("status").asText()).isEqualTo("pending");
        assertThat(pending.path("approvals")).anySatisfy(a -> {
            assertThat(a.at("/user/userId").asText()).isEqualTo(bob.userId());
            assertThat(a.path("status").asText()).isEqualTo("pending");
        });
        String correctionPath = "/groups/" + groupId + "/corrections/" + correctionId;
        String expenseSearch = "/groups/" + groupId + "/expenses?participantUserId=" + bob.userId();
        JsonNode pendingExpenses = request(200, "GET", expenseSearch + "&hasPendingCorrection=true",
                bob.device(), bob.token(), null, null);
        assertThat(pendingExpenses.path("data")).hasSize(1);
        assertThat(pendingExpenses.at("/data/0/id").asText()).isEqualTo(sharedExpenseId);
        assertThat(pendingExpenses.at("/data/0/pendingCorrectionId").asText()).isEqualTo(correctionId);
        JsonNode noPendingExpenses = request(200, "GET", expenseSearch + "&hasPendingCorrection=false",
                bob.device(), bob.token(), null, null);
        assertThat(noPendingExpenses.path("data")).isEmpty();
        assertThat(request(403, "POST", correctionPath + "/approve", carol.device(), carol.token(), null, null)
                .at("/error/code").asText()).isEqualTo("NOT_REQUIRED_APPROVER");
        assertThat(request(403, "POST", correctionPath + "/reject", carol.device(), carol.token(), null, "{}")
                .at("/error/code").asText()).isEqualTo("NOT_REQUIRED_APPROVER");

        JsonNode alreadyPending = request(409, "POST",
                "/groups/" + groupId + "/expenses/" + sharedExpenseId + "/corrections", alice.device(),
                alice.token(), "corr-shared-2", correctionBody(alice.userId(), bob.userId(), "130.00"));
        assertThat(alreadyPending.at("/error/code").asText()).isEqualTo("CORRECTION_ALREADY_PENDING");

        JsonNode aliceAlreadyApproved = request(409, "POST",
                "/groups/" + groupId + "/corrections/" + correctionId + "/approve", alice.device(), alice.token(),
                null, "");
        assertThat(aliceAlreadyApproved.at("/error/code").asText()).isEqualTo("ALREADY_RESPONDED");

        JsonNode approved = request(200, "POST", "/groups/" + groupId + "/corrections/" + correctionId + "/approve",
                bob.device(), bob.token(), null, "");
        assertThat(approved.path("status").asText()).isEqualTo("completed");
        JsonNode afterApproval = request(200, "GET", expenseSearch + "&hasPendingCorrection=false",
                bob.device(), bob.token(), null, null);
        assertThat(afterApproval.path("data")).hasSize(1);
        assertThat(afterApproval.at("/data/0/id").asText()).isEqualTo(sharedExpenseId);
        assertThat(request(409, "POST", correctionPath + "/approve", bob.device(), bob.token(), null, null)
                .at("/error/code").asText()).isEqualTo("CORRECTION_NOT_PENDING");

        JsonNode sharedAfter = request(200, "GET", "/groups/" + groupId + "/expenses/" + sharedExpenseId,
                alice.device(), alice.token(), null, null);
        assertThat(sharedAfter.path("totalAmount").asText()).isEqualTo("120.00");
        assertThat(sharedAfter.path("splits")).allSatisfy(s -> assertThat(s.path("amount").asText()).isEqualTo("60.00"));

        // E3: a rejected correction leaves the expense untouched.
        String rejectableId = createExpense(alice, groupId, "Snacks", "50.00",
                "[{\"userId\":\"" + alice.userId() + "\"},{\"userId\":\"" + bob.userId() + "\"}]", "rej-1");
        JsonNode rejPending = request(201, "POST",
                "/groups/" + groupId + "/expenses/" + rejectableId + "/corrections", alice.device(), alice.token(),
                "corr-rej-1", correctionBody(alice.userId(), bob.userId(), "80.00"));
        JsonNode rejected = request(200, "POST",
                "/groups/" + groupId + "/corrections/" + rejPending.path("id").asText() + "/reject", bob.device(),
                bob.token(), null, "{\"note\":\"Didn't agree\"}");
        assertThat(rejected.path("status").asText()).isEqualTo("rejected");
        JsonNode rejExpense = request(200, "GET", "/groups/" + groupId + "/expenses/" + rejectableId, alice.device(),
                alice.token(), null, null);
        assertThat(rejExpense.path("totalAmount").asText()).isEqualTo("50.00");

        // E4: the creator can withdraw before the other side responds.
        String withdrawableId = createExpense(alice, groupId, "Taxi", "20.00",
                "[{\"userId\":\"" + alice.userId() + "\"},{\"userId\":\"" + bob.userId() + "\"}]", "with-1");
        JsonNode withPending = request(201, "POST",
                "/groups/" + groupId + "/expenses/" + withdrawableId + "/corrections", alice.device(), alice.token(),
                "corr-with-1", correctionBody(alice.userId(), bob.userId(), "25.00"));
        JsonNode withdrawn = request(200, "POST",
                "/groups/" + groupId + "/corrections/" + withPending.path("id").asText() + "/withdraw",
                alice.device(), alice.token(), null, "");
        assertThat(withdrawn.path("status").asText()).isEqualTo("withdrawn");

        JsonNode history = request(200, "GET", "/groups/" + groupId + "/expenses/" + sharedExpenseId + "/corrections",
                alice.device(), alice.token(), null, null);
        assertThat(history.path("data")).hasSize(1);
        assertThat(history.at("/original/totalAmount").asText()).isEqualTo("100.00");

        JsonNode farPage = request(200, "GET",
                "/groups/" + groupId + "/expenses/" + sharedExpenseId + "/corrections?page=2147483647",
                alice.device(), alice.token(), null, null);
        assertThat(farPage.path("data")).isEmpty();
        assertThat(farPage.at("/pagination/totalItems").asInt()).isOne();

        JsonNode detail = request(200, "GET", "/groups/" + groupId + "/corrections/" + correctionId, bob.device(),
                bob.token(), null, null);
        assertThat(detail.path("status").asText()).isEqualTo("completed");
    }

    private String correctionBody(String aliceId, String bobId, String amount) {
        return """
                {"type":"correction","reason":"Price changed","baseVersion":1,
                 "proposed":{"totalAmount":"%s","participants":[{"userId":"%s"},{"userId":"%s"}]}}
                """.formatted(amount, aliceId, bobId);
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
