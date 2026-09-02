package com.aksa.mailer.render.template;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aksa.mailer.render.usecase.HtmlKacis.kacir;

/**
 * Toplanti Ciktilari maili. Yapi AKSA_Sprint_Mail_Studio prototipinin
 * "meeting_outcomes" sablonundan.
 *
 * YONETICI OZETI ILE NEDEN AYRI: ikisi de gorusulen konu / karar / aksiyon
 * tasiyor ama baglamlari farkli. Yonetici Ozeti bir TAKIMIN SPRINT'ine
 * bagli; bu ise HERHANGI BIR TOPLANTIYA ve takimlar ustu. Birlestirilseydi
 * her iki durumda da yarisi bos kalan bir form cikardi.
 *
 * Uc gorunur farki var:
 *
 * 1. TOPLANTI KUTUSU. Toplantinin adi, moderatoru ve katilimcilari bu mailin
 *    en cok sorulan bilgisi - "bu karari kim, nerede aldi". Sprint mailinde
 *    boyle bir soru yok.
 * 2. Kararlarda KAPSAM sutunu. Resmi bir karar kaydinda "hangi alanda karar
 *    alindi" ayri bir bilgi (Mimari, Kapsam, Tasarim...).
 * 3. Baglanti kartlari YOK. Bu mail bir tutanak; ek erisim baglantilari
 *    Yonetici Ozeti'ne ait.
 *
 * Bolum basliklari numarali ("1.", "2.") - tutanak gibi okunuyor. Numarayi
 * icerik tasiyor, sablon uretmiyor: kullanici bolum basligini degistirebilir.
 */
@Component
public class ToplantiCiktilariTemplate extends MailIskeleti {

    private static final String TAMAMLANDI = "Tamamlandı";

    /** Aksiyon durumunun alabilecegi degerler - TEK KAYNAK, arayuze bildirilir. */
    private static final List<String> DURUMLAR =
            List.of("Bekliyor", "Devam Ediyor", "Karar Bekliyor", TAMAMLANDI);

    private static final String BOLUM_GORUSULEN = "discussed";
    private static final String BOLUM_KARARLAR = "decisions";
    private static final String BOLUM_AKSIYONLAR = "actions";

    @Override
    public TemplateType tip() {
        return TemplateType.TOPLANTI_CIKTILARI;
    }

    /**
     * Toplanti logosu - BUTUN TAKIMLARDA AYNI. Bu sablon sprint'e degil
     * herhangi bir toplantiya bagli; takim logosu yaniltici olurdu.
     */
    @Override
    public ThemeImage heroGorseli(MailTheme tema) {
        return tema.heroToplantiCiktilari();
    }

    // --- toplanti kutusu --------------------------------------------------------

    /**
     * Sayaclardan ONCE toplanti kutusunu cizer.
     *
     * Iskelet "sayaclar" adimini tipe biraktigi icin kutuyu buraya
     * yerlestiriyoruz - hero ile sayaclarin arasi, prototipteki yer.
     */
    @Override
    protected void sayaclar(StringBuilder html, MailContent content, MailTheme tema) {
        toplantiKutusu(html, content, tema);

        if (content.sections().isEmpty()) {
            return;
        }
        int gorusulen = satirlar(content, BOLUM_GORUSULEN).size();
        int karar = satirlar(content, BOLUM_KARARLAR).size();
        int acikAksiyon = acikAksiyonSayisi(content);
        int ekip = ilgiliEkipSayisi(content);

        sayacBasla(html, tema);
        sayacKutusu(html, tema, 25, gorusulen, "GÖRÜŞÜLEN KONU", tema.blue().counterText(), true);
        sayacKutusu(html, tema, 25, karar, "ALINAN KARAR", tema.green().counterText(), true);
        sayacKutusu(html, tema, 25, acikAksiyon, "AÇIK AKSİYON", tema.orange().counterText(), true);
        sayacKutusu(html, tema, 25, ekip, "İLGİLİ EKİP", tema.blue().counterText(), false);
        sayacBitir(html);
    }

    /**
     * Toplantinin adi, zamani, yeri, moderatoru ve katilimcilari.
     *
     * Hicbiri dolu degilse kutu HIC cizilmez: bos etiketlerle dolu bir kutu
     * mailin en ustunde durup "burasi eksik" der.
     */
    private void toplantiKutusu(StringBuilder html, MailContent content, MailTheme tema) {
        MailContent.Meeting m = content.meeting();
        if (m == null) {
            return;
        }
        boolean solDolu = dolu(m.title()) || dolu(m.date()) || dolu(m.time()) || dolu(m.place());
        boolean sagDolu = dolu(m.moderator()) || dolu(m.attendees());
        if (!solDolu && !sagDolu) {
            return;
        }

        html.append(kutuBasla(tema))
                .append("<td width=\"50%\" valign=\"top\" style=\"padding:14px 18px\">");
        kutuAlani(html, tema, "TOPLANTI KONUSU / ADI", "meeting.title", m.title(), true);
        zamanVeYer(html, tema, m);
        html.append("</td>")
                .append("<td width=\"50%\" valign=\"top\" style=\"padding:14px 18px;border-left:1px solid ")
                .append(tema.border()).append("\">");
        kutuAlani(html, tema, "MODERATÖR / NOT ALAN", "meeting.moderator", m.moderator(), true);
        kutuAlani(html, tema, "KATILIMCI EKİPLER / PAYDAŞLAR", "meeting.attendees", m.attendees(), false);
        html.append("</td></tr></table>");
    }

    /** Tarih, saat ve yer tek satirda; her biri KENDI adresini tasir. */
    private void zamanVeYer(StringBuilder html, MailTheme tema, MailContent.Meeting m) {
        if (!dolu(m.date()) && !dolu(m.time()) && !dolu(m.place())) {
            return;
        }
        html.append(etiket(tema, "TARİH · SAAT · YER"))
                .append("<div style=\"font-family:Arial,sans-serif;font-size:12px;margin-top:3px;color:")
                .append(tema.bodyText()).append("\">");

        List<String> parcalar = new java.util.ArrayList<>();
        if (dolu(m.date())) parcalar.add(parca("meeting.date", m.date()));
        if (dolu(m.time())) parcalar.add(parca("meeting.time", m.time()));
        if (dolu(m.place())) parcalar.add(parca("meeting.place", m.place()));
        html.append(String.join(" &nbsp;&middot;&nbsp; ", parcalar)).append("</div>");
    }

    private String parca(String yol, String deger) {
        return "<span" + adres(yol) + ">" + kacir(deger) + "</span>";
    }

    /**
     * Bir etiket + deger cifti. Deger BOSSA hic cizilmez - ama adres yine de
     * gerekli olurdu; bos alan zaten mailde gorunmuyor, kullanici onu sol
     * panelden doldurur (arayuzle mutabik kalinan kural).
     */
    private void kutuAlani(StringBuilder html, MailTheme tema, String baslik,
                           String yol, String deger, boolean altBosluk) {
        if (!dolu(deger)) {
            return;
        }
        html.append(etiket(tema, baslik))
                .append("<div").append(adres(yol))
                .append(" style=\"font-family:Arial,sans-serif;font-size:13px;font-weight:bold;")
                .append("margin-top:3px;color:").append(tema.blue().keyText()).append("\">")
                .append(kacir(deger)).append("</div>");
        if (altBosluk) {
            html.append("<div style=\"height:12px;line-height:12px\">&nbsp;</div>");
        }
    }

    private String etiket(MailTheme tema, String metin) {
        return "<div style=\"font-family:Arial,sans-serif;font-size:10px;font-weight:bold;"
                + "letter-spacing:1px;color:" + tema.bodyText() + "\">" + kacir(metin) + "</div>";
    }

    private boolean dolu(String deger) {
        return deger != null && !deger.isBlank();
    }

    // --- sayac hesaplari ---------------------------------------------------------

    /** Tamamlanmamis aksiyonlar. Durumu bos olan da acik sayilir. */
    private int acikAksiyonSayisi(MailContent content) {
        int sayi = 0;
        for (Map<String, String> satir : satirlar(content, BOLUM_AKSIYONLAR)) {
            String durum = satir.get("status");
            if (durum == null || !TAMAMLANDI.equalsIgnoreCase(durum.trim())) {
                sayi++;
            }
        }
        return sayi;
    }

    /**
     * Toplantida adi gecen tekrarsiz ekip sayisi.
     *
     * UC BOLUMU DE tariyor, yalnizca gorusulen konulari degil: bir ekip hic
     * konu acmamis ama karar veya aksiyon almis olabilir - o da toplantiya
     * dahildir.
     */
    private int ilgiliEkipSayisi(MailContent content) {
        Set<String> ekipler = new LinkedHashSet<>();
        for (String bolum : List.of(BOLUM_GORUSULEN, BOLUM_KARARLAR, BOLUM_AKSIYONLAR)) {
            for (Map<String, String> satir : satirlar(content, bolum)) {
                String ekip = satir.get("team");
                if (ekip != null && !ekip.isBlank()) {
                    ekipler.add(ekip.trim());
                }
            }
        }
        return ekipler.size();
    }

    private List<Map<String, String>> satirlar(MailContent content, String anahtar) {
        return content.sections().stream()
                .filter(b -> anahtar.equals(b.key()))
                .findFirst()
                .map(MailSection::rows)
                .orElseGet(List::of);
    }

    // --- bolumler ----------------------------------------------------------------

    @Override
    protected void bolumler(StringBuilder html, MailContent content, MailTheme tema) {
        for (MailSection bolum : content.sections()) {
            bolumBasligi(html, bolum, tema);
            if (bolum.rows().isEmpty() || bolum.columns().isEmpty()) {
                continue;
            }
            Map<String, List<Satir>> tekGrup = new LinkedHashMap<>();
            tekGrup.put("", numaralandir(bolum));
            tablo(html, bolum.key(), bolum.columns(), tekGrup, tema, tema.colors(bolum.tone()));
        }
    }

    /** Karar numarasi cizerken uretilir - Yonetici Ozeti'ndeki gerekcenin aynisi. */
    private List<Satir> numaralandir(MailSection bolum) {
        List<Satir> hepsi = Satir.hepsi(bolum.rows());
        if (!bolum.columns().contains("no")) {
            return hepsi;
        }
        List<Satir> satirlar = new java.util.ArrayList<>(hepsi.size());
        for (Satir satir : hepsi) {
            Map<String, String> kopya = new LinkedHashMap<>(satir.degerler());
            String mevcut = kopya.get("no");
            if (mevcut == null || mevcut.isBlank()) {
                kopya.put("no", String.format("K-%02d", satir.index() + 1));
            }
            satirlar.add(new Satir(satir.index(), kopya));
        }
        return satirlar;
    }

    @Override
    protected boolean uretilenSutun(String bolumAnahtari, String sutun) {
        return BOLUM_KARARLAR.equals(bolumAnahtari) && "no".equals(sutun);
    }

    @Override
    protected String secenekler(String bolumAnahtari, String sutun) {
        if (BOLUM_AKSIYONLAR.equals(bolumAnahtari) && "status".equals(sutun)) {
            return seceneklerNiteligi(DURUMLAR);
        }
        return "";
    }
}
