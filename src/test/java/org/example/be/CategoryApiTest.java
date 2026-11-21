package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class CategoryApiTest extends AbstractApiTest {

    @Test
    void createListRenameAndDeleteFollowTheApiContract() throws Exception {
        Account me = registerUser("cat");

        JsonNode list = request(200, "GET", "/categories", me.device(), me.token(), null, null);
        assertThat(list.path("data")).anyMatch(c -> c.path("name").asText().equals("Uncategorized")
                && c.path("isSystem").asBoolean());

        String id = UUID.randomUUID().toString();
        JsonNode created = request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + id + "\",\"name\":\"Hobbies\"}");
        assertThat(created.path("id").asText()).isEqualTo(id);
        assertThat(created.path("version").asInt()).isEqualTo(1);
        assertThat(created.path("isSystem").asBoolean()).isFalse();

        // Same id + same content -> idempotent 200, not a new record (timestamps aren't compared:
        // Postgres truncates TIMESTAMP to microseconds, so the pre-persist in-memory value from the
        // first call can differ from the re-read value by sub-microsecond noise).
        JsonNode replay = request(200, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + id + "\",\"name\":\"Hobbies\"}");
        assertThat(replay.path("id").asText()).isEqualTo(id);
        assertThat(replay.path("version").asInt()).isEqualTo(1);
        assertThat(replay.path("name").asText()).isEqualTo("Hobbies");

        // Duplicate name (case-insensitive) -> 409 with the existing category in details.
        JsonNode dupName = request(409, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"hobbies\"}");
        assertThat(dupName.at("/error/code").asText()).isEqualTo("CATEGORY_NAME_EXISTS");
        assertThat(dupName.at("/error/details/0/existing/id").asText()).isEqualTo(id);

        JsonNode searched = request(200, "GET", "/categories?q=hobb", me.device(), me.token(), null, null);
        assertThat(searched.path("data")).hasSize(1);

        // Stale version -> 409 VERSION_CONFLICT with error.current.
        JsonNode staleRename = request(409, "PATCH", "/categories/" + id, me.device(), me.token(), null,
                "{\"version\":99,\"name\":\"Renamed\"}");
        assertThat(staleRename.at("/error/code").asText()).isEqualTo("VERSION_CONFLICT");
        assertThat(staleRename.at("/error/current").isMissingNode()).isFalse();

        JsonNode renamed = request(200, "PATCH", "/categories/" + id, me.device(), me.token(), null,
                "{\"version\":1,\"name\":\"Renamed\"}");
        assertThat(renamed.at("/category/name").asText()).isEqualTo("Renamed");
        assertThat(renamed.at("/category/version").asInt()).isEqualTo(2);
        assertThat(renamed.at("/movedExpenseCount").asInt()).isZero();

        String systemId = null;
        for (JsonNode c : list.path("data")) {
            if (c.path("isSystem").asBoolean()) {
                systemId = c.path("id").asText();
                break;
            }
        }
        assertThat(systemId).isNotNull();
        JsonNode systemRename = request(422, "PATCH", "/categories/" + systemId, me.device(), me.token(), null,
                "{\"version\":1,\"name\":\"Nope\"}");
        assertThat(systemRename.at("/error/code").asText()).isEqualTo("SYSTEM_CATEGORY_READ_ONLY");

        JsonNode deleted = request(200, "DELETE", "/categories/" + id + "?version=2", me.device(), me.token(), null,
                null);
        assertThat(deleted.path("deletedCategoryId").asText()).isEqualTo(id);
        assertThat(deleted.at("/movedTo/name").asText()).isEqualTo("Uncategorized");

        JsonNode afterDelete = request(200, "GET", "/categories", me.device(), me.token(), null, null);
        assertThat(afterDelete.path("data")).noneMatch(c -> c.path("id").asText().equals(id));
    }

}
