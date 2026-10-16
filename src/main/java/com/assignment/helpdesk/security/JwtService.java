package com.assignment.helpdesk.security;

import com.assignment.helpdesk.configs.JwtProperties;
import com.assignment.helpdesk.entities.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final String CLAIM_ORGANIZATION = "org";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public String generateToken(Long userId, Long organizationId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(userId))
                .claim(CLAIM_ORGANIZATION, organizationId)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiration())))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return properties.expiration().toSeconds();
    }

    public Optional<TokenClaims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Number organizationId = claims.get(CLAIM_ORGANIZATION, Number.class);
            String role = claims.get(CLAIM_ROLE, String.class);
            if (claims.getSubject() == null || organizationId == null || role == null) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(
                    Long.valueOf(claims.getSubject()),
                    organizationId.longValue(),
                    Role.valueOf(role)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record TokenClaims(Long userId, Long organizationId, Role role) {
    }
}
