package com.aksa.mailer.render.usecase;

/**
 * HTML uretiminde kullanilan kacis yardimcilari.
 *
 * DIKKAT: Turkce karakterler KACIRILMAZ. Mailde <meta charset="UTF-8"> var ve
 * govde base64/UTF-8 gonderiliyor; "ş" karakterini &#351; gibi varliga
 * cevirmek hem gereksiz hem de bazi istemcilerde bozuk goruntuye yol aciyor.
 * Yalnizca HTML'i kiran bes karakter kacirilir.
 */
public final class HtmlKacis {

    private HtmlKacis() {
    }

    /** Metni HTML govdesine guvenle yazilabilir hale getirir. */
    public static String kacir(String deger) {
        if (deger == null || deger.isEmpty()) {
            return "";
        }
        StringBuilder cikti = new StringBuilder(deger.length() + 16);
        for (int i = 0; i < deger.length(); i++) {
            char c = deger.charAt(i);
            switch (c) {
                case '&' -> cikti.append("&amp;");
                case '<' -> cikti.append("&lt;");
                case '>' -> cikti.append("&gt;");
                case '"' -> cikti.append("&quot;");
                case '\'' -> cikti.append("&#39;");
                default -> cikti.append(c);
            }
        }
        return cikti.toString();
    }

    /**
     * Cok satirli metni kacirir ve satir sonlarini &lt;br&gt; yapar.
     *
     * Gerekcesi: textarea'ya yazilan uc paragraf, duz kacirildiktan sonra
     * HTML'de tek satira yapisir. Prototipte yasanan somut hata buydu
     * (bkz. docs/BRIEF.md, Kural 2 sonundaki not).
     */
    public static String kacirSatirlariKoru(String deger) {
        return kacir(deger).replace("\r\n", "\n").replace("\n", "<br>");
    }
}
