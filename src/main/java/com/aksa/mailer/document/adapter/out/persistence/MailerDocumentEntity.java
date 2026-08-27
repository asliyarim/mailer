package com.aksa.mailer.document.adapter.out.persistence;

import com.aksa.mailer.document.domain.DocumentStatus;
import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.TemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "mailer_documents")
public class MailerDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false)
    private TemplateType templateType;

    @Column(nullable = false)
    private String title;

    private String subject;

    /**
     * jsonb eslemesi. columnDefinition YAZILMAZ - Hibernate dialect'e gore
     * jsonb secer (bkz. docs/BRIEF.md, veri modeli notu). Serilestirme
     * Jackson ile yapilir; MailContent bir record agaci.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private MailContent content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MailerDocumentEntity() {
        // JPA icin
    }

    MailerDocumentEntity(Long id, Long teamId, TemplateType templateType, String title, String subject,
                         MailContent content, DocumentStatus status, int currentVersion,
                         String createdBy, String updatedBy) {
        this.id = id;
        this.teamId = teamId;
        this.templateType = templateType;
        this.title = title;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.currentVersion = currentVersion;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    @PrePersist
    void olusturulurken() {
        Instant simdi = Instant.now();
        createdAt = simdi;
        updatedAt = simdi;
    }

    @PreUpdate
    void guncellenirken() {
        updatedAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    Long getTeamId() {
        return teamId;
    }

    TemplateType getTemplateType() {
        return templateType;
    }

    String getTitle() {
        return title;
    }

    String getSubject() {
        return subject;
    }

    MailContent getContent() {
        return content;
    }

    DocumentStatus getStatus() {
        return status;
    }

    int getCurrentVersion() {
        return currentVersion;
    }

    String getCreatedBy() {
        return createdBy;
    }

    String getUpdatedBy() {
        return updatedBy;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void guncelle(String title, String subject, MailContent content, int currentVersion, String updatedBy) {
        this.title = title;
        this.subject = subject;
        this.content = content;
        this.currentVersion = currentVersion;
        this.updatedBy = updatedBy;
    }
}
