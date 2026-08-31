package com.aksa.mailer.render.theme;

import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;
import com.aksa.mailer.render.domain.ToneColors;

/**
 * RPA takiminin temasi. Renkler ve olculer v2 prototipinden BIREBIR alindi
 * (RPA_Sprint_Kapanis_Duzenlenebilir_Uygulama_v2.html, outlookHtml()).
 *
 * Gorseller de v2'nin icinden cikarildi; gosterildikleri olcuye indirilip
 * 8-bit palete cevrildiler (265 KB -> 85 KB). Uretilen mail 124 KB, sinir
 * 300 KB - EmlBuilderTest bunu kontrol ediyor.
 *
 * Asagidaki olculer DOSYALARIN gercek olculeriyle ayni olmak zorunda:
 * buyuk dosyayi HTML'de kucultmek boyutu bosa sisirir.
 * Ayrinti: docs/tema-gorselleri.md
 */
public final class RpaTheme {

    private RpaTheme() {
    }

    public static final String KEY = "rpa";

    public static MailTheme tema() {
        return new MailTheme(
                KEY,
                "RPA Takımı",
                "#eef4f9",   // sayfa zemini
                "#003b78",   // hero ve footer laciverdi
                "#95d05d",   // hero alt basligi
                "#10233f",   // govde metni
                "#d7e3ef",   // kutu cercevesi
                "#d9e6f1",   // tablo hucre cizgisi
                "#8ccc4f",   // footer ayiraci
                "#97d35d",   // footer ilk satiri
                new ToneColors(
                        "#064b95",   // bolum basligi zemini
                        "#fbfdff",   // tablo baslik zemini
                        "#0a4c98",   // tablo baslik yazisi
                        "#0b50a7",   // JIRA sutunu
                        "#084a9f",   // sayac rakami
                        "#084a9f"),  // madde isareti
                new ToneColors(
                        "#48ac35",
                        "#f7fbf4",
                        "#328d2d",
                        "#368f31",
                        "#339131",
                        "#43a437"),
                // Turuncu v2'de YOK - Yonetici Ozeti'yle geldi. Degerler
                // AKSA_Sprint_Mail_Studio prototipinden (--orange #ef7a16,
                // tablo basligi #db6c12).
                new ToneColors(
                        "#db6c12",
                        "#fff7ef",
                        "#b4560b",
                        "#c05e0d",
                        "#e07714",
                        "#ef7a16"),
                new ThemeImage("hero", "themes/rpa/hero.png", 315, 235),
                new ThemeImage("intro", "themes/rpa/intro.png", 120, 120),
                new ThemeImage("notes", "themes/rpa/notes.png", 225, 151),
                // Footer logosu: yalnizca genislik verilir, yukseklik oranla.
                new ThemeImage("logo", "themes/rpa/logo.png", 285),
                new ThemeImage("mascot", "themes/rpa/mascot.png", 108, 99));
    }
}
