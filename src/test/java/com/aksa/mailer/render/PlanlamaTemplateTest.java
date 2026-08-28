package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.document.domain.Tone;
import com.aksa.mailer.render.template.KapanisTemplate;
import com.aksa.mailer.render.template.PlanlamaTemplate;
import com.aksa.mailer.render.theme.RpaTheme;
import com.aksa.mailer.render.usecase.MailHtmlRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint Planlama sablonu. Ortak govde MailIskeleti'nde ve KapanisTemplate
 * testleriyle zaten kapsanıyor; burada yalnizca PLANLAMAYA OZGU davranis
 * test ediliyor.
 */
class PlanlamaTemplateTest {

    private MailHtmlRenderer renderer;
    private String html;

    @BeforeEach
    void kurulum() {
        renderer = new MailHtmlRenderer(List.of(new KapanisTemplate(), new PlanlamaTemplate()));
        html = renderer.uret(OrnekIcerik.planlama(), TemplateType.PLANLAMA, RpaTheme.KEY);
    }

    @Test
    @DisplayName("planlama alanları başlıklarıyla çizilir")
    void planlamaSutunlari() {
        assertThat(html)
                .contains("KONU TÜRÜ")
                .contains("JIRA")
                .contains("ÖZET")
                .contains("DURUM")
                .contains("BEKLENEN KONULAR")
                .contains("PAYDAŞLAR");
    }

    @Test
    @DisplayName("tek geniş tablo - sektöre göre gruplama yok")
    void gruplamaYok() {
        // Kapanis sektore gore grupluyor; planlamada konular kullanicinin
        // girdigi sirada tek listede durmali.
        assertThat(html).contains("RPA-1128").contains("RPA-1127").contains("RPA-1301");
        assertThat(html.indexOf("RPA-1128")).isLessThan(html.indexOf("RPA-1127"));
        assertThat(html.indexOf("RPA-1127")).isLessThan(html.indexOf("RPA-1301"));
    }

    @Test
    @DisplayName("sayaçlar duruma göre dağılım gösterir")
    void durumSayaclari() {
        // 3 konu: 2 UAT, 1 DEVELOPMENT
        assertThat(html)
                .contains("TOPLAM KONU")
                .contains("UAT")
                .contains("DEVELOPMENT");
        assertThat(html).contains(">3</b>");
    }

    @Test
    @DisplayName("durum sütunu yoksa yalnızca toplam sayacı çizilir")
    void durumsuzSayac() {
        MailContent durumsuz = new MailContent(
                1,
                new MailContent.Header("Başlık", "Dönem", "Takım"),
                new MailContent.Meeting("", "", ""),
                List.of("Giriş"),
                List.of(new MailSection("topics", "SPRINT KONULARI", Tone.BLUE,
                        List.of("jira", "summary"),
                        List.of(Map.of("jira", "RPA-1", "summary", "Özet")))),
                List.of(),
                new MailContent.Footer("", ""));

        String cikti = renderer.uret(durumsuz, TemplateType.PLANLAMA, RpaTheme.KEY);

        assertThat(cikti).contains("TOPLAM KONU").doesNotContain("UAT");
    }

    @Test
    @DisplayName("üçten fazla durum varsa sayaç şeridi taşmaz")
    void enFazlaUcDurum() {
        // Sayac seridi Outlook'ta sabit genislikte; dorduncu kutu sikistirinca
        // rakamlar okunmaz hale geliyor. Kalan durumlar zaten tabloda.
        MailContent besDurum = new MailContent(
                1,
                new MailContent.Header("Başlık", "Dönem", "Takım"),
                new MailContent.Meeting("", "", ""),
                List.of("Giriş"),
                List.of(new MailSection("topics", "SPRINT KONULARI", Tone.BLUE,
                        List.of("jira", "status"),
                        List.of(
                                Map.of("jira", "A-1", "status", "UAT"),
                                Map.of("jira", "A-2", "status", "DEV"),
                                Map.of("jira", "A-3", "status", "TEST"),
                                Map.of("jira", "A-4", "status", "ANALİZ"),
                                Map.of("jira", "A-5", "status", "BEKLEMEDE")))),
                List.of(),
                new MailContent.Footer("", ""));

        String cikti = renderer.uret(besDurum, TemplateType.PLANLAMA, RpaTheme.KEY);

        // Toplam + en fazla uc durum = dort sayac kutusu.
        // "font-size:32px;color:" yalnizca sayac rakaminda gecer; sade
        // "font-size:32px" hero basligiyla da eslesiyordu.
        int sayacKutusu = cikti.split("font-size:32px;color:", -1).length - 1;
        assertThat(sayacKutusu).isEqualTo(4);
        // Ilk uc durum sayacta, besincisi yalnizca tabloda.
        assertThat(cikti).contains("BEKLEMEDE");
    }

    @Test
    @DisplayName("Outlook kuralları planlamada da geçerli")
    void outlookKurallari() {
        assertThat(html)
                .doesNotContain("display:flex")
                .doesNotContain("border-radius")
                .doesNotContain("linear-gradient")
                .doesNotContain("background-image")
                .doesNotContain("max-width")
                .doesNotContain("<style")
                .doesNotContain("data:image");
        assertThat(html)
                .contains("<meta charset=\"UTF-8\">")
                .contains("width:760px")
                .contains("src=\"cid:hero\"");
    }

    @Test
    @DisplayName("Türkçe karakterler korunur")
    void turkceKorunur() {
        assertThat(html)
                .contains("Şebeke Operasyonları raporlama iyileştirmesi")
                .contains("İlknur Özgün Tarı")
                .contains("öğleden önce")
                .doesNotContain("&#350;");
    }
}
