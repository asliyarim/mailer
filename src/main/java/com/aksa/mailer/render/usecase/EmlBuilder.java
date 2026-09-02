package com.aksa.mailer.render.usecase;

import com.aksa.mailer.render.domain.ThemeImage;
import java.util.List;
import com.aksa.mailer.render.domain.MailTheme;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * HTML + gorseller -> Outlook'un cift tiklayinca actigi .eml dosyasi.
 *
 * Yapi v2 prototipinden birebir; dort ayrinti kritik:
 *
 * 1. multipart/related + cid:  Gorseller mailin ICINE gomulur. data: URI
 *    Outlook'ta calismaz; uzak URL "gorselleri indir" uyarisina takilir ve
 *    kullanici tiklayana kadar mail bombos gorunur (Mimari Kural 3).
 *
 * 2. X-Unsent: 1  Outlook dosyayi GONDERILMEMIS mail olarak acar; kullanici
 *    alicilari yazip gonderebilir. Bu baslik olmadan okunmus bir mail gibi
 *    acilir ve "Gonder" dugmesi cikmaz.
 *
 * 3. Konu basligi RFC 2047 ile kodlanir. Turkce karakterli konu ham
 *    gonderilirse Outlook'ta bozuk gorunur.
 *
 * 4. Govde base64. Uzun satirlar ve UTF-8, quoted-printable'da satir
 *    kirilmasina takiliyor; base64 bunu tamamen kaldiriyor.
 *
 * Satir sonlari CRLF - RFC 5322 boyle istiyor.
 */
@Component
public class EmlBuilder {

    private static final String CRLF = "\r\n";
    /** Base64 govde satirlari 76 karakterde kirilir (RFC 2045). */
    private static final int SATIR_UZUNLUGU = 76;

    private final CidImageResolver gorselCozucu;

    public EmlBuilder(CidImageResolver gorselCozucu) {
        this.gorselCozucu = gorselCozucu;
    }

    /**
     * @param html  MailHtmlRenderer'in urettigi HTML - DEGISTIRILMEDEN gomulur
     * @param konu  mail konusu
     * @param gorselListesi maile gomulecek gorseller - SABLONUN sectigi hero
     *                      ile birlikte (bkz. MailTemplate.gorseller)
     */
    public byte[] uret(String html, String konu, List<ThemeImage> gorselListesi) {
        Map<String, byte[]> gorseller = gorselCozucu.gorseller(gorselListesi);
        // Sinir dizesi govdede GECMEYEN benzersiz bir metin olmali; HTML'in
        // ozeti yeterli. Once tema anahtari da giriyordu, artik tema burada
        // yok (gorseller liste olarak geliyor).
        String sinir = "----=_AksaMailer_" + Integer.toHexString(html.hashCode());

        StringBuilder eml = new StringBuilder(html.length() * 2);
        eml.append("MIME-Version: 1.0").append(CRLF)
                .append("Subject: ").append(konuBasligi(konu)).append(CRLF)
                .append("X-Unsent: 1").append(CRLF)
                .append("Content-Type: multipart/related; boundary=\"").append(sinir).append("\"").append(CRLF)
                .append(CRLF);

        eml.append("--").append(sinir).append(CRLF)
                .append("Content-Type: text/html; charset=UTF-8").append(CRLF)
                .append("Content-Transfer-Encoding: base64").append(CRLF)
                .append(CRLF)
                .append(base64Satirlari(html.getBytes(StandardCharsets.UTF_8)))
                .append(CRLF);

        for (Map.Entry<String, byte[]> gorsel : gorseller.entrySet()) {
            String cid = gorsel.getKey();
            eml.append("--").append(sinir).append(CRLF)
                    .append("Content-Type: image/png; name=\"").append(cid).append(".png\"").append(CRLF)
                    .append("Content-Transfer-Encoding: base64").append(CRLF)
                    .append("Content-ID: <").append(cid).append(">").append(CRLF)
                    .append("Content-Disposition: inline; filename=\"").append(cid).append(".png\"").append(CRLF)
                    .append(CRLF)
                    .append(base64Satirlari(gorsel.getValue()))
                    .append(CRLF);
        }

        eml.append("--").append(sinir).append("--").append(CRLF);
        return eml.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** RFC 2047: =?UTF-8?B?<base64>?= - Turkce konu bozulmasin diye. */
    private String konuBasligi(String konu) {
        String temiz = (konu == null ? "" : konu).replaceAll("[\\r\\n]+", " ").trim();
        String kodlu = Base64.getEncoder().encodeToString(temiz.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + kodlu + "?=";
    }

    private String base64Satirlari(byte[] veri) {
        String kodlu = Base64.getEncoder().encodeToString(veri);
        StringBuilder satirlar = new StringBuilder(kodlu.length() + kodlu.length() / SATIR_UZUNLUGU * 2);
        for (int i = 0; i < kodlu.length(); i += SATIR_UZUNLUGU) {
            satirlar.append(kodlu, i, Math.min(i + SATIR_UZUNLUGU, kodlu.length())).append(CRLF);
        }
        return satirlar.toString();
    }
}
