package com.aksa.mailer.render.theme;

import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;
import com.aksa.mailer.render.domain.ToneColors;

/**
 * Is Zekasi takiminin temasi. Renkler v6.1 prototipindeki takim tanimindan
 * (#D97706 / #9A5605).
 *
 * IKINCI TEMA NEDEN SIMDI: yol haritasi "ikinci temayi Sprint 2'nin ILK isi
 * yap - render motorunun gercekten soyut olup olmadigini yalnizca bu gosterir"
 * diyor. Renk tarafini simdi ekleyerek o kontrolu bedavaya yapiyoruz: RPA'ya
 * ait gizli renk varsayimi kalmissa hemen goruluyor.
 *
 * GORSELLER HENUZ YER TUTUCU - RPA'ninkiler kullaniliyor. Bu takimin kendi
 * maskotu ve illustrasyonlari gelince yalnizca asagidaki bes yol degisecek;
 * baska hicbir yere dokunulmayacak. Iste bu yuzden gorsel yollari temada.
 */
public final class IsZekasiTheme {

    private IsZekasiTheme() {
    }

    public static final String KEY = "is-zekasi";

    public static MailTheme tema() {
        return new MailTheme(
                KEY,
                "İş Zekâsı Takımı",
                "#f8f4ee",
                "#9a5605",
                "#f2c56b",
                "#241a0f",
                "#e6d9c4",
                "#eadfcc",
                "#f0b54a",
                "#f5cd80",
                new ToneColors(
                        "#a8600a",
                        "#fdfaf5",
                        "#8a4d05",
                        "#96550a",
                        "#8a4d05",
                        "#a8600a"),
                new ToneColors(
                        "#4c7a2a",
                        "#f7faf3",
                        "#3d6420",
                        "#3d6420",
                        "#43702a",
                        "#4c7a2a"),
                new ThemeImage("hero", "themes/rpa/hero.png", 315, 235),
                new ThemeImage("intro", "themes/rpa/intro.png", 120, 120),
                new ThemeImage("notes", "themes/rpa/notes.png", 225, 151),
                new ThemeImage("logo", "themes/rpa/logo.png", 285),
                new ThemeImage("mascot", "themes/rpa/mascot.png", 108, 99));
    }
}
