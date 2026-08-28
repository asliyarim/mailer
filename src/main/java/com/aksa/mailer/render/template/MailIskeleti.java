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
            Map.entry("department", "DEPARTMAN"));

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
            Map.entry("department", 14));

    @Override
    public final String uret(MailContent content, MailTheme tema) {
        StringBuilder html = new StringBuilder(8192);

        html.append("<!doctype html><html lang=\"tr\"><head><meta charset=\"UTF-8\">")
                .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">")
                .append("<title>").append(kacir(content.header().title())).append("</title>")
                .append("</head>")
                .append("<body style=\"margin:0;padding:0;background:").append(tema.pageBackground()).append("\">");

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
        ThemeImage gorsel = tema.hero();
        html.append("<tr>")
                .append("<td width=\"").append(HERO_SOL).append("\" height=\"").append(gorsel.height())
                .append("\" valign=\"middle\" style=\"width:").append(HERO_SOL).append("px;height:")
                .append(gorsel.height()).append("px;background:").append(tema.heroBackground())
                .append(";padding:0 18px 0 30px;color:#ffffff\">")
                // nowrap YOK - v2'de vardi ve uzun baslik maskotun altina girip
                // kirpiliyordu. Satir kaymasi, kirpilmaya yeglenir.
                .append("<div style=\"font-size:32px;line-height:37px;font-weight:900\">")
                .append(kacir(content.header().title())).append("</div>")
                .append("<div style=\"font-size:22px;line-height:27px;font-weight:900;color:")
                .append(tema.heroAccent()).append(";margin-top:6px\">")
                .append(kacir(content.header().period())).append("</div>")
                .append("<div style=\"font-size:18px;line-height:24px;margin-top:12px\">")
                .append(kacir(content.header().teamLabel())).append("</div>")
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
        for (int i = 0; i < paragraflar.size(); i++) {
            html.append("<p style=\"margin:0").append(i < paragraflar.size() - 1 ? " 0 12px" : "").append("\">")
                    .append(kacirSatirlariKoru(paragraflar.get(i)))
                    .append("</p>");
        }

        String toplanti = toplantiSatiri(content);
        if (!toplanti.isEmpty()) {
            html.append("<p style=\"margin:12px 0 0;font-weight:bold;color:")
                    .append(tema.blue().keyText()).append("\">").append(toplanti).append("</p>");
        }

        html.append("</td></tr></table>");
    }

    /** "Toplantı: 03.09.2026 · 10:00 · Toplantı Salonu" - yalnizca dolu alanlar. */
    private String toplantiSatiri(MailContent content) {
        MailContent.Meeting m = content.meeting();
        if (m == null) {
            return "";
        }
        List<String> parcalar = new ArrayList<>();
        if (m.date() != null && !m.date().isBlank()) parcalar.add(kacir(m.date()));
        if (m.time() != null && !m.time().isBlank()) parcalar.add(kacir(m.time()));
        if (m.place() != null && !m.place().isBlank()) parcalar.add(kacir(m.place()));
        return parcalar.isEmpty() ? "" : "Toplantı: " + String.join(" &middot; ", parcalar);
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
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"")
                .append(" style=\"margin-top:16px;border-collapse:collapse\"><tr>")
                .append("<td style=\"height:54px;padding:0 20px;background:")
                .append(tema.colors(bolum.tone()).sectionHeader())
                .append(";color:#ffffff;font-family:Arial,sans-serif;font-size:18px;font-weight:900;")
                .append("vertical-align:middle\">")
                .append(kacir(bolum.title()))
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
    protected void tablo(StringBuilder html, List<String> sutunlar,
                         Map<String, List<Map<String, String>>> gruplar,
                         MailTheme tema, ToneColors renk) {
        List<Integer> yuzdeler = sutunYuzdeleri(sutunlar);

        html.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"")
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

        for (Map.Entry<String, List<Map<String, String>>> grup : gruplar.entrySet()) {
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
            for (Map<String, String> satir : grup.getValue()) {
                html.append("<tr>");
                for (int i = 0; i < sutunlar.size(); i++) {
                    String deger = kacirSatirlariKoru(satir.getOrDefault(sutunlar.get(i), ""));
                    // Ilk sutun anahtar sutunudur - kalin ve renkli.
                    String icerik = i == 0
                            ? "<b style=\"color:" + renk.keyText() + "\">" + deger + "</b>"
                            : deger;
                    html.append(hucre(icerik, "", i == sutunlar.size() - 1, tema));
                }
                html.append("</tr>");
            }
        }
        html.append("</table>");
    }

    private String hucre(String icerik, String ekStil, boolean sonSutun, MailTheme tema) {
        return "<td style=\"padding:10px;border-bottom:1px solid " + tema.cellBorder() + ";"
                + (sonSutun ? "" : "border-right:1px solid " + tema.cellBorder() + ";")
                + "vertical-align:top;font-family:Arial,sans-serif;font-size:12px;line-height:17px;color:"
                + tema.bodyText() + ";" + ekStil + "\">" + icerik + "</td>";
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
            html.append("<p style=\"margin:0").append(i < notlar.size() - 1 ? " 0 14px" : "").append("\">")
                    .append("<b style=\"color:").append(tema.colors(not.tone()).bullet())
                    .append("\">&#9679;</b>&nbsp; ")
                    .append(kacirSatirlariKoru(not.text()))
                    .append("</p>");
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
                .append("<div style=\"color:").append(tema.footerAccent())
                .append(";font-size:17px;font-weight:bold\">")
                .append(kacir(content.footer() == null ? "" : content.footer().line1())).append("</div>")
                .append("<div style=\"font-size:21px;font-weight:900;margin-top:5px\">")
                .append(kacir(content.footer() == null ? "" : content.footer().line2())).append("</div>")
                .append("</td>")
                .append("<td width=\"112\" valign=\"bottom\" align=\"center\">")
                .append(img(tema.mascot())).append("</td>")
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
        StringBuilder etiket = new StringBuilder("<img src=\"cid:").append(gorsel.cid())
                .append("\" width=\"").append(gorsel.width()).append("\"");
        if (gorsel.height() != null) {
            etiket.append(" height=\"").append(gorsel.height()).append("\"");
        }
        etiket.append(" alt=\"\" border=\"0\" style=\"display:block;").append(gorsel.boyutStili()).append("\">");
        return etiket.toString();
    }
}
