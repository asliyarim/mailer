package com.aksa.mailer.document.usecase;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.document.domain.Tone;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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

    /**
     * "sector" VARSAYILAN OLARAK KAPALI.
     *
     * Acik oldugunda tablo sektore gore GRUPLANIYOR ve sutun olarak
     * cizilmiyor; yani kullanici sutunu gormeden davranisi degistiriyor.
     * Sektor ayrimina ihtiyaci olan takim sutun secimiden acar - o zaman
     * gruplama da geri gelir. Ihtiyaci olmayan icin sade bir tablo kaliyor.
     */
    private static final List<String> KAPANIS_SUTUNLARI =
            List.of("jira", "ci", "process", "stage", "stake", "note");

    /**
     * Planlama alanlari v6.1'in planningRowCard'indan. "sprint", "sector" ve
     * "department" de kullanilabilir ama VARSAYILAN OLARAK KAPALI: govde
     * 760px sabit, alti sutundan fazlasi Outlook'ta okunmaz hale geliyor.
     * Ihtiyaci olan takim columns'a ekler.
     */
    private static final List<String> PLANLAMA_SUTUNLARI =
            List.of("topicType", "jira", "summary", "status", "expected", "stake");

    /**
     * Turkce buyuk harf. VARSAYILAN LOCALE KULLANILAMAZ: "Dijital" ingilizce
     * kurallarla "DIJITAL" olur, noktali I kaybolur.
     */
    private static final Locale TURKCE = Locale.forLanguageTag("tr");

    /**
     * Baslikta takim adindaki "Takimi"/"Ekibi" sozcugu atilir - v2 ve v6.1
     * prototiplerinde basligin "DIJITAL UYGULAMALAR SPRINT BILGILENDIRME"
     * olmasinin sebebi bu. teamLabel'da ise ad OLDUGU GIBI kalir.
     */
    private static final Pattern TAKIM_SOZCUGU =
            Pattern.compile("\\s+(Takımı|Ekibi)(?=\\s|$)");

    static MailContent uret(TemplateType tip, String takimAdi) {
        MailContent bos = MailContent.bos();
        return new MailContent(
                bos.schemaVersion(),
                baslik(tip, takimAdi),
                bos.meeting(),
                girisParagraflari(tip, takimAdi),
                bolumler(tip),
                bos.notes(),
                kapanis());
    }

    /**
     * Giris paragraflari DOLU dogar.
     *
     * Bu metinler her mailde neredeyse aynen tekrarlanan kurumsal ifadeler;
     * kullaniciyi her seferinde bastan yazdirmanin anlami yok. Prototiplerdeki
     * metinlerin aynisi. Kullanici degistirebilir - baslangic degeri.
     *
     * Takim adi metne giriyor ama SABIT DEGIL: "Ürün Geliştirme olarak..."
     * diye baslayan cumle her takimda kendi adiyla cikiyor.
     */
    private static List<String> girisParagraflari(TemplateType tip, String takimAdi) {
        String ad = takimAdi == null || takimAdi.isBlank() ? "Ekibimiz" : takimAdi.trim();
        // "RPA Takımı olarak" degil "RPA olarak" - cumlede daha dogal duruyor.
        String ozne = TAKIM_SOZCUGU.matcher(ad).replaceAll("");

        return switch (tip) {
            case KAPANIS -> List.of(
                    ozne + " olarak, bu sprint döneminde canlıya alınan ve analiz çalışmaları "
                            + "tamamlanan işler aşağıda bilgilerinize sunulmuştur.",
                    "Gerçekleştirilen çalışmaların ilgili paydaşlarla paylaşılması ve sonuçların "
                            + "birlikte değerlendirilmesi amacıyla Sprint Kapanış Toplantısı planlanmıştır.");
            case PLANLAMA -> List.of(
                    ozne + " olarak, bu sprint içerisinde ele alacağımız çalışmalar aşağıda "
                            + "bilgilerinize sunulmuştur.",
                    "Çalışmaların planlanan takvime uygun ilerleyebilmesi amacıyla, “Bekleyen "
                            + "Konular” alanında belirtilen bilgi, doküman, onay ve aksiyonların "
                            + "ilgili paydaşlar tarafından tamamlanmasını rica ederiz.");
            case YONETICI_OZETI -> List.of(
                    "Gerçekleştirilen Sprint Bilgilendirme ve Değerlendirme Toplantısı kapsamında "
                            + "ekiplerin güncel çalışmaları ve sprint çıktıları değerlendirilmiştir.",
                    "Toplantıda görüşülen konular, alınan kararlar ve takip edilmesi gereken "
                            + "aksiyonlar aşağıda bilgilerinize sunulmuştur.");
            case TOPLANTI_CIKTILARI -> List.of(
                    "Gerçekleştirilen toplantı kapsamında görüşülen konular, alınan kararlar ve "
                            + "takip edilecek aksiyon maddeleri aşağıda bilgilerinize sunulmuştur.");
        };
    }

    /** Kapanis satirlari da dolu dogar - dort mailde de ayni ifade. */
    private static MailContent.Footer kapanis() {
        return new MailContent.Footer("Bilgilerinize sunar,", "iyi çalışmalar dileriz!");
    }

    /**
     * Baslik ve takim etiketi DOLU dogar; ikisi de MailContentValidator'un
     * zorunlu tuttugu alanlar. Bos dogsalardi belge dogdugu anda kendi
     * kuralini ihlal eder, kullanici tek harf yazmadan onizlemede hata
     * gorurdu - yasandi.
     *
     * Uydurma degil TURETME: metin takimin kayitli adindan cikiyor. Sabit
     * yazilsaydi her takimin mailinde ayni takimin adi gorunurdu.
     * Kullanici ikisini de degistirebilir, bunlar yalnizca baslangic degeri.
     */
    private static MailContent.Header baslik(TemplateType tip, String takimAdi) {
        String ad = takimAdi == null ? "" : takimAdi.trim();
        String kisa = TAKIM_SOZCUGU.matcher(ad).replaceAll("").toUpperCase(TURKCE);

        // Toplanti Ciktilari TAKIMLAR USTU: basligina takim adi konmaz.
        // Sprint disi bir toplantinin ciktisi bir takimin adiyla baslamaz.
        if (tip == TemplateType.TOPLANTI_CIKTILARI) {
            return new MailContent.Header("TOPLANTI ÇIKTILARI VE ALINAN KARARLAR", "", ad);
        }

        String son = switch (tip) {
            // Iki prototip de ayni ibareyi kullaniyor; tip ayrimi baslikta
            // degil belgenin kendisinde gorunuyor.
            case KAPANIS, PLANLAMA -> "SPRINT BİLGİLENDİRME";
            // Yonetici ozetinin prototipi henuz yok; "bilgilendirme" demek
            // yaniltici olurdu.
            case YONETICI_OZETI -> "SPRINT YÖNETİCİ ÖZETİ";
            case TOPLANTI_CIKTILARI -> throw new IllegalStateException("yukarida ele alindi");
        };

        // period BOS birakiliyor: sprint numarasi ve tarih araligi takimdan
        // takima degisir, tahmin edilecek bir sey degil.
        return new MailContent.Header(
                kisa.isEmpty() ? son : kisa + " " + son, "", ad);
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
            // Yonetici Ozeti. Yapi AKSA_Sprint_Mail_Studio prototipinin "cto"
            // sablonundan birebir: uc tablo + bir kart bloku.
            //
            // Tonlar rol tasiyor: gorusulenler notr (mavi), kararlar olumlu
            // (yesil), bekleyenler dikkat (turuncu). Renk kodu ICERIKTE YOK,
            // tema cozuyor (Mimari Kural 4).
            case YONETICI_OZETI -> List.of(
                    new MailSection("discussed", "GÖRÜŞÜLEN KONULAR", Tone.BLUE,
                            List.of("team", "topic", "detail"), List.of()),
                    new MailSection("decisions", "ALINAN KARARLAR", Tone.GREEN,
                            List.of("no", "decision", "team"), List.of()),
                    new MailSection("actions", "BEKLEYEN KONULAR VE AKSİYONLAR", Tone.ORANGE,
                            List.of("team", "pending", "owner", "due", "status"), List.of()),
                    // Tablo degil kart olarak cizilir - sablon karar verir,
                    // icerik yapisi diger bolumlerle ayni kalir.
                    new MailSection("links", "İNCELEME VE ERİŞİM BAĞLANTILARI", Tone.BLUE,
                            List.of("linkType", "title", "description", "button", "url"), List.of()));
            // Toplanti Ciktilari. Yonetici Ozeti'ne benziyor ama baglanti
            // kartlari YOK ve kararlarda KAPSAM sutunu VAR - resmi bir karar
            // kaydinda "hangi alanda karar alindi" ayri bir bilgi.
            // Bolum basliklari numarali: bu mail bir tutanak gibi okunuyor.
            case TOPLANTI_CIKTILARI -> List.of(
                    new MailSection("discussed", "1. KONUŞULAN VE DEĞERLENDİRİLEN KONULAR", Tone.BLUE,
                            List.of("team", "topic", "detail"), List.of()),
                    new MailSection("decisions", "2. TOPLANTIDA ALINAN RESMİ KARARLAR", Tone.GREEN,
                            List.of("no", "decision", "scope", "team"), List.of()),
                    new MailSection("actions", "3. BEKLENEN AKSİYONLAR VE SORUMLULAR", Tone.ORANGE,
                            List.of("team", "pending", "owner", "due", "status"), List.of()));
        };
    }
}
