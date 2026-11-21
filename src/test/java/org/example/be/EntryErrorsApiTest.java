package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class EntryErrorsApiTest extends AbstractApiTest {

    @Test
    void deletedRecordCannotBeRecreatedWithTheSameId() throws Exception {
        Account me = registerUser("del");
        String categoryId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + categoryId + "\",\"name\":\"Deletable\"}");

        String entryId = UUID.randomUUID().toString();
        request(201, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"income","name":"Gift","amount":"5.00","categoryId":"%s"}
                """.formatted(entryId, categoryId));
        request(204, "DELETE", "/entries/" + entryId + "?version=1", me.device(), me.token(), null, null);

        JsonNode recreated = request(409, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"income","name":"Gift again","amount":"5.00","categoryId":"%s"}
                """.formatted(entryId, categoryId));
        assertThat(recreated.at("/error/code").asText()).isEqualTo("RECORD_DELETED");
    }

    @Test
    void incomeCannotHaveABucketAndAnExplicitBucketMustTrackTheCategory() throws Exception {
        Account me = registerUser("bk");
        String categoryId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + categoryId + "\",\"name\":\"Salary2\"}");
        String otherCategoryId = UUID.randomUUID().toString();
        request(201, "POST", "/categories", me.device(), me.token(), null,
                "{\"id\":\"" + otherCategoryId + "\",\"name\":\"Other\"}");
        String bucketId = UUID.randomUUID().toString();
        request(201, "POST", "/buckets", me.device(), me.token(), null, """
                {"id":"%s","name":"Other budget","monthlyTarget":"50.00","categoryIds":["%s"]}
                """.formatted(bucketId, otherCategoryId));

        JsonNode incomeWithBucket = request(422, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"income","name":"Salary","amount":"100.00","categoryId":"%s","bucketId":"%s"}
                """.formatted(UUID.randomUUID(), categoryId, bucketId));
        assertThat(incomeWithBucket.at("/error/code").asText()).isEqualTo("BUCKET_NOT_ALLOWED_FOR_INCOME");

        JsonNode mismatch = request(422, "POST", "/entries", me.device(), me.token(), null, """
                {"id":"%s","type":"expense","name":"Mismatched","amount":"10.00","categoryId":"%s","bucketId":"%s"}
                """.formatted(UUID.randomUUID(), categoryId, bucketId));
        assertThat(mismatch.at("/error/code").asText()).isEqualTo("BUCKET_CATEGORY_MISMATCH");
    }

    @Test
    void sharedEntryOnlyAllowsCategoryBucketAndNoteChanges() throws Exception {
        Account alice = registerUser("alice");
        Account bob = registerUser("bob");
        String groupId = request(201, "POST", "/groups", alice.device(), alice.token(), null,
                "{\"name\":\"Trip\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", alice.device(), alice.token(),
                null, "{}").path("code").asText();
        request(200, "POST", "/invitations/" + code + "/accept", bob.device(), bob.token(), null, "");
        request(201, "POST", "/groups/" + groupId + "/expenses", alice.device(), alice.token(), "shared-ro-1", """
                {"title":"Dinner","totalAmount":"40.00","category":"Food","payerUserId":"%s","splitMethod":"equal",
                 "participants":[{"userId":"%s"},{"userId":"%s"}]}
                """.formatted(alice.userId(), alice.userId(), bob.userId()));

        JsonNode entries = request(200, "GET", "/entries", bob.device(), bob.token(), null, null);
        JsonNode sharedEntry = entries.path("data").get(0);
        assertThat(sharedEntry.path("isShared").asBoolean()).isTrue();
        String entryId = sharedEntry.path("id").asText();
        int version = sharedEntry.path("version").asInt();

        JsonNode readOnly = request(422, "PATCH", "/entries/" + entryId, bob.device(), bob.token(), null,
                "{\"version\":" + version + ",\"amount\":\"999.00\"}");
        assertThat(readOnly.at("/error/code").asText()).isEqualTo("SHARED_ENTRY_READ_ONLY");
        assertThat(readOnly.at("/error/details/0/fields/0").asText()).isEqualTo("amount");

        // Note is explicitly allowed to change on a shared entry.
        JsonNode noteOk = request(200, "PATCH", "/entries/" + entryId, bob.device(), bob.token(), null,
                "{\"version\":" + version + ",\"note\":\"My private note\"}");
        assertThat(noteOk.path("note").asText()).isEqualTo("My private note");
    }

}
