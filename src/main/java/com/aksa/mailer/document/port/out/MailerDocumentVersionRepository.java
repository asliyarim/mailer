package com.aksa.mailer.document.port.out;

import com.aksa.mailer.document.domain.MailContent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Versiyon gecmisi. Ayri bir port cunku farkli bir tabloyu (ve farkli bir
 * yasam dongusunu) temsil ediyor: belge silinse de gecmis silinme sirasi
 * veritabaninda ON DELETE CASCADE ile yonetiliyor.
 */
public interface MailerDocumentVersionRepository {

    void ekle(Long documentId, int version, MailContent content, String sicil);

    /** Yeniden eskiye sirali, content TASIMAZ. */
    List<VersionRow> gecmis(Long documentId);

    Optional<MailContent> icerik(Long documentId, int version);

    record VersionRow(int version, String createdBy, Instant createdAt) {
    }
}
