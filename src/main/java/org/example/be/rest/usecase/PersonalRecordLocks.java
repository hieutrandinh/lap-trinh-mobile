package org.example.be.rest.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersonalRecordLocks {
    private final JdbcTemplate jdbc;

    public void lockCreation(String type, String id) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                row -> { }, "finance:create:" + type + ":" + id);
    }
}
