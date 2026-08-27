package com.aksa.mailer.auth.security;

import com.aksa.mailer.auth.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Access token'in (JWT) DOGRULANMASI. Token'i bu servis URETMEZ - uretim
 * odyssey-auth servisinde yapilir; burada sadece ayni imza anahtariyla
 * (app.jwt.secret) dogrulanip claim'ler okunur. Rol claim olarak geldigi
 * icin kullanici tablosuna ihtiyac yok (stateless).
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TEAM_ID = "teamId";
    private static final String CLAIM_TEAM_IDS = "teamIds";
    private static final String CLAIM_FULL_NAME = "fullName";
    private static final String CLAIM_DEPARTMENT = "department";

    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Optional<AccessTokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String sicil = claims.getSubject();
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            String fullName = claims.get(CLAIM_FULL_NAME, String.class);
            String department = claims.get(CLAIM_DEPARTMENT, String.class);
            Number teamIdNumber = claims.get(CLAIM_TEAM_ID, Number.class);
            Long teamId = teamIdNumber != null ? teamIdNumber.longValue() : null;
            List<?> rawTeamIds = claims.get(CLAIM_TEAM_IDS, List.class);
            List<Long> teamIds = rawTeamIds != null
                    ? rawTeamIds.stream().map(n -> ((Number) n).longValue()).toList()
                    : (teamId != null ? List.of(teamId) : List.of());
            return Optional.of(new AccessTokenClaims(sicil, fullName, role, teamId, department, teamIds));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * teamId / teamIds, Odyssey'in ortak token'indan gelir ve Capacity Planner'in
     * takim tablosuna isaret eder. Aksa Mailer'in kendi mail_teams tablosu ayridir;
     * eslestirme Sprint 2'de (takim bazli yetki) yapilacak.
     */
    public record AccessTokenClaims(String sicil, String fullName, Role role, Long teamId, String department,
                                    List<Long> teamIds) {
    }
}
