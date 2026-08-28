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
 * @param height null ise yalnizca genislik yazilir, yukseklik oranla belirlenir
 *               (footer logosu boyle).
 */
public record ThemeImage(String cid, String resourcePath, int width, Integer height) {

    public ThemeImage(String cid, String resourcePath, int width) {
        this(cid, resourcePath, width, null);
    }

    /** style icine giren olcu bildirimi - Outlook her ikisini de gormeli. */
    public String boyutStili() {
        return height != null
                ? "width:%dpx;height:%dpx".formatted(width, height)
                : "width:%dpx;height:auto".formatted(width);
    }
}
