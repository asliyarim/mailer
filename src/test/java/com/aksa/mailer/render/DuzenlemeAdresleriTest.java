package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.document.domain.Tone;
import com.aksa.mailer.render.domain.ThemeRegistry;
import com.aksa.mailer.render.template.KapanisTemplate;
import com.aksa.mailer.render.template.PlanlamaTemplate;
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
            List.of(new KapanisTemplate(), new PlanlamaTemplate(), new YoneticiOzetiTemplate()));
    private final DuzenlemeAdresleri adresler = new DuzenlemeAdresleri();

    private String uret(TemplateType tip) {
        return renderer.uret(
                switch (tip) {
                    case KAPANIS -> OrnekIcerik.kapanis();
                    case PLANLAMA -> OrnekIcerik.planlama();
                    case YONETICI_OZETI -> OrnekIcerik.yoneticiOzeti();
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
