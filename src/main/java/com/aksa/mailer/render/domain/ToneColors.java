package com.aksa.mailer.render.domain;

/**
 * Bir TON'un (mavi/yesil) tema icindeki renk karsiliklari.
 *
 * Icerik JSON'u "blue" ya da "green" yazar - bir ROL. Gercek renk buradan
 * gelir. Bu ayrim sayesinde yeni takim eklemek yalnizca yeni bir tema sinifi
 * demek; kullanicinin doldurdugu icerige dokunulmaz (Mimari Kural 4).
 *
 * @param sectionHeader        bolum basligi seridinin zemini (ANALIZ CALISMALARI)
 * @param tableHeaderBackground tablo baslik satirinin zemini
 * @param tableHeaderText      tablo baslik satirinin yazi rengi
 * @param keyText              satirin ilk sutunu (JIRA) - kalin ve renkli
 * @param counterText          sayac kutusundaki buyuk rakam
 * @param bullet               alt not kutusundaki madde isareti
 */
public record ToneColors(
        String sectionHeader,
        String tableHeaderBackground,
        String tableHeaderText,
        String keyText,
        String counterText,
        String bullet) {
}
