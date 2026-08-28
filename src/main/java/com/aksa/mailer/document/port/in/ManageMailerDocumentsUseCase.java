package com.aksa.mailer.document.port.in;

import com.aksa.mailer.document.domain.DownloadFormat;
import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.domain.TemplateType;

import java.time.Instant;
import java.util.List;

/**
 * GIRIS portu. api/ katmani sadece bunu bilir; MailerDocumentService'i degil.
 */
public interface ManageMailerDocumentsUseCase {

    List<DocumentSummary> takimBelgeleri(Long teamId);

    MailerDocument getir(Long id);

    MailerDocument olustur(NewDocumentCommand komut);

    MailerDocument kaydet(SaveDocumentCommand komut);

    List<VersionSummary> versiyonlar(Long documentId);

    MailerDocument geriAl(Long documentId, int version, String sicil);

    /** Indirme olcumu. Basarisiz olmasi indirmeyi bozmamali (docs/api.md §10). */
    void indirmeKaydet(Long documentId, DownloadFormat format, String sicil);

    /** Liste ekrani icin hafif ozet - content TASIMAZ. */
    record DocumentSummary(
            Long id,
            Long teamId,
            TemplateType templateType,
            String title,
            String status,
            int currentVersion,
            String updatedBy,
            Instant updatedAt) {
    }

    /** Versiyon gecmisi satiri - content TASIMAZ, liste hafif kalsin. */
    record VersionSummary(int version, String createdBy, Instant createdAt) {
    }

    record NewDocumentCommand(Long teamId, TemplateType templateType, String title, String sicil) {
    }

    /**
     * expectedVersion iyimser kilit: sunucudaki currentVersion bundan
     * farkliysa VersionConflictException atilir (bkz. docs/api.md §5).
     */
    record SaveDocumentCommand(
            Long documentId,
            String title,
            String subject,
            MailContent content,
            int expectedVersion,
            String sicil) {
    }
}
