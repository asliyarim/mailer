package com.aksa.mailer.document.adapter.out.persistence;

import com.aksa.mailer.document.domain.DownloadFormat;
import com.aksa.mailer.document.port.out.MailerDownloadLogRepository;
import org.springframework.stereotype.Component;

/** port/out'un JPA gerceklemesi. Entity bu sinifin disina cikmaz. */
@Component
class MailerDownloadLogPersistenceAdapter implements MailerDownloadLogRepository {

    private final MailerDownloadLogJpaRepository jpaRepository;

    MailerDownloadLogPersistenceAdapter(MailerDownloadLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void ekle(Long documentId, Long teamId, DownloadFormat format, String sicil) {
        jpaRepository.save(new MailerDownloadLogEntity(documentId, teamId, format, sicil));
    }
}
