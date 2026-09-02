package com.aksa.mailer.render.theme;

import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;
import com.aksa.mailer.render.domain.ToneColors;

/**
 * Takim temasi kurucusu.
 *
 * RpaTheme, v2 prototipinden BIREBIR alinan referans temadir; renkleri elle
 * yazilidir ve oyle kalmalidir. Diger takimlar ayni yapiyi paylasip yalnizca
 * kurumsal renklerinde ayrisiyor - her biri icin elle palet yazmak hem
 * tekrar hem de sapma kaynagi olurdu.
 *
 * Yesil ton BILEREK takima gore degismiyor: "gelistirme / tamamlandi"
 * rolunu tasiyor ve uc mail tipinde de ayni anlama geliyor. Takim rengi
 * yalnizca mavi rolun yerini aliyor.
 *
 * Gorseller: her takim kendi klasorunu gosterir. Klasor henuz yoksa RPA'nin
 * gorselleri yer tutucu olarak kullanilir - tema eklemek gorselleri
 * beklemesin diye. Gercek gorseller gelince tek satir degisir.
 */
final class KurumsalTema {

    private KurumsalTema() {
    }

    /** RPA'nin yesil tonu - rol rengi, takima gore degismez. */
    private static final ToneColors YESIL = new ToneColors(
            "#48ac35", "#f7fbf4", "#328d2d", "#368f31", "#339131", "#43a437");

    /**
     * Turuncu de rol rengi: "bekliyor / dikkat". Yesil gibi takima gore
     * degismez - bekleyen is her takimda ayni sey demek.
     */
    private static final ToneColors TURUNCU = new ToneColors(
            "#db6c12", "#fff7ef", "#b4560b", "#c05e0d", "#e07714", "#ef7a16");

    /**
     * @param key         mail_teams.theme_key
     * @param displayName ekranda gorunen ad
     * @param ana         takimin kurumsal rengi - bolum basliklari ve sayaclar
     * @param koyu        hero/footer zemini; ananin koyu tonu
     * @param vurgu       hero alt basligi - koyu zeminde okunabilir olmali
     * @param gorselKlasoru  themes/<klasor>/ ; kendi gorselleri yoksa "rpa"
     */
    static MailTheme olustur(String key, String displayName, String ana, String koyu,
                             String vurgu, String gorselKlasoru) {
        // HERO takima ozel: kendi renginde zemin, ustte logo bandi, altinda
        // takim damgasi (scripts/tema-gorselleri-uret.js uretiyor).
        String heroYolu = "themes/" + gorselKlasoru + "/";
        // Digerleri TAKIMA GORE DEGISMIYOR - kurumsal logo ve dekoratif
        // gorseller. Her klasore kopyalamak sekiz kat yer kaplar ve birinde
        // yapilan duzeltme otekilere gecmezdi.
        String ortak = "themes/ortak/";
        return new MailTheme(
                key,
                displayName,
                "#eef4f9",
                koyu,
                vurgu,
                "#10233f",
                "#d7e3ef",
                "#d9e6f1",
                "#8ccc4f",
                "#97d35d",
                new ToneColors(ana, "#fbfdff", ana, ana, ana, ana),
                YESIL,
                TURUNCU,
                new ThemeImage("hero", heroYolu + "hero.png", 315, 235),
                new ThemeImage("hero", heroYolu + "hero-yonetici.png", 315, 235),
                new ThemeImage("hero", heroYolu + "hero-toplanti.png", 315, 235),
                new ThemeImage("intro", ortak + "intro.png", 120, 120),
                new ThemeImage("notes", ortak + "notes.png", 225, 151),
                // Logo da TAKIMA OZEL: zemini takimin footer rengiyle ayni.
                // Ortak (lacivert zeminli) surum bordo footer'da mavi bir
                // dikdortgen gosteriyordu; saydam surum ise Outlook'a
                // yapistirinca kayboluyordu (Word beyaza duzlestiriyor,
                // beyaz yazi gorunmez oluyor).
                new ThemeImage("logo", heroYolu + "logo.png", 285, 34),
                // Maskot artik cizilmiyor (footer'dan kaldirildi) ama tema
                // alani duruyor; RPA'nin dosyasi yer tutucu.
                new ThemeImage("mascot", "themes/rpa/mascot.png", 108, 99));
    }
}
