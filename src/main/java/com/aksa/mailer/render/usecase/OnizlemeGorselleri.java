package com.aksa.mailer.render.usecase;

import java.util.List;
import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

/**
 * ONIZLEME ICIN gorsel referanslarini cozer. HTML URETMEZ.
 *
 * Renderer tek bir HTML uretir ve gorselleri cid: ile isaret eder
 * (Mimari Kural 3) - .eml icin dogru olan budur, cunku dosyalar
 * multipart/related govdesine gomulur. Ama TARAYICI cid: adresini
 * cozemez: onizleme iframe'inde bes gorselin besi de kirik cikar.
 *
 * Burada yapilan sey mailin YENIDEN URETILMESI DEGIL, ayni metnin
 * tasinmasidir: yalnizca src="cid:X" nitelikleri, ayni dosyanin base64
 * hali ile degistirilir. Duzen, renk, olcu, hicbir sey degismez.
 *
 * Neden data: URI? Onizleme iframe'i sandbox="" ile calisiyor (onizlenen
 * HTML uygulamanin oturumuna erisemesin diye) ve opak bir kaynak uzerinde
 * duruyor - oradan yapilan istek cereze erisemez, yani korumali bir
 * /themes/... ucundan gorsel cekemez. data: URI ise istek gerektirmez.
 * Outlook data: URI'yi anlamaz ama Outlook'a giden .eml'dir, bu HTML degil.
 *
 * Bu sinifa "sadece onizlemede sunu da duzeltelim" turu bir ekleme YAPILMAZ.
 * Ilk satirdaki kural bunun icin var: onizleme ile mail ayrisirsa iki
 * prototipte de yasanan hataya donulur.
 */
@Component
public class OnizlemeGorselleri {

    private final CidImageResolver cozucu;

    public OnizlemeGorselleri(CidImageResolver cozucu) {
        this.cozucu = cozucu;
    }

    /** cid: referanslarini ayni dosyanin base64 haliyle degistirir. */
    public String gomulu(String html, List<ThemeImage> gorselListesi) {
        Map<String, byte[]> icerikler = cozucu.gorseller(gorselListesi);
        String sonuc = html;
        for (ThemeImage gorsel : gorselListesi) {
            byte[] veri = icerikler.get(gorsel.cid());
            if (veri == null) {
                continue;
            }
            String dataUri = "data:" + mimeTipi(gorsel.resourcePath()) + ";base64,"
                    + Base64.getEncoder().encodeToString(veri);
            sonuc = sonuc.replace("src=\"cid:" + gorsel.cid() + "\"", "src=\"" + dataUri + "\"");
        }
        return sonuc;
    }

    private String mimeTipi(String yol) {
        String kucuk = yol.toLowerCase();
        if (kucuk.endsWith(".png")) {
            return "image/png";
        }
        if (kucuk.endsWith(".jpg") || kucuk.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (kucuk.endsWith(".gif")) {
            return "image/gif";
        }
        // Bilinmeyen uzanti: tarayici icerige bakip karar versin.
        return "application/octet-stream";
    }
}
