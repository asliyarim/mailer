package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.Tone;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Testlerde kullanilan ornek icerik. Veriler v2/v6.1 prototiplerinin ornek
 * verisinden alindi - uretilen HTML gercek maille kiyaslanabilsin diye.
 *
 * BRIEF: test verisine şğıİçöü koy. Asagidaki satirlar bunu kasten tasiyor.
 */
public final class OrnekIcerik {

    private OrnekIcerik() {
    }

    public static final String TURKCE_SUREC = "Şebeke Operasyonları Aylık Hakediş Faturaları";

    public static MailContent kapanis() {
        return new MailContent(
                1,
                new MailContent.Header(
                        "DİJİTAL UYGULAMALAR SPRINT BİLGİLENDİRME",
                        "Ağustos 2026 Sprint Kapanışı",
                        "BT – RPA Takımı"),
                new MailContent.Meeting("03.09.2026", "10:00", "Yüz Yüze / Toplantı Salonu"),
                List.of(
                        "BT – RPA Takımı olarak bu sprint döneminde tamamlanan çalışmaları, elde edilen "
                                + "çıktıları ve bir sonraki sprint için planlanan adımları özetledik.",
                        "Toplantıya katılımınızın değerli katkı sağlayacağına inanıyor, değerlendirmelerinizi "
                                + "ve geri bildirimlerinizi bekliyoruz.",
                        "İyi çalışmalar dileriz."),
                List.of(
                        new MailSection("analysis", "ANALİZ ÇALIŞMALARI", Tone.BLUE,
                                List.of("sector", "jira", "ci", "process", "stake"),
                                List.of(
                                        satir("Elektrik", "RPA-2066", "CI9353", TURKCE_SUREC, "Tuba Kaya İşler"),
                                        satir("Holding", "RPA-2049", "CI9321", "Multisport Üye Yönetimi",
                                                "İlknur Özgün Tarı, Tuba Nur Adatepe"),
                                        satir("Holding", "RPA-1742", "CI9348", "Petrol Ofisi Faturaları",
                                                "Alper Alyağız, Özgür Çaylı"))),
                        new MailSection("development", "GELİŞTİRME ÇALIŞMALARI", Tone.GREEN,
                                List.of("sector", "jira", "process", "stage", "stake"),
                                List.of(
                                        gelistirme("Doğalgaz", "RPA-2059", "Merkez Faturalama Kontrol ve Oluşturma",
                                                "Geliştirme 2/2", "Furkan Samyeli, Seçkin Soysal"),
                                        gelistirme("Holding", "RPA-846", "Pluxee Yemek Yükleme Adımlarının RPA Süreci",
                                                "Geliştirme", "İlknur Özgün Tarı, Tuba Nur Adatepe")))),
                List.of(
                        new MailContent.Note(Tone.BLUE,
                                "Sprint kapsamında gerçekleştirilen çalışmaların mevcut durumu, elde edilen "
                                        + "çıktılar, varsa açık noktalar ve bir sonraki adımlar toplantıda "
                                        + "değerlendirilecektir."),
                        new MailContent.Note(Tone.GREEN,
                                "Toplantıya katılımınızın değerli katkı sağlayacağına inanıyor, ilave "
                                        + "paydaşların bulunması halinde toplantı davetini ilgili kişilerle "
                                        + "paylaşmanızı rica ederiz.")),
                new MailContent.Footer("Teşekkür ederiz.", "Başarılar dileriz!"));
    }

    /**
     * Sprint Planlama ornegi. Veriler v6.1'in samplePlanning dizisinden.
     * Tek bolum - planlamada analiz/gelistirme ayrimi yok.
     */
    public static MailContent planlama() {
        return new MailContent(
                1,
                new MailContent.Header(
                        "DİJİTAL UYGULAMALAR SPRINT PLANLAMA",
                        "Eylül 2026 Sprint Planlaması",
                        "BT – RPA Takımı"),
                new MailContent.Meeting("06.05.2026", "09:30", "Toplantı Salonu"),
                List.of("Önümüzdeki sprintte ele alınacak konular aşağıda özetlenmiştir."),
                List.of(new MailSection("topics", "SPRINT KONULARI", Tone.BLUE,
                        List.of("topicType", "jira", "summary", "status", "expected", "stake"),
                        List.of(
                                planKonusu("Hikaye", "RPA-1128",
                                        "CI8809 – Banka Mutabakat Süreci Geliştirme 3/3", "UAT",
                                        "06.05.2026 tarihinde öğleden önce UAT toplantısı planlanacak.",
                                        "Tuba Kaya İşler"),
                                planKonusu("Hikaye", "RPA-1127",
                                        "CI8809 – Banka Mutabakat Süreci Geliştirme 2/3", "DEVELOPMENT",
                                        "Filtreleme işlemleri için iş birimi kontrollerine yönelik veri hazırlanacak.",
                                        "Tuba Kaya İşler"),
                                planKonusu("Görev", "RPA-1301",
                                        "Şebeke Operasyonları raporlama iyileştirmesi", "UAT",
                                        "Test senaryoları paydaşlarla gözden geçirilecek.",
                                        "İlknur Özgün Tarı")))),
                List.of(new MailContent.Note(Tone.BLUE,
                        "Planlanan konular sprint boyunca güncellenebilir; değişiklikler ayrıca paylaşılacaktır.")),
                new MailContent.Footer("Teşekkür ederiz.", "İyi çalışmalar!"));
    }

    private static Map<String, String> planKonusu(String tur, String anahtar, String ozet,
                                                  String durum, String beklenen, String paydas) {
        Map<String, String> s = new LinkedHashMap<>();
        s.put("topicType", tur);
        s.put("jira", anahtar);
        s.put("summary", ozet);
        s.put("status", durum);
        s.put("expected", beklenen);
        s.put("stake", paydas);
        return s;
    }

    private static Map<String, String> satir(String sektor, String jira, String ci, String surec, String paydas) {
        Map<String, String> s = new LinkedHashMap<>();
        s.put("sector", sektor);
        s.put("jira", jira);
        s.put("ci", ci);
        s.put("process", surec);
        s.put("stake", paydas);
        return s;
    }

    private static Map<String, String> gelistirme(String sektor, String jira, String surec,
                                                  String asama, String paydas) {
        Map<String, String> s = new LinkedHashMap<>();
        s.put("sector", sektor);
        s.put("jira", jira);
        s.put("process", surec);
        s.put("stage", asama);
        s.put("stake", paydas);
        return s;
    }
}
