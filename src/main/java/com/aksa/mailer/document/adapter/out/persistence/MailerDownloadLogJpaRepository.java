package com.aksa.mailer.document.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data arayuzu. Disariya SIZMAZ - sadece adapter kullanir. */
interface MailerDownloadLogJpaRepository extends JpaRepository<MailerDownloadLogEntity, Long> {
}
