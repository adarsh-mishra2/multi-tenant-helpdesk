package com.assignment.helpdesk.security;

import com.assignment.helpdesk.configs.JwtProperties;
import com.assignment.helpdesk.entities.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "sIfZpGKgpV9hhm+gn6kiLu3RLXhwEulqxL5BisMV1js=";
    private static final String OTHER_SECRET = "7tZ6Vbv0LDUWWc8wBX6iPiPIWgG9ncbPjz/uD5Jw4Y1bGKIkMxe3Q4vZhEmGZ0ZLqDMlEn+dtyxuZR2V/je3JQ==";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15), "test-issuer"));

    @Test
    void generatedTokenRoundTripsClaims() {
        String token = jwtService.generateToken(7L, 3L, Role.AGENT);

        assertThat(jwtService.parseToken(token)).hasValueSatisfying(claims -> {
            assertThat(claims.userId()).isEqualTo(7L);
            assertThat(claims.organizationId()).isEqualTo(3L);
            assertThat(claims.role()).isEqualTo(Role.AGENT);
        });
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(900L);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtService attacker = new JwtService(new JwtProperties(OTHER_SECRET, Duration.ofMinutes(15), "test-issuer"));
        String forged = attacker.generateToken(1L, 1L, Role.ORG_ADMIN);

        assertThat(jwtService.parseToken(forged)).isEmpty();
    }

    @Test
    void tokenFromDifferentIssuerIsRejected() {
        JwtService other = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15), "someone-else"));

        assertThat(jwtService.parseToken(other.generateToken(1L, 1L, Role.AGENT))).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-120), "test-issuer"));

        assertThat(jwtService.parseToken(shortLived.generateToken(1L, 1L, Role.AGENT))).isEmpty();
    }

    @Test
    void garbageIsRejectedWithoutThrowing() {
        assertThat(jwtService.parseToken("not-a-jwt")).isEmpty();
        assertThat(jwtService.parseToken("")).isEmpty();
    }
}
