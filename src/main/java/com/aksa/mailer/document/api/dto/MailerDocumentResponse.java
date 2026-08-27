package com.aksa.mailer.document.api.dto;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.domain.TemplateType;

import java.time.Instant;

/**
 * Tam belge yaniti (docs/api.md §4).
 *
 * themeKey kolaylik olsun diye burada da doner - istemci ayrica /teams
 * cagirmasin. Domain nesnesinde yok cunku takim modulune ait bir bilgi;
 * controller birlestiriyor.
 */
public record MailerDocumentResponse(
        Long id,
        Long teamId,
        String themeKey,
        TemplateType templateType,
        String title,
        String subject,
        String status,
        int currentVersion,
        MailContent content,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {

    public static MailerDocumentResponse of(MailerDocument d, String themeKey) {
        return new MailerDocumentResponse(
                d.id(), d.teamId(), themeKey, d.templateType(), d.title(), d.subject(),
                d.status().name(), d.currentVersion(), d.content(),
                d.createdBy(), d.updatedBy(), d.createdAt(), d.updatedAt());
    }
}
