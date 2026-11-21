package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class GroupExpenseApiTest extends AbstractApiTest {

    @Test
    void splitPreviewSearchAndDetailFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");

        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(), alice.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null, "");

        JsonNode preview = request(200, "POST", "/groups/" + groupId + "/expenses/split-preview", alice.device(),
                alice.token(), null, """
                {"totalAmount":"100.01","splitMethod":"equal","payerUserId":"%s",
                 "participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId(), bob.userId()));
        // 100.01 split 2 ways: participant order [alice, bob] means alice's share gets the extra
        // cent (50.01), so as payer-and-participant her net change is 100.01 - 50.01 = 50.00 exactly.
        assertThat(preview.path("roundingAdjustmentTotal").asText()).isEqualTo("0.01");
        assertThat(preview.path("balanceEffects")).anySatisfy(effect -> {
            assertThat(effect.at("/user/userId").asText()).isEqualTo(alice.userId());
            assertThat(effect.path("change").asText()).isEqualTo("50.00");
        }).anySatisfy(effect -> {
            assertThat(effect.at("/user/userId").asText()).isEqualTo(bob.userId());
            assertThat(effect.path("change").asText()).isEqualTo("-50.00");
        });

        String participants = """
                [{"userId":"%s","amount":"40.00"},{"userId":"%s","amount":"60.00"}]
                """.formatted(alice.userId(), bob.userId());
        JsonNode exact = request(200, "POST", "/groups/" + groupId + "/expenses/split-preview", alice.device(),
                alice.token(), null, """
                {"totalAmount":"100.00","splitMethod":"exact","payerUserId":"%s","participants":%s}
                """.formatted(alice.userId(), participants));
        assertThat(exact.path("roundingAdjustmentTotal").asText()).isEqualTo("0.00");
        assertThat(exact.path("splits")).hasSize(2);
        assertThat(exact.at("/splits/0/amount").asText()).isEqualTo("40.00");
        assertThat(exact.at("/splits/1/amount").asText()).isEqualTo("60.00");

        String foodExpenseId = request(201, "POST", "/groups/" + groupId + "/expenses", alice.device(),
                alice.token(), "exp-food", """
                {"title":"Dinner","totalAmount":"100.00","category":"Food","payerUserId":"%s",
                 "splitMethod":"exact","participants":%s}
                """.formatted(alice.userId(), participants)).path("id").asText();

        String soloExpenseId = request(201, "POST", "/groups/" + groupId + "/expenses", alice.device(),
                alice.token(), "exp-solo", """
                {"title":"Personal snack","totalAmount":"5.00","category":"Snacks","payerUserId":"%s",
                 "splitMethod":"equal","participants":[{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId())).path("id").asText();

        // Alice is the only participant, so cancelling it (via correction) completes immediately.
        request(201, "POST", "/groups/" + groupId + "/expenses/" + soloExpenseId + "/corrections", alice.device(),
                alice.token(), "cancel-solo", "{\"type\":\"cancellation\",\"baseVersion\":1}");

        JsonNode byCategory = request(200, "GET", "/groups/" + groupId + "/expenses?category=food",
                alice.device(), alice.token(), null, null);
        assertThat(byCategory.path("data")).hasSize(1);
        assertThat(byCategory.path("data").get(0).path("id").asText()).isEqualTo(foodExpenseId);

        JsonNode byPayer = request(200, "GET", "/groups/" + groupId + "/expenses?payerUserId=" + alice.userId(),
                alice.device(), alice.token(), null, null);
        assertThat(byPayer.path("data")).hasSizeGreaterThanOrEqualTo(1);

        JsonNode byParticipant = request(200, "GET",
                "/groups/" + groupId + "/expenses?participantUserId=" + bob.userId(), alice.device(), alice.token(),
                null, null);
        assertThat(byParticipant.path("data")).hasSize(1);
        assertThat(byParticipant.path("data").get(0).path("id").asText()).isEqualTo(foodExpenseId);

        // Default status=active excludes the cancelled solo expense.
        JsonNode activeOnly = request(200, "GET", "/groups/" + groupId + "/expenses", alice.device(), alice.token(),
                null, null);
        assertThat(activeOnly.path("data")).noneMatch(e -> e.path("id").asText().equals(soloExpenseId));

        JsonNode cancelledOnly = request(200, "GET", "/groups/" + groupId + "/expenses?status=cancelled",
                alice.device(), alice.token(), null, null);
        assertThat(cancelledOnly.path("data")).hasSize(1);
        assertThat(cancelledOnly.path("data").get(0).path("id").asText()).isEqualTo(soloExpenseId);
        assertThat(cancelledOnly.path("data").get(0).path("status").asText()).isEqualTo("cancelled");

        JsonNode all = request(200, "GET", "/groups/" + groupId + "/expenses?status=all", alice.device(),
                alice.token(), null, null);
        assertThat(all.path("data")).hasSize(2);

        JsonNode notFound = request(404, "GET", "/groups/" + groupId + "/expenses/not-a-real-id", alice.device(),
                alice.token(), null, null);
        assertThat(notFound.at("/error/code").asText()).isEqualTo("NOT_FOUND");

        JsonNode detail = request(200, "GET", "/groups/" + groupId + "/expenses/" + foodExpenseId, bob.device(),
                bob.token(), null, null);
        assertThat(detail.at("/myEntry/entryId").isMissingNode()).isFalse();
        assertThat(detail.at("/myEntry/entryId").asText()).isNotBlank();
        assertThat(detail.path("splits")).anySatisfy(split -> {
            assertThat(split.at("/user/userId").asText()).isEqualTo(bob.userId());
            assertThat(split.path("amount").asText()).isEqualTo("60.00");
        });
        assertThat(detail.path("corrections")).isEmpty();
    }

}
