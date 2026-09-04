package com.aksa.mailer.render.theme;

import com.aksa.mailer.render.domain.MailTheme;

import java.util.List;

/**
 * Odyssey takimlarinin temalari. Takim kimlikleri odyssey-auth'un
 * user_team_ids tablosundakiyle AYNI - token'daki teamIds dogrudan
 * mail_teams.id'ye karsilik gelsin diye (bkz. V3__takimlar.sql).
 *
 * TEK PALET: butun takimlar ayni maviyi kullanir (yonetici karari).
 * Onceden her takimin kendi kurumsal tonu vardi - sekiz farkli renk hem
 * kurumsal kimlikle ortusmuyordu hem de bazi takimlarin tonu birbirine
 * cok yakin dusuyordu. Ayirt edicilik artik hero'daki TAKIM DAMGASINDAN
 * ve takim adindan geliyor, renkten degil.
 *
 * Palet (Color Hunt): #E3F2FD / #90CAF9 / #2196F3 / #0D47A1.
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
                        "#2196f3", "#0d47a1", "#90caf9", "#e3f2fd", "is-zekasi"),

                // 3 - Takimin kendi sectigi palet (Color Hunt):
                //   #E3F2FD  tablo baslik zemini
                //   #90CAF9  hero alt basligi - koyu zeminde okunur
                //   #2196F3  bolum basliklari, sayaclar, tablo baslik yazisi
                //   #0D47A1  hero ve footer zemini
                // Onceden gul tonundaydi; o ton CBS'in teal'iyle cakismasin
                // diye secilmisti. Palet degisince o gerekce dustu, ama YENI
                // bir yakinlik dogdu: mobil (#1a3a8f) ve dijital-uygulamalar
                // (#052c59) de lacivert. Uc takimin maili artik ayni aileden
                // gorunuyor - ayirt edicilik damgadan ve takim adindan geliyor.
                KurumsalTema.olustur("urun-gelistirme", "Ürün Geliştirme Takımı",
                        "#2196f3", "#0d47a1", "#90caf9", "#e3f2fd", "urun-gelistirme"),

                // 4 - v6.1: #6D5BD0 / #4C3DB4
                KurumsalTema.olustur("yapay-zeka", "Yapay Zeka Takımı",
                        "#2196f3", "#0d47a1", "#90caf9", "#e3f2fd", "yapay-zeka"),

                // 5 - v6.1: #063B78 / #052C59
                KurumsalTema.olustur("dijital-uygulamalar", "Dijital Uygulamalar Takımı",
                        "#2196f3", "#0d47a1", "#90caf9", "#e3f2fd", "dijital-uygulamalar"),

                // 6 - v6.1: #64748B / #475569
                KurumsalTema.olustur("dokuman", "Doküman ve Süreç Yönetimi Takımı",
                        "#2196f3", "#0d47a1", "#90caf9", "#e3f2fd", "dokuman"),

                // 7 - v6.1: #0F766E / #0A4F49 (CBS)
                KurumsalTema.olustur("cbs", "Konum Tabanlı Ürün Geliştirme Ekibi (CBS)",
                        "#2196f3", "#0d47a1", "#90caf9", "#e3f2fd", "cbs"),

                // 8 - v6.1: #2563EB / #1D4ED8
                KurumsalTema.olustur("mobil", "Mobil Uygulamalar Takımı",
                        "#2196f3", "#0d47a1", "#90caf9", "#e3f2fd", "mobil"));
    }
}
