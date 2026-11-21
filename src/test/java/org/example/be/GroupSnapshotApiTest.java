package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

class GroupSnapshotApiTest extends AbstractApiTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void rejoiningRestoresLiveAccessAndLeavingAgainReplacesTheSnapshot() throws Exception {
        var fixture = createGroupWithMember();
        Account owner = fixture.owner();
        Account member = fixture.member();
        String group = fixture.group();
        String expenses = group + "/expenses";
        JsonNode firstLeave = request(200, "POST", group + "/leave", member.device(), member.token(), null, null);
        String newExpense = createSoloExpense(owner, expenses, "after-first-leave", "15.00");
        request(404, "GET", expenses + "/" + newExpense, member.device(), member.token(), null, null);

        JsonNode rejoined = request(200, "POST", fixture.join(), member.device(), member.token(), null, null);
        assertThat(rejoined.path("accessMode").asText()).isEqualTo("full");
        assertThat(rejoined.path("snapshotCapturedAt").isMissingNode()).isTrue();
        request(200, "GET", expenses + "/" + newExpense, member.device(), member.token(), null, null);

        JsonNode secondLeave = request(200, "POST", group + "/leave", member.device(), member.token(), null, null);
        assertThat(secondLeave.path("accessMode").asText()).isEqualTo("snapshot");
        assertThat(OffsetDateTime.parse(secondLeave.path("snapshotCapturedAt").asText()))
                .isAfter(OffsetDateTime.parse(firstLeave.path("snapshotCapturedAt").asText()));
        createSoloExpense(owner, expenses, "after-second-leave", "30.00");
        JsonNode frozen = request(200, "GET", expenses, member.device(), member.token(), null, null);
        assertThat(frozen.path("data")).hasSize(1);
        assertThat(frozen.at("/data/0/id").asText()).isEqualTo(newExpense);
    }

    @Test
    void activityFeedRemainsFrozenAfterLeavingAndClosing() throws Exception {
        var fixture = createGroupWithMember();
        Account owner = fixture.owner();
        Account member = fixture.member();
        String group = fixture.group();
        String expenses = group + "/expenses";
        String beforeLeave = createSoloExpense(owner, expenses, "before-leave", "10.00");
        request(200, "POST", group + "/leave", member.device(), member.token(), null, null);
        String activities = group + "/activities";
        JsonNode frozen = request(200, "GET", activities, member.device(), member.token(), null, null).path("data");
        assertThat(frozen).anySatisfy(activity ->
                assertThat(activity.path("targetId").asText()).isEqualTo(beforeLeave));

        String afterLeave = createSoloExpense(owner, expenses, "after-leave", "20.00");
        assertThat(request(200, "GET", activities, member.device(), member.token(), null, null).path("data"))
                .isEqualTo(frozen);

        request(200, "POST", group + "/close", owner.device(), owner.token(), null, null);
        assertThat(request(200, "GET", activities, member.device(), member.token(), null, null).path("data"))
                .isEqualTo(frozen);
        JsonNode current = request(200, "GET", activities, owner.device(), owner.token(), null, null).path("data");
        assertThat(current).anySatisfy(activity ->
                assertThat(activity.path("targetId").asText()).isEqualTo(afterLeave))
                .anySatisfy(activity -> assertThat(activity.path("type").asText()).isEqualTo("group_closed"));
    }

    @Test
    void leavingDuringAnExpenseCorrectionKeepsAmountsAndSplitsFromOneVersion() throws Exception {
        var fixture = createGroupWithMember();
        Account owner = fixture.owner();
        Account member = fixture.member();
        String group = fixture.group();
        String expenses = group + "/expenses";
        String expense = expenses + "/" + createSoloExpense(owner, expenses, "concurrent-expense", "25.00");
        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<JsonNode> leaving;
            try (var lock = jdbc.getDataSource().getConnection()) {
                lock.setAutoCommit(false);
                int blockerPid;
                try (var lockMember = lock.prepareStatement("""
                        SELECT pg_backend_pid() FROM tblGroupMember
                        WHERE tblGroupid = ? AND tblUserid = ? FOR UPDATE
                        """)) {
                    lockMember.setString(1, fixture.groupId());
                    lockMember.setString(2, member.userId());
                    try (var row = lockMember.executeQuery()) {
                        assertThat(row.next()).isTrue();
                        blockerPid = row.getInt(1);
                    }
                }
                leaving = executor.submit(() ->
                        request(200, "POST", group + "/leave", member.device(), member.token(), null, null));
                try {
                    awaitBlockedTransaction(blockerPid);
                    JsonNode correction = request(201, "POST", expense + "/corrections", owner.device(),
                            owner.token(), "concurrent-correction", """
                            {"type":"correction","reason":"Update the amount","baseVersion":1,
                             "proposed":{"totalAmount":"50.00","participants":[{"userId":"%s"}]}}
                            """.formatted(owner.userId()));
                    assertThat(correction.path("status").asText()).isEqualTo("completed");
                } finally {
                    lock.rollback();
                }
            }
            assertThat(leaving.get(10, TimeUnit.SECONDS).path("accessMode").asText()).isEqualTo("snapshot");
        }
        JsonNode frozen = request(200, "GET", expense, member.device(), member.token(), null, null);
        assertThat(frozen.path("version").asInt()).isOne();
        assertThat(frozen.path("totalAmount").asText()).isEqualTo("25.00");
        assertThat(frozen.at("/splits/0/amount").asText()).isEqualTo("25.00");
        JsonNode current = request(200, "GET", expense, owner.device(), owner.token(), null, null);
        assertThat(current.path("version").asInt()).isEqualTo(2);
        assertThat(current.path("totalAmount").asText()).isEqualTo("50.00");
        assertThat(current.at("/splits/0/amount").asText()).isEqualTo("50.00");
    }

    private void awaitBlockedTransaction(int blockerPid) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            Boolean blocked = jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid)))
                    """, Boolean.class, blockerPid);
            if (Boolean.TRUE.equals(blocked)) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("The leave request did not wait on the membership row lock");
    }

    private record GroupFixture(Account owner, Account member, String groupId, String join) {
        String group() {
            return "/groups/" + groupId;
        }
    }

    private GroupFixture createGroupWithMember() throws Exception {
        Account owner = registerUser("snapshot-owner");
        Account member = registerUser("snapshot-member");
        String groupId = request(201, "POST", "/groups", owner.device(), owner.token(), null,
                "{\"name\":\"Snapshot group\"}").path("id").asText();
        String code = request(201, "POST", "/groups/" + groupId + "/invitations", owner.device(), owner.token(), null,
                "{}").path("code").asText();
        String join = "/invitations/" + code + "/accept";
        request(200, "POST", join, member.device(), member.token(), null, null);
        return new GroupFixture(owner, member, groupId, join);
    }

    @Test
    void formerMembersSeeLeaveTimeGroupAndExpensesEvenAfterClosure() throws Exception {
        Account owner = registerUser("snapshot-owner");
        Account former = registerUser("snapshot-former");
        Account newcomer = registerUser("snapshot-newcomer");
        String groupId = request(201, "POST", "/groups", owner.device(), owner.token(), null,
                "{\"name\":\"Original name\",\"description\":\"Original description\"}").path("id").asText();
        String group = "/groups/" + groupId;
        String code = request(201, "POST", group + "/invitations", owner.device(), owner.token(), null,
                "{}").path("code").asText();
        String join = "/invitations/" + code + "/accept";
        request(200, "POST", join, former.device(), former.token(), null, null);
        String expenses = group + "/expenses";
        String first = createSoloExpense(owner, expenses, "first", "25.00");
        String second = createSoloExpense(owner, expenses, "second", "15.00");
        request(200, "POST", group + "/leave", former.device(), former.token(), null, null);

        request(200, "POST", join, newcomer.device(), newcomer.token(), null, null);
        request(200, "PATCH", group, owner.device(), owner.token(), null,
                "{\"name\":\"Current name\",\"description\":\"Current description\"}");
        String third = createSoloExpense(owner, expenses, "third", "5.00");
        request(201, "POST", expenses + "/" + first + "/corrections", owner.device(), owner.token(), "update-first",
                """
                {"type":"correction","reason":"Update the amount","baseVersion":1,
                 "proposed":{"totalAmount":"40.00","participants":[{"userId":"%s"}]}}
                """.formatted(owner.userId()));

        JsonNode detail = request(200, "GET", group, former.device(), former.token(), null, null);
        assertThat(detail.path("description").asText()).isEqualTo("Original description");
        assertThat(detail.path("members")).hasSize(2).noneMatch(member ->
                newcomer.userId().equals(member.path("userId").asText()));
        JsonNode firstPage = request(200, "GET", expenses + "?sort=totalAmount&pageSize=1&category=food",
                former.device(), former.token(), null, null);
        assertThat(firstPage.at("/data/0/id").asText()).isEqualTo(second);
        assertThat(firstPage.at("/pagination/totalItems").asInt()).isEqualTo(2);
        JsonNode secondPage = request(200, "GET", expenses + "?sort=totalAmount&pageSize=1&page=2",
                former.device(), former.token(), null, null);
        assertThat(secondPage.at("/data/0/id").asText()).isEqualTo(first);
        assertThat(secondPage.at("/data/0/accessMode").isMissingNode()).isTrue();
        JsonNode frozenExpense = request(200, "GET", expenses + "/" + first, former.device(), former.token(), null, null);
        assertThat(frozenExpense.path("totalAmount").asText()).isEqualTo("25.00");
        assertThat(frozenExpense.at("/splits/0/amount").asText()).isEqualTo("25.00");
        assertThat(frozenExpense.path("version").asInt()).isOne();
        request(404, "GET", expenses + "/" + third, former.device(), former.token(), null, null);
        assertThat(request(200, "GET", expenses + "/" + first, owner.device(), owner.token(), null, null)
                .path("totalAmount").asText()).isEqualTo("40.00");

        request(200, "POST", group + "/close", owner.device(), owner.token(), null, null);
        JsonNode closed = request(200, "GET", group, former.device(), former.token(), null, null);
        assertThat(closed.path("accessMode").asText()).isEqualTo("closed");
        assertThat(closed.path("name").asText()).isEqualTo("Original name");
        request(404, "GET", expenses + "/" + third, former.device(), former.token(), null, null);

        // The old storage format had no members or expenses to replay.
        assertThat(jdbc.update("""
                UPDATE tblGroupMember SET snapshotData = ? WHERE tblGroupid = ? AND tblUserid = ?
                """, "{\"balance\":\"0.00\"}", groupId, former.userId())).isOne();
        JsonNode legacy = request(200, "GET", group, former.device(), former.token(), null, null);
        assertThat(legacy.path("name").asText()).isEqualTo("Original name");
        assertThat(legacy.path("members")).isEmpty();
        assertThat(request(200, "GET", expenses, former.device(), former.token(), null, null).path("data")).isEmpty();
        request(404, "GET", expenses + "/" + first, former.device(), former.token(), null, null);
    }

    private String createSoloExpense(Account owner, String expenses, String title, String amount) throws Exception {
        return request(201, "POST", expenses, owner.device(), owner.token(), title, """
                {"title":"%s","totalAmount":"%s","category":"Food","payerUserId":"%s",
                 "splitMethod":"equal","participants":[{"userId":"%s"}]}
                """.formatted(title, amount, owner.userId(), owner.userId())).path("id").asText();
    }
}
