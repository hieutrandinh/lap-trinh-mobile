package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AuthFlowApiTest extends AbstractApiTest {

    @Test
    void loginRefreshLogoutAndInvalidCredentialsFollowTheApiContract() throws Exception {
        String device = UUID.randomUUID().toString();
        String email = "flow-" + device + "@example.test";
        String password = "TestPassword123";

        JsonNode registered = request(201, "POST", "/auth/register", device, null, null, """
                {"email":"%s","password":"%s","displayName":"Flow","platform":"web"}
                """.formatted(email, password));
        String firstRefreshToken = registered.path("refreshToken").asText();
        assertThat(firstRefreshToken).isNotBlank();

        JsonNode wrongPassword = request(401, "POST", "/auth/login", device, null, null, """
                {"email":"%s","password":"WrongPassword1","platform":"web"}
                """.formatted(email));
        assertThat(wrongPassword.at("/error/code").asText()).isEqualTo("INVALID_CREDENTIALS");

        JsonNode login = request(200, "POST", "/auth/login", device, null, null, """
                {"email":"%s","password":"%s","platform":"web"}
                """.formatted(email, password));
        String token = login.path("accessToken").asText();
        String refreshToken = login.path("refreshToken").asText();
        assertThat(login.at("/user/email").asText()).isEqualTo(email);
        assertThat(login.path("onlineSummary").isMissingNode() || login.path("onlineSummary").isNull()
                || login.at("/onlineSummary/entryCount").asInt() == 0).isTrue();

        JsonNode refreshed = request(200, "POST", "/auth/refresh", device, null, null,
                "{\"refreshToken\":\"" + refreshToken + "\"}");
        String rotatedRefreshToken = refreshed.path("refreshToken").asText();
        assertThat(refreshed.path("accessToken").asText()).isNotBlank();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        // Refresh tokens rotate on use: the one just consumed is no longer valid.
        JsonNode reuseOldRefresh = request(401, "POST", "/auth/refresh", device, null, null,
                "{\"refreshToken\":\"" + refreshToken + "\"}");
        assertThat(reuseOldRefresh.at("/error/code").asText()).isEqualTo("INVALID_REFRESH_TOKEN");

        request(204, "POST", "/auth/logout", device, token, null,
                "{\"refreshToken\":\"" + rotatedRefreshToken + "\"}");

        // Logout revokes the device's refresh token.
        JsonNode afterLogout = request(401, "POST", "/auth/refresh", device, null, null,
                "{\"refreshToken\":\"" + rotatedRefreshToken + "\"}");
        assertThat(afterLogout.at("/error/code").asText()).isEqualTo("INVALID_REFRESH_TOKEN");

        // Logout is idempotent: calling it again with an already-revoked token still succeeds.
        request(204, "POST", "/auth/logout", device, token, null,
                "{\"refreshToken\":\"" + rotatedRefreshToken + "\"}");
    }

    @Test
    void duplicateEmailRegistrationIsRejected() throws Exception {
        String device = UUID.randomUUID().toString();
        String email = "dup-" + device + "@example.test";
        request(201, "POST", "/auth/register", device, null, null, """
                {"email":"%s","password":"TestPassword123","displayName":"Dup","platform":"web"}
                """.formatted(email));

        String device2 = UUID.randomUUID().toString();
        JsonNode dup = request(409, "POST", "/auth/register", device2, null, null, """
                {"email":"%s","password":"AnotherPass123","displayName":"Dup2","platform":"web"}
                """.formatted(email));
        assertThat(dup.at("/error/code").asText()).isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

}
