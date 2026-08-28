package com.aksa.mailer.render.theme;

import com.aksa.mailer.render.domain.MailTheme;

import java.util.List;

/**
 * Odyssey takimlarinin temalari. Takim kimlikleri odyssey-auth'un
 * user_team_ids tablosundakiyle AYNI - token'daki teamIds dogrudan
 * mail_teams.id'ye karsilik gelsin diye (bkz. V3__takimlar.sql).
 *
 * Renkler v6.1 prototipinin takim tanimlarindan. Orada karsiligi olmayan
 * takimlar icin kurumsal paletle uyumlu ton secildi.
 *
 * GORSELLER: RPA disindaki takimlar henuz kendi gorsellerini tasimiyor,
 * hepsi RPA'ninkileri kullaniyor. Bir takimin gorselleri gelince
 * src/main/resources/themes/<klasor>/ altina konur ve asagidaki son
 * parametre degistirilir. Baska hicbir yere dokunulmaz.
 */
public final class Temalar {

    private Temalar() {
    }

    public static List<MailTheme> hepsi() {
        return List.of(
                // 1 - v2 prototipinden birebir, referans tema.
                RpaTheme.tema(),

                // 2 - v6.1: #D97706 / #9A5605
                KurumsalTema.olustur("is-zekasi", "İş Zekâsı Takımı",
                        "#a8600a", "#7a4405", "#f2c56b", "rpa"),

                // 3 - v6.1'de karsiligi yok. Teal SECILMEDI: CBS zaten o tonu
                // kullaniyor (#0F766E) ve iki takimin maili birbirine
                // benzerdi. Ayirt edilebilir bir gul tonu verildi.
                KurumsalTema.olustur("urun-gelistirme", "Ürün Geliştirme Takımı",
                        "#9d174d", "#701a3a", "#f0a3bd", "rpa"),

                // 4 - v6.1: #6D5BD0 / #4C3DB4
                KurumsalTema.olustur("yapay-zeka", "Yapay Zeka Takımı",
                        "#6d5bd0", "#3b2f8f", "#b9aef0", "rpa"),

                // 5 - v6.1: #063B78 / #052C59
                KurumsalTema.olustur("dijital-uygulamalar", "Dijital Uygulamalar Takımı",
                        "#063b78", "#052c59", "#95d05d", "rpa"),

                // 6 - v6.1: #64748B / #475569
                KurumsalTema.olustur("dokuman", "Doküman ve Süreç Yönetimi Takımı",
                        "#475569", "#2f3b4a", "#a9b6c6", "rpa"),

                // 7 - v6.1: #0F766E / #0A4F49 (CBS)
                KurumsalTema.olustur("cbs", "Konum Tabanlı Ürün Geliştirme Ekibi (CBS)",
                        "#0f766e", "#0a4f49", "#7fd4c8", "rpa"),

                // 8 - v6.1: #2563EB / #1D4ED8
                KurumsalTema.olustur("mobil", "Mobil Uygulamalar Takımı",
                        "#2563eb", "#1a3a8f", "#9dc0ff", "rpa"));
    }
}
