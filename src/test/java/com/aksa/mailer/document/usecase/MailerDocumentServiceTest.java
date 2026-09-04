package com.aksa.mailer.document.usecase;

import com.aksa.mailer.common.domain.NotFoundException;
import com.aksa.mailer.common.domain.VersionConflictException;
import com.aksa.mailer.document.domain.DonemArtirici;
import com.aksa.mailer.document.domain.DownloadFormat;
import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailContentValidator;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.document.domain.Tone;
import com.aksa.mailer.document.port.in.ManageMailerDocumentsUseCase;
import com.aksa.mailer.document.port.in.ManageMailerDocumentsUseCase.DocumentSummary;
import com.aksa.mailer.document.port.out.MailerDocumentRepository;
import com.aksa.mailer.document.port.out.MailerDocumentVersionRepository;
import com.aksa.mailer.document.port.out.MailerDownloadLogRepository;
import com.aksa.mailer.team.domain.MailTeam;
import com.aksa.mailer.team.port.in.GetTeamsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sahte port gerceklemeleriyle - veritabani, Spring context yok. Hexagonal
 * mimarinin asil faydasi bu: is mantigi altyapisiz test edilebiliyor.
 */
class MailerDocumentServiceTest {

    private static final String SICIL = "10234";
    private static final String TURKCE = "Şebeke Operasyonları · İĞÜÇÖşğıçö";

    private SahteDocumentRepo documentRepo;
    private SahteVersionRepo versionRepo;
    private SahteIndirmeRepo indirmeRepo;
    private MailerDocumentService service;

    @BeforeEach
    void kurulum() {
        documentRepo = new SahteDocumentRepo();
        versionRepo = new SahteVersionRepo();
        indirmeRepo = new SahteIndirmeRepo();
        service = new MailerDocumentService(documentRepo, versionRepo, indirmeRepo, new SahteTakimlar());
    }

    /**
     * 1 numarali takim "RPA Takımı", 5 numarali "Dijital Uygulamalar Takımı".
     * Baskasi sorulursa NotFoundException - gercek gerceklemenin sozlesmesi bu.
     */
    private static final class SahteTakimlar implements GetTeamsUseCase {
        @Override
        public List<MailTeam> erisilebilirTakimlar(boolean adminMi, List<Long> teamIds) {
            // Gercek gerceklemenin sozlesmesi: ISTENEN kimlikleri doner.
            // Girdiyi yok sayan bir sahte, takim adi cozumunu test etmiyor
            // gibi gosterirdi.
            return teamIds.stream()
                    .filter(id -> id == 1L || id == 5L)
                    .map(this::takim)
                    .toList();
        }

        @Override
        public MailTeam takim(Long id) {
            if (id == 1L) {
                return new MailTeam(1L, "RPA", "RPA Takımı", "rpa", true);
            }
            if (id == 5L) {
                return new MailTeam(5L, "DIJITAL", "Dijital Uygulamalar Takımı",
                        "dijital-uygulamalar", true);
            }
            throw new NotFoundException("Takım bulunamadı: " + id);
        }
    }

    private MailerDocument yeniKapanis() {
        return service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Ağustos 2026 Sprint Kapanışı", SICIL));
    }

    private static MailContent icerik(String baslik) {
        return new MailContent(
                1,
                new MailContent.Header(baslik, "Ağustos 2026", "RPA Takımı"),
                new MailContent.Meeting("03.09.2026", "10:00", "Toplantı Salonu"),
                List.of("giriş"),
                List.of(new MailSection("analysis", "ANALİZ ÇALIŞMALARI", Tone.BLUE,
                        List.of("sector", "process"),
                        List.of(Map.of("sector", "Elektrik", "process", TURKCE)))),
                List.of(),
                new MailContent.Footer("Teşekkür ederiz.", ""));
    }

    @Test
    @DisplayName("yeni taslak sürüm 1'de doğar ve tipin varsayılan bölümlerini taşır")
    void taslakVarsayilanlaDogar() {
        MailerDocument taslak = yeniKapanis();

        assertThat(taslak.currentVersion()).isEqualTo(1);
        assertThat(taslak.content().sections())
                .extracting(MailSection::key)
                .containsExactly("analysis", "development");
        // Olusturma da bir versiyon satiri yazar - gecmis 1'den baslar.
        assertThat(versionRepo.gecmis(taslak.id())).hasSize(1);
    }

    @Test
    @DisplayName("yeni taslak KENDI kuralini gecer - dogar dogmaz onizlenebilir")
    void taslakGecerliDogar() {
        // Regresyon: header bos dogdugu icin belge, olusturuldugu anda
        // MailContentValidator'a takiliyordu. Kullanici tek harf yazmadan
        // onizlemede "Başlık boş olamaz" hatasi goruyordu.
        assertThatCode(() -> MailContentValidator.dogrula(yeniKapanis().content()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("başlık ve takım etiketi takımın adından türetilir, sabit değildir")
    void baslikTakimdanTuretilir() {
        MailerDocument rpa = yeniKapanis();
        MailerDocument dijital = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                5L, TemplateType.KAPANIS, "Eylül planlaması", SICIL));

        // "Takımı" sozcugu baslikta atilir, etikette KALIR.
        assertThat(rpa.content().header().title()).isEqualTo("RPA SPRINT BİLGİLENDİRME");
        assertThat(rpa.content().header().teamLabel()).isEqualTo("RPA Takımı");

        // Sabit yazilsaydi bu iki baslik ayni olurdu - asil yakalamak
        // istedigim hata bu. Ayrica Turkce buyuk harf: "Dijital" -> "DİJİTAL",
        // varsayilan locale'de noktali I kaybolup "DIJITAL" olurdu.
        assertThat(dijital.content().header().title())
                .isEqualTo("DİJİTAL UYGULAMALAR SPRINT BİLGİLENDİRME");
    }

    @Test
    @DisplayName("varsayılan içerik, oluşturulan belgenin içeriğinin AYNISI")
    void varsayilanIcerikOlusturulanlaAyni() {
        // Arayuz acilista bu icerigi cizip kullanici "Kaydet" deyince belge
        // olusturuyor. Ikisi ayrisirsa kullanici ekranda bir sey gorup baska
        // bir sey kaydetmis olur - onizlemenin yalan soylemesi.
        assertThat(service.varsayilanIcerik(1L, TemplateType.KAPANIS))
                .isEqualTo(yeniKapanis().content());

        assertThat(service.varsayilanIcerik(5L, TemplateType.PLANLAMA))
                .isEqualTo(service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                        5L, TemplateType.PLANLAMA, "Planlama", SICIL)).content());
    }

    @Test
    @DisplayName("son belgeler birden çok takımı birleştirir, sınır sunucuda")
    void sonBelgelerBirdenCokTakim() {
        service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Bir", SICIL));
        service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                5L, TemplateType.PLANLAMA, "İki", SICIL));

        // Giris sayfasi tek istekle iki takimin belgesini de gormeli - bir
        // PO'nun birden cok takimi olabiliyor.
        assertThat(service.sonBelgeler(List.of(1L, 5L), 10, null)).hasSize(2);
        assertThat(service.sonBelgeler(List.of(1L), 10, null)).hasSize(1);

        // Istemci limit=100000 yollayip butun tabloyu cekemez; limit=0 da
        // sessizce bos liste dondurmez.
        assertThat(service.sonBelgeler(List.of(1L, 5L), 100_000, null)).hasSize(2);
        assertThat(service.sonBelgeler(List.of(1L, 5L), 0, null)).hasSize(1);
    }

    @Test
    @DisplayName("arama başlıkta VE dönemde arar")
    void aramaBaslikVeDonemdeArar() {
        MailerDocument belge = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Ağustos Kapanışı", SICIL));
        // Donemi doldur - sprint numarasi burada yasiyor.
        MailContent icerik = new MailContent(
                1,
                new MailContent.Header("Başlık", "Sprint 42 · Eylül 2026", "RPA Takımı"),
                belge.content().meeting(), belge.content().intro(),
                belge.content().sections(), belge.content().notes(), belge.content().footer());
        service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                belge.id(), "Ağustos Kapanışı", null, icerik, 1, SICIL));

        // Baslikla
        assertThat(service.sonBelgeler(List.of(1L), 10, "ağustos")).hasSize(1);
        // Sprint numarasiyla - donem ozette TASINMASAYDI bu imkansizdi
        assertThat(service.sonBelgeler(List.of(1L), 10, "Sprint 42")).hasSize(1);
        assertThat(service.sonBelgeler(List.of(1L), 10, "eylül")).hasSize(1);
        // Eslesmeyen
        assertThat(service.sonBelgeler(List.of(1L), 10, "mart")).isEmpty();
        // Bos arama suzmez
        assertThat(service.sonBelgeler(List.of(1L), 10, "   ")).hasSize(1);
    }

    @Test
    @DisplayName("takım listesi q ile süzülür - başlık ve dönem")
    void takimBelgeleriAramaylaSuzulur() {
        MailerDocument belge = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Ağustos Kapanışı", SICIL));
        MailContent icerik = new MailContent(
                1,
                new MailContent.Header("Başlık", "Sprint 42 · Eylül 2026", "RPA Takımı"),
                belge.content().meeting(), belge.content().intro(),
                belge.content().sections(), belge.content().notes(), belge.content().footer());
        service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                belge.id(), "Ağustos Kapanışı", null, icerik, 1, SICIL));
        service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.PLANLAMA, "Mart Planlaması", SICIL));

        // Bos/null suzmez - kutu temizlenince liste geri gelir.
        assertThat(service.takimBelgeleri(1L, null)).hasSize(2);
        assertThat(service.takimBelgeleri(1L, "   ")).hasSize(2);

        // Baslikla
        assertThat(service.takimBelgeleri(1L, "ağustos")).hasSize(1);
        assertThat(service.takimBelgeleri(1L, "mart")).hasSize(1);
        // Donemle - sonBelgeler ile AYNI davranis
        assertThat(service.takimBelgeleri(1L, "Sprint 42")).hasSize(1);
        // Turkce kucuk harf ve sapkali katlama da ayni
        assertThat(service.takimBelgeleri(1L, "AĞUSTOS")).hasSize(1);
        // Eslesmeyen
        assertThat(service.takimBelgeleri(1L, "kasım")).isEmpty();
    }

    @Test
    @DisplayName("takım listesinde TAKIM ADI aranmaz - hiçbir şeyi süzmeyen filtre olurdu")
    void takimBelgeleriTakimAdiniAramaz() {
        yeniKapanis();
        // Takim ZATEN secili: butun satirlar "RPA Takımı". Takim adi da
        // taransaydi bu arama listeyi oldugu gibi dondururdu.
        assertThat(service.takimBelgeleri(1L, "RPA")).isEmpty();
    }

    @Test
    @DisplayName("arama TAKIM ADIYLA da süzer - admin'in en doğal filtresi")
    void aramaTakimAdiylaSuzer() {
        service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Bir", SICIL));
        service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                5L, TemplateType.PLANLAMA, "İki", SICIL));

        // Regresyon: once yalnizca baslik ve donem taraniyordu. ADMIN butun
        // takimlarin belgelerini goruyor ve "dijital" yazip o takimin
        // belgelerini suzmek en dogal beklenti - hicbir sey bulunmuyordu.
        List<DocumentSummary> dijital = service.sonBelgeler(List.of(1L, 5L), 10, "dijital");
        assertThat(dijital).hasSize(1);
        assertThat(dijital.get(0).teamId()).isEqualTo(5L);

        assertThat(service.sonBelgeler(List.of(1L, 5L), 10, "rpa")).hasSize(1);

        // Ozet takim adini TASIMALI - arayuz admin listesinde takimi gostersin.
        assertThat(service.sonBelgeler(List.of(1L), 10, null))
                .allSatisfy(o -> assertThat(o.teamName()).isEqualTo("RPA Takımı"));
    }

    @Test
    @DisplayName("şapkalı sesli aranırken katlanır ama Türkçe harfler katlanmaz")
    void sapkaliSesliKatlanir() {
        MailerDocument belge = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "İş Zekâsı Raporu", SICIL));
        assertThat(belge.id()).isNotNull();

        // Kayitli ad sapkali (â), kullanici sapkasiz yaziyor - klavyede
        // sapkali a yok denecek kadar az kullaniliyor.
        assertThat(service.sonBelgeler(List.of(1L), 10, "zekası")).hasSize(1);
        assertThat(service.sonBelgeler(List.of(1L), 10, "zekâsı")).hasSize(1);

        // AMA ı/ş/ğ/ü/ö/ç KATLANMAZ - Turkce'de ayri harfler.
        // "is" yazan biri "İş"i bulmamali, baska kelime.
        assertThat(service.sonBelgeler(List.of(1L), 10, "is zekasi")).isEmpty();
    }

    @Test
    @DisplayName("arama Türkçe büyük/küçük harf kurallarını kullanır")
    void aramaTurkceHarfKurallari() {
        MailerDocument belge = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "İZMİR Sahası Sprinti", SICIL));
        assertThat(belge.id()).isNotNull();

        // Varsayilan locale'de "İZMİR".toLowerCase() -> "i̇zmir" (i + birlesik
        // nokta) olur ve kullanicinin yazdigi "izmir" ile ESLESMEZ.
        assertThat(service.sonBelgeler(List.of(1L), 10, "izmir")).hasSize(1);
        assertThat(service.sonBelgeler(List.of(1L), 10, "İZMİR")).hasSize(1);
    }

    @Test
    @DisplayName("hiçbir takıma erişimi olmayan boş liste alır, hata değil")
    void takimsizKullaniciBosListeAlir() {
        service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Bir", SICIL));

        assertThat(service.sonBelgeler(List.of(), 10, null)).isEmpty();
    }

    @Test
    @DisplayName("varsayılan içeriği sormak hiçbir şey yazmaz")
    void varsayilanIcerikYazmaz() {
        service.varsayilanIcerik(1L, TemplateType.KAPANIS);
        service.varsayilanIcerik(1L, TemplateType.PLANLAMA);

        // Her acilista belge dogsaydi terk edilmis "Yeni mail" taslaklari
        // birikir, Belgelerim listesi kullanilmaz hale gelirdi.
        assertThat(documentRepo.takimBelgeleri(1L)).isEmpty();
    }

    @Test
    @DisplayName("olmayan takımın varsayılan içeriği sorulamaz")
    void olmayanTakiminVarsayilaniYok() {
        assertThatThrownBy(() -> service.varsayilanIcerik(999L, TemplateType.KAPANIS))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("olmayan takımla belge oluşturulamaz - 404, yabancı anahtar hatası değil")
    void olmayanTakimaBelgeAcilmaz() {
        assertThatThrownBy(() -> service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                999L, TemplateType.KAPANIS, "Hayalet takım", SICIL)))
                .isInstanceOf(NotFoundException.class);

        assertThat(documentRepo.takimBelgeleri(999L)).isEmpty();
    }

    @Test
    @DisplayName("kaydetmek sürümü artırır ve yeni versiyon satırı yazar")
    void kaydetSurumArtirir() {
        MailerDocument taslak = yeniKapanis();

        MailerDocument kaydedilen = service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                taslak.id(), "Yeni başlık", "Konu", icerik("DİJİTAL UYGULAMALAR"), 1, SICIL));

        assertThat(kaydedilen.currentVersion()).isEqualTo(2);
        assertThat(versionRepo.gecmis(taslak.id())).hasSize(2);
    }

    // ── "Geçen sprintten devam et" ────────────────────────────────────

    @Test
    @DisplayName("kopya İÇERİĞİ taşır - kopyalamanın bütün değeri orada")
    void kopyaIcerigiTasir() {
        MailerDocument kaynak = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Sprint 24 Kapanışı", SICIL));
        service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                kaynak.id(), "Sprint 24 Kapanışı", "Konu", icerik("SPRINT BİLGİLENDİRME"), 1, SICIL));

        MailerDocument kopya = service.kopyala(kaynak.id(), "99999");

        // Satirlar, bolumler, notlar aynen tasinmali.
        assertThat(kopya.content().sections())
                .extracting(MailSection::key)
                .isEqualTo(service.getir(kaynak.id()).content().sections()
                        .stream().map(MailSection::key).toList());
        assertThat(kopya.content().sections().get(0).rows())
                .isEqualTo(service.getir(kaynak.id()).content().sections().get(0).rows());
    }

    @Test
    @DisplayName("kopyada dönem ve başlık bir sonraki sprinte taşınır")
    void kopyaDonemiArtirir() {
        MailerDocument kaynak = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Sprint 24 Kapanışı", SICIL));

        MailerDocument kopya = service.kopyala(kaynak.id(), SICIL);

        assertThat(kopya.title()).isEqualTo("Sprint 25 Kapanışı");
        // Kural DonemArtirici'de; burada yalnizca BAGLANDIGI dogrulaniyor.
        assertThat(kopya.content().header().period())
                .isEqualTo(DonemArtirici.artir(kaynak.content().header().period())
                        .orElse(kaynak.content().header().period()));
    }

    @Test
    @DisplayName("artırılacak sayı yoksa başlığa (kopya) eklenir - iki aynı isim yan yana durmasın")
    void kopyaAdiCakismaz() {
        MailerDocument kaynak = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.TOPLANTI_CIKTILARI, "Ürün Yol Haritası Toplantısı", SICIL));

        MailerDocument kopya = service.kopyala(kaynak.id(), SICIL);

        assertThat(kopya.title()).isEqualTo("Ürün Yol Haritası Toplantısı (kopya)");
    }

    @Test
    @DisplayName("kopya YENİ bir taslak - sürüm 1, kendi geçmişi, kopyalayanın adına")
    void kopyaYeniTaslaktir() {
        MailerDocument kaynak = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Sprint 24 Kapanışı", SICIL));
        service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                kaynak.id(), "Sprint 24 Kapanışı", "Konu", icerik("BAŞLIK"), 1, SICIL));

        MailerDocument kopya = service.kopyala(kaynak.id(), "99999");

        assertThat(kopya.id()).isNotEqualTo(kaynak.id());
        assertThat(kopya.currentVersion()).isEqualTo(1);
        assertThat(kopya.createdBy()).isEqualTo("99999");
        assertThat(versionRepo.gecmis(kopya.id())).hasSize(1);
        // Kaynagin gecmisi kopyaya TASINMAZ - kaynak iki surumde kalmali.
        assertThat(versionRepo.gecmis(kaynak.id())).hasSize(2);
    }

    @Test
    @DisplayName("kopyada konu satırı taşınmaz - yanlış konuyla gönderilmesin")
    void kopyaKonuyuTasimaz() {
        MailerDocument kaynak = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Sprint 24 Kapanışı", SICIL));
        service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                kaynak.id(), "Sprint 24 Kapanışı", "24. Sprint sonuçları", icerik("BAŞLIK"), 1, SICIL));

        assertThat(service.kopyala(kaynak.id(), SICIL).subject()).isNull();
    }

    @Test
    @DisplayName("kopya kaynağı DEĞİŞTİRMEZ")
    void kopyaKaynagaDokunmaz() {
        MailerDocument kaynak = service.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                1L, TemplateType.KAPANIS, "Sprint 24 Kapanışı", SICIL));

        service.kopyala(kaynak.id(), SICIL);

        MailerDocument sonrasi = service.getir(kaynak.id());
        assertThat(sonrasi.title()).isEqualTo("Sprint 24 Kapanışı");
        assertThat(sonrasi.currentVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("olmayan belge kopyalanamaz - 404")
    void olmayanBelgeKopyalanamaz() {
        assertThatThrownBy(() -> service.kopyala(9999L, SICIL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("beklenen sürüm tutmazsa 409 - ikinci sekme birincisini sessizce silmez")
    void surumCakismasi() {
        MailerDocument taslak = yeniKapanis();
        service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                taslak.id(), "İlk kayıt", null, icerik("Başlık"), 1, SICIL));

        // Ikinci sekme hala surum 1'i biliyor.
        assertThatThrownBy(() -> service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                taslak.id(), "İkinci kayıt", null, icerik("Başlık"), 1, SICIL)))
                .isInstanceOf(VersionConflictException.class)
                .hasMessageContaining("Sunucudaki sürüm: 2");
    }

    @Test
    @DisplayName("geçersiz içerik kaydedilmez - istemciye güvenilmez")
    void gecersizIcerikReddedilir() {
        MailerDocument taslak = yeniKapanis();
        MailContent bosBaslikli = icerik("   ");

        assertThatThrownBy(() -> service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                taslak.id(), "Başlık", null, bosBaslikli, 1, SICIL)))
                .hasMessageContaining("Başlık boş olamaz");

        // Reddedilen kayit versiyon YAZMAMALI.
        assertThat(versionRepo.gecmis(taslak.id())).hasSize(1);
    }

    @Test
    @DisplayName("geri alma geçmişi silmez, eski içeriği yeni sürüm olarak yazar")
    void geriAlmaYeniSurumYazar() {
        MailerDocument taslak = yeniKapanis();
        service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                taslak.id(), "v2", null, icerik("İKİNCİ SÜRÜM"), 1, SICIL));
        service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                taslak.id(), "v3", null, icerik("ÜÇÜNCÜ SÜRÜM"), 2, SICIL));

        MailerDocument geriAlinan = service.geriAl(taslak.id(), 2, SICIL);

        assertThat(geriAlinan.currentVersion()).isEqualTo(4);
        assertThat(geriAlinan.content().header().title()).isEqualTo("İKİNCİ SÜRÜM");
        assertThat(versionRepo.gecmis(taslak.id())).hasSize(4);
    }

    @Test
    @DisplayName("olmayan sürüme geri alma 404")
    void olmayanSurumeGeriAlma() {
        MailerDocument taslak = yeniKapanis();

        assertThatThrownBy(() -> service.geriAl(taslak.id(), 99, SICIL))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99. sürüm bulunamadı");
    }

    @Test
    @DisplayName("olmayan belge 404")
    void olmayanBelge() {
        assertThatThrownBy(() -> service.getir(4242L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Belge bulunamadı: 4242");
    }

    @Test
    @DisplayName("Türkçe karakterler içerikte bozulmadan kalır")
    void turkceKarakterlerKorunur() {
        MailerDocument taslak = yeniKapanis();
        MailerDocument kaydedilen = service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                taslak.id(), "Başlık", null, icerik("DİJİTAL"), 1, SICIL));

        String saklananSurec = kaydedilen.content().sections().get(0).rows().get(0).get("process");
        assertThat(saklananSurec).isEqualTo(TURKCE);
    }

    @Test
    @DisplayName("indirme kaydı belgenin takımıyla yazılır")
    void indirmeKaydi() {
        MailerDocument taslak = yeniKapanis();

        service.indirmeKaydet(taslak.id(), DownloadFormat.EML, SICIL);

        assertThat(indirmeRepo.kayitlar).hasSize(1);
        SahteIndirmeRepo.Kayit k = indirmeRepo.kayitlar.get(0);
        // teamId istemciden DEGIL belgeden gelir - olcum carpitilamasin.
        assertThat(k.teamId()).isEqualTo(1L);
        assertThat(k.format()).isEqualTo(DownloadFormat.EML);
        assertThat(k.sicil()).isEqualTo(SICIL);
    }

    @Test
    @DisplayName("olmayan belgenin indirmesi kaydedilmez")
    void olmayanBelgeIndirmesi() {
        assertThatThrownBy(() -> service.indirmeKaydet(4242L, DownloadFormat.EML, SICIL))
                .isInstanceOf(NotFoundException.class);

        assertThat(indirmeRepo.kayitlar).isEmpty();
    }

    // --- sahte portlar -----------------------------------------------------

    private static class SahteIndirmeRepo implements MailerDownloadLogRepository {
        record Kayit(Long documentId, Long teamId, DownloadFormat format, String sicil) {
        }

        private final List<Kayit> kayitlar = new ArrayList<>();

        @Override
        public void ekle(Long documentId, Long teamId, DownloadFormat format, String sicil) {
            kayitlar.add(new Kayit(documentId, teamId, format, sicil));
        }
    }

    private static class SahteDocumentRepo implements MailerDocumentRepository {
        private final Map<Long, MailerDocument> kayitlar = new HashMap<>();
        private final AtomicLong sonrakiId = new AtomicLong(1);

        @Override
        public List<MailerDocument> takimBelgeleri(Long teamId) {
            return kayitlar.values().stream().filter(d -> d.teamId().equals(teamId)).toList();
        }

        @Override
        public List<MailerDocument> sonBelgeler(List<Long> teamIds, int limit) {
            // Gercek gerceklemenin sozlesmesi: en yeni once, en fazla limit.
            return kayitlar.values().stream()
                    .filter(d -> teamIds.contains(d.teamId()))
                    .sorted(java.util.Comparator.comparing(
                            MailerDocument::id, java.util.Comparator.reverseOrder()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public Optional<MailerDocument> bul(Long id) {
            return Optional.ofNullable(kayitlar.get(id));
        }

        @Override
        public MailerDocument kaydet(MailerDocument document) {
            Long id = document.id() != null ? document.id() : sonrakiId.getAndIncrement();
            MailerDocument saklanan = new MailerDocument(
                    id, document.teamId(), document.templateType(), document.title(), document.subject(),
                    document.content(), document.status(), document.currentVersion(),
                    document.createdBy(), document.updatedBy(), Instant.now(), Instant.now());
            kayitlar.put(id, saklanan);
            return saklanan;
        }
    }

    private static class SahteVersionRepo implements MailerDocumentVersionRepository {
        private final Map<Long, List<VersionRow>> satirlar = new HashMap<>();
        private final Map<String, MailContent> icerikler = new HashMap<>();

        @Override
        public void ekle(Long documentId, int version, MailContent content, String sicil) {
            satirlar.computeIfAbsent(documentId, k -> new ArrayList<>())
                    .add(new VersionRow(version, sicil, Instant.now()));
            icerikler.put(documentId + ":" + version, content);
        }

        @Override
        public List<VersionRow> gecmis(Long documentId) {
            return satirlar.getOrDefault(documentId, List.of()).stream()
                    .sorted((a, b) -> Integer.compare(b.version(), a.version()))
                    .toList();
        }

        @Override
        public Optional<MailContent> icerik(Long documentId, int version) {
            return Optional.ofNullable(icerikler.get(documentId + ":" + version));
        }
    }
}
