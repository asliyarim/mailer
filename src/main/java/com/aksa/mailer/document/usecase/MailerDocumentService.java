package com.aksa.mailer.document.usecase;

import com.aksa.mailer.common.domain.NotFoundException;
import com.aksa.mailer.common.domain.VersionConflictException;
import com.aksa.mailer.document.domain.DownloadFormat;
import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailContentValidator;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.port.in.ManageMailerDocumentsUseCase;
import com.aksa.mailer.document.port.out.MailerDocumentRepository;
import com.aksa.mailer.document.port.out.MailerDocumentVersionRepository;
import com.aksa.mailer.document.port.out.MailerDownloadLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Belge is mantigi. JPA veya HTTP sinifi import ETMEZ.
 *
 * Kaydetme ve geri alma iki tabloya birden yazdigi icin @Transactional:
 * belge guncellenip versiyon satiri yazilamazsa gecmis eksik kalirdi.
 */
@Service
public class MailerDocumentService implements ManageMailerDocumentsUseCase {

    private final MailerDocumentRepository documentRepository;
    private final MailerDocumentVersionRepository versionRepository;
    private final MailerDownloadLogRepository downloadLogRepository;

    public MailerDocumentService(MailerDocumentRepository documentRepository,
                                 MailerDocumentVersionRepository versionRepository,
                                 MailerDownloadLogRepository downloadLogRepository) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.downloadLogRepository = downloadLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummary> takimBelgeleri(Long teamId) {
        return documentRepository.takimBelgeleri(teamId).stream()
                .map(d -> new DocumentSummary(
                        d.id(), d.teamId(), d.templateType(), d.title(),
                        d.status().name(), d.currentVersion(), d.updatedBy(), d.updatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MailerDocument getir(Long id) {
        return documentRepository.bul(id)
                .orElseThrow(() -> new NotFoundException("Belge bulunamadı: " + id));
    }

    @Override
    @Transactional
    public MailerDocument olustur(NewDocumentCommand komut) {
        // Taslak, tipin varsayilan icerigiyle DOGAR - istemci bos content
        // gondermez. Tek dogru kaynak sunucu (bkz. docs/api.md §3).
        MailContent varsayilan = VarsayilanIcerik.uret(komut.templateType());

        MailerDocument taslak = MailerDocument.yeniTaslak(
                komut.teamId(), komut.templateType(), komut.title(), varsayilan, komut.sicil());

        MailerDocument kaydedilen = documentRepository.kaydet(taslak);
        versionRepository.ekle(kaydedilen.id(), 1, varsayilan, komut.sicil());
        return kaydedilen;
    }

    @Override
    @Transactional
    public MailerDocument kaydet(SaveDocumentCommand komut) {
        MailerDocument mevcut = getir(komut.documentId());

        if (mevcut.currentVersion() != komut.expectedVersion()) {
            throw new VersionConflictException(
                    "Belge siz düzenlerken değişti. Sunucudaki sürüm: " + mevcut.currentVersion()
                            + ", gönderilen: " + komut.expectedVersion() + ".");
        }

        // Istemciye GUVENILMEZ - nihai dogrulama burada.
        MailContentValidator.dogrula(komut.content());

        MailerDocument guncel = mevcut.guncellenmis(
                komut.title(), komut.subject(), komut.content(), komut.sicil());

        MailerDocument kaydedilen = documentRepository.kaydet(guncel);
        versionRepository.ekle(
                kaydedilen.id(), kaydedilen.currentVersion(), komut.content(), komut.sicil());
        return kaydedilen;
    }

    @Override
    @Transactional
    public void indirmeKaydet(Long documentId, DownloadFormat format, String sicil) {
        // teamId'yi belgeden aliyoruz - istemcinin gonderdigine guvenmiyoruz.
        MailerDocument belge = getir(documentId);
        downloadLogRepository.ekle(belge.id(), belge.teamId(), format, sicil);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionSummary> versiyonlar(Long documentId) {
        getir(documentId); // yoksa 404
        return versionRepository.gecmis(documentId).stream()
                .map(v -> new VersionSummary(v.version(), v.createdBy(), v.createdAt()))
                .toList();
    }

    @Override
    @Transactional
    public MailerDocument geriAl(Long documentId, int version, String sicil) {
        MailerDocument mevcut = getir(documentId);

        MailContent eskiIcerik = versionRepository.icerik(documentId, version)
                .orElseThrow(() -> new NotFoundException(
                        "Belge " + documentId + " için " + version + ". sürüm bulunamadı."));

        // Gecmis SILINMEZ: eski icerik YENI bir surum olarak yazilir, boylece
        // geri alma da geri alinabilir (bkz. docs/api.md §7).
        MailerDocument geriAlinmis = mevcut.guncellenmis(
                mevcut.title(), mevcut.subject(), eskiIcerik, sicil);

        MailerDocument kaydedilen = documentRepository.kaydet(geriAlinmis);
        versionRepository.ekle(
                kaydedilen.id(), kaydedilen.currentVersion(), eskiIcerik, sicil);
        return kaydedilen;
    }
}
