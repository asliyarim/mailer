package com.aksa.mailer.document.adapter.out.persistence;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.port.out.MailerDocumentRepository;
import com.aksa.mailer.document.port.out.MailerDocumentVersionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Iki port'un da JPA gerceklemesi. Entity <-> domain cevrimi TAM BURADA olur;
 * bu sinifin disina Entity cikmaz.
 */
@Component
class MailerDocumentPersistenceAdapter implements MailerDocumentRepository, MailerDocumentVersionRepository {

    private final MailerDocumentJpaRepository documentJpa;
    private final MailerDocumentVersionJpaRepository versionJpa;

    MailerDocumentPersistenceAdapter(MailerDocumentJpaRepository documentJpa,
                                     MailerDocumentVersionJpaRepository versionJpa) {
        this.documentJpa = documentJpa;
        this.versionJpa = versionJpa;
    }

    // --- MailerDocumentRepository ---

    @Override
    public List<MailerDocument> takimBelgeleri(Long teamId) {
        return documentJpa.findByTeamIdOrderByUpdatedAtDesc(teamId).stream()
                .map(MailerDocumentPersistenceAdapter::domaine)
                .toList();
    }

    @Override
    public List<MailerDocument> sonBelgeler(List<Long> teamIds, int limit) {
        // Bos liste ile IN sorgusu bazi veritabanlarinda sozdizimi hatasi
        // verir; hicbir takima erisimi olmayan kullaniciya bos liste doner.
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return documentJpa
                .findByTeamIdInOrderByUpdatedAtDesc(teamIds, PageRequest.of(0, limit))
                .stream()
                .map(MailerDocumentPersistenceAdapter::domaine)
                .toList();
    }

    @Override
    public Optional<MailerDocument> bul(Long id) {
        return documentJpa.findById(id).map(MailerDocumentPersistenceAdapter::domaine);
    }

    @Override
    public MailerDocument kaydet(MailerDocument document) {
        MailerDocumentEntity entity;
        if (document.id() == null) {
            entity = new MailerDocumentEntity(
                    null, document.teamId(), document.templateType(), document.title(), document.subject(),
                    document.content(), document.status(), document.currentVersion(),
                    document.createdBy(), document.updatedBy());
        } else {
            // Var olan satiri YUKLEYIP guncelliyoruz; yeni bir entity'yi
            // merge etseydik createdAt/createdBy gibi updatable=false alanlar
            // sessizce kaybolurdu.
            entity = documentJpa.findById(document.id())
                    .orElseThrow(() -> new IllegalStateException("Kaydedilecek belge yok: " + document.id()));
            entity.guncelle(document.title(), document.subject(), document.content(),
                    document.currentVersion(), document.updatedBy());
        }
        return domaine(documentJpa.save(entity));
    }

    // --- MailerDocumentVersionRepository ---

    @Override
    public void ekle(Long documentId, int version, MailContent content, String sicil) {
        versionJpa.save(new MailerDocumentVersionEntity(documentId, version, content, sicil));
    }

    @Override
    public List<VersionRow> gecmis(Long documentId) {
        return versionJpa.findByDocumentIdOrderByVersionDesc(documentId).stream()
                .map(v -> new VersionRow(v.getVersion(), v.getCreatedBy(), v.getCreatedAt()))
                .toList();
    }

    @Override
    public Optional<MailContent> icerik(Long documentId, int version) {
        return versionJpa.findByDocumentIdAndVersion(documentId, version)
                .map(MailerDocumentVersionEntity::getContent);
    }

    private static MailerDocument domaine(MailerDocumentEntity e) {
        return new MailerDocument(
                e.getId(), e.getTeamId(), e.getTemplateType(), e.getTitle(), e.getSubject(),
                e.getContent(), e.getStatus(), e.getCurrentVersion(),
                e.getCreatedBy(), e.getUpdatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
