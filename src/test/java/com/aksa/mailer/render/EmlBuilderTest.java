package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeRegistry;
import com.aksa.mailer.render.template.KapanisTemplate;
import com.aksa.mailer.render.theme.RpaTheme;
import com.aksa.mailer.render.usecase.CidImageResolver;
import com.aksa.mailer.render.usecase.EmlBuilder;
import com.aksa.mailer.render.usecase.MailHtmlRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * .eml uretimi. Gorseller gercekten classpath'ten okunuyor - bu test ayni
 * zamanda "tema gorselleri repoda ve okunabilir mi" kontrolu.
 */
class EmlBuilderTest {

    private static final String KONU = "RPA Sprint Kapanış Bilgilendirme – Ağustos 2026";

    private byte[] eml;
    private String metin;
    private MailTheme tema;

    @BeforeEach
    void kurulum() {
        MailHtmlRenderer renderer = new MailHtmlRenderer(List.of(new KapanisTemplate()));
        tema = ThemeRegistry.tema(RpaTheme.KEY);
        String html = renderer.uret(OrnekIcerik.kapanis(), TemplateType.KAPANIS, RpaTheme.KEY);
        eml = new EmlBuilder(new CidImageResolver()).uret(html, KONU, tema);
        metin = new String(eml, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Outlook maili yeni/gönderilmemiş olarak açar")
    void xUnsent() {
        // Bu baslik olmadan okunmus mail gibi acilir ve "Gonder" cikmaz.
        assertThat(metin).contains("X-Unsent: 1");
    }

    @Test
    @DisplayName("multipart/related - görseller mailin içinde")
    void multipartRelated() {
        assertThat(metin)
                .contains("MIME-Version: 1.0")
                .contains("Content-Type: multipart/related; boundary=");
    }

    @Test
    @DisplayName("beş görselin hepsi Content-ID ile gömülü")
    void besGorsel() {
        for (var gorsel : tema.images()) {
            assertThat(metin)
                    .as("cid: " + gorsel.cid())
                    .contains("Content-ID: <" + gorsel.cid() + ">")
                    .contains("Content-Disposition: inline; filename=\"" + gorsel.cid() + ".png\"");
        }
    }

    @Test
    @DisplayName("Türkçe konu RFC 2047 ile kodlanır")
    void konuKodlanir() {
        String beklenen = "=?UTF-8?B?" + Base64.getEncoder()
                .encodeToString(KONU.getBytes(StandardCharsets.UTF_8)) + "?=";

        assertThat(metin).contains("Subject: " + beklenen);
        // Ham Turkce konu satirinda GECMEMELI - Outlook'ta bozuk gorunurdu.
        assertThat(metin).doesNotContain("Subject: RPA Sprint Kapanış");
    }

    @Test
    @DisplayName("gövde base64 ve çözülünce HTML çıkıyor")
    void govdeBase64() {
        assertThat(metin).contains("Content-Type: text/html; charset=UTF-8")
                .contains("Content-Transfer-Encoding: base64");

        String govde = metin.split("Content-Transfer-Encoding: base64\r\n\r\n", 2)[1]
                .split("\r\n--", 2)[0]
                .replace("\r\n", "");
        String html = new String(Base64.getDecoder().decode(govde), StandardCharsets.UTF_8);

        assertThat(html)
                .startsWith("<!doctype html>")
                .contains(OrnekIcerik.TURKCE_SUREC);
    }

    @Test
    @DisplayName("satır sonları CRLF - RFC 5322 böyle istiyor")
    void crlf() {
        assertThat(metin.lines().count()).isGreaterThan(20);
        assertThat(metin).contains("\r\n");
        // Yalniz LF ile biten satir olmamali.
        assertThat(metin.replace("\r\n", "")).doesNotContain("\n");
    }

    @Test
    @DisplayName("mail 300 KB sınırının altında")
    void boyut() {
        int kb = eml.length / 1024;

        // BRIEF'in koydugu sinir. Gorseller gosterildikleri olcuye indirilip
        // 8-bit palete cevrildikten sonra mail ~130 KB'ye dustu; sinir artik
        // gercek bir tavan, asilirsa bu test kirilir.
        //
        // Kirilirsa once gorsellere bak: tema klasorune buyuk bir PNG
        // eklenmis olmasi en olasi sebep (bkz. docs/tema-gorselleri.md).
        assertThat(kb)
                .as("mail boyutu %d KB - sınır 300 KB", kb)
                .isLessThan(300);
    }
}
