package com.aksa.mailer.document.port.out;

import com.aksa.mailer.document.domain.MailerDocument;

import java.util.List;
import java.util.Optional;

/** CIKIS portu. Gerceklemesi adapter/out/persistence altinda. */
public interface MailerDocumentRepository {

    /** En son guncellenen basta. */
    List<MailerDocument> takimBelgeleri(Long teamId);

    Optional<MailerDocument> bul(Long id);

    MailerDocument kaydet(MailerDocument document);
}
