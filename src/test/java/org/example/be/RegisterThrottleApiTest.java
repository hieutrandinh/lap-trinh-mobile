package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.auth.register-limit=1")
class RegisterThrottleApiTest extends AbstractApiTest {

    @Test
    void repeatedRegistrationsFromTheSameAddressAreRateLimited() throws Exception {
        registerUser("first-registration");
        String device = UUID.randomUUID().toString();
        var blocked = request(429, "POST", "/auth/register", device, null, null, """
                {"email":"second-%s@example.test","password":"TestPassword123","displayName":"Second","platform":"web"}
                """.formatted(device));
        assertThat(blocked.at("/error/code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(blocked.at("/error/details/0/retryAfter").asLong()).isPositive();
    }
}
