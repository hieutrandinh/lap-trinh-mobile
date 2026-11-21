package org.example.be.core.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.exception.ErrorRes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json) throws Exception {
        var bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        org.springframework.security.web.AuthenticationEntryPoint authentication = (request, response, failure) -> {
            bearerEntryPoint.commence(request, response, failure);
            ErrorCode code = ErrorCode.UNAUTHENTICATED;
            if (failure.getCause() instanceof JwtValidationException invalid
                    && invalid.getErrors().stream().anyMatch(error -> error.getDescription() != null
                        && error.getDescription().startsWith("Jwt expired at"))) {
                code = ErrorCode.TOKEN_EXPIRED;
            }
            writeError(response, json, code);
        };
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/auth/register", "/auth/login", "/auth/refresh").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(authentication)
                        .accessDeniedHandler((request, response, failure) -> writeError(response, json, ErrorCode.FORBIDDEN)))
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint(authentication)
                        .jwt(jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }

    private static void writeError(HttpServletResponse response, ObjectMapper json, ErrorCode code) throws IOException {
        response.setStatus(code.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getOutputStream(), ErrorRes.of(code.name(), code.getMessage()));
    }

}
