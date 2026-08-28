package com.aksa.mailer.auth;

import com.aksa.mailer.auth.domain.Role;
import com.aksa.mailer.auth.security.JwtTokenProvider;
import com.aksa.mailer.auth.security.OturumKullanicisi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Takim yetkisi. Bu testler bir GUVENLIK sinirini koruyor: PO yalnizca
 * token'indaki takimlarin verisine erisebilmeli.
 */
class OturumKullanicisiTest {

    private static Authentication oturum(Role role, List<Long> teamIds) {
        var claims = new JwtTokenProvider.AccessTokenClaims(
                "30816", "Ece Sena Salan", role, teamIds.isEmpty() ? null : teamIds.get(0),
                "İş Zekası Ekibi", teamIds);
        var auth = new UsernamePasswordAuthenticationToken("30816", null, List.of());
        auth.setDetails(claims);
        return auth;
    }

    @Test
    @DisplayName("PO yalnızca kendi takımlarına erişir")
    void poKendiTakimlari() {
        OturumKullanicisi k = OturumKullanicisi.of(oturum(Role.PO, List.of(5L, 7L)));

        assertThat(k.erisebilir(5L)).isTrue();
        assertThat(k.erisebilir(7L)).isTrue();
        assertThat(k.erisebilir(1L)).isFalse();
        assertThat(k.adminMi()).isFalse();
    }

    @Test
    @DisplayName("başka takıma erişim 403 ile reddedilir")
    void baskaTakim() {
        OturumKullanicisi k = OturumKullanicisi.of(oturum(Role.PO, List.of(2L)));

        assertThatThrownBy(() -> k.dogrula(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("erişim yetkiniz yok");
    }

    @Test
    @DisplayName("kendi takımına erişim engellenmez")
    void kendiTakimi() {
        OturumKullanicisi k = OturumKullanicisi.of(oturum(Role.PO, List.of(2L)));

        assertThatCode(() -> k.dogrula(2L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ADMIN her takıma erişir")
    void admin() {
        OturumKullanicisi k = OturumKullanicisi.of(oturum(Role.ADMIN, List.of()));

        assertThat(k.adminMi()).isTrue();
        assertThatCode(() -> k.dogrula(99L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("takımsız PO hiçbir şeye erişemez")
    void takimsizPo() {
        OturumKullanicisi k = OturumKullanicisi.of(oturum(Role.PO, List.of()));

        assertThat(k.erisebilir(1L)).isFalse();
        assertThatThrownBy(() -> k.dogrula(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("teamId null ise erişim yok - sessizce geçilmez")
    void bosTeamId() {
        OturumKullanicisi k = OturumKullanicisi.of(oturum(Role.PO, List.of(2L)));

        assertThat(k.erisebilir(null)).isFalse();
    }

    @Test
    @DisplayName("oturum bilgisi okunamazsa erişim reddedilir")
    void oturumsuz() {
        assertThatThrownBy(() -> OturumKullanicisi.of(null))
                .isInstanceOf(AccessDeniedException.class);

        var detaysiz = new UsernamePasswordAuthenticationToken("x", null, List.of());
        assertThatThrownBy(() -> OturumKullanicisi.of(detaysiz))
                .isInstanceOf(AccessDeniedException.class);
    }
}
