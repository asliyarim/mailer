package com.aksa.mailer.document.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data arayuzu. Disariya SIZMAZ - sadece adapter kullanir. */
interface MailerDocumentJpaRepository extends JpaRepository<MailerDocumentEntity, Long> {

    List<MailerDocumentEntity> findByTeamIdOrderByUpdatedAtDesc(Long teamId);

    List<MailerDocumentEntity> findByTeamIdInOrderByUpdatedAtDesc(List<Long> teamIds, Pageable sayfa);
}
