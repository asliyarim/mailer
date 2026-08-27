package com.aksa.mailer.document.domain;

import java.time.Instant;

/**
 * Bir mail belgesi. Saf is nesnesi - JPA, HTTP, Spring yok.
 *
 * currentVersion iyimser kilit olarak da kullanilir: kaydederken istemci
 * beklediği surumu gonderir, tutmazsa 409 doner (bkz. docs/api.md §5).
 */
public record MailerDocument(
        Long id,
        Long teamId,
        TemplateType templateType,
        String title,
        String subject,
        MailContent content,
        DocumentStatus status,
        int currentVersion,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {

    /** Yeni taslak: surum 1, DRAFT, bos icerik disaridan verilir. */
    public static MailerDocument yeniTaslak(Long teamId, TemplateType templateType, String title,
                                            MailContent content, String sicil) {
        return new MailerDocument(
                null, teamId, templateType, title, null, content,
                DocumentStatus.DRAFT, 1, sicil, sicil, null, null);
    }

    /**
     * Kaydedilmis yeni hali. Surumu BIR ARTIRIR - her kayit yeni bir versiyon
     * satiri yazacagi icin (bkz. docs/api.md §5).
     */
    public MailerDocument guncellenmis(String yeniTitle, String yeniSubject, MailContent yeniContent, String sicil) {
        return new MailerDocument(
                id, teamId, templateType, yeniTitle, yeniSubject, yeniContent,
                status, currentVersion + 1, createdBy, sicil, createdAt, updatedAt);
    }
}
