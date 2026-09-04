package com.aksa.mailer.render.domain;

import com.aksa.mailer.document.domain.Tone;

import java.util.List;

/**
 * Bir takimin gorsel kimligi: renkler ve bes gorsel.
 *
 * TEMA KODDA YASAR, icerik veritabaninda (Mimari Kural 4). Yeni takim eklemek
 * = bir tema sinifi + bir gorsel klasoru + bir PR. Bu sinir bulanirsa sablon
 * yonetim paneli yazmak gerekir; o kapsam disi.
 *
 * Renkler v2 prototipinden birebir alindi - uretilen mail onunla yan yana
 * konuldugunda ayirt edilemesin diye.
 */
public record MailTheme(
        String key,
        String displayName,
        /** Mail govdesinin disindaki zemin. */
        String pageBackground,
        /** Hero ve footer seridinin zemini. */
        String heroBackground,
        /** Hero'daki ikinci satirin (alt baslik) rengi. */
        String heroAccent,
        /** Govde metni. */
        String bodyText,
        /** Kutu cercevesi. */
        String border,
        /** Tablo hucre cizgisi. */
        String cellBorder,
        /** Footer'daki dikey ayirac cizgisi. */
        String footerRule,
        /** Footer'daki ilk satirin rengi. */
        String footerAccent,
        ToneColors blue,
        ToneColors green,
        ToneColors orange,
        ToneColors pembe,
        ToneColors sari,
        ToneColors mor,
        ThemeImage hero,
        /**
         * Yonetici Ozeti ve Toplanti Ciktilari sablonlarinin hero'su.
         *
         * LOGO butun takimlarda AYNI (bolumun cark logosu / toplanti logosu)
         * ama ZEMIN takimin rengi kaliyor - ortak tek dosya olsaydi bordo bir
         * mailde lacivert panel gorunurdu. Hangisinin kullanilacagina SABLON
         * karar veriyor (MailTemplate.heroGorseli).
         */
        ThemeImage heroYoneticiOzeti,
        ThemeImage heroToplantiCiktilari,
        ThemeImage intro,
        ThemeImage notes,
        ThemeImage logo,
        ThemeImage mascot) {

    /** Ton rolunu bu temanin gercek renklerine cevirir. */
    public ToneColors colors(Tone tone) {
        // Ton belirtilmemisse maviyle cizilir - render asamasinda patlamaz.
        // (Bos ton dogrulamada zaten yakalaniyor; burasi son emniyet.)
        if (tone == null) {
            return blue;
        }
        return switch (tone) {
            case GREEN -> green;
            case ORANGE -> orange;
            case PEMBE -> pembe;
            case SARI -> sari;
            case MOR -> mor;
            case BLUE -> blue;
        };
    }

    /**
     * Maile GOMULECEK gorseller, mailde gectikleri sirayla.
     *
     * mascot BU LISTEDE YOK: footer maskotu kaldirildi (hero'da zaten ayni
     * maskot var). Listede biraksaydik .eml'e kullanilmayan bir ek olarak
     * gomulur, Outlook maili atacli gosterirdi.
     *
     * Alan temada duruyor - geri istenirse iki satir.
     */
    public List<ThemeImage> images() {
        return List.of(hero, intro, notes, logo);
    }

    /** Verilen hero ile birlikte maile gomulecek gorseller. */
    public List<ThemeImage> images(ThemeImage secilenHero) {
        return List.of(secilenHero, intro, notes, logo);
    }
}
