package com.aksa.mailer.document.port.out;

import com.aksa.mailer.document.domain.MailerDocument;

import java.util.List;
import java.util.Optional;

/** CIKIS portu. Gerceklemesi adapter/out/persistence altinda. */
public interface MailerDocumentRepository {

    /** En son guncellenen basta. */
    List<MailerDocument> takimBelgeleri(Long teamId);

    /** Birden cok takimin belgeleri, en son guncellenen basta, en fazla limit tane. */
    List<MailerDocument> sonBelgeler(List<Long> teamIds, int limit);

    Optional<MailerDocument> bul(Long id);

    MailerDocument kaydet(MailerDocument document);

    /**
     * Belgeyi KALICI olarak siler.
     *
     * Surum gecmisi ve indirme kayitlari veritabaninda ON DELETE CASCADE ile
     * bagli; ayrica silmeye gerek yok, ama silinecekleri de bilinerek kabul
     * ediliyor - belge gidince gecmisi de gider.
     */
    void sil(Long id);
}
