package com.aksa.mailer.document.adapter.out.persistence;

import com.aksa.mailer.document.domain.MailContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "mailer_document_versions")
public class MailerDocumentVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private MailContent content;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MailerDocumentVersionEntity() {
        // JPA icin
    }

    MailerDocumentVersionEntity(Long documentId, int version, MailContent content, String createdBy) {
        this.documentId = documentId;
        this.version = version;
        this.content = content;
        this.createdBy = createdBy;
    }

    @PrePersist
    void olusturulurken() {
        createdAt = Instant.now();
    }

    int getVersion() {
        return version;
    }

    MailContent getContent() {
        return content;
    }

    String getCreatedBy() {
        return createdBy;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
