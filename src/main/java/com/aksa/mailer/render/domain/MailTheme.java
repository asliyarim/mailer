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
        ThemeImage hero,
        ThemeImage intro,
        ThemeImage notes,
        ThemeImage logo,
        ThemeImage mascot) {

    /** Ton rolunu bu temanin gercek renklerine cevirir. */
    public ToneColors colors(Tone tone) {
        return tone == Tone.GREEN ? green : blue;
    }

    /** Maile gomulecek bes gorsel, mailde gectikleri sirayla. */
    public List<ThemeImage> images() {
        return List.of(hero, intro, notes, logo, mascot);
    }
}
