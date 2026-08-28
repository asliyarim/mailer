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
 * Sprint Kapanış maili. Ortak govde MailIskeleti'nde; burada yalnizca orta
 * kisim var: bolum basliklari ve sektore gore gruplu tablolar.
 *
 * Yapi ve renkler v2 prototipinden birebir
 * (RPA_Sprint_Kapanis_Duzenlenebilir_Uygulama_v2.html, outlookHtml()).
 *
 * v2'den bilerek ayrildigimiz nokta: tablolar SEKTORE GORE GRUPLU
 * (docs/BRIEF.md §2). Grup basligi zaten sektoru soyledigi icin grup
 * icindeki Sektor sutunu tekrar cizilmez - v6.1 ciziyordu, genislik israfiydi.
 */
@Component
public class KapanisTemplate extends MailIskeleti {

    @Override
    public TemplateType tip() {
        return TemplateType.KAPANIS;
    }

    @Override
    protected void bolumler(StringBuilder html, MailContent content, MailTheme tema) {
        for (MailSection bolum : content.sections()) {
            bolumBasligi(html, bolum, tema);
            bolumTablosu(html, bolum, tema);
        }
    }

    private void bolumTablosu(StringBuilder html, MailSection bolum, MailTheme tema) {
        if (bolum.rows().isEmpty()) {
            return;
        }

        // Sektor gorunur bir sutunsa gruplama yapilir ve o sutun tablodan
        // cikarilir - grup basligi zaten sektoru soyluyor.
        boolean sektoreGoreGrupla = bolum.columns().contains("sector");
        List<String> sutunlar = bolum.columns().stream()
                .filter(s -> !(sektoreGoreGrupla && s.equals("sector")))
                .toList();
        if (sutunlar.isEmpty()) {
            return;
        }

        tablo(html, sutunlar, grupla(bolum, sektoreGoreGrupla), tema, tema.colors(bolum.tone()));
    }

    /** Satirlari sektore gore, ilk gorunme sirasini koruyarak gruplar. */
    private Map<String, List<Map<String, String>>> grupla(MailSection bolum, boolean sektoreGore) {
        Map<String, List<Map<String, String>>> gruplar = new LinkedHashMap<>();
        for (Map<String, String> satir : bolum.rows()) {
            String anahtar = sektoreGore ? satir.getOrDefault("sector", "") : "";
            gruplar.computeIfAbsent(anahtar == null ? "" : anahtar, k -> new ArrayList<>()).add(satir);
        }
        return gruplar;
    }
}
