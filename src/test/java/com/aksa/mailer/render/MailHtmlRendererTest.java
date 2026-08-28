package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.template.KapanisTemplate;
import com.aksa.mailer.render.template.PlanlamaTemplate;
import com.aksa.mailer.render.theme.RpaTheme;
import com.aksa.mailer.render.usecase.MailHtmlRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Renderer testleri. En degerlisi "Outlook kurallari" bolumu: yasak CSS
 * listesi burada TEST olarak duruyor, boylece kimse yanlislikla ekleyemiyor.
 *
 * Spring context yok, veritabani yok - saf fonksiyon testi.
 */
class MailHtmlRendererTest {

    private MailHtmlRenderer renderer;
    private String html;

    @BeforeEach
    void kurulum() {
        renderer = new MailHtmlRenderer(List.of(new KapanisTemplate(), new PlanlamaTemplate()));
        html = renderer.uret(OrnekIcerik.kapanis(), TemplateType.KAPANIS, RpaTheme.KEY);
    }

    @Nested
    @DisplayName("Outlook kuralları")
    class OutlookKurallari {

        /**
         * Outlook masaustu HTML'i Word'un motoruyla cizer. Asagidakilerin
         * hicbirini desteklemez; Chrome'daki onizlemede calisip Outlook'ta
         * calismamalari, onizlemenin yalan soylemesi demektir.
         *
         * Ikinci prototip tam olarak buradan battı (Mimari Kural 2).
         */
        @Test
        @DisplayName("yasak CSS üretilmez")
        void yasakCssYok() {
            assertThat(html)
                    .doesNotContain("display:flex")
                    .doesNotContain("display:grid")
                    .doesNotContain("position:absolute")
                    .doesNotContain("position:relative")
                    .doesNotContain("position:fixed")
                    .doesNotContain("border-radius")
                    .doesNotContain("box-shadow")
                    .doesNotContain("linear-gradient")
                    .doesNotContain("radial-gradient")
                    .doesNotContain("background-image")
                    .doesNotContain("max-width")
                    .doesNotContain("min-width");
        }

        @Test
        @DisplayName("<style> bloğu ve harici CSS yok - her şey satır içi")
        void styleBlokuYok() {
            assertThat(html)
                    .doesNotContain("<style")
                    .doesNotContain("<link")
                    .doesNotContain("@media")
                    .doesNotContain("@font-face");
        }

        @Test
        @DisplayName("görseller data: URI değil cid: ile gelir")
        void gorsellerCidIle() {
            assertThat(html).doesNotContain("data:image");
            assertThat(html)
                    .contains("src=\"cid:hero\"")
                    .contains("src=\"cid:intro\"")
                    .contains("src=\"cid:notes\"")
                    .contains("src=\"cid:logo\"")
                    .contains("src=\"cid:mascot\"");
        }

        @Test
        @DisplayName("her görselin width/height niteliği açıkça yazılır")
        void gorsellerinOlculeriYazili() {
            // Outlook, olculeri verilmeyen gorseli dogal boyutunda cizer -
            // dosya buyukse mail dagilir.
            assertThat(html)
                    .contains("src=\"cid:hero\" width=\"315\" height=\"235\"")
                    .contains("src=\"cid:intro\" width=\"120\" height=\"120\"")
                    .contains("src=\"cid:notes\" width=\"225\" height=\"151\"")
                    .contains("src=\"cid:mascot\" width=\"108\" height=\"99\"");
        }

        @Test
        @DisplayName("düzen tablolarla kurulur ve gövde sabit piksel genişliktedir")
        void tabloDuzeni() {
            assertThat(html)
                    .contains("role=\"presentation\"")
                    .contains("width:760px");
        }

        @Test
        @DisplayName("yalnızca Arial / Segoe UI kullanılır")
        void webFontuYok() {
            assertThat(html).contains("font-family:Arial");
            assertThat(html).doesNotContain("fonts.googleapis.com");
        }

        @Test
        @DisplayName("head ve meta charset mutlaka var")
        void charsetVar() {
            // Olmazsa Turkce karakterler bazi istemcilerde bozulur.
            assertThat(html)
                    .contains("<head>")
                    .contains("<meta charset=\"UTF-8\">");
        }
    }

    @Nested
    @DisplayName("İçerik")
    class Icerik {

        @Test
        @DisplayName("Türkçe karakterler varlığa çevrilmeden korunur")
        void turkceKorunur() {
            assertThat(html)
                    .contains(OrnekIcerik.TURKCE_SUREC)
                    .contains("İlknur Özgün Tarı")
                    .contains("Doğalgaz")
                    .contains("Başarılar dileriz!")
                    .doesNotContain("&#350;")
                    .doesNotContain("&#304;");
        }

        @Test
        @DisplayName("sayaçlar satır sayısından hesaplanır")
        void sayaclar() {
            // 3 analiz + 2 gelistirme = 5 toplam
            assertThat(html)
                    .contains(">5</b>")
                    .contains(">3</b>")
                    .contains(">2</b>")
                    .contains("TOPLAM SÜREÇ");
        }

        @Test
        @DisplayName("satırlar sektöre göre gruplanır, grup içinde sektör sütunu tekrarlanmaz")
        void sektorGruplama() {
            assertThat(html).contains("Elektrik").contains("Holding").contains("Doğalgaz");
            // Grup basligi zaten sektoru soyluyor - sutun basligi cizilmemeli.
            assertThat(html).doesNotContain(">SEKTÖR</b>");
        }

        @Test
        @DisplayName("toplantı bilgisi yazılır")
        void toplantiBilgisi() {
            assertThat(html).contains("Toplantı: 03.09.2026").contains("Yüz Yüze / Toplantı Salonu");
        }

        @Test
        @DisplayName("çok satırlı metinde satır sonları korunur")
        void satirSonlariKorunur() {
            MailContent icerik = OrnekIcerik.kapanis();
            MailContent cokSatirli = new MailContent(
                    icerik.schemaVersion(), icerik.header(), icerik.meeting(),
                    List.of("birinci satır\nikinci satır"),
                    icerik.sections(), icerik.notes(), icerik.footer());

            String cikti = renderer.uret(cokSatirli, TemplateType.KAPANIS, RpaTheme.KEY);

            assertThat(cikti).contains("birinci satır<br>ikinci satır");
        }

        @Test
        @DisplayName("HTML kıran karakterler kaçırılır")
        void kacisYapilir() {
            MailContent icerik = OrnekIcerik.kapanis();
            MailContent zararli = new MailContent(
                    icerik.schemaVersion(),
                    new MailContent.Header("<script>alert(1)</script>", "Dönem", "Takım"),
                    icerik.meeting(), icerik.intro(), icerik.sections(), icerik.notes(), icerik.footer());

            String cikti = renderer.uret(zararli, TemplateType.KAPANIS, RpaTheme.KEY);

            assertThat(cikti).doesNotContain("<script>alert");
            assertThat(cikti).contains("&lt;script&gt;");
        }
    }

    @Nested
    @DisplayName("Tema")
    class Tema {

        @Test
        @DisplayName("RPA renkleri v2 prototipiyle aynı")
        void rpaRenkleri() {
            assertThat(html)
                    .contains("#003b78")   // hero ve footer laciverdi
                    .contains("#95d05d")   // hero alt basligi
                    .contains("#064b95")   // analiz bolum basligi
                    .contains("#48ac35");  // gelistirme bolum basligi
        }

        @Test
        @DisplayName("ikinci tema kendi renklerini üretir - motorda gizli RPA varsayımı yok")
        void ikinciTema() {
            String isZekasi = renderer.uret(OrnekIcerik.kapanis(), TemplateType.KAPANIS, "is-zekasi");

            assertThat(isZekasi).contains("#9a5605").doesNotContain("#003b78");
        }

        @Test
        @DisplayName("tanımsız tema sessizce varsayılana düşmez")
        void tanimsizTema() {
            assertThatThrownBy(() -> renderer.uret(OrnekIcerik.kapanis(), TemplateType.KAPANIS, "boyle-tema-yok"))
                    .hasMessageContaining("Tanımlı olmayan tema");
        }
    }

    @Test
    @DisplayName("henüz yazılmamış mail tipi açıkça hata verir")
    void hazirOlmayanTip() {
        // Yönetici Özeti Sprint 2'de gelecek; o zamana kadar sessizce yanlış
        // şablon üretmek yerine açıkça söylüyor.
        assertThatThrownBy(() ->
                renderer.uret(OrnekIcerik.kapanis(), TemplateType.YONETICI_OZETI, RpaTheme.KEY))
                .hasMessageContaining("henüz üretilemiyor");
    }

    @Test
    @DisplayName("geçersiz içerik render edilmez")
    void gecersizIcerik() {
        MailContent bos = MailContent.bos();

        assertThatThrownBy(() -> renderer.uret(bos, TemplateType.KAPANIS, RpaTheme.KEY))
                .hasMessageContaining("Başlık boş olamaz");
    }
}
