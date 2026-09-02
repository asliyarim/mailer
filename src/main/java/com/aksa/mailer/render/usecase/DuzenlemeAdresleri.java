package com.aksa.mailer.render.usecase;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * MAIL ICIN duzenleme adreslerini SOYAR. HTML URETMEZ.
 *
 * Sablonlar her duzenlenebilir alana data-alan="header.title" gibi bir adres
 * basar. Arayuz bu adresleri okuyup kullanicinin onizlemedeki alana tiklayip
 * yazmasini sagliyor: hangi hucrenin hangi veriye karsilik geldigini yalnizca
 * SUNUCU bilir, cunku sutun sirasi tipe ve kullanicinin sutun secimine gore
 * degisir. Istemci hucre sayarak eslestirseydi ilk sutun degisikliginde yanlis
 * alana yazardi.
 *
 * Outlook'a giden mailde bu adreslerin isi yok - yalnizca editor icin varlar.
 * Burada tek yaptigimiz sey onlari cikarmak; baska hicbir sey degismiyor.
 *
 * OnizlemeGorselleri'nin AYNADAKI ESI: orada mail HTML'i onizleme icin bir
 * noktada degisiyor (cid: -> data:), burada onizleme HTML'i mail icin bir
 * noktada degisiyor (adresler cikiyor). Ikisi de tek noktada, ikisi de
 * adlandirilmis, ikisi de cift yonlu duman kontrolleriyle kilitli.
 *
 * FARK SAYISI IKI VE OYLE KALMALI. Buraya "sadece mailde sunu da degistirelim"
 * turu bir ekleme YAPILMAZ - fark sessizce artarsa iki prototipte de yasanan
 * "onizlemede duzelen mailde duzelmiyor" hatasina donulur (Mimari Kural 1).
 */
@Component
public class DuzenlemeAdresleri {

    /**
     * Editor icin basilan butun nitelikler. Tirnak icinde tirnak yok -
     * degerler kacirilmis geliyor.
     *
     * YENI BIR data-* NITELIGI EKLERSEN BURAYA DA EKLE. Eklenmezse sessizce
     * Outlook'a gider; DuzenlemeAdresleriTest "geriye hic data- kalmaz" diye
     * bakarak bunu yakaliyor.
     */
    private static final Pattern EDITOR_NITELIKLERI =
            Pattern.compile("\\s+data-(alan|secenekler|bolum)=\"[^\"]*\"");

    public String soy(String html) {
        return EDITOR_NITELIKLERI.matcher(html).replaceAll("");
    }
}
