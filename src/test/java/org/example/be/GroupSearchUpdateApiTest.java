package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class GroupSearchUpdateApiTest extends AbstractApiTest {

    @Test
    void oversizedPageOffsetsDoNotReturnServerErrors() throws Exception {
        Account me = registerUser("page-boundary");
        String groupId = request(201, "POST", "/groups", me.device(), me.token(), null,
                "{\"name\":\"Pagination\"}").path("id").asText();
        String groupPath = "/groups/" + groupId;
        String query = "?page=2147483647&pageSize=20";

        for (String path : List.of("/groups", groupPath + "/activities", groupPath + "/invitations",
                "/notifications")) {
            JsonNode result = request(200, "GET", path + query, me.device(), me.token(), null, null);
            assertThat(result.path("data").isArray()).as(path).isTrue();
            assertThat(result.path("data")).as(path).isEmpty();
            assertThat(result.at("/pagination/page").asInt()).as(path).isEqualTo(Integer.MAX_VALUE);
        }
        for (String path : List.of("/entries", groupPath + "/expenses", groupPath + "/repayments")) {
            JsonNode result = request(422, "GET", path + query, me.device(), me.token(), null, null);
            assertThat(result.at("/error/code").asText()).as(path).isEqualTo("VALIDATION_ERROR");
        }
    }

    @Test
    void updateSearchAndMemberErrorsFollowTheApiContract() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");

        String groupAId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Alpha trip\"}").path("id").asText();
        String groupBId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Beta trip\"}").path("id").asText();

        JsonNode renamed = request(200, "PATCH", "/groups/" + groupAId, alice.device(), alice.token(), null,
                "{\"name\":\"Alpha trip renamed\",\"description\":\"Updated\"}");
        assertThat(renamed.path("name").asText()).isEqualTo("Alpha trip renamed");
        assertThat(renamed.path("description").asText()).isEqualTo("Updated");

        JsonNode noFields = request(422, "PATCH", "/groups/" + groupBId, alice.device(), alice.token(), null, "{}");
        assertThat(noFields.at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");

        JsonNode notMember = request(404, "PATCH", "/groups/" + groupAId, bob.device(), bob.token(), null,
                "{\"name\":\"Hijack\"}");
        assertThat(notMember.at("/error/code").asText()).isEqualTo("GROUP_NOT_AVAILABLE");

        JsonNode searched = request(200, "GET", "/groups?q=renamed", alice.device(), alice.token(), null, null);
        assertThat(searched.path("data")).hasSize(1);
        assertThat(searched.path("data").get(0).path("id").asText()).isEqualTo(groupAId);

        JsonNode sortedByName = request(200, "GET", "/groups?sort=name", alice.device(), alice.token(), null, null);
        assertThat(sortedByName.path("data")).hasSize(2);
        assertThat(sortedByName.path("data").get(0).path("name").asText()).isEqualTo("Alpha trip renamed");

        JsonNode paged = request(200, "GET", "/groups?pageSize=1&page=1", alice.device(), alice.token(), null,
                null);
        assertThat(paged.path("data")).hasSize(1);
        assertThat(paged.at("/pagination/totalItems").asInt()).isEqualTo(2);
        assertThat(paged.at("/pagination/totalPages").asInt()).isEqualTo(2);

        // Member-role error paths on groupA: Bob isn't a member yet.
        JsonNode memberNotFound = request(404, "PATCH", "/groups/" + groupAId + "/members/" + bob.userId(),
                alice.device(), alice.token(), null, "{\"role\":\"admin\"}");
        assertThat(memberNotFound.at("/error/code").asText()).isEqualTo("MEMBER_NOT_FOUND");

        String code = request(201, "POST", "/groups/" + groupAId + "/invitations", alice.device(), alice.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null, "");

        JsonNode cannotChangeOwner = request(422, "PATCH", "/groups/" + groupAId + "/members/" + alice.userId(),
                alice.device(), alice.token(), null, "{\"role\":\"member\"}");
        assertThat(cannotChangeOwner.at("/error/code").asText()).isEqualTo("CANNOT_CHANGE_OWNER_ROLE");

        JsonNode invalidRole = request(422, "PATCH", "/groups/" + groupAId + "/members/" + bob.userId(),
                alice.device(), alice.token(), null, "{\"role\":\"owner\"}");
        assertThat(invalidRole.at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");
    }

}
