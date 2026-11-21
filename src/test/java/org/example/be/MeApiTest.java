package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.example.be.core.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;

class MeApiTest extends AbstractApiTest {

    @Autowired
    JwtService jwtService;

    @Value("${app.jwt.secret}")
    String jwtSecret;

    @Test
    void actuatorDiscoveryRequiresAuthenticationAndAdvertisesHealth() throws Exception {
        assertThat(request(401, "GET", "/actuator", null, null, null, null)
                .at("/error/code").asText()).isEqualTo("UNAUTHENTICATED");
        Account me = registerUser("actuator");
        JsonNode links = request(200, "GET", "/actuator", me.device(), me.token(), null, null);
        assertThat(links.at("/_links/health/href").asText()).endsWith("/actuator/health");
        assertThat(links.at("/_links/health-path/templated").asBoolean()).isTrue();
        request(401, "GET", "/actuator/health/db", null, null, null, null);
        request(404, "GET", "/actuator/health/db", me.device(), me.token(), null, null);
    }

    @Test
    void expiredAccessTokenReturnsTheSessionError() throws Exception {
        Account me = registerUser("expired-token");
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .subject(me.userId())
                .claim("did", me.device())
                .issueTime(Date.from(now.minusSeconds(600)))
                .expirationTime(Date.from(now.minusSeconds(300)))
                .build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        token.sign(new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8)));
        assertThat(request(401, "GET", "/me", me.device(), token.serialize(), null, null)
                .at("/error/code").asText()).isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void validTokenForAnAbsentUserReturnsUserNotFound() throws Exception {
        String device = UUID.randomUUID().toString();
        String token = jwtService.generateAccessToken(UUID.randomUUID().toString(), device);
        assertThat(request(404, "GET", "/me", device, token, null, null)
                .at("/error/code").asText()).isEqualTo("USER_NOT_FOUND");
        assertThat(request(404, "PATCH", "/me/notification-settings", device, token, null,
                "{\"groupAlerts\":false}").at("/error/code").asText()).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void healthIsAvailableWithoutAuthentication() throws Exception {
        JsonNode health = request(200, "GET", "/actuator/health", null, null, null, null);
        assertThat(health.path("status").asText()).isEqualTo("UP");
    }

    @Test
    void getMeAndUnauthenticatedAccessFollowTheApiContract() throws Exception {
        Account me = registerUser("me");

        JsonNode profile = request(200, "GET", "/me", me.device(), me.token(), null, null);
        assertThat(profile.path("id").asText()).isEqualTo(me.userId());
        assertThat(profile.path("email").asText()).isEqualTo(me.email());
        assertThat(profile.at("/notificationSettings/bucketAlerts").asBoolean()).isTrue();
        assertThat(profile.at("/notificationSettings/groupAlerts").asBoolean()).isTrue();

        JsonNode unauthenticated = request(401, "GET", "/me", me.device(), null, null, null);
        assertThat(unauthenticated.at("/error/code").asText()).isEqualTo("UNAUTHENTICATED");

        JsonNode garbageToken = request(401, "GET", "/me", me.device(), "not-a-real-jwt", null, null);
        assertThat(garbageToken.at("/error/code").asText()).isEqualTo("UNAUTHENTICATED");
    }

}
