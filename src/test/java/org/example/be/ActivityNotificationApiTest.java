package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ActivityNotificationApiTest extends AbstractApiTest {

    @Test
    void activityFeedNotificationsAndSettingsFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");

        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(), alice.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null, "");

        JsonNode activities = request(200, "GET", "/groups/" + groupId + "/activities", alice.device(),
                alice.token(), null, null);
        assertThat(activities.path("data")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(activities.path("data")).anySatisfy(a -> {
            assertThat(a.path("type").asText()).isEqualTo("group_created");
            assertThat(a.path("summary").asText()).isNotBlank();
        });

        JsonNode memberOnly = request(200, "GET", "/groups/" + groupId + "/activities?category=member",
                alice.device(), alice.token(), null, null);
        assertThat(memberOnly.path("data")).isNotEmpty()
                .allSatisfy(a -> assertThat(a.path("type").asText()).isEqualTo("member_joined"));

        // Bob already has one unread notification (group_joined). Mute group alerts, then confirm the next
        // group event (ownership transfer) is not delivered to him.
        JsonNode unreadAfterJoin = request(200, "GET", "/notifications?unreadOnly=true", bob.device(), bob.token(),
                null, null);
        assertThat(unreadAfterJoin.path("unreadCount").asLong()).isEqualTo(1);
        String firstNotificationId = unreadAfterJoin.path("data").get(0).path("id").asText();
        assertThat(unreadAfterJoin.path("data").get(0).path("title").asText()).isNotBlank();

        request(204, "POST", "/notifications/" + firstNotificationId + "/read", alice.device(), alice.token(),
                null, null);
        JsonNode unreadAfterOtherUserAttempt = request(200, "GET", "/notifications?unreadOnly=true",
                bob.device(), bob.token(), null, null);
        assertThat(unreadAfterOtherUserAttempt.at("/data/0/id").asText()).isEqualTo(firstNotificationId);

        JsonNode settings = request(200, "PATCH", "/me/notification-settings", bob.device(), bob.token(), null,
                "{\"groupAlerts\":false}");
        assertThat(settings.path("groupAlerts").asBoolean()).isFalse();

        request(200, "POST", "/groups/" + groupId + "/transfer-ownership", alice.device(), alice.token(), null,
                "{\"newOwnerUserId\":\"" + bob.userId() + "\"}");

        JsonNode stillOne = request(200, "GET", "/notifications?unreadOnly=true", bob.device(), bob.token(), null,
                null);
        assertThat(stillOne.path("unreadCount").asLong()).isEqualTo(1);

        request(204, "POST", "/notifications/" + firstNotificationId + "/read", bob.device(), bob.token(), null,
                null);
        JsonNode afterRead = request(200, "GET", "/notifications?unreadOnly=true", bob.device(), bob.token(), null,
                null);
        assertThat(afterRead.path("unreadCount").asLong()).isZero();

        // Re-enable alerts and generate a fresh one to exercise read-all.
        request(200, "PATCH", "/me/notification-settings", bob.device(), bob.token(), null,
                "{\"groupAlerts\":true}");
        request(200, "POST", "/groups/" + groupId + "/transfer-ownership", bob.device(), bob.token(), null,
                "{\"newOwnerUserId\":\"" + alice.userId() + "\"}");
        // Alice is now owner again; transfer back once more so Bob (now admin) receives the notification.
        request(200, "POST", "/groups/" + groupId + "/transfer-ownership", alice.device(), alice.token(), null,
                "{\"newOwnerUserId\":\"" + bob.userId() + "\"}");

        JsonNode beforeReadAll = request(200, "GET", "/notifications?unreadOnly=true", bob.device(), bob.token(),
                null, null);
        assertThat(beforeReadAll.path("unreadCount").asLong()).isGreaterThanOrEqualTo(1);

        JsonNode readAll = request(200, "POST", "/notifications/read-all", bob.device(), bob.token(), null, "");
        assertThat(readAll.path("updated").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode afterReadAll = request(200, "GET", "/notifications?unreadOnly=true", bob.device(), bob.token(),
                null, null);
        assertThat(afterReadAll.path("unreadCount").asLong()).isZero();
        JsonNode repeatedReadAll = request(200, "POST", "/notifications/read-all", bob.device(), bob.token(), null, "");
        assertThat(repeatedReadAll.path("updated").asInt()).isZero();
    }

}
