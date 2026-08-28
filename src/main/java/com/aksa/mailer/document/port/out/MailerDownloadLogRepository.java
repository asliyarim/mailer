package com.aksa.mailer.document.port.out;

import com.aksa.mailer.document.domain.DownloadFormat;

/**
 * Indirme olcumu. Ayri bir port cunku farkli bir tabloyu temsil ediyor ve
 * yasam dongusu belgeden bagimsiz (belge silinirse ON DELETE CASCADE).
 */
public interface MailerDownloadLogRepository {

    void ekle(Long documentId, Long teamId, DownloadFormat format, String sicil);
}
