package com.aksa.mailer.document.usecase;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.document.domain.Tone;

import java.util.List;

/**
 * Yeni taslagin dogdugu icerik. Tipe gore bolum iskeleti degisir.
 *
 * DIKKAT - bu TEMA DEGIL. Burada renk, logo, maskot yok; sadece kullanicinin
 * dolduracagi bos yapinin sekli var. Tema kodda ayri yasar (render/theme/).
 *
 * sections SABIT ikili degil: takim bolum ekleyip cikarabilir, bu yalnizca
 * baslangic hali.
 */
final class VarsayilanIcerik {

    private VarsayilanIcerik() {
    }

    private static final List<String> KAPANIS_SUTUNLARI =
            List.of("sector", "jira", "ci", "process", "stage", "stake", "note");

    /**
     * Planlama alanlari v6.1'in planningRowCard'indan. "sprint", "sector" ve
     * "department" de kullanilabilir ama VARSAYILAN OLARAK KAPALI: govde
     * 760px sabit, alti sutundan fazlasi Outlook'ta okunmaz hale geliyor.
     * Ihtiyaci olan takim columns'a ekler.
     */
    private static final List<String> PLANLAMA_SUTUNLARI =
            List.of("topicType", "jira", "summary", "status", "expected", "stake");

    static MailContent uret(TemplateType tip) {
        MailContent bos = MailContent.bos();
        return new MailContent(
                bos.schemaVersion(),
                bos.header(),
                bos.meeting(),
                bos.intro(),
                bolumler(tip),
                bos.notes(),
                bos.footer());
    }

    private static List<MailSection> bolumler(TemplateType tip) {
        return switch (tip) {
            case KAPANIS -> List.of(
                    new MailSection("analysis", "ANALİZ ÇALIŞMALARI", Tone.BLUE, KAPANIS_SUTUNLARI, List.of()),
                    new MailSection("development", "GELİŞTİRME ÇALIŞMALARI", Tone.GREEN, KAPANIS_SUTUNLARI, List.of()));
            // Tek bolum: planlamada analiz/gelistirme ayrimi yok, butun
            // konular tek listede durur (docs/BRIEF.md §2).
            case PLANLAMA -> List.of(
                    new MailSection("topics", "SPRINT KONULARI", Tone.BLUE, PLANLAMA_SUTUNLARI, List.of()));
            // Yonetici ozeti satir listesi degil; sayac + sektor ozeti + dikkat
            // gerektiren konular. Tasarimi Sprint 2'de, bir yoneticiye
            // onaylatildiktan SONRA yazilacak (yol haritasi).
            case YONETICI_OZETI -> List.of(
                    new MailSection("attention", "DİKKAT GEREKTİREN KONULAR", Tone.BLUE,
                            List.of("sector", "process", "note"), List.of()));
        };
    }
}
