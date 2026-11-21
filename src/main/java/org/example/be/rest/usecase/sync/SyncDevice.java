package org.example.be.rest.usecase.sync;

import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SyncDevice {
    private SyncDevice() {}

    public static void requireMatchingHeader(Jwt jwt, String deviceId) {
        if (!deviceId.equals(jwt.getClaimAsString("did"))) {
            throw new BusinessLogicException(ErrorCode.UNAUTHENTICATED);
        }
    }

    static boolean isActive(JdbcTemplate jdbc, String userId, String deviceId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM tblDevice WHERE id = ? AND tblUserid = ? AND signedOutAt IS NULL)
                """, Boolean.class, deviceId, userId));
    }
}
