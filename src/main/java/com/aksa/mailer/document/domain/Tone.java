package com.aksa.mailer.document.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Bolum ve not kutusunun rengi DEGIL, ROLU. Gercek renk temadan gelir
 * (render/theme/*.java). Istemci asla renk kodu gondermez - "blue" yazar,
 * RPA temasinda ne ise o olur.
 *
 * JSON'da kucuk harf ("blue"), Java'da enum. Jackson anotasyonlari burada
 * duruyor cunku bu sinif jsonb govdesinin bir parcasi; JPA veya HTTP
 * bagimliligi degil - domain kurali korunuyor.
 */
public enum Tone {
    BLUE("blue"),
    GREEN("green"),
    /**
     * Dikkat/bekleme rolu. Yonetici Ozeti'ndeki "BEKLEYEN KONULAR VE
     * AKSIYONLAR" tablosu bunu kullaniyor: tamamlanmis is yesil, devam eden
     * mavi, bekleyen turuncu okunuyor.
     */
    ORANGE("orange");

    private final String jsonDegeri;

    Tone(String jsonDegeri) {
        this.jsonDegeri = jsonDegeri;
    }

    @JsonValue
    public String jsonDegeri() {
        return jsonDegeri;
    }

    @JsonCreator
    public static Tone fromJson(String deger) {
        for (Tone tone : values()) {
            if (tone.jsonDegeri.equalsIgnoreCase(deger)) {
                return tone;
            }
        }
        throw new IllegalArgumentException("Geçersiz ton: " + deger);
    }
}
