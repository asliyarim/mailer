package com.aksa.mailer.render.template;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.MailTheme;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sprint Planlama maili. Ortak govde MailIskeleti'nde.
 *
 * Kapanis'tan iki farki var:
 *
 * 1. TEK GENIS TABLO (docs/BRIEF.md §2). Kapanis analiz/gelistirme diye
 *    ikiye ayrilir; planlamada boyle bir ayrim yok, butun konular tek
 *    listede durur. Icerik semasinda yine "sections" var - bir takim
 *    isterse ikiye bolebilsin diye - ama tipik kullanim tek bolum.
 *
 * 2. Sayaclar farkli. "TOPLAM SÜREÇ" yerine "TOPLAM KONU"; sektor yerine
 *    DURUM'a gore dagilim daha anlamli, cunku planlama mailinin sorusu
 *    "hangi konu hangi asamada" - v6.1'in Durum alani (UAT, DEVELOPMENT)
 *    tam bunu tasiyor.
 *
 * Alanlar v6.1'in planningRowCard'indan: topicType, jira (konu anahtari),
 * summary, status, sprint, expected, stake, sector, department.
 */
@Component
public class PlanlamaTemplate extends MailIskeleti {

    @Override
    public TemplateType tip() {
        return TemplateType.PLANLAMA;
    }

    /**
     * Toplam konu + duruma gore dagilim. Durum sutunu yoksa yalnizca toplam.
     *
     * En fazla uc durum gosterilir: sayac seridi Outlook'ta sabit genislikte
     * ve dorduncu kutu sikistirinca rakamlar okunmaz hale geliyor. Kalanlar
     * zaten tabloda gorunuyor.
     */
    @Override
    protected void sayaclar(StringBuilder html, MailContent content, MailTheme tema) {
        if (content.sections().isEmpty()) {
            return;
        }
        Map<String, Integer> durumlar = durumDagilimi(content);
        int kutuSayisi = 1 + Math.min(durumlar.size(), 3);
        int genislik = 100 / kutuSayisi;

        sayacBasla(html, tema);
        sayacKutusu(html, tema, genislik, content.toplamSatirSayisi(), "TOPLAM KONU",
                tema.blue().counterText(), kutuSayisi > 1);

        int yazilan = 0;
        for (Map.Entry<String, Integer> durum : durumlar.entrySet()) {
            if (yazilan == 3) {
                break;
            }
            yazilan++;
            // Ton donusumlu: ilk durum mavi, sonraki yesil, sonraki mavi...
            String renk = yazilan % 2 == 1 ? tema.green().counterText() : tema.blue().counterText();
            sayacKutusu(html, tema, genislik, durum.getValue(),
                    com.aksa.mailer.render.usecase.HtmlKacis.kacir(durum.getKey().toUpperCase()),
                    renk, yazilan < Math.min(durumlar.size(), 3));
        }
        sayacBitir(html);
    }

    /** Durum -> konu sayisi, ilk gorunme sirasini koruyarak. Bos durumlar sayilmaz. */
    private Map<String, Integer> durumDagilimi(MailContent content) {
        Map<String, Integer> dagilim = new LinkedHashMap<>();
        for (MailSection bolum : content.sections()) {
            if (!bolum.columns().contains("status")) {
                continue;
            }
            for (Map<String, String> satir : bolum.rows()) {
                String durum = satir.get("status");
                if (durum != null && !durum.isBlank()) {
                    dagilim.merge(durum.trim(), 1, Integer::sum);
                }
            }
        }
        return dagilim;
    }

    @Override
    protected void bolumler(StringBuilder html, MailContent content, MailTheme tema) {
        for (MailSection bolum : content.sections()) {
            bolumBasligi(html, bolum, tema);
            if (bolum.rows().isEmpty() || bolum.columns().isEmpty()) {
                continue;
            }
            // Kapanis'in aksine gruplama YOK: planlamada konular tek listede,
            // sirasi kullanicinin girdigi sira.
            Map<String, List<Satir>> tekGrup = new LinkedHashMap<>();
            tekGrup.put("", Satir.hepsi(bolum.rows()));
            tablo(html, bolum.key(), bolum.columns(), tekGrup, tema, tema.colors(bolum.tone()));
        }
    }
}
