package com.aksa.mailer.document.domain;

import java.util.List;
import java.util.Map;

/**
 * Mailin bir bolumu (orn. "ANALIZ CALISMALARI") ve satirlari.
 *
 * SATIRLAR NESNE, DIZI DEGIL. rows elemani Map<String,String>: alanlara
 * anahtarla erisilir (row.get("sector")), indeksle degil. Prototipte
 * data[0]/data[1] kullaniliyordu ve bir sutun eklenince her sey kayiyordu
 * (bkz. docs/api.md, content semasi kural 1).
 *
 * columns, satirlarin hangi alanlarini hangi SIRAYLA gosterecegimizi soyler;
 * satirdaki fazladan anahtarlar cizilmez. Boylece sutun eklemek/cikarmak
 * veri kaybettirmez.
 */
public record MailSection(
        String key,
        String title,
        Tone tone,
        List<String> columns,
        List<Map<String, String>> rows) {

    public MailSection {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    /** Sayaclar SAKLANMAZ - her zaman buradan hesaplanir. */
    public int satirSayisi() {
        return rows.size();
    }
}
