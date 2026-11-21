package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

class InvitationErrorsApiTest extends AbstractApiTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void expiredInvitationAndClosedGroupCannotBeJoined() throws Exception {
        Account owner = registerUser("invite-owner");
        Account invitee = registerUser("invite-guest");
        String group = "/groups/" + request(201, "POST", "/groups", owner.device(), owner.token(),
                null, "{\"name\":\"Invite lifecycle\"}").path("id").asText();
        String expiredCode = request(201, "POST", group + "/invitations", owner.device(), owner.token(),
                null, "{}").path("code").asText();
        assertThat(jdbc.update("""
                UPDATE tblInvitation
                SET createdAt = LOCALTIMESTAMP - INTERVAL '2 days', expiresAt = LOCALTIMESTAMP - INTERVAL '1 day'
                WHERE code = ?
                """, expiredCode)).isOne();
        assertThat(request(410, "POST", "/invitations/" + expiredCode + "/accept", invitee.device(),
                invitee.token(), null, null).at("/error/code").asText()).isEqualTo("INVITATION_EXPIRED");

        String activeCode = request(201, "POST", group + "/invitations", owner.device(), owner.token(),
                null, "{}").path("code").asText();
        request(200, "POST", group + "/close", owner.device(), owner.token(), null, null);
        assertThat(request(410, "POST", "/invitations/" + activeCode + "/accept", invitee.device(),
                invitee.token(), null, null).at("/error/code").asText()).isEqualTo("INVITATION_REVOKED");
        // Simulate an active invitation left behind by older data for a closed group.
        assertThat(jdbc.update("UPDATE tblInvitation SET status = 'active' WHERE code = ?", activeCode)).isOne();
        assertThat(request(409, "POST", "/invitations/" + activeCode + "/accept", invitee.device(),
                invitee.token(), null, null).at("/error/code").asText()).isEqualTo("GROUP_CLOSED");
    }

    @Test
    void listRevokeAndAcceptErrorsFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        Account carol = registerUser("carol");

        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();

        // maxUses = 1: the second person to try is told the invitation has no uses left.
        JsonNode singleUse = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(),
                alice.token(), null, "{\"maxUses\":1}");
        String singleUseCode = singleUse.path("code").asText();
        request(200, "POST", "/invitations/" + singleUseCode + "/accept", bob.device(), bob.token(), null, "");
        JsonNode usedUp = request(410, "POST", "/invitations/" + singleUseCode + "/accept", carol.device(),
                carol.token(), null, "");
        assertThat(usedUp.at("/error/code").asText()).isEqualTo("INVITATION_USED_UP");

        // A revoked invitation can no longer be used, and can't be revoked twice.
        String revokableId = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(),
                alice.token(), null, "{}").path("id").asText();
        JsonNode revokableInvite = request(200, "GET", "/groups/" + groupId + "/invitations", alice.device(),
                alice.token(), null, null);
        String revokableCode = null;
        for (JsonNode i : revokableInvite.path("data")) {
            if (i.path("id").asText().equals(revokableId)) {
                revokableCode = i.path("code").asText();
            }
        }
        assertThat(revokableCode).isNotNull();

        request(204, "DELETE", "/groups/" + groupId + "/invitations/" + revokableId, alice.device(), alice.token(),
                null, null);
        JsonNode alreadyRevoked = request(409, "DELETE", "/groups/" + groupId + "/invitations/" + revokableId,
                alice.device(), alice.token(), null, null);
        assertThat(alreadyRevoked.at("/error/code").asText()).isEqualTo("INVITATION_ALREADY_REVOKED");

        JsonNode revokedAccept = request(410, "POST", "/invitations/" + revokableCode + "/accept", carol.device(),
                carol.token(), null, "");
        assertThat(revokedAccept.at("/error/code").asText()).isEqualTo("INVITATION_REVOKED");

        JsonNode notFound = request(404, "POST", "/invitations/NOSUCH99/accept", carol.device(), carol.token(),
                null, "");
        assertThat(notFound.at("/error/code").asText()).isEqualTo("INVITATION_NOT_FOUND");

        // Only owner/admin may create or revoke invitations.
        JsonNode memberCreate = request(403, "POST", "/groups/" + groupId + "/invitations", bob.device(),
                bob.token(), null, "{}");
        assertThat(memberCreate.at("/error/code").asText()).isEqualTo("FORBIDDEN");

        JsonNode revokedList = request(200, "GET", "/groups/" + groupId + "/invitations?status=revoked",
                alice.device(), alice.token(), null, null);
        assertThat(revokedList.path("data")).hasSize(1);
        assertThat(revokedList.path("data").get(0).path("id").asText()).isEqualTo(revokableId);

        JsonNode activeList = request(200, "GET", "/groups/" + groupId + "/invitations?status=active",
                alice.device(), alice.token(), null, null);
        assertThat(activeList.path("data")).noneMatch(i -> i.path("id").asText().equals(revokableId));
    }

}
