package com.aksa.mailer.render.template;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;
import com.aksa.mailer.render.domain.ToneColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.aksa.mailer.render.usecase.HtmlKacis.kacir;
import static com.aksa.mailer.render.usecase.HtmlKacis.kacirSatirlariKoru;

/**
 * Butun mail tiplerinin ORTAK govdesi: hero, giris kutusu, sayaclar,
 * alt notlar, footer. Tipe gore degisen tek sey ortadaki bolum/tablo kismi;
 * onu alt siniflar {@link #bolumler} ile doldurur.
 *
 * NEDEN ORTAK BIR ISKELET: uc mail tipi de ayni kurumsal kimligi tasiyor.
 * Her tip kendi HTML'ini bastan yazsaydi zamanla ayrisirlardi - birinde
 * duzeltilen hizalama otekinde duzelmezdi. Projenin bastan kacindigi hata
 * tam olarak buydu (Mimari Kural 1'in ayni mantigi, sablonlar arasinda).
 *
 * Outlook kisitlari burada da gecerli: tablo duzeni, satir ici style, sabit
 * piksel. flexbox, grid, position, border-radius, gradient, background-image,
 * max-width ve web fontu YASAK (Mimari Kural 2).
 */
abstract class MailIskeleti implements MailTemplate {

    /**
     * Bir tablo satiri ve ICERIKTEKI sirasi.
     *
     * Indeks sart, cunku cizim sirasi icerik sirasiyla ayni olmak zorunda
     * degil: Kapanis satirlari sektore gore grupluyor. Duzenleme adresi
     * cizim sirasini tasisaydi kullanici ucuncu satira yazarken icerikte
     * baska bir satir degisirdi.
     */
    protected record Satir(int index, Map<String, String> degerler) {

        /** Bir bolumun satirlarini oldugu sirayla sarar. */
        static List<Satir> hepsi(List<Map<String, String>> satirlar) {
            List<Satir> sonuc = new ArrayList<>(satirlar.size());
            for (int i = 0; i < satirlar.size(); i++) {
                sonuc.add(new Satir(i, satirlar.get(i)));
            }
            return sonuc;
        }
    }

    /** Govde genisligi. Sabit piksel: Outlook yuzde genislikli ic ice kutuyu bozar. */
    protected static final int GOVDE_GENISLIK = 760;
    private static final int HERO_SOL = 445;

    protected static final Map<String, String> SUTUN_BASLIKLARI = Map.ofEntries(
            // Kapanis
            Map.entry("sector", "SEKTÖR"),
            Map.entry("jira", "JIRA"),
            Map.entry("ci", "CI"),
            Map.entry("process", "SÜREÇ"),
            Map.entry("stage", "AŞAMA"),
            Map.entry("stake", "PAYDAŞLAR"),
            Map.entry("note", "KRİTİK NOT"),
            Map.entry("gain", "KAZANÇ (SAAT/YIL)"),
            // Planlama
            Map.entry("topicType", "KONU TÜRÜ"),
            Map.entry("summary", "ÖZET"),
            Map.entry("status", "DURUM"),
            Map.entry("sprint", "SPRINT"),
            Map.entry("expected", "BEKLENEN KONULAR"),
            Map.entry("department", "DEPARTMAN"),
            // Yonetici Ozeti. Ayni kavramin iki tablodaki basligi farkli
            // oldugu icin ANAHTARLARI da ayri: "topic" gorusulen konu,
            // "pending" bekleyen konu. Tek anahtar kullanilsaydi baslik
            // haritasi global oldugundan ikisinden biri yanlis yazilirdi.
            Map.entry("team", "EKİP"),
            Map.entry("topic", "KONU"),
            Map.entry("detail", "AÇIKLAMA"),
            Map.entry("no", "NO"),
            Map.entry("decision", "ALINAN KARAR"),
            Map.entry("pending", "BEKLEYEN KONU"),
            Map.entry("owner", "AKSİYON SAHİBİ"),
            Map.entry("due", "HEDEF"),
            Map.entry("linkType", "TÜR"),
            Map.entry("title", "BAŞLIK"),
            Map.entry("description", "AÇIKLAMA"),
            Map.entry("button", "BUTON"),
            Map.entry("url", "BAĞLANTI"),
            // Toplanti Ciktilari: resmi kararda "hangi alanda" ayri bir bilgi.
            Map.entry("scope", "KAPSAM / ALAN"));

    /** Sutun genislik agirliklari; yuzdeye cevrilir. */
    private static final Map<String, Integer> SUTUN_AGIRLIKLARI = Map.ofEntries(
            Map.entry("sector", 12),
            Map.entry("jira", 15),
            Map.entry("ci", 12),
            Map.entry("process", 34),
            Map.entry("stage", 15),
            Map.entry("stake", 26),
            Map.entry("note", 26),
            Map.entry("gain", 11),
            Map.entry("topicType", 12),
            Map.entry("summary", 34),
            Map.entry("status", 13),
            Map.entry("sprint", 14),
            Map.entry("expected", 32),
            Map.entry("department", 14),
            Map.entry("team", 15),
            Map.entry("topic", 22),
            Map.entry("detail", 45),
            Map.entry("no", 8),
            Map.entry("decision", 52),
            Map.entry("pending", 34),
            Map.entry("owner", 18),
            Map.entry("due", 13),
            Map.entry("linkType", 12),
            Map.entry("title", 20),
            Map.entry("description", 34),
            Map.entry("button", 16),
            Map.entry("url", 26),
            Map.entry("scope", 16));

    @Override
    public final String uret(MailContent content, MailTheme tema) {
        StringBuilder html = new StringBuilder(8192);

        html.append("<!doctype html><html lang=\"tr\"><head><meta charset=\"UTF-8\">")
                .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">")
                .append("<title>").append(kacir(content.header().title())).append("</title>")
                .append("</head>")
                // print-color-adjust: PDF/yazdirma ciktisinda ZEMINLER de
                // bassin. Varsayilan olarak tarayici arka planlari atiyor ve
                // renkli bolum seritleri beyaz kagitta gri yaziya donuyordu -
                // kullanici ekranda bir sey gorup PDF'te baskasini aliyordu.
                // Ozellik KALITILIYOR, bu yuzden body'de bir kez yeter.
                // Outlook bilmedigi ozelligi yok sayar; mail etkilenmez.
                .append("<body style=\"margin:0;padding:0;")
                .append("-webkit-print-color-adjust:exact;print-color-adjust:exact;background:")
                .append(tema.pageBackground()).append("\">");

        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"")
                .append(" style=\"background:").append(tema.pageBackground()).append("\"><tr>")
                .append("<td align=\"center\" style=\"padding:18px 0\">")
                .append("<table role=\"presentation\" width=\"").append(GOVDE_GENISLIK)
                .append("\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:").append(GOVDE_GENISLIK)
                .append("px;background:#ffffff;border-collapse:collapse;font-family:Arial,'Segoe UI',sans-serif\">");

        hero(html, content, tema);

        html.append("<tr><td colspan=\"2\" style=\"padding:18px\">");
        girisKutusu(html, content, tema);
        sayaclar(html, content, tema);
        bolumler(html, content, tema);
        altNotlar(html, content, tema);
        html.append("</td></tr>");

        footer(html, content, tema);

        html.append("</table></td></tr></table></body></html>");
        return html.toString();
    }

    /** Tipe ozgu orta kisim: bolumler ve tablolar. */
    protected abstract void bolumler(StringBuilder html, MailContent content, MailTheme tema);

    /**
     * Sayac kutulari. Varsayilan: toplam + her bolum icin bir tane.
     * Tip farkli sayaclar istiyorsa ezer.
     */
    protected void sayaclar(StringBuilder html, MailContent content, MailTheme tema) {
        List<MailSection> bolumler = content.sections();
        if (bolumler.isEmpty()) {
            return;
        }
        int kutuSayisi = bolumler.size() + 1;
        int genislik = 100 / kutuSayisi;

        sayacBasla(html, tema);
        sayacKutusu(html, tema, genislik, content.toplamSatirSayisi(), "TOPLAM SÜREÇ",
                tema.blue().counterText(), true);
        for (int i = 0; i < bolumler.size(); i++) {
            MailSection bolum = bolumler.get(i);
            sayacKutusu(html, tema, genislik, bolum.satirSayisi(), kacir(bolum.title()),
                    tema.colors(bolum.tone()).counterText(), i < bolumler.size() - 1);
        }
        sayacBitir(html);
    }

    // --- hero ---------------------------------------------------------------

    private void hero(StringBuilder html, MailContent content, MailTheme tema) {
        // Tema DEGIL sablon seciyor: iki toplanti sablonu butun takimlarda
        // ayni logoyu kullaniyor (bkz. MailTemplate.heroGorseli).
        ThemeImage gorsel = heroGorseli(tema);
        html.append("<tr>")
                .append("<td width=\"").append(HERO_SOL).append("\" height=\"").append(gorsel.height())
                .append("\" valign=\"middle\" style=\"width:").append(HERO_SOL).append("px;height:")
                .append(gorsel.height()).append("px;background:").append(tema.heroBackground())
                .append(";padding:0 18px 0 30px;color:#ffffff\">")
                // nowrap YOK - v2'de vardi ve uzun baslik maskotun altina girip
                // kirpiliyordu. Satir kaymasi, kirpilmaya yeglenir.
                .append("<div").append(adres("header.title"))
                .append(" style=\"font-size:32px;line-height:37px;font-weight:900;color:#ffffff\">")
                .append(beyaz(kacir(content.header().title()))).append("</div>")
                .append("<div").append(adres("header.period"))
                .append(" style=\"font-size:22px;line-height:27px;font-weight:900;color:")
                .append(tema.heroAccent()).append(";margin-top:6px\">")
                .append(renkli(kacir(content.header().period()), tema.heroAccent())).append("</div>")
                .append("<div").append(adres("header.teamLabel"))
                .append(" style=\"font-size:18px;line-height:24px;margin-top:12px;color:#ffffff\">")
                .append(beyaz(kacir(content.header().teamLabel()))).append("</div>")
                .append("</td>")
                .append("<td width=\"").append(gorsel.width()).append("\" height=\"").append(gorsel.height())
                .append("\" style=\"width:").append(gorsel.width()).append("px;height:").append(gorsel.height())
                .append("px;padding:0;background:").append(tema.heroBackground()).append("\">")
                .append(img(gorsel))
                .append("</td></tr>");
    }

    // --- giris kutusu --------------------------------------------------------

    private void girisKutusu(StringBuilder html, MailContent content, MailTheme tema) {
        ThemeImage gorsel = tema.intro();
        html.append(kutuBasla(tema))
                .append("<td width=\"150\" align=\"center\" valign=\"middle\" style=\"padding:14px\">")
                .append(img(gorsel)).append("</td>")
                .append("<td valign=\"middle\" style=\"padding:14px 18px;font-family:Arial,sans-serif;")
                .append("font-size:14px;line-height:21px;color:").append(tema.bodyText()).append("\">");

        List<String> paragraflar = content.intro().stream().filter(p -> p != null && !p.isBlank()).toList();
        // Adres icerikteki asil indeksi tasimali: bos paragraflar cizilmiyor,
        // filtrelenmis listenin sirasi kullanilsaydi kullanici ikinci
        // paragrafa yazarken ucuncusu degisirdi.
        List<String> hamGiris = content.intro();
        for (int i = 0, cizilen = 0; i < hamGiris.size(); i++) {
            String paragraf = hamGiris.get(i);
            if (paragraf == null || paragraf.isBlank()) {
                continue;
            }
            cizilen++;
            html.append("<p").append(adres("intro." + i))
                    .append(" style=\"margin:0").append(cizilen < paragraflar.size() ? " 0 12px" : "").append("\">")
                    .append(kacirSatirlariKoru(paragraf))
                    .append("</p>");
        }

        String toplanti = toplantiSatiri(content);
        if (!toplanti.isEmpty()) {
            html.append("<p style=\"margin:12px 0 0;font-weight:bold;color:")
                    .append(tema.blue().keyText()).append("\">").append(toplanti).append("</p>");
        }

        html.append("</td></tr></table>");
    }

    /**
     * "Toplantı: 03.09.2026 · 10:00 · Toplantı Salonu" - yalnizca dolu alanlar.
     * Her parca kendi adresini tasiyor: uc alan tek metne aksaydi kullanici
     * saate tiklayip yazdiginda hangi alanin degistigi belirsiz olurdu.
     */
    private String toplantiSatiri(MailContent content) {
        MailContent.Meeting m = content.meeting();
        if (m == null) {
            return "";
        }
        List<String> parcalar = new ArrayList<>();
        parcaEkle(parcalar, "meeting.date", m.date());
        parcaEkle(parcalar, "meeting.time", m.time());
        parcaEkle(parcalar, "meeting.place", m.place());
        return parcalar.isEmpty() ? "" : "Toplantı: " + String.join(" &middot; ", parcalar);
    }

    private void parcaEkle(List<String> parcalar, String yol, String deger) {
        if (deger != null && !deger.isBlank()) {
            parcalar.add("<span" + adres(yol) + ">" + kacir(deger) + "</span>");
        }
    }

    // --- sayac yardimcilari ---------------------------------------------------

    protected void sayacBasla(StringBuilder html, MailTheme tema) {
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"")
                .append(" style=\"margin-top:16px;border:1px solid ").append(tema.border())
                .append(";border-collapse:collapse\"><tr>");
    }

    protected void sayacBitir(StringBuilder html) {
        html.append("</tr></table>");
    }

    protected void sayacKutusu(StringBuilder html, MailTheme tema, int genislik, int deger,
                               String etiket, String renk, boolean sagCizgi) {
        html.append("<td width=\"").append(genislik).append("%\" align=\"center\" style=\"padding:14px")
                .append(sagCizgi ? ";border-right:1px solid " + tema.border() : "").append("\">")
                .append("<b style=\"font-size:32px;color:").append(renk).append("\">").append(deger).append("</b>")
                .append("<br><span style=\"font-size:11px;font-weight:bold;color:").append(tema.bodyText())
                .append("\">").append(etiket).append("</span></td>");
    }

    // --- bolum basligi ve tablo -----------------------------------------------

    protected void bolumBasligi(StringBuilder html, MailSection bolum, MailTheme tema) {
        html.append("<table role=\"presentation\"").append(bolumIsareti(bolum.key()))
                .append(" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"")
                .append(" style=\"margin-top:16px;border-collapse:collapse\"><tr>")
                .append("<td").append(adres("sections." + bolum.key() + ".title"))
                .append(" style=\"height:54px;padding:0 20px;background:")
                .append(tema.colors(bolum.tone()).sectionHeader())
                .append(";color:#ffffff;font-family:Arial,sans-serif;font-size:18px;font-weight:900;")
                .append("vertical-align:middle\">")
                .append(beyaz(kacir(bolum.title())))
                .append("</td></tr></table>");
    }

    /**
     * TEK tablo. Gruplar ayri tablolar degil, tablo icinde colspan'li baslik
     * satirlaridir - baslik bir kez yazilir ve sutunlar butun gruplarda
     * hizali kalir.
     *
     * @param gruplar grup adi -> satirlar. Gruplama istenmiyorsa tek bir
     *                bos anahtarli girdi verilir.
     */
    protected void tablo(StringBuilder html, String bolumAnahtari, List<String> sutunlar,
                         Map<String, List<Satir>> gruplar,
                         MailTheme tema, ToneColors renk) {
        List<Integer> yuzdeler = sutunYuzdeleri(sutunlar);

        html.append("<table").append(bolumIsareti(bolumAnahtari))
                .append(" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"")
                .append(" style=\"table-layout:fixed;border-collapse:collapse\">");
        for (int yuzde : yuzdeler) {
            html.append("<col width=\"").append(yuzde).append("%\">");
        }

        html.append("<tr>");
        for (int i = 0; i < sutunlar.size(); i++) {
            String baslik = SUTUN_BASLIKLARI.getOrDefault(sutunlar.get(i), sutunlar.get(i).toUpperCase());
            html.append(hucre("<b style=\"color:" + renk.tableHeaderText() + "\">" + kacir(baslik) + "</b>",
                    "background:" + renk.tableHeaderBackground() + ";", i == sutunlar.size() - 1, tema));
        }
        html.append("</tr>");

        for (Map.Entry<String, List<Satir>> grup : gruplar.entrySet()) {
            if (!grup.getKey().isBlank()) {
                html.append("<tr><td colspan=\"").append(sutunlar.size())
                        .append("\" style=\"padding:9px 12px;background:").append(renk.tableHeaderBackground())
                        .append(";border-bottom:1px solid ").append(tema.cellBorder())
                        .append(";border-left:3px solid ").append(renk.sectionHeader())
                        .append(";font-family:Arial,sans-serif;font-size:13px;font-weight:bold;color:")
                        .append(renk.tableHeaderText()).append("\">")
                        .append(kacir(grup.getKey()))
                        .append("</td></tr>");
            }
            for (Satir satir : grup.getValue()) {
                // Satirin kendisi de adreslenir: arayuz satir ekleme/silme/
                // siralama duğmelerini bunun uzerine konumlandiriyor.
                // Hucrelerden cikarim yapmak zorunda kalmasin.
                html.append("<tr").append(adres("sections." + bolumAnahtari + ".rows." + satir.index()))
                        .append(">");
                for (int i = 0; i < sutunlar.size(); i++) {
                    String sutun = sutunlar.get(i);
                    String deger = kacirSatirlariKoru(satir.degerler().getOrDefault(sutun, ""));
                    // Ilk sutun anahtar sutunudur - kalin ve renkli.
                    String icerik = i == 0
                            ? "<b style=\"color:" + renk.keyText() + "\">" + deger + "</b>"
                            : deger;
                    // Uretilen sutuna adres BASILMAZ: adres "bu HTML sunu
                    // gosteriyor" demek, uretilen sutunda ise icerikteki
                    // deger bos - yol yanlis bir sey iddia ederdi. Basilsaydi
                    // kullanici tiklayip yazar, sonraki cizimde sunucu
                    // yazdigini ezerdi (sessiz veri kaybi).
                    String adres = uretilenSutun(bolumAnahtari, sutun)
                            ? ""
                            : adres("sections." + bolumAnahtari + ".rows." + satir.index() + "." + sutun)
                              + secenekler(bolumAnahtari, sutun);
                    html.append(hucre(icerik, "", i == sutunlar.size() - 1, tema, adres));
                }
                html.append("</tr>");
            }
        }
        html.append("</table>");
    }

    private String hucre(String icerik, String ekStil, boolean sonSutun, MailTheme tema) {
        return hucre(icerik, ekStil, sonSutun, tema, "");
    }

    private String hucre(String icerik, String ekStil, boolean sonSutun, MailTheme tema, String adres) {
        return "<td" + adres + " style=\"padding:10px;border-bottom:1px solid " + tema.cellBorder() + ";"
                + (sonSutun ? "" : "border-right:1px solid " + tema.cellBorder() + ";")
                + "vertical-align:top;font-family:Arial,sans-serif;font-size:12px;line-height:17px;color:"
                + tema.bodyText() + ";" + ekStil + "\">" + icerik + "</td>";
    }

    // --- duzenleme adresleri ----------------------------------------------------

    /**
     * data-alan niteligi. Arayuz bunu okuyup kullanicinin onizlemedeki alana
     * tiklayip yazmasini sagliyor; .eml'e giderken DuzenlemeAdresleri soyuyor.
     *
     * Yol kacirilarak yaziliyor: bolum anahtari kullanici icerigi ve tirnak
     * icerebilir - kacirilmasa nitelikten disari tasip HTML'i bozardi.
     */
    protected String adres(String yol) {
        return " data-alan=\"" + kacir(yol) + "\"";
    }

    // --- Outlook'a yapistirma ---------------------------------------------------

    /**
     * Koyu zemin uzerindeki yaziyi beyaz yapar - METIN DUZEYINDE.
     *
     * Rengi kapsayan kutuya (td/div) yazmak EKRANDA yetiyor ama OUTLOOK'A
     * YAPISTIRINCA yetmiyor: Word'un HTML donusturucusu blok elemanin
     * rengini kendi varsayilaniyla eziyor ve yazi SIYAH cikiyor. Renkli
     * kartin uzerinde siyah yazi okunmuyor - yasandi.
     *
     * font/span gibi METIN duzeyindeki bir elemanda duran rengi ise
     * koruyor. O yuzden kutuda da yaziyoruz (ekran icin), burada da
     * (yapistirma icin) - ikisi birbirini tekrar ediyor gibi gorunse de
     * farkli iki istemciyi hedefliyorlar.
     */
    protected String beyaz(String icerik) {
        return renkli(icerik, "#ffffff");
    }

    /**
     * Gorselin alt metni. Yalnizca BILGI TASIYAN gorsellere yaziliyor.
     *
     * Dekoratif olanlar (hero, giris, notlar) bos kaliyor: ekran okuyucunun
     * onlari okumasi kullaniciya bir sey katmaz, gurultu olur. Logo ise
     * bilgi tasiyor - cizilemedigi anda hic olmazsa ne oldugu yazsin.
     */
    private String altMetni(String cid) {
        return "logo".equals(cid) ? "Aksa | Kazancı Holding" : "";
    }

    /**
     * Rengi UC KEZ yazar ve bu bilerek yapiliyor - her biri baska bir
     * istemciyi hedefliyor:
     *
     *   <font color>   Word'un HTML donusturucusu ESKI etiketi CSS'ten daha
     *                  guvenilir uyguluyor. Outlook'a yapistirmada belirleyici
     *                  olan bu - yalnizca CSS ile denendi, yazi siyah kaldi.
     *   style="color"  tarayici ve modern istemciler icin.
     *   mso-color-alt  Word'un kendi ozelligi; bazi surumlerde CSS rengini
     *                  ezerken bunu dinliyor.
     *
     * Tekrar gibi gorunuyor ve oyle - ama uc istemcinin ucunde de dogru
     * renk cikmasinin baska yolu yok.
     */
    protected String renkli(String icerik, String renk) {
        return "<font color=\"" + renk + "\">"
                + "<span style=\"color:" + renk + ";mso-color-alt:" + renk + "\">"
                + icerik
                + "</span></font>";
    }

    /**
     * Degeri KULLANICIDAN degil sablondan gelen sutun.
     *
     * Boyle bir sutun duzenlenebilir gorunmemeli: icerikte karsiligi bos
     * oldugu icin adres yanlis olur, ve kullanici yazsa bile sonraki cizimde
     * uretilen deger onu ezer. Varsayilan: hicbir sutun uretilmiyor.
     */
    protected boolean uretilenSutun(String bolumAnahtari, String sutun) {
        return false;
    }

    /**
     * Sabit secenekli bir sutunsa secenekleri data-secenekler ile bildirir.
     *
     * Boylece liste TEK YERDE kalir: arayuz onu ikinci kez yazmaz. Yazsaydi
     * biri digerine eklenen bir durumu kacirir ve kullanici mailde gecerli
     * ama listede olmayan bir deger gorurdu.
     *
     * Varsayilan: hicbir sutun sabit secenekli - serbest metin sutununa liste
     * dayatmak veri kaybettirir (Planlama'nin "status"u boyle).
     */
    protected String secenekler(String bolumAnahtari, String sutun) {
        return "";
    }

    /** data-secenekler="A|B|C". Ayirac dikey cizgi - degerlerde gecmiyor. */
    protected String seceneklerNiteligi(List<String> degerler) {
        return " data-secenekler=\"" + kacir(String.join("|", degerler)) + "\"";
    }

    /**
     * "Bu DOM parcasi su bolume ait" isareti. Bolum basliginda ve tablosunda.
     *
     * data-alan'dan AYRI olmasinin sebebi: data-alan bir ICERIK YOLUDUR,
     * "sections.discussed.title" duzenlenecek METNI gosterir. Arayuzun "+
     * satir ekle" dugmesini konumlandirmak icin ise bolumun ALANINI bilmesi
     * gerekiyor - baska bir soru, baska bir nitelik.
     *
     * Bolum BOS olsa bile baslik cizildigi icin bu isaret hep var: kullanici
     * hicbir sey doldurmamisken de her basligin altinda satir ekleyebilir.
     */
    protected String bolumIsareti(String bolumAnahtari) {
        return " data-bolum=\"" + kacir(bolumAnahtari) + "\"";
    }

    /** Agirliklari yuzdeye cevirir; toplam farki ilk sutuna eklenir. */
    private List<Integer> sutunYuzdeleri(List<String> sutunlar) {
        int toplamAgirlik = sutunlar.stream().mapToInt(s -> SUTUN_AGIRLIKLARI.getOrDefault(s, 20)).sum();
        List<Integer> yuzdeler = new ArrayList<>(sutunlar.size());
        int birikmis = 0;
        for (String sutun : sutunlar) {
            int yuzde = SUTUN_AGIRLIKLARI.getOrDefault(sutun, 20) * 100 / toplamAgirlik;
            yuzdeler.add(yuzde);
            birikmis += yuzde;
        }
        if (!yuzdeler.isEmpty() && birikmis != 100) {
            yuzdeler.set(0, yuzdeler.get(0) + (100 - birikmis));
        }
        return yuzdeler;
    }

    // --- alt notlar ----------------------------------------------------------

    private void altNotlar(StringBuilder html, MailContent content, MailTheme tema) {
        if (content.notes().isEmpty()) {
            return;
        }
        html.append(kutuBasla(tema))
                .append("<td width=\"250\" align=\"center\" valign=\"middle\" style=\"background:")
                .append(tema.blue().tableHeaderBackground()).append(";padding:12px\">")
                .append(img(tema.notes())).append("</td>")
                .append("<td valign=\"middle\" style=\"padding:18px;font-family:Arial,sans-serif;")
                .append("font-size:13px;line-height:20px;color:").append(tema.bodyText()).append("\">");

        List<MailContent.Note> notlar = content.notes();
        for (int i = 0; i < notlar.size(); i++) {
            MailContent.Note not = notlar.get(i);
            // Adres madde isaretini DEGIL yalnizca metni sariyor: kullanici
            // noktaya degil yaziya tikliyor.
            html.append("<p style=\"margin:0").append(i < notlar.size() - 1 ? " 0 14px" : "").append("\">")
                    .append("<b style=\"color:").append(tema.colors(not.tone()).bullet())
                    .append("\">&#9679;</b>&nbsp; ")
                    .append("<span").append(adres("notes." + i + ".text")).append(">")
                    .append(kacirSatirlariKoru(not.text()))
                    .append("</span></p>");
        }
        html.append("</td></tr></table>");
    }

    // --- footer --------------------------------------------------------------

    private void footer(StringBuilder html, MailContent content, MailTheme tema) {
        html.append("<tr><td colspan=\"2\" style=\"padding:0;background:").append(tema.heroBackground())
                .append("\">")
                .append("<table role=\"presentation\" width=\"100%\" height=\"120\" cellspacing=\"0\"")
                .append(" cellpadding=\"0\" border=\"0\" style=\"border-collapse:collapse\"><tr>")
                .append("<td width=\"300\" align=\"center\" style=\"padding:0 10px\">")
                .append(img(tema.logo())).append("</td>")
                .append("<td style=\"border-left:3px solid ").append(tema.footerRule())
                .append(";padding-left:20px;color:#ffffff;font-family:Arial,sans-serif\">")
                .append("<div").append(adres("footer.line1"))
                .append(" style=\"color:").append(tema.footerAccent())
                .append(";font-size:17px;font-weight:bold\">")
                .append(renkli(kacir(content.footer() == null ? "" : content.footer().line1()), tema.footerAccent())).append("</div>")
                .append("<div").append(adres("footer.line2"))
                .append(" style=\"font-size:21px;font-weight:900;margin-top:5px;color:#ffffff\">")
                .append(beyaz(kacir(content.footer() == null ? "" : content.footer().line2()))).append("</div>")
                .append("</td>")
                // Footer maskotu KALDIRILDI (yonetici istegi): ayni maskot
                // hero gorselinde zaten var, altta ikinci kez cikmasi tekrar
                // oluyordu. Gorsel MailTheme.images() listesinden de cikti -
                // yoksa .eml'e kullanilmayan bir ek olarak gomulur ve Outlook
                // maili atacli gosterirdi.
                .append("</tr></table></td></tr>");
    }

    // --- ortak ---------------------------------------------------------------

    protected String kutuBasla(MailTheme tema) {
        return "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\""
                + " style=\"margin-top:16px;border:1px solid " + tema.border() + ";border-collapse:collapse\"><tr>";
    }

    /**
     * cid: referansi. width/height HTML nitelikleri OLARAK DA yazilir -
     * Outlook style'daki olculeri her zaman dikkate almaz.
     */
    protected String img(ThemeImage gorsel) {
        // Yukseklik KOSULSUZ yaziliyor. Once "verilmisse yaz" seklindeydi ve
        // footer logosu yuksekliksiz tanimlandigi icin Outlook'a yapistirilan
        // mailde HIC CIKMIYORDU - Word height:auto'yu anlamiyor.
        StringBuilder etiket = new StringBuilder("<img src=\"cid:").append(gorsel.cid())
                .append("\" width=\"").append(gorsel.width())
                .append("\" height=\"").append(gorsel.height()).append("\"");
        // alt BOS BIRAKILMIYOR: gorsel cizilemedigi anda kullanici bos bir
        // kutu yerine ne oldugunu goruyor. Dekoratif olanlar bos kaliyor -
        // ekran okuyucu onlari gereksiz yere okumasin.
        etiket.append(" alt=\"").append(kacir(altMetni(gorsel.cid())))
                .append("\" border=\"0\" style=\"display:block;")
                .append(gorsel.boyutStili()).append("\">");
        return etiket.toString();
    }
}
