package com.aksa.mailer.document.api;

import com.aksa.mailer.document.api.dto.CreateDocumentRequest;
import com.aksa.mailer.document.api.dto.MailerDocumentResponse;
import com.aksa.mailer.document.api.dto.SaveDocumentRequest;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.port.in.ManageMailerDocumentsUseCase;
import com.aksa.mailer.team.port.in.GetTeamsUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * docs/api.md §2-§7. Bu sinif is mantigi ICERMEZ - istegi komuta cevirir,
 * use case'i cagirir, yaniti DTO'ya sarar. Hata cevrimi
 * GlobalExceptionHandler'da, o yuzden try/catch yok.
 */
@RestController
@RequestMapping("/api/mailer/documents")
public class MailerDocumentController {

    private final ManageMailerDocumentsUseCase documents;
    private final GetTeamsUseCase teams;

    public MailerDocumentController(ManageMailerDocumentsUseCase documents, GetTeamsUseCase teams) {
        this.documents = documents;
        this.teams = teams;
    }

    @GetMapping
    public List<ManageMailerDocumentsUseCase.DocumentSummary> listele(@RequestParam Long teamId) {
        return documents.takimBelgeleri(teamId);
    }

    @GetMapping("/{id}")
    public MailerDocumentResponse getir(@PathVariable Long id) {
        return yanit(documents.getir(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MailerDocumentResponse olustur(@Valid @RequestBody CreateDocumentRequest istek,
                                          Authentication authentication) {
        MailerDocument olusan = documents.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                istek.teamId(), istek.templateType(), istek.title(), authentication.getName()));
        return yanit(olusan);
    }

    @PutMapping("/{id}")
    public MailerDocumentResponse kaydet(@PathVariable Long id,
                                         @Valid @RequestBody SaveDocumentRequest istek,
                                         Authentication authentication) {
        MailerDocument kaydedilen = documents.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                id, istek.title(), istek.subject(), istek.content(),
                istek.expectedVersion(), authentication.getName()));
        return yanit(kaydedilen);
    }

    @GetMapping("/{id}/versions")
    public List<ManageMailerDocumentsUseCase.VersionSummary> versiyonlar(@PathVariable Long id) {
        return documents.versiyonlar(id);
    }

    @PostMapping("/{id}/versions/{version}/rollback")
    public MailerDocumentResponse geriAl(@PathVariable Long id,
                                         @PathVariable int version,
                                         Authentication authentication) {
        return yanit(documents.geriAl(id, version, authentication.getName()));
    }

    private MailerDocumentResponse yanit(MailerDocument document) {
        String themeKey = teams.takim(document.teamId()).themeKey();
        return MailerDocumentResponse.of(document, themeKey);
    }
}
