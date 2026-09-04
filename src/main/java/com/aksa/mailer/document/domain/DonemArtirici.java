package com.aksa.mailer.document.domain;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Geçen sprintten devam et" kopyalarken dönemi bir sonraki sprinte taşır.
 *
 * NEDEN AYRI BIR SINIF: kural göründüğünden ince ve iki yerde (belge başlığı
 * ve content.header.period) aynı davranmak zorunda. Servisin içine gömülseydi
 * test etmek için veritabanı gerekirdi; burası saf metin işi.
 *
 * KURALI GERCEK VERI BELIRLEDI. Yerel veritabanındaki dönem alanları:
 *   "01.09.2026 – 14.09.2026 · Sprint 42"   tarih aralığı + sprint numarası
 *   "01.09.2026 – 14.09.2026 Sprint Kapanışı"  yalnız aralık
 *   "16.09.2026 · Sprint Değerlendirme"      tek tarih
 *   "Ağustos 2026 Sprint Kapanışı"           tarih yok, sayı yok
 *   "Sprint 24"                              yalnız numara
 *
 * Bu yüzden iki ayrı iş var:
 *
 * 1. TARIH ARALIGI KAYDIRILIR. İki tarih bulunursa aradaki gün sayısı bir
 *    sonraki aralığın uzunluğu sayılır: "01.09 – 14.09" (14 gün) sonrası
 *    "15.09 – 28.09". Sprint uzunluğu böylece takımdan takıma değişebilir,
 *    kodda sabit bir "14 gün" yok.
 *
 * 2. TARIHLERIN DISINDA KALAN SAYI ARTIRILIR. Sprint numarası oradadır.
 *    Tarihlerin içindeki gün/ay sayıları bu taramaya GIRMEZ - girseydi
 *    "16.09.2026" tek tarihi "16.10.2026" olurdu, yani ay atlardı.
 *
 * Yıl gibi duran dört basamaklı sayılar (1900-2100) hiçbir zaman aday
 * değildir: "Ağustos 2026 Sprint Kapanışı" artırılsaydı belge sessizce bir
 * sonraki yıla taşınırdı.
 *
 * Hiçbir kural tutmazsa metne DOKUNULMAZ. Uydurmaktansa kullanıcıya bırakmak
 * doğru: yanlış dönem sessiz bir hatadır, eksik dönem görünür.
 */
public final class DonemArtirici {

    /** gg.aa.yyyy - yerel verideki tek tarih biçimi. */
    private static final Pattern TARIH = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})");

    private static final Pattern SAYI = Pattern.compile("\\d+");

    /** Dört basamaklı ve bu aralıktaysa yıl sayılır, aday olmaz. */
    private static final int YIL_ALT = 1900;
    private static final int YIL_UST = 2100;

    /** 10 basamaktan uzun sayı sprint numarası değildir (sicil, id, telefon). */
    private static final int EN_UZUN_ADAY = 9;

    private DonemArtirici() {
    }

    /**
     * @param metin başlık ya da dönem metni; null/boş olabilir
     * @return bir sonraki sprintin metni - değiştirilecek bir şey yoksa boş
     */
    public static Optional<String> artir(String metin) {
        if (metin == null || metin.isBlank()) {
            return Optional.empty();
        }

        List<Aralik> tarihler = tarihleriBul(metin);
        List<Degisiklik> degisiklikler = new ArrayList<>(kaydirilmisAralik(metin, tarihler));

        artirilacakSayi(metin, tarihler).ifPresent(degisiklikler::add);

        if (degisiklikler.isEmpty()) {
            return Optional.empty();
        }

        // Konuma gore siralanmali: sayi tarihlerden ONCE de gelebiliyor
        // ("Sprint 42 · 01.09.2026 – 14.09.2026"). Siralanmadan birlestirilirse
        // parcalar birbirine karisir.
        degisiklikler.sort(Comparator.comparingInt(Degisiklik::baslangic));
        StringBuilder sonuc = new StringBuilder();
        int imlec = 0;
        for (Degisiklik d : degisiklikler) {
            sonuc.append(metin, imlec, d.baslangic());
            sonuc.append(d.yeniMetin());
            imlec = d.bitis();
        }
        sonuc.append(metin.substring(imlec));
        return Optional.of(sonuc.toString());
    }

    /**
     * İki tarih varsa aralığı bir sonraki döneme kaydırır.
     *
     * Tam olarak iki tarih arıyoruz: tek tarih (bir toplantı günü) neyin ne
     * kadar sonra tekrarlanacağını söylemez, üç ve fazlası da bir aralık
     * değildir. İkisinde de tarihe dokunmamak doğru olan.
     */
    private static List<Degisiklik> kaydirilmisAralik(String metin, List<Aralik> tarihler) {
        if (tarihler.size() != 2) {
            return List.of();
        }

        LocalDate baslangic = tarihler.get(0).tarih();
        LocalDate bitis = tarihler.get(1).tarih();
        if (baslangic == null || bitis == null || bitis.isBefore(baslangic)) {
            return List.of();
        }

        long uzunluk = ChronoUnit.DAYS.between(baslangic, bitis);
        LocalDate yeniBaslangic = bitis.plusDays(1);
        LocalDate yeniBitis = yeniBaslangic.plusDays(uzunluk);

        return List.of(
                tarihler.get(0).ile(bicimle(yeniBaslangic, tarihler.get(0))),
                tarihler.get(1).ile(bicimle(yeniBitis, tarihler.get(1))));
    }

    /**
     * Tarihlerin DISINDA kalan son uygun sayıyı bir artırır.
     *
     * Sondan seçiliyor: "3. Çeyrek Sprint 42" gibi başlıklarda sprint
     * numarası sonda olur.
     */
    private static Optional<Degisiklik> artirilacakSayi(String metin, List<Aralik> tarihler) {
        Matcher eslesme = SAYI.matcher(metin);
        Degisiklik aday = null;

        while (eslesme.find()) {
            String parca = eslesme.group();
            if (parca.length() > EN_UZUN_ADAY || yilMi(parca) || tarihIcinde(eslesme.start(), tarihler)) {
                continue;
            }

            long artan = Long.parseLong(parca) + 1;
            String yeni = String.valueOf(artan);
            // Basamak korunur: "09" -> "10". "99" -> "100" tasar, dogal olan bu.
            if (parca.startsWith("0") && yeni.length() < parca.length()) {
                yeni = "0".repeat(parca.length() - yeni.length()) + yeni;
            }
            aday = new Degisiklik(eslesme.start(), eslesme.end(), yeni);
        }

        return Optional.ofNullable(aday);
    }

    private static List<Aralik> tarihleriBul(String metin) {
        List<Aralik> bulunanlar = new ArrayList<>();
        Matcher eslesme = TARIH.matcher(metin);
        while (eslesme.find()) {
            bulunanlar.add(new Aralik(
                    eslesme.start(), eslesme.end(),
                    ayrisitir(eslesme.group(1), eslesme.group(2), eslesme.group(3)),
                    eslesme.group(1).length(), eslesme.group(2).length()));
        }
        return bulunanlar;
    }

    /** Geçersiz tarih (31.02.2026 gibi) null döner - o zaman kaydırma yapılmaz. */
    private static LocalDate ayrisitir(String gun, String ay, String yil) {
        try {
            return LocalDate.of(Integer.parseInt(yil), Integer.parseInt(ay), Integer.parseInt(gun));
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
    }

    /** Kaynaktaki basamak genişliği korunur: "01.09" yazan "15.09" yazsın. */
    private static String bicimle(LocalDate tarih, Aralik kaynak) {
        String gun = String.valueOf(tarih.getDayOfMonth());
        String ay = String.valueOf(tarih.getMonthValue());
        if (kaynak.gunBasamak() == 2 && gun.length() == 1) gun = "0" + gun;
        if (kaynak.ayBasamak() == 2 && ay.length() == 1) ay = "0" + ay;
        return gun + "." + ay + "." + tarih.getYear();
    }

    private static boolean tarihIcinde(int konum, List<Aralik> tarihler) {
        return tarihler.stream().anyMatch(t -> konum >= t.baslangic() && konum < t.bitis());
    }

    private static boolean yilMi(String parca) {
        if (parca.length() != 4) {
            return false;
        }
        int deger = Integer.parseInt(parca);
        return deger >= YIL_ALT && deger <= YIL_UST;
    }

    private record Degisiklik(int baslangic, int bitis, String yeniMetin) {
    }

    private record Aralik(int baslangic, int bitis, LocalDate tarih, int gunBasamak, int ayBasamak) {
        Degisiklik ile(String yeniMetin) {
            return new Degisiklik(baslangic, bitis, yeniMetin);
        }
    }
}
