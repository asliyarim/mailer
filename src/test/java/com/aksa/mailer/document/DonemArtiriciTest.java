package com.aksa.mailer.document;

import com.aksa.mailer.document.domain.DonemArtirici;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Geçen sprintten devam et" dönem artırma kuralı.
 *
 * Buradaki testlerin çoğu YANLIS ARTIRMAYI önlüyor, doğru artırmayı değil.
 * Sebep: yanlış artırma sessiz bir hata - kullanıcı "Ağustos 2027" yazan bir
 * maili fark etmeden gönderebilir. Artıramamak ise görünür, kullanıcı elle
 * düzeltir.
 */
@DisplayName("DonemArtirici")
class DonemArtiriciTest {

    @Nested
    @DisplayName("sprint numarasını bulup artırır")
    class Artirir {

        @Test
        @DisplayName("düz sprint numarası")
        void duzNumara() {
            assertThat(DonemArtirici.artir("Sprint 24")).contains("Sprint 25");
        }

        @Test
        @DisplayName("başlığın ortasındaki numara")
        void ortadaki() {
            assertThat(DonemArtirici.artir("Sprint 42 Yönetici Özeti"))
                    .contains("Sprint 43 Yönetici Özeti");
        }

        @Test
        @DisplayName("sıra sayısı - Eylül 1. Sprint")
        void siraSayisi() {
            assertThat(DonemArtirici.artir("Eylül 1. Sprint")).contains("Eylül 2. Sprint");
        }

        @Test
        @DisplayName("basamak taşarken - 9 sonrası 10")
        void basamakTasar() {
            assertThat(DonemArtirici.artir("Sprint 9")).contains("Sprint 10");
        }

        @Test
        @DisplayName("başında sıfır varsa genişlik korunur")
        void bastakiSifir() {
            assertThat(DonemArtirici.artir("Sprint 09")).contains("Sprint 10");
            assertThat(DonemArtirici.artir("Sprint 08")).contains("Sprint 09");
        }

        @Test
        @DisplayName("birden çok sayı varsa SONUNCUSU artar")
        void sonuncusu() {
            // "3. Çeyrek Sprint 42" -> sprint numarasi sonda.
            assertThat(DonemArtirici.artir("3. Çeyrek Sprint 42"))
                    .contains("3. Çeyrek Sprint 43");
        }
    }

    @Nested
    @DisplayName("yılı sprint numarası sanmaz")
    class YilaDokunmaz {

        @Test
        @DisplayName("yıl tek başınaysa metin değişmez")
        void yalnizYil() {
            // "2026" artirilsaydi belge sessizce bir sonraki yila taşınırdı.
            assertThat(DonemArtirici.artir("Ağustos 2026 Sprint Kapanışı")).isEmpty();
        }

        @Test
        @DisplayName("yıl ve sprint numarası birlikteyse numara artar")
        void yilVeNumara() {
            assertThat(DonemArtirici.artir("2026 Sprint 7")).contains("2026 Sprint 8");
        }

        @Test
        @DisplayName("yıl sondaysa bile atlanır")
        void yilSonda() {
            assertThat(DonemArtirici.artir("Sprint 12 · 2026")).contains("Sprint 13 · 2026");
        }

        @Test
        @DisplayName("yıl aralığı dışındaki dört basamak numaradır")
        void aralikDisi() {
            // 1899 ve 2101 yil araligi disinda - normal sayi sayilir.
            assertThat(DonemArtirici.artir("Sprint 2101")).contains("Sprint 2102");
        }
    }

    /**
     * Bu blok YEREL VERITABANINDAKI gerçek dönem metinleriyle yazıldı.
     * Kuralı bu veriler belirledi: ilk yazdığım sürüm yalnızca sayı arıyordu
     * ve "01.09.2026 – 14.09.2026" aralığını "14.10.2026" yapıyordu - yani
     * bitiş tarihini bir ay ileri atıyordu.
     */
    @Nested
    @DisplayName("tarih aralığını bir sonraki sprinte kaydırır")
    class TarihAraligi {

        @Test
        @DisplayName("iki haftalık aralık - gerçek veri")
        void ikiHaftalik() {
            // 01.09 - 14.09 arası 14 gün; sonraki sprint 15.09 - 28.09.
            assertThat(DonemArtirici.artir("01.09.2026 – 14.09.2026 Sprint Kapanışı"))
                    .contains("15.09.2026 – 28.09.2026 Sprint Kapanışı");
        }

        @Test
        @DisplayName("aralık VE sprint numarası birlikte artar - gerçek veri")
        void aralikVeNumara() {
            assertThat(DonemArtirici.artir("01.09.2026 – 14.09.2026 · Sprint 42"))
                    .contains("15.09.2026 – 28.09.2026 · Sprint 43");
        }

        @Test
        @DisplayName("sprint numarası tarihlerden önce gelse de doğru birleşir")
        void numaraOnce() {
            assertThat(DonemArtirici.artir("Sprint 42 · 01.09.2026 – 14.09.2026"))
                    .contains("Sprint 43 · 15.09.2026 – 28.09.2026");
        }

        @Test
        @DisplayName("aralık uzunluğu kodda sabit değil - bir haftalık sprint")
        void birHaftalik() {
            assertThat(DonemArtirici.artir("01.09.2026 - 07.09.2026"))
                    .contains("08.09.2026 - 14.09.2026");
        }

        @Test
        @DisplayName("ay ve yıl sınırını doğru geçer")
        void aySinirini() {
            assertThat(DonemArtirici.artir("15.12.2026 – 28.12.2026"))
                    .contains("29.12.2026 – 11.01.2027");
        }

        @Test
        @DisplayName("basamak genişliği korunur")
        void basamakGenisligi() {
            // Kaynak tek basamakli yazmissa sonuc da tek basamakli.
            assertThat(DonemArtirici.artir("1.9.2026 – 7.9.2026")).contains("8.9.2026 – 14.9.2026");
        }

        @Test
        @DisplayName("TEK tarih varsa tarihe dokunulmaz - gerçek veri")
        void tekTarih() {
            // "16.09.2026" tek basina bir toplanti gunu; bir sonrakinin ne
            // zaman oldugunu soylemiyor. Icindeki 16 ve 09 sprint numarasi
            // sanilirsa ay atlar - o yuzden sayi taramasi tarihlere girmez.
            assertThat(DonemArtirici.artir("16.09.2026 · Sprint Değerlendirme")).isEmpty();
        }

        @Test
        @DisplayName("tek tarih + sprint numarası: yalnız numara artar")
        void tekTarihVeNumara() {
            assertThat(DonemArtirici.artir("16.09.2026 · Sprint 42"))
                    .contains("16.09.2026 · Sprint 43");
        }

        @Test
        @DisplayName("ters sıralı aralık kaydırılmaz")
        void tersAralik() {
            assertThat(DonemArtirici.artir("14.09.2026 – 01.09.2026")).isEmpty();
        }

        @Test
        @DisplayName("geçersiz tarih kaydırılmaz")
        void gecersizTarih() {
            assertThat(DonemArtirici.artir("31.02.2026 – 14.09.2026")).isEmpty();
        }
    }

    @Nested
    @DisplayName("artıracak sayı yoksa dokunmaz")
    class Dokunmaz {

        @Test
        @DisplayName("hiç sayı yok")
        void sayiYok() {
            assertThat(DonemArtirici.artir("Ürün Yol Haritası Toplantısı")).isEmpty();
        }

        @Test
        @DisplayName("null ve boş")
        void bosDeger() {
            assertThat(DonemArtirici.artir(null)).isEmpty();
            assertThat(DonemArtirici.artir("")).isEmpty();
            assertThat(DonemArtirici.artir("   ")).isEmpty();
        }

        @Test
        @DisplayName("çok uzun sayı numara sayılmaz")
        void cokUzunSayi() {
            // Sicil, telefon ya da id yapistirilmis olabilir; artirmak anlamsiz.
            assertThat(DonemArtirici.artir("Toplantı 1234567890")).isEmpty();
        }
    }
}
