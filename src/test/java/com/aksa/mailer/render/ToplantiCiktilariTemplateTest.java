package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.template.ToplantiCiktilariTemplate;
import com.aksa.mailer.render.usecase.MailHtmlRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Toplanti Ciktilari sablonunun Yonetici Ozeti'nden AYRILDIGI noktalar.
 *
 * Ortak davranislar (adresler, .eml temizligi, bolum capalari) zaten
 * DuzenlemeAdresleriTest'te dort tip icin birden sinaniyor; burada yalnizca
 * bu tipe ozgu olanlar var.
 */
class ToplantiCiktilariTemplateTest {

    private final MailHtmlRenderer renderer =
            new MailHtmlRenderer(List.of(new ToplantiCiktilariTemplate()));

    private String uret(MailContent icerik) {
        return renderer.uret(icerik, TemplateType.TOPLANTI_CIKTILARI, "rpa");
    }

    @Test
    @DisplayName("toplantı kutusu moderatör ve katılımcıları gösterir")
    void toplantiKutusuCizilir() {
        String html = uret(OrnekIcerik.toplantiCiktilari());

        assertThat(html)
                .contains("TOPLANTI KONUSU / ADI")
                .contains("Dijital Dönüşüm &amp; Süreç İyileştirme Değerlendirme Toplantısı")
                .contains("MODERATÖR / NOT ALAN")
                .contains("Ahmet Yılmaz")
                .contains("KATILIMCI EKİPLER / PAYDAŞLAR");
    }

    @Test
    @DisplayName("toplantı kutusunun her alanı kendi adresini taşır")
    void kutuAlanlariAdresli() {
        String html = uret(OrnekIcerik.toplantiCiktilari());

        // Kullanici bu alanlari da onizlemeden duzenleyebilmeli.
        assertThat(html)
                .contains("data-alan=\"meeting.title\"")
                .contains("data-alan=\"meeting.moderator\"")
                .contains("data-alan=\"meeting.attendees\"")
                .contains("data-alan=\"meeting.date\"")
                .contains("data-alan=\"meeting.time\"")
                .contains("data-alan=\"meeting.place\"");
    }

    @Test
    @DisplayName("toplantı bilgisi hiç yoksa kutu ÇİZİLMEZ")
    void bosKutuCizilmez() {
        MailContent icerik = OrnekIcerik.toplantiCiktilari();
        MailContent toplantisiz = new MailContent(
                icerik.schemaVersion(), icerik.header(),
                new MailContent.Meeting("", "", "", "", "", ""),
                icerik.intro(), icerik.sections(), icerik.notes(), icerik.footer());

        // Bos etiketlerle dolu bir kutu mailin en ustunde durup "burasi
        // eksik" derdi. Yeni belge tam da bu halde doguyor.
        assertThat(uret(toplantisiz)).doesNotContain("TOPLANTI KONUSU / ADI");
    }

    @Test
    @DisplayName("sayaçlar: açık aksiyon tamamlananı saymaz")
    void acikAksiyonSayaci() {
        // Ornekte iki aksiyon var, biri "Tamamlandı".
        assertThat(sayac(uret(OrnekIcerik.toplantiCiktilari()), "AÇIK AKSİYON")).isEqualTo("1");
    }

    @Test
    @DisplayName("ilgili ekip sayacı ÜÇ bölümü de tarar")
    void ilgiliEkipUcBolumdenSayilir() {
        // Ornekte gorusulen konularda 2 ekip var (Dijital Uygulamalar, RPA);
        // aksiyonlarda ucuncu bir ekip geciyor (Test Ekibi). Sayac yalnizca
        // gorusulen konulara bakiyor olsaydi 2 derdi - bir ekip hic konu
        // acmadan karar veya aksiyon almis olabilir.
        assertThat(sayac(uret(OrnekIcerik.toplantiCiktilari()), "İLGİLİ EKİP")).isEqualTo("3");
    }

    @Test
    @DisplayName("kararlarda KAPSAM sütunu var, bağlantı kartları YOK")
    void kapsamVarBaglantiYok() {
        String html = uret(OrnekIcerik.toplantiCiktilari());

        assertThat(html)
                .contains("KAPSAM / ALAN")
                .contains("Mimari")
                // Baglanti kartlari Yonetici Ozeti'ne ait; bu mail bir tutanak.
                .doesNotContain("İNCELEME VE ERİŞİM BAĞLANTILARI");
    }

    @Test
    @DisplayName("karar numarası çizerken üretilir ve adres taşımaz")
    void kararNumarasi() {
        String html = uret(OrnekIcerik.toplantiCiktilari());

        assertThat(html).contains("K-01");
        // Kullanici yazsa sonraki cizimde sunucu ezerdi.
        assertThat(html).doesNotContain("data-alan=\"sections.decisions.rows.0.no\"");
    }

    @Test
    @DisplayName("Outlook'ta yasak CSS yok")
    void yasakCssYok() {
        assertThat(uret(OrnekIcerik.toplantiCiktilari()))
                .doesNotContain("display:flex")
                .doesNotContain("display:grid")
                .doesNotContain("border-radius")
                .doesNotContain("linear-gradient")
                .doesNotContain("position:absolute");
    }

    /** Etiketin hemen oncesindeki sayac rakami. */
    private String sayac(String html, String etiket) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(">(\\d+)</b><br><span[^>]*>" + etiket + "<")
                .matcher(html);
        return m.find() ? m.group(1) : "BULUNAMADI";
    }
}
