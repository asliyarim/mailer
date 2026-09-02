package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.document.domain.Tone;
import com.aksa.mailer.render.domain.ThemeRegistry;
import com.aksa.mailer.render.template.KapanisTemplate;
import com.aksa.mailer.render.template.MailTemplate;
import com.aksa.mailer.render.template.PlanlamaTemplate;
import com.aksa.mailer.render.template.ToplantiCiktilariTemplate;
import com.aksa.mailer.render.template.YoneticiOzetiTemplate;
import com.aksa.mailer.render.usecase.DuzenlemeAdresleri;
import com.aksa.mailer.render.usecase.MailHtmlRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Onizleme ile mail arasindaki IKINCI farkin kilidi.
 *
 * Birincisi gorsel referanslariydi (OnizlemeGorselleriTest). Bu da duzenleme
 * adresleri: onizlemede var, .eml'de yok. Ikisi de tek noktada yapiliyor ve
 * ikisi de cift yonlu kilitli - korkulacak sey farkin sayisi degil, sessizce
 * artmasi.
 */
class DuzenlemeAdresleriTest {

    private final MailHtmlRenderer renderer = new MailHtmlRenderer(
            List.of(new KapanisTemplate(), new PlanlamaTemplate(),
                    new YoneticiOzetiTemplate(), new ToplantiCiktilariTemplate()));
    private final DuzenlemeAdresleri adresler = new DuzenlemeAdresleri();

    private String uret(TemplateType tip) {
        return renderer.uret(
                switch (tip) {
                    case KAPANIS -> OrnekIcerik.kapanis();
                    case PLANLAMA -> OrnekIcerik.planlama();
                    case YONETICI_OZETI -> OrnekIcerik.yoneticiOzeti();
                    case TOPLANTI_CIKTILARI -> OrnekIcerik.toplantiCiktilari();
                },
                tip,
                ThemeRegistry.tema("rpa").key());
    }

    @Test
    @DisplayName("soyulunca geriye HİÇ data- niteliği kalmaz")
    void soyulanHtmldeEditorNitelgiKalmaz() {
        // Tek tek "data-alan" aramiyoruz: yarin eklenen bir data-* niteligi
        // soyma listesine yazilmazsa sessizce Outlook'a giderdi. Genel arama
        // o hatayi da yakalar.
        for (TemplateType tip : TemplateType.values()) {
            assertThat(adresler.soy(uret(tip)))
                    .as("%s", tip)
                    .doesNotContain("data-");
        }
    }

    @Test
    @DisplayName("sabit seçenekli sütun seçeneklerini bildirir")
    void secenekliSutunSecenekleriBildirir() {
        // Liste TEK YERDE: arayuz durumlari ikinci kez yazmasin.
        assertThat(uret(TemplateType.YONETICI_OZETI))
                .contains("data-secenekler=\"Bekliyor|Devam Ediyor|Karar Bekliyor|Tamamlandı\"");

        // Planlama'nin "status"u SERBEST METIN - liste dayatmak veri
        // kaybettirirdi, durumlar takimdan takima degisiyor.
        assertThat(uret(TemplateType.PLANLAMA)).doesNotContain("data-secenekler");
    }

    @Test
    @DisplayName("BOŞ belgede de her bölümün çapası var - hiçbir şey doldurmadan satır eklenebilsin")
    void bosBelgedeBolumCapasiVar() {
        // Kullanicinin en cok yasadigi an: belge yeni acildi, soldaki form
        // bos, sagdaki sablona bakiyor. Her basligin altinda "+ satir ekle"
        // cikabilmesi icin bolum capasinin BOS bolumde de basilmasi sart.
        for (TemplateType tip : TemplateType.values()) {
            MailContent bos = new MailContent(
                    1,
                    new MailContent.Header("Başlık", "", "Takım"),
                    new MailContent.Meeting("", "", ""),
                    List.of(),
                    bosBolumler(tip),
                    List.of(),
                    new MailContent.Footer("", ""));

            String html = renderer.uret(bos, tip, "rpa");

            for (MailSection bolum : bos.sections()) {
                assertThat(html)
                        .as("%s / %s", tip, bolum.key())
                        .contains("data-bolum=\"" + bolum.key() + "\"");
            }
        }
    }

    /** Tipin varsayilan bolumleri, SATIRSIZ. */
    private List<MailSection> bosBolumler(TemplateType tip) {
        return switch (tip) {
            case KAPANIS -> List.of(
                    new MailSection("analysis", "ANALİZ", Tone.BLUE, List.of("jira", "process"), List.of()),
                    new MailSection("development", "GELİŞTİRME", Tone.GREEN, List.of("jira", "process"), List.of()));
            case PLANLAMA -> List.of(
                    new MailSection("topics", "KONULAR", Tone.BLUE, List.of("jira", "summary"), List.of()));
            case YONETICI_OZETI -> List.of(
                    new MailSection("discussed", "GÖRÜŞÜLEN", Tone.BLUE, List.of("team", "topic"), List.of()),
                    new MailSection("decisions", "KARARLAR", Tone.GREEN, List.of("no", "decision"), List.of()),
                    new MailSection("actions", "AKSİYONLAR", Tone.ORANGE, List.of("team", "pending"), List.of()),
                    new MailSection("links", "BAĞLANTILAR", Tone.BLUE, List.of("title", "url"), List.of()));
            case TOPLANTI_CIKTILARI -> List.of(
                    new MailSection("discussed", "1. KONULAR", Tone.BLUE, List.of("team", "topic"), List.of()),
                    new MailSection("decisions", "2. KARARLAR", Tone.GREEN, List.of("no", "decision"), List.of()),
                    new MailSection("actions", "3. AKSİYONLAR", Tone.ORANGE, List.of("team", "pending"), List.of()));
        };
    }

    @Test
    @DisplayName("dolu bölümde çapa hem başlıkta hem tabloda")
    void doluBolumdeCapaIkiYerde() {
        String html = uret(TemplateType.YONETICI_OZETI);

        // Baslikta ve tabloda: arayuz "+ satir ekle"yi tablonun ALTINA
        // koyabilsin, bos bolumde de basligin altina.
        assertThat(html.split("data-bolum=\"discussed\"", -1).length - 1)
                .as("discussed için iki işaret bekleniyor")
                .isEqualTo(2);

        // Baglanti kartlari tablo()'dan gecmiyor - isareti ayrica konuldu.
        assertThat(html).contains("data-bolum=\"links\"");
        assertThat(html).contains("data-alan=\"sections.links.rows.0\"");
    }

    @Test
    @DisplayName("satırın kendisi de adreslenir - ekleme/silme düğmeleri için")
    void satirinKendisiAdreslenir() {
        assertThat(uret(TemplateType.YONETICI_OZETI))
                .contains("<tr data-alan=\"sections.discussed.rows.0\">")
                .contains("<tr data-alan=\"sections.discussed.rows.1\">");
    }

    @Test
    @DisplayName("soymak adreslerden BAŞKA hiçbir şeyi değiştirmez")
    void soymakBaskaSeyiDegistirmez() {
        for (TemplateType tip : TemplateType.values()) {
            String onizleme = uret(tip);
            String mail = adresler.soy(onizleme);

            // Elle silme kalibi BILEREK genel: uretimdeki kalip adi adi
            // sayilmis bir liste, buradaki ise "butun data-*". Ayni ifadeyi
            // iki kez yazsaydim test totoloji olurdu. Boyle yazinca asil
            // riski yakaliyor: uretimdeki kalip bir karakter FAZLASINI alirsa
            // (komsu isaretlemeyi yerse) esitlik bozulur ve bunu ancak
            // Outlook'ta gorurduk.
            String elleSoyulmus = onizleme.replaceAll("\\s+data-[a-z]+=\"[^\"]*\"", "");
            assertThat(mail).as("%s", tip).isEqualTo(elleSoyulmus);

            // Govde ayakta - soyma yalnizca nitelik cikardi, HTML'i bozmadi.
            assertThat(mail).as("%s", tip)
                    .startsWith("<!doctype html")
                    .endsWith("</html>")
                    .contains("Teşekkür ederiz.");
        }
    }

    @Test
    @DisplayName("önizlemede her mail tipi düzenleme adresi taşır")
    void onizlemedeAdresVar() {
        for (TemplateType tip : TemplateType.values()) {
            assertThat(uret(tip))
                    .as("%s", tip)
                    .contains("data-alan=\"header.title\"")
                    .contains("data-alan=\"footer.line1\"");
        }
    }

    @Test
    @DisplayName("gruplama satır sırasını değiştirse bile adres İÇERİK sırasını taşır")
    void satirAdresiIcerikSirasiniTasir() {
        // Sektorler A, B, A. Kapanis sektore gore gruplayinca cizim sirasi
        // 0, 2, 1 olur. Adres cizim sirasini tasisaydi kullanici ikinci
        // gorunen satira yazarken icerikteki UCUNCU satir degisirdi.
        MailContent icerik = new MailContent(
                1,
                new MailContent.Header("Başlık", "Dönem", "Takım"),
                new MailContent.Meeting("", "", ""),
                List.of(),
                List.of(new MailSection("analysis", "ANALİZ", Tone.BLUE,
                        List.of("sector", "process"),
                        List.of(
                                Map.of("sector", "Elektrik", "process", "BIRINCI"),
                                Map.of("sector", "Holding", "process", "IKINCI"),
                                Map.of("sector", "Elektrik", "process", "UCUNCU")))),
                List.of(),
                new MailContent.Footer("Teşekkür ederiz.", ""));

        String html = renderer.uret(icerik, TemplateType.KAPANIS, "rpa");

        // Cizim sirasi gercekten degisti mi - once bunu dogrula, yoksa test
        // bir sey kanitlamiyor demektir.
        assertThat(html.indexOf("UCUNCU"))
                .as("gruplama satirlari yeniden siralamali")
                .isLessThan(html.indexOf("IKINCI"));

        // Ve adresler yine de icerikteki indeksi tasiyor: UCUNCU -> rows.2
        int ucuncununAdresi = html.indexOf("data-alan=\"sections.analysis.rows.2.process\"");
        assertThat(ucuncununAdresi).isNotNegative();
        assertThat(html.indexOf("UCUNCU")).isGreaterThan(ucuncununAdresi);
    }

    @Test
    @DisplayName("üretilen sütun adres TAŞIMAZ - kullanıcı yazsa sunucu ezerdi")
    void uretilenSutunAdresTasimaz() {
        String html = uret(TemplateType.YONETICI_OZETI);

        // Karar numarasi cizerken uretiliyor, icerikteki karsiligi bos.
        // Adres tasisaydi kullanici K-01'e tiklayip yazabilir, sonraki
        // cizimde sunucu yazdigini ezerdi - sessiz veri kaybi.
        assertThat(html)
                .contains("K-01")
                .doesNotContain("data-alan=\"sections.decisions.rows.0.no\"");

        // Ayni satirin diger sutunlari duzenlenebilir kalmali.
        assertThat(html).contains("data-alan=\"sections.decisions.rows.0.decision\"");
    }

    @Test
    @DisplayName("hero ŞABLONA göre değişir - iki toplantı tipinde ortak logo")
    void heroSablonaGoreDegisir() {
        var tema = ThemeRegistry.tema("rpa");

        // Sprint sablonlari takimin KENDI logosunu kullanir.
        assertThat(new KapanisTemplate().heroGorseli(tema).resourcePath())
                .isEqualTo(tema.hero().resourcePath());
        assertThat(new PlanlamaTemplate().heroGorseli(tema).resourcePath())
                .isEqualTo(tema.hero().resourcePath());

        // Iki toplanti sablonu ORTAK logo kullanir - bu mailler bir takimin
        // degil bolumun/toplantinin ozeti; takim logosu yaniltici olurdu.
        assertThat(new YoneticiOzetiTemplate().heroGorseli(tema).resourcePath())
                .contains("hero-yonetici.png")
                .isNotEqualTo(tema.hero().resourcePath());
        assertThat(new ToplantiCiktilariTemplate().heroGorseli(tema).resourcePath())
                .contains("hero-toplanti.png")
                .isNotEqualTo(tema.hero().resourcePath());
    }

    @Test
    @DisplayName("GÖMÜLEN görsel, HTML'in gösterdiği DOSYA ile aynı olmalı")
    void gomulenGorselHtmldekiyleAyni() {
        // Asil tuzak: hero'nun cid'i her sablonda "hero". Gomulen liste
        // temadan alinsaydi HTML dogru dosyayi isaret eder ama .eml YANLIS
        // dosyayi AYNI cid altinda gomerdi - Yonetici Ozeti mailinde takim
        // logosu cikardi ve kimse kirik gorsel gormedigi icin fark etmezdi.
        var tema = ThemeRegistry.tema("rpa");
        for (MailTemplate sablon : List.of(new KapanisTemplate(), new PlanlamaTemplate(),
                new YoneticiOzetiTemplate(), new ToplantiCiktilariTemplate())) {

            String gomulenHeroDosyasi = sablon.gorseller(tema).stream()
                    .filter(g -> g.cid().equals("hero"))
                    .findFirst().orElseThrow().resourcePath();

            assertThat(gomulenHeroDosyasi)
                    .as("%s", sablon.tip())
                    .isEqualTo(sablon.heroGorseli(tema).resourcePath());
        }
    }

    @Test
    @DisplayName("bölüm adresi indeks değil anahtar kullanır")
    void bolumAdresiAnahtarKullanir() {
        String html = uret(TemplateType.YONETICI_OZETI);

        // Anahtar kalici; indeks kullanici bolum ekleyip silince kayar.
        assertThat(html)
                .contains("data-alan=\"sections.discussed.title\"")
                .contains("data-alan=\"sections.actions.title\"")
                .doesNotContain("data-alan=\"sections.0.");
    }
}
