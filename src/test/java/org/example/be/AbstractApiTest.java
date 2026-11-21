package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared HTTP helper for black-box API tests: real server, real Testcontainers Postgres, no mocks.
 * register-limit is raised because these tests register many accounts from the same loopback
 * address in one run; production keeps its default of 5/hour/IP (see AuthThrottle).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.auth.register-limit=1000")
abstract class AbstractApiTest {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper json;

    private final HttpClient http = HttpClient.newHttpClient();

    record Account(String device, String token, String userId, String email) {
    }

    Account registerUser(String label) throws Exception {
        String device = UUID.randomUUID().toString();
        String email = label + "-" + device + "@example.test";
        JsonNode res = request(201, "POST", "/auth/register", device, null, null, """
                {"email":"%s","password":"TestPassword123","displayName":"%s","platform":"web"}
                """.formatted(email, label));
        return new Account(device, res.path("accessToken").asText(), res.at("/user/id").asText(), email);
    }

    JsonNode request(int status, String method, String path, String device, String token, String key, String body)
            throws Exception {
        return request(status, method, path, device, token, key, body, "application/json");
    }

    JsonNode request(int status, String method, String path, String device, String token, String key, String body,
                     String contentType) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (device != null) {
            builder.header("X-Device-Id", device);
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (key != null) {
            builder.header("Idempotency-Key", key);
        }
        if (body != null) {
            builder.header("Content-Type", contentType);
        }
        var response = http.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("%s %s -> %s: %s", method, path, response.statusCode(), response.body())
                .isEqualTo(status);
        return status == 204 || response.body().isBlank() ? null : json.readTree(response.body());
    }

}
