package org.example.be.rest.usecase.auth.support;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.auth.AuthRes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthThrottle {
    private final JdbcTemplate jdbc;

    /** Production default is 5/hour per IP; tests override this to avoid tripping it across many registrations. */
    @Value("${app.auth.register-limit:5}")
    @NonFinal
    private int registerLimit;

    @Transactional
    public AuthRes register(String address, Supplier<AuthRes> action) {
        lock("register", address, registerLimit, 3600, ErrorCode.RATE_LIMITED);
        AuthRes result = action.get();
        add("register", address, 3600);
        return result;
    }

    @Transactional
    public Login login(String address, String email, Supplier<AuthRes> action) {
        String identity = address + "\n" + email.strip().toLowerCase(Locale.ROOT);
        lock("login", identity, 5, 900, ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        try {
            AuthRes result = action.get();
            jdbc.update("DELETE FROM auth_throttle WHERE scope = 'login' AND identity = ?", identity);
            return new Login(result, null);
        } catch (BusinessLogicException failure) {
            if (failure.getErrorCode() != ErrorCode.INVALID_CREDENTIALS) throw failure;
            add("login", identity, 900);
            return new Login(null, failure);
        }
    }

    private void lock(String scope, String identity, int limit, int window, ErrorCode error) {
        jdbc.update("""
                DELETE FROM auth_throttle WHERE (scope, identity) IN (
                    SELECT scope, identity FROM auth_throttle WHERE touched_at < clock_timestamp() - INTERVAL '2 hours'
                    LIMIT 100 FOR UPDATE SKIP LOCKED
                )
                """);
        jdbc.update("INSERT INTO auth_throttle (scope, identity) VALUES (?, ?) ON CONFLICT DO NOTHING", scope, identity);
        jdbc.queryForObject("SELECT identity FROM auth_throttle WHERE scope = ? AND identity = ? FOR UPDATE",
                String.class, scope, identity);
        Long retryAfter = jdbc.queryForObject("""
                SELECT CASE WHEN count(*) >= ? THEN
                    CEIL(EXTRACT(EPOCH FROM min(attempt) + make_interval(secs => ?) - clock_timestamp()))::bigint
                    ELSE 0 END
                FROM auth_throttle CROSS JOIN LATERAL unnest(attempts) AS attempt
                WHERE scope = ? AND identity = ? AND attempt > clock_timestamp() - make_interval(secs => ?)
                """, Long.class, limit, window, scope, identity, window);
        if (retryAfter != null && retryAfter > 0) {
            throw new BusinessLogicException(error, Map.of("retryAfter", retryAfter));
        }
    }

    private void add(String scope, String identity, int window) {
        jdbc.update("""
                UPDATE auth_throttle SET attempts = array_append(ARRAY(
                    SELECT attempt FROM unnest(attempts) AS attempt
                    WHERE attempt > clock_timestamp() - make_interval(secs => ?)
                ), clock_timestamp()), touched_at = clock_timestamp()
                WHERE scope = ? AND identity = ?
                """, window, scope, identity);
    }

    public record Login(AuthRes result, BusinessLogicException failure) {
        public AuthRes response() {
            if (failure != null) throw failure;
            return result;
        }
    }
}
