package com.aksa.mailer.auth.security;

import com.aksa.mailer.auth.domain.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Oturumdaki kullanicinin token'dan gelen bilgileri.
 *
 * JwtCookieAuthFilter, dogruladigi claim'leri Authentication.details'e
 * koyuyor; controller'lar oraya elle uzanmasin diye bu sarmalayici var.
 *
 * YETKI KURALI:
 *   ADMIN  - butun takimlar
 *   PO     - yalnizca token'daki teamIds
 *
 * teamIds odyssey-auth'un user_team_ids tablosundan geliyor ve
 * mail_teams.id ile AYNI kimlikleri tasiyor (bkz. V3__takimlar.sql).
 */
public record OturumKullanicisi(String sicil, Role role, List<Long> teamIds) {

    public static OturumKullanicisi of(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails()
                instanceof JwtTokenProvider.AccessTokenClaims claims)) {
            // Buraya normalde gelinmez - SecurityConfig cerezsiz istegi zaten
            // 401'e cevirir. Yine de sessizce "yetkisi yok" sayip gecmiyoruz.
            throw new AccessDeniedException("Oturum bilgisi okunamadı.");
        }
        return new OturumKullanicisi(claims.sicil(), claims.role(), claims.teamIds());
    }

    public boolean adminMi() {
        return role == Role.ADMIN;
    }

    public boolean erisebilir(Long teamId) {
        return adminMi() || (teamId != null && teamIds.contains(teamId));
    }

    /**
     * Erisim yoksa AccessDeniedException - GlobalExceptionHandler bunu 403'e
     * cevirir. 404 DONDURULMUYOR: belgenin varligini gizlemek bu ic uygulamada
     * bir kazanc saglamiyor, "yetkin yok" demek kullaniciya daha yardimci.
     */
    public void dogrula(Long teamId) {
        if (!erisebilir(teamId)) {
            throw new AccessDeniedException(
                    "Bu takıma erişim yetkiniz yok. Yetkili olduğunuz takımlar: " + teamIds);
        }
    }
}
