package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.ThemeRegistry;
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
                    .contains("src=\"cid:logo\"");
            // Maskot footer'dan kaldirildi (yonetici istegi): hero'da
            // zaten ayni maskot var. Mailde HIC gecmemeli.
            assertThat(html).doesNotContain("cid:mascot");
        }

        @Test
        @DisplayName("HİÇBİR görsel ölçüsüz çizilmez - yükseklik dahil")
        void gorsellerinOlculeriYazili() {
            // Regresyon, yasandi: footer logosu yalnizca genislikle
            // taniminca style'a "height:auto" yaziliyordu. Word'un
            // donusturucusu onu anlamiyor ve gorseli YERLESTIREMIYOR -
            // Outlook'a yapistirilan mailde logonun yerinde bos beyaz bir
            // dikdortgen kaliyordu. Diger uc gorsel olculu oldugu icin
            // cikiyordu; fark yalnizca buydu.
            //
            // Tek tek gorsel saymak yerine DESEN ariyoruz: yarin eklenen bir
            // gorsel de ayni tuzaga dusmesin.
            assertThat(html).doesNotContain("height:auto");

            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("<img src=\"cid:([a-z]+)\"([^>]*)>")
                    .matcher(html);
            int sayilan = 0;
            while (m.find()) {
                sayilan++;
                assertThat(m.group(2))
                        .as("cid:%s ölçüsüz çiziliyor", m.group(1))
                        .containsPattern("width=\"\\d+\"")
                        .containsPattern("height=\"\\d+\"");
            }
            assertThat(sayilan).as("hiç görsel bulunamadı").isPositive();
        }

        @Test
        @DisplayName("logo alt metni taşır - çizilemezse ne olduğu görünsün")
        void logoAltMetniTasir() {
            assertThat(html).contains("src=\"cid:logo\"");
            assertThat(html).contains("alt=\"Aksa | Kazancı Holding\"");
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
            // Uc alan tek metne akmiyor, her biri kendi adresini tasiyor:
            // kullanici saate tiklayip yazdiginda hangi alanin degistigi
            // belirsiz kalmasin.
            assertThat(html)
                    .contains("Toplantı: ")
                    .contains("<span data-alan=\"meeting.date\">03.09.2026</span>")
                    .contains("<span data-alan=\"meeting.place\">Yüz Yüze / Toplantı Salonu</span>");
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
        @DisplayName("ortak palet mailde geçiyor")
        void ortakPalet() {
            // Sekiz takim da AYNI paleti kullaniyor (bkz. Temalar.java).
            // Yesil rol rengi degismedi - o takimdan bagimsiz.
            assertThat(html)
                    .contains("#0d47a1")   // hero, footer ve bolum bandi
                    .contains("#90caf9")   // hero alt basligi
                    .contains("#2196f3")   // sayac / JIRA / tablo baslik yazisi
                    .contains("#48ac35");  // gelistirme bolum basligi (rol rengi)

            // v2 prototipinin RPA'ya ozel tonlari artik hicbir yerde yok.
            assertThat(html)
                    .doesNotContain("#003b78")
                    .doesNotContain("#064b95")
                    .doesNotContain("#95d05d");
        }

        @Test
        @DisplayName("her tema AYNI paleti üretir, ama KENDİ görselini gösterir")
        void herTemaOrtakPaletKendiGorseli() {
            // Onceden bu test "her takim FARKLI renk uretir" diyordu. Tek
            // palete gecilince o kural dustu; yerine gelen kural bu:
            // renk ortak, AYIRT EDICILIK GORSELDE.
            //
            // Renk sabiti YAZILMIYOR: beklenen deger temanin kendisinden
            // okunuyor, boylece palet degisirse test degil tema guncellenir.
            ThemeRegistry.hepsi().forEach((anahtar, tema) -> {
                String cikti = renderer.uret(OrnekIcerik.kapanis(), TemplateType.KAPANIS, anahtar);

                assertThat(cikti)
                        .as("%s temasi ortak paleti kullanmali", anahtar)
                        .contains(tema.heroBackground())
                        .contains(tema.blue().sectionHeader());

                // Hero gorseli takima OZEL: kendi klasorunden gelmeli.
                // Bu kaymasaydi butun takimlar ayni damgayi gosterirdi ve
                // tek palete gecince maili birbirinden ayirt etmek
                // imkansiz olurdu.
                assertThat(tema.hero().resourcePath())
                        .as("%s kendi hero gorselini gostermeli", anahtar)
                        .contains("themes/" + anahtar + "/");
            });
        }

        @Test
        @DisplayName("sekiz takımın da paleti birebir aynı")
        void paletButunTakimlardaAyni() {
            var rpa = ThemeRegistry.tema(RpaTheme.KEY);
            ThemeRegistry.hepsi().forEach((anahtar, tema) -> assertThat(tema.heroBackground())
                    .as("%s hero rengi RPA ile ayni olmali", anahtar)
                    .isEqualTo(rpa.heroBackground()));
        }

        @Test
        @DisplayName("bütün takımların teması tanımlı")
        void butunTemalarTanimli() {
            // V3__takimlar.sql'deki theme_key degerleri. Biri eksik kalirsa
            // o takim mail uretemez - Flyway degil, bu test yakalasin.
            assertThat(ThemeRegistry.hepsi()).containsKeys(
                    "rpa", "is-zekasi", "urun-gelistirme", "yapay-zeka",
                    "dijital-uygulamalar", "dokuman", "cbs", "mobil");
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
