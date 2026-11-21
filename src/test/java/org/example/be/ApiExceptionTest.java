package org.example.be;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.be.core.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;

class ApiExceptionTest extends AbstractApiTest {

    @Test
    void invalidHttpRequestsReturnStructuredErrors() throws Exception {
        Account me = registerUser("http-errors");
        assertThat(request(400, "POST", "/groups", me.device(), me.token(), null, "{")
                .at("/error/code").asText()).isEqualTo("MALFORMED_REQUEST");
        assertThat(request(400, "POST", "/groups", me.device(), me.token(), null, "group", "text/plain")
                .at("/error/code").asText()).isEqualTo("MALFORMED_REQUEST");
        assertThat(request(405, "PUT", "/groups", me.device(), me.token(), null, "{}")
                .at("/error/code").asText()).isEqualTo("MALFORMED_REQUEST");
        assertThat(request(422, "POST", "/groups", me.device(), me.token(), null, "{\"name\":[]}")
                .at("/error/details/0/field").asText()).isEqualTo("name");
        assertThat(request(422, "GET", "/sync/changes", null, me.token(), null, null)
                .at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(request(422, "GET", "/sync/changes?limit=bad", me.device(), me.token(), null, null)
                .at("/error/code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(request(404, "GET", "/unknown-api-route", me.device(), me.token(), null, null)
                .at("/error/code").asText()).isEqualTo("NOT_FOUND");
    }

    @Test
    void unexpectedExceptionsReturnAGenericErrorAndRequestId() {
        var response = new GlobalExceptionHandler().handleUnexpected(new IllegalStateException("fixture failure"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getError().getRequestId()).startsWith("req_");
        assertThat(response.getBody().getError().getMessage()).doesNotContain("fixture failure");
    }
}
