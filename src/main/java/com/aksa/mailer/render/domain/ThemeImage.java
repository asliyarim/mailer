package com.aksa.mailer.render.domain;

/**
 * Temanin tasidigi bir gorsel.
 *
 * Mailde <img src="cid:HERO"> diye gecer ve dosya multipart/related govdesine
 * gomulur. data: URI Outlook'ta calismaz, uzak URL ise "gorselleri indir"
 * uyarisina takilir - cid: tek guvenilir yol (Mimari Kural 3).
 *
 * width/height HTML'e ACIKCA yazilir. Outlook, olculeri verilmeyen gorseli
 * dogal boyutunda cizer; dosya gosterilecek olcuden buyukse mail dagilir.
 *
 * YUKSEKLIK ZORUNLU. Once "yalnizca genislik ver, yukseklik oranla belirlensin"
 * diye bir kisayol vardi ve footer logosu ondan geciyordu; sonucu su oldu:
 * Outlook'a YAPISTIRILAN mailde o logo HIC CIKMADI. Word'un donusturucusu
 * height:auto'yu anlamiyor ve yuksekligi verilmemis gorseli yerlestiremiyor.
 * Diger uc gorsel olculu oldugu icin cikiyordu - fark yalnizca buydu.
 *
 * Kisayol kurucu bu yuzden KALDIRILDI: bir daha kimse yanlislikla yuksekliksiz
 * gorsel tanimlayamasin.
 */
public record ThemeImage(String cid, String resourcePath, int width, int height) {

    /** style icine giren olcu bildirimi - Outlook her ikisini de gormeli. */
    public String boyutStili() {
        return "width:%dpx;height:%dpx".formatted(width, height);
    }
}
