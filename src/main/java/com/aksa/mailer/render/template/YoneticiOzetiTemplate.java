package com.aksa.mailer.render.template;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;
import com.aksa.mailer.render.domain.ToneColors;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aksa.mailer.render.usecase.HtmlKacis.kacir;
import static com.aksa.mailer.render.usecase.HtmlKacis.kacirSatirlariKoru;

/**
 * Yonetici Ozeti maili. Yapi AKSA_Sprint_Mail_Studio prototipinin "cto"
 * sablonundan alindi.
 *
 * Diger iki tipten iki farki var:
 *
 * 1. DORT SAYAC ve hicbiri "toplam satir" degil. Bu mailin sorusu "sprint'te
 *    ne yapildi" degil, "kim vardi, ne konusuldu, ne karara baglandi, ne
 *    acikta kaldi". Sayaclar da onu cevapliyor.
 *
 * 2. Son bolum TABLO DEGIL KART. Baglantilar tabloya konsaydi URL'ler ham
 *    metin olarak akar ve satirlari tasirdi; kart hem butonu tasiyor hem de
 *    uc baglantiyi yan yana sigdiriyor.
 *
 * Prototipteki yuvarlak koseler ve gradyanlar BILEREK alinmadi: Outlook
 * border-radius'u yok sayar, kart kare gorunur ve tasarim iki istemcide iki
 * turlu cikardi (Mimari Kural 2).
 */
@Component
public class YoneticiOzetiTemplate extends MailIskeleti {

    /** Sayilmayan durum: bekleyen sayacina girmez. */
    private static final String TAMAMLANDI = "Tamamlandı";

    /**
     * Aksiyon durumunun ALABILECEGI degerler - TEK KAYNAK.
     * Arayuze data-secenekler ile bildiriliyor; ikinci bir liste yazilmaz.
     */
    private static final List<String> DURUMLAR =
            List.of("Bekliyor", "Devam Ediyor", "Karar Bekliyor", TAMAMLANDI);

    private static final String BOLUM_GORUSULEN = "discussed";
    private static final String BOLUM_KARARLAR = "decisions";
    private static final String BOLUM_AKSIYONLAR = "actions";
    private static final String BOLUM_BAGLANTILAR = "links";

    /** Kart zeminleri - prototipteki uclu dizilim, sirayla dolasilir. */
    private static final List<String> KART_ZEMINLERI = List.of("#f3f7fd", "#f3f8ed", "#fff7ef");

    @Override
    public TemplateType tip() {
        return TemplateType.YONETICI_OZETI;
    }

    /**
     * Bolumun cark logosu - BUTUN TAKIMLARDA AYNI. Bu mail bir takimin degil
     * bolumun ozeti; takim logosu yaniltici olurdu. Zemin yine takimin
     * renginde kaliyor.
     */
    @Override
    public ThemeImage heroGorseli(MailTheme tema) {
        return tema.heroYoneticiOzeti();
    }

    // --- sayaclar -------------------------------------------------------------

    /**
     * Dort sayac. Hepsi SUNUCUDA hesaplaniyor - istemci gonderemez, cunku
     * gonderebilseydi ekranda gorunen sayi ile mailde giden sayi ayrisirdi.
     */
    @Override
    protected void sayaclar(StringBuilder html, MailContent content, MailTheme tema) {
        if (content.sections().isEmpty()) {
            return;
        }
        int ekip = yerAlanEkipSayisi(content);
        int gorusulen = satirSayisi(content, BOLUM_GORUSULEN);
        int karar = satirSayisi(content, BOLUM_KARARLAR);
        int bekleyen = bekleyenSayisi(content);

        sayacBasla(html, tema);
        sayacKutusu(html, tema, 25, ekip, "YER ALAN EKİP", tema.blue().counterText(), true);
        sayacKutusu(html, tema, 25, gorusulen, "GÖRÜŞÜLEN KONU", tema.blue().counterText(), true);
        sayacKutusu(html, tema, 25, karar, "ALINAN KARAR", tema.green().counterText(), true);
        sayacKutusu(html, tema, 25, bekleyen, "BEKLEYEN KONU", tema.orange().counterText(), false);
        sayacBitir(html);
    }

    /** Gorusulen konularda gecen TEKRARSIZ ekip sayisi. Bos hucreler sayilmaz. */
    private int yerAlanEkipSayisi(MailContent content) {
        Set<String> ekipler = new LinkedHashSet<>();
        for (Map<String, String> satir : satirlar(content, BOLUM_GORUSULEN)) {
            String ekip = satir.get("team");
            if (ekip != null && !ekip.isBlank()) {
                ekipler.add(ekip.trim());
            }
        }
        return ekipler.size();
    }

    /**
     * Tamamlanmamis aksiyonlar. Durumu bos olan da bekliyor sayilir: bos
     * birakilmis bir aksiyon "bitti" demek degildir.
     */
    private int bekleyenSayisi(MailContent content) {
        int sayi = 0;
        for (Map<String, String> satir : satirlar(content, BOLUM_AKSIYONLAR)) {
            String durum = satir.get("status");
            if (durum == null || !TAMAMLANDI.equalsIgnoreCase(durum.trim())) {
                sayi++;
            }
        }
        return sayi;
    }

    private int satirSayisi(MailContent content, String anahtar) {
        return satirlar(content, anahtar).size();
    }

    private List<Map<String, String>> satirlar(MailContent content, String anahtar) {
        return content.sections().stream()
                .filter(b -> anahtar.equals(b.key()))
                .findFirst()
                .map(MailSection::rows)
                .orElseGet(List::of);
    }

    // --- bolumler --------------------------------------------------------------

    @Override
    protected void bolumler(StringBuilder html, MailContent content, MailTheme tema) {
        for (MailSection bolum : content.sections()) {
            bolumBasligi(html, bolum, tema);
            if (bolum.rows().isEmpty() || bolum.columns().isEmpty()) {
                continue;
            }
            if (BOLUM_BAGLANTILAR.equals(bolum.key())) {
                baglantiKartlari(html, bolum, tema);
                continue;
            }
            Map<String, List<Satir>> tekGrup = new LinkedHashMap<>();
            tekGrup.put("", numaralandir(bolum));
            tablo(html, bolum.key(), bolum.columns(), tekGrup, tema, tema.colors(bolum.tone()));
        }
    }

    /**
     * Karar numarasi kullanicidan gelmiyor, asagida uretiliyor - o yuzden
     * duzenleme adresi de almiyor. Alsaydi kullanici K-01'e tiklayip "K-05"
     * yazabilir, sonraki cizimde numara yeniden uretilip yazdigini ezerdi.
     */
    @Override
    protected boolean uretilenSutun(String bolumAnahtari, String sutun) {
        return BOLUM_KARARLAR.equals(bolumAnahtari) && "no".equals(sutun);
    }

    /**
     * Aksiyon durumu sabit dort deger. Serbest metin olsaydi yazim hatasi
     * ("Tamamlandi") bekleyen sayacini bozardi - sayac tam bu degeri ariyor.
     *
     * Ayni alan Sprint Planlama'da serbest metindir ve oyle kalmali; oradaki
     * durumlar takimdan takima degisiyor.
     */
    @Override
    protected String secenekler(String bolumAnahtari, String sutun) {
        if (BOLUM_AKSIYONLAR.equals(bolumAnahtari) && "status".equals(sutun)) {
            return seceneklerNiteligi(DURUMLAR);
        }
        return "";
    }

    /**
     * "no" sutunu bos birakilmissa K-01, K-02 diye doldurur.
     *
     * Numarayi ICERIGE yazmiyoruz, yalnizca cizerken uretiyoruz: kullanici bir
     * karari silince kalanlar kendiliginden yeniden numaralanmali. Kaydedilmis
     * olsaydi K-01, K-03 diye bosluklu giderdi.
     */
    private List<Satir> numaralandir(MailSection bolum) {
        List<Satir> hepsi = Satir.hepsi(bolum.rows());
        if (!bolum.columns().contains("no")) {
            return hepsi;
        }
        List<Satir> satirlar = new ArrayList<>(hepsi.size());
        for (Satir satir : hepsi) {
            Map<String, String> kopya = new LinkedHashMap<>(satir.degerler());
            String mevcut = kopya.get("no");
            if (mevcut == null || mevcut.isBlank()) {
                kopya.put("no", String.format("K-%02d", satir.index() + 1));
            }
            // Indeks KORUNUYOR: numara cizim sirasindan, adres icerikteki
            // sirasindan geliyor. Ikisi karisirsa duzenleme yanlis karara gider.
            satirlar.add(new Satir(satir.index(), kopya));
        }
        return satirlar;
    }

    // --- baglanti kartlari ------------------------------------------------------

    /**
     * Uc kart yan yana. Kart sayisi degisebilir; genislik esit bolunur.
     * Tek satirda kalirlar - Outlook kaydirmaz, dar kart okunmaz hale
     * gelecegi icin kart sayisini arayuz sinirlamali.
     */
    private void baglantiKartlari(StringBuilder html, MailSection bolum, MailTheme tema) {
        List<Map<String, String>> kartlar = bolum.rows();
        int genislik = 100 / kartlar.size();
        ToneColors renk = tema.colors(bolum.tone());

        // Kart bloku tablo()'dan gecmiyor, isareti burada koyuyoruz - yoksa
        // arayuz baglanti kartlarina kart ekleyemezdi.
        html.append("<table role=\"presentation\"").append(bolumIsareti(bolum.key()))
                .append(" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"")
                .append(" border=\"0\" style=\"table-layout:fixed;border-collapse:collapse\"><tr>");

        for (int i = 0; i < kartlar.size(); i++) {
            Map<String, String> kart = kartlar.get(i);
            String zemin = KART_ZEMINLERI.get(i % KART_ZEMINLERI.size());
            String kartYolu = "sections." + bolum.key() + ".rows." + i + ".";

            // Kartin kendisi de "satir" - ekleme/silme/siralama icin capa.
            html.append("<td").append(adres("sections." + bolum.key() + ".rows." + i))
                    .append(" width=\"").append(genislik).append("%\" valign=\"top\" style=\"padding:8px\">")
                    .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"")
                    .append(" border=\"0\" style=\"background:").append(zemin)
                    .append(";border:1px solid ").append(tema.border())
                    .append(";border-collapse:collapse\"><tr><td style=\"padding:14px\">");

            String tur = kart.getOrDefault("linkType", "");
            if (!tur.isBlank()) {
                html.append("<div").append(adres(kartYolu + "linkType"))
                        .append(" style=\"font-family:Arial,sans-serif;font-size:10px;font-weight:bold;")
                        .append("letter-spacing:1px;color:").append(tema.bodyText())
                        .append("\">").append(kacir(tur.toUpperCase())).append("</div>");
            }

            html.append("<div").append(adres(kartYolu + "title"))
                    .append(" style=\"font-family:Arial,sans-serif;font-size:14px;font-weight:bold;")
                    .append("margin-top:6px;color:").append(renk.keyText()).append("\">")
                    .append(kacir(kart.getOrDefault("title", ""))).append("</div>")
                    .append("<div").append(adres(kartYolu + "description"))
                    .append(" style=\"font-family:Arial,sans-serif;font-size:11px;line-height:16px;")
                    .append("margin-top:6px;color:").append(tema.bodyText()).append("\">")
                    .append(kacirSatirlariKoru(kart.getOrDefault("description", ""))).append("</div>");

            baglantiButonu(html, kart, renk, kartYolu);

            html.append("</td></tr></table></td>");
        }
        html.append("</tr></table>");
    }

    /**
     * Buton yalnizca URL varsa cizilir. URL'siz bir buton maildeki en can
     * sikici seydir: tiklanir, hicbir sey olmaz.
     */
    private void baglantiButonu(StringBuilder html, Map<String, String> kart, ToneColors renk, String kartYolu) {
        String url = kart.getOrDefault("url", "");
        if (url.isBlank()) {
            return;
        }
        String metin = kart.getOrDefault("button", "");
        if (metin.isBlank()) {
            metin = "İncele";
        }
        html.append("<div style=\"margin-top:12px\">")
                .append("<a href=\"").append(kacir(url)).append("\"")
                .append(adres(kartYolu + "button"))
                .append(" style=\"display:inline-block;padding:9px 14px;background:").append(renk.sectionHeader())
                .append(";color:#ffffff;font-family:Arial,sans-serif;font-size:11px;font-weight:bold;")
                .append("text-decoration:none\">")
                .append(kacir(metin))
                .append("</a></div>");
    }
}
