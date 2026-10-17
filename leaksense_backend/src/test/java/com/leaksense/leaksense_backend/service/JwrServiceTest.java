package com.leaksense.leaksense_backend.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "a-completely-different-test-secret-also-32-bytes-or-more";

    private JwtService jwtService;

    private JwtService build(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationMs", expirationMs);
        return service;
    }

    @BeforeEach
    void setUp() {
        jwtService = build(SECRET, 3_600_000L);
    }

    @Test
    @DisplayName("The username can be read back from a token")
    void usernameRoundTrip() {
        String token = jwtService.generateToken("admin", "ADMIN");

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    @DisplayName("The token carries the role claim")
    void tokenCarriesRole() {
        String token = jwtService.generateToken("operator", "OPERATOR");
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String role = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().get("role", String.class);

        assertThat(role).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("A fresh token is valid")
    void freshTokenIsValid() {
        String token = jwtService.generateToken("admin", "ADMIN");

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("The token expires about one hour after it is created")
    void tokenExpiresAfterConfiguredTime() {
        String token = jwtService.generateToken("admin", "ADMIN");
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        Date expiry = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getExpiration();
        long millisLeft = expiry.getTime() - System.currentTimeMillis();

        assertThat(millisLeft).isBetween(3_590_000L, 3_600_000L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "not.a.token", "aaa.bbb.ccc"})
    @DisplayName("Garbage text is never a valid token")
    void garbageIsInvalid(String token) {
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("A token signed with a different secret is rejected")
    void tokenFromAnotherSecretIsInvalid() {
        String forged = build(OTHER_SECRET, 3_600_000L).generateToken("admin", "ADMIN");

        assertThat(jwtService.isValid(forged)).isFalse();
    }

    @Test
    @DisplayName("An expired token is rejected")
    void expiredTokenIsInvalid() {
        JwtService expiredIssuer = build(SECRET, -60_000L);
        String expired = expiredIssuer.generateToken("admin", "ADMIN");

        assertThat(jwtService.isValid(expired)).isFalse();
        assertThatThrownBy(() -> jwtService.extractUsername(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
