package com.aksa.mailer.document.api;

import com.aksa.mailer.auth.security.OturumKullanicisi;
import com.aksa.mailer.document.api.dto.CreateDocumentRequest;
import com.aksa.mailer.document.api.dto.DownloadLogRequest;
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
    public List<ManageMailerDocumentsUseCase.DocumentSummary> listele(@RequestParam Long teamId,
                                                                      Authentication authentication) {
        OturumKullanicisi.of(authentication).dogrula(teamId);
        return documents.takimBelgeleri(teamId);
    }

    @GetMapping("/{id}")
    public MailerDocumentResponse getir(@PathVariable Long id, Authentication authentication) {
        return yanit(erisilebilirBelge(id, authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MailerDocumentResponse olustur(@Valid @RequestBody CreateDocumentRequest istek,
                                          Authentication authentication) {
        OturumKullanicisi.of(authentication).dogrula(istek.teamId());
        MailerDocument olusan = documents.olustur(new ManageMailerDocumentsUseCase.NewDocumentCommand(
                istek.teamId(), istek.templateType(), istek.title(), authentication.getName()));
        return yanit(olusan);
    }

    @PutMapping("/{id}")
    public MailerDocumentResponse kaydet(@PathVariable Long id,
                                         @Valid @RequestBody SaveDocumentRequest istek,
                                         Authentication authentication) {
        erisilebilirBelge(id, authentication);
        MailerDocument kaydedilen = documents.kaydet(new ManageMailerDocumentsUseCase.SaveDocumentCommand(
                id, istek.title(), istek.subject(), istek.content(),
                istek.expectedVersion(), authentication.getName()));
        return yanit(kaydedilen);
    }

    /** Indirme olcumu. Govde yok, 204 doner (docs/api.md §10). */
    @PostMapping("/{id}/downloads")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void indirmeKaydet(@PathVariable Long id,
                              @Valid @RequestBody DownloadLogRequest istek,
                              Authentication authentication) {
        erisilebilirBelge(id, authentication);
        documents.indirmeKaydet(id, istek.format(), authentication.getName());
    }

    @GetMapping("/{id}/versions")
    public List<ManageMailerDocumentsUseCase.VersionSummary> versiyonlar(@PathVariable Long id,
                                                                         Authentication authentication) {
        erisilebilirBelge(id, authentication);
        return documents.versiyonlar(id);
    }

    @PostMapping("/{id}/versions/{version}/rollback")
    public MailerDocumentResponse geriAl(@PathVariable Long id,
                                         @PathVariable int version,
                                         Authentication authentication) {
        erisilebilirBelge(id, authentication);
        return yanit(documents.geriAl(id, version, authentication.getName()));
    }

    /**
     * Belgeyi getirir ve kullanicinin o takima yetkisi oldugunu dogrular.
     * Her belge ucunun basinda cagrilir - yetki kontrolu tek bir yerde
     * dursun ve yeni bir uc eklenirken unutulmasi zorlassin.
     */
    private MailerDocument erisilebilirBelge(Long id, Authentication authentication) {
        MailerDocument belge = documents.getir(id);
        OturumKullanicisi.of(authentication).dogrula(belge.teamId());
        return belge;
    }

    private MailerDocumentResponse yanit(MailerDocument document) {
        String themeKey = teams.takim(document.teamId()).themeKey();
        return MailerDocumentResponse.of(document, themeKey);
    }
}
