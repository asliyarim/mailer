package com.aksa.mailer.render.theme;

import com.aksa.mailer.render.domain.ToneColors;

/**
 * Kullanicinin bolum basligi icin secebildigi EK tonlar.
 *
 * Mavi/yesil/turuncu bir ROL tasiyor (takim rengi / tamamlandi / bekliyor).
 * Bunlar tasimiyor: yalnizca gorsel tercih. O yuzden takima gore degismiyor
 * ve iki temanin da ayni tanimi kullanmasi icin burada tek noktada duruyor -
 * RpaTheme ve KurumsalTema'ya ayri ayri yazilsaydi zamanla birbirinden
 * ayrisirlardi.
 *
 * Renkler kullanicinin sectigi paletlerden (Color Hunt).
 *
 * BASLIK YAZISI KOYU: bu tonlarin hepsi acik renk. Beyaz yazi olculdu ve
 * hicbirinde okunmuyor - pembe 1.92:1, sari 1.10:1, mor 1.91:1 (WCAG esigi
 * buyuk yazi icin 3.0). Koyu yaziyla 5.7 ile 7.1 arasinda cikiyor. Bu yuzden
 * ToneColors'a sectionHeaderText alani eklendi; mavi/yesil/turuncu koyu
 * zeminli oldugu icin onlarda beyaz kaliyor.
 *
 * Tablo ve sayac yazilari BEYAZ zemine biniyor, o yuzden ayri (daha koyu)
 * tonlar: pembe 6.09:1, sari 5.26:1, mor 6.46:1.
 */
final class EkTonlar {

    private EkTonlar() {
    }

    /** #FE9EC7 */
    static final ToneColors PEMBE = new ToneColors(
            "#fe9ec7",   // bolum basligi zemini
            "#6b2140",   // bolum basligi yazisi - koyu
            "#fff1f7",   // tablo baslik zemini
            "#a83a63",   // tablo baslik yazisi
            "#a83a63",   // JIRA sutunu
            "#a83a63",   // sayac rakami
            "#a83a63");  // madde isareti

    /** #F9F6C4 */
    static final ToneColors SARI = new ToneColors(
            "#f9f6c4",
            "#5c5218",
            "#fdfcee",
            "#7a6c1f",
            "#7a6c1f",
            "#7a6c1f",
            "#7a6c1f");

    /** #BDB2FF */
    static final ToneColors MOR = new ToneColors(
            "#bdb2ff",
            "#2f2470",
            "#f3f0ff",
            "#5b4bc4",
            "#5b4bc4",
            "#5b4bc4",
            "#5b4bc4");
}
