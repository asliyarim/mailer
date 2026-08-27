package com.aksa.mailer.document.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface MailerDocumentVersionJpaRepository extends JpaRepository<MailerDocumentVersionEntity, Long> {

    List<MailerDocumentVersionEntity> findByDocumentIdOrderByVersionDesc(Long documentId);

    Optional<MailerDocumentVersionEntity> findByDocumentIdAndVersion(Long documentId, int version);
}
