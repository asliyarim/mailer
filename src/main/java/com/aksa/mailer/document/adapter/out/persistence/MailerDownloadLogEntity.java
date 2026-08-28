package com.aksa.mailer.document.adapter.out.persistence;

import com.aksa.mailer.document.domain.DownloadFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "mailer_download_logs")
public class MailerDownloadLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DownloadFormat format;

    /** Sicil. Sutun V2'de user_email'den yeniden adlandirildi. */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MailerDownloadLogEntity() {
        // JPA icin
    }

    MailerDownloadLogEntity(Long documentId, Long teamId, DownloadFormat format, String createdBy) {
        this.documentId = documentId;
        this.teamId = teamId;
        this.format = format;
        this.createdBy = createdBy;
    }

    @PrePersist
    void olusturulurken() {
        createdAt = Instant.now();
    }
}
