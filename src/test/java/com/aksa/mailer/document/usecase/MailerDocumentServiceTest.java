package com.aksa.mailer.document.usecase;

import com.aksa.mailer.common.domain.NotFoundException;
import com.aksa.mailer.common.domain.VersionConflictException;
import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailSection;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.document.domain.Tone;
import com.aksa.mailer.document.port.in.ManageMailerDocumentsUseCase;
import com.aksa.mailer.document.port.out.MailerDocumentRepository;
import com.aksa.mailer.document.port.out.MailerDocumentVersionRepository;
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
    private MailerDocumentService service;

    @BeforeEach
    void kurulum() {
        documentRepo = new SahteDocumentRepo();
        versionRepo = new SahteVersionRepo();
        service = new MailerDocumentService(documentRepo, versionRepo);
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
    @DisplayName("kaydetmek sürümü artırır ve yeni versiyon satırı yazar")
    void kaydetSurumArtirir() {
        MailerDocument taslak = yeniKapanis();

        MailerDocument kaydedilen = service.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                taslak.id(), "Yeni başlık", "Konu", icerik("DİJİTAL UYGULAMALAR"), 1, SICIL));

        assertThat(kaydedilen.currentVersion()).isEqualTo(2);
        assertThat(versionRepo.gecmis(taslak.id())).hasSize(2);
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

    // --- sahte portlar -----------------------------------------------------

    private static class SahteDocumentRepo implements MailerDocumentRepository {
        private final Map<Long, MailerDocument> kayitlar = new HashMap<>();
        private final AtomicLong sonrakiId = new AtomicLong(1);

        @Override
        public List<MailerDocument> takimBelgeleri(Long teamId) {
            return kayitlar.values().stream().filter(d -> d.teamId().equals(teamId)).toList();
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
