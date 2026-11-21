package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class GroupLifecycleApiTest extends AbstractApiTest {

    @Test
    void membershipRoleTransferLeaveAndCloseFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");

        JsonNode group = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\",\"description\":\"Da Lat\"}");
        String groupId = group.path("id").asText();
        assertThat(group.path("myRole").asText()).isEqualTo("owner");
        assertThat(group.path("accessMode").asText()).isEqualTo("full");
        assertThat(group.path("memberCount").asInt()).isEqualTo(1);

        JsonNode invitation = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(),
                alice.token(), null, "{}");
        String code = invitation.path("code").asText();
        assertThat(code).matches("[A-Z0-9]{8}");
        assertThat(invitation.path("status").asText()).isEqualTo("active");

        JsonNode joined = request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null,
                "");
        assertThat(joined.path("myRole").asText()).isEqualTo("member");
        assertThat(joined.path("id").asText()).isEqualTo(groupId);

        JsonNode alreadyMember = request(409, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(),
                null, "");
        assertThat(alreadyMember.at("/error/code").asText()).isEqualTo("ALREADY_MEMBER");

        JsonNode detail = request(200, "GET", "/groups/" + groupId, alice.device(), alice.token(), null, null);
        assertThat(detail.path("members")).hasSize(2);
        assertThat(detail.path("memberCount").asInt()).isEqualTo(2);

        JsonNode hideWhileActive = request(409, "DELETE", "/me/groups/" + groupId, alice.device(),
                alice.token(), null, null);
        assertThat(hideWhileActive.at("/error/code").asText()).isEqualTo("MUST_LEAVE_FIRST");

        JsonNode roleChanged = request(200, "PATCH", "/groups/" + groupId + "/members/" + bob.userId(),
                alice.device(), alice.token(), null, "{\"role\":\"admin\"}");
        assertThat(roleChanged.path("role").asText()).isEqualTo("admin");

        JsonNode notOwner = request(403, "PATCH", "/groups/" + groupId + "/members/" + bob.userId(), bob.device(),
                bob.token(), null, "{\"role\":\"member\"}");
        assertThat(notOwner.at("/error/code").asText()).isEqualTo("FORBIDDEN");

        JsonNode transferred = request(200, "POST", "/groups/" + groupId + "/transfer-ownership", alice.device(),
                alice.token(), null, "{\"newOwnerUserId\":\"" + bob.userId() + "\"}");
        assertThat(transferred.path("myRole").asText()).isEqualTo("admin");

        JsonNode ownerLeaveBlocked = request(409, "POST", "/groups/" + groupId + "/leave", bob.device(), bob.token(),
                null, "");
        assertThat(ownerLeaveBlocked.at("/error/code").asText()).isEqualTo("OWNER_MUST_TRANSFER");

        JsonNode left = request(200, "POST", "/groups/" + groupId + "/leave", alice.device(), alice.token(), null,
                "");
        assertThat(left.path("accessMode").asText()).isEqualTo("snapshot");
        assertThat(left.path("snapshotCapturedAt").asText()).isNotBlank();

        request(200, "PATCH", "/groups/" + groupId, bob.device(), bob.token(), null,
                "{\"name\":\"Renamed trip\"}");
        JsonNode snapshot = request(200, "GET", "/groups/" + groupId, alice.device(), alice.token(), null, null);
        assertThat(snapshot.path("name").asText()).isEqualTo("Trip");
        assertThat(snapshot.path("accessMode").asText()).isEqualTo("snapshot");
        assertThat(request(403, "PATCH", "/groups/" + groupId, alice.device(), alice.token(), null,
                "{\"name\":\"Former member edit\"}").at("/error/code").asText()).isEqualTo("GROUP_READ_ONLY");

        JsonNode afterLeave = request(200, "GET", "/groups/" + groupId, bob.device(), bob.token(), null, null);
        assertThat(afterLeave.path("members")).hasSize(1);

        JsonNode closed = request(200, "POST", "/groups/" + groupId + "/close", bob.device(), bob.token(), null,
                "");
        assertThat(closed.path("status").asText()).isEqualTo("closed");
        assertThat(closed.path("accessMode").asText()).isEqualTo("closed");

        JsonNode writeAfterClose = request(403, "PATCH", "/groups/" + groupId, bob.device(), bob.token(), null,
                "{\"name\":\"Nope\"}");
        assertThat(writeAfterClose.at("/error/code").asText()).isEqualTo("GROUP_READ_ONLY");

        request(204, "DELETE", "/me/groups/" + groupId, alice.device(), alice.token(), null, null);
        JsonNode myGroups = request(200, "GET", "/groups", alice.device(), alice.token(), null, null);
        assertThat(myGroups.path("data")).noneMatch(g -> g.path("id").asText().equals(groupId));
        assertThat(request(404, "GET", "/groups/" + groupId, alice.device(), alice.token(), null, null)
                .at("/error/code").asText()).isEqualTo("GROUP_NOT_AVAILABLE");
        JsonNode stillVisibleToOwner = request(200, "GET", "/groups/" + groupId, bob.device(),
                bob.token(), null, null);
        assertThat(stillVisibleToOwner.path("status").asText()).isEqualTo("closed");
    }

}
