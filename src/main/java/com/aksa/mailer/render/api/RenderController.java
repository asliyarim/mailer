package com.aksa.mailer.render.api;

import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.port.in.ManageMailerDocumentsUseCase;
import com.aksa.mailer.render.api.dto.PreviewRequest;
import com.aksa.mailer.render.usecase.EmlBuilder;
import com.aksa.mailer.render.usecase.MailHtmlRenderer;
import com.aksa.mailer.team.port.in.GetTeamsUseCase;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * docs/api.md §8-§9.
 *
 * Iki uc da AYNI HTML'i kullanir: onizleme onu text/html olarak doner, .eml
 * onu multipart govdesine gomer. Ayrisma imkani yok - tek uretim noktasi
 * MailHtmlRenderer (Mimari Kural 1).
 */
@RestController
@RequestMapping("/api/mailer")
public class RenderController {

    private final MailHtmlRenderer renderer;
    private final EmlBuilder emlBuilder;
    private final ManageMailerDocumentsUseCase documents;
    private final GetTeamsUseCase teams;

    public RenderController(MailHtmlRenderer renderer, EmlBuilder emlBuilder,
                            ManageMailerDocumentsUseCase documents, GetTeamsUseCase teams) {
        this.renderer = renderer;
        this.emlBuilder = emlBuilder;
        this.documents = documents;
        this.teams = teams;
    }

    /** Govdedeki JSON'u HTML'e cevirir. Istemci bunu oldugu gibi iframe'e basar. */
    @PostMapping(value = "/render/preview", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public String onizleme(@Valid @RequestBody PreviewRequest istek) {
        String themeKey = teams.takim(istek.teamId()).themeKey();
        return renderer.uret(istek.content(), istek.templateType(), themeKey);
    }

    /** "Outlook Maili İndir" - message/rfc822, gorseller cid: ile gomulu. */
    @GetMapping("/documents/{id}/export.eml")
    public ResponseEntity<Resource> emlIndir(@PathVariable Long id) {
        MailerDocument belge = documents.getir(id);
        String themeKey = teams.takim(belge.teamId()).themeKey();

        String html = renderer.uret(belge.content(), belge.templateType(), themeKey);
        String konu = belge.subject() != null && !belge.subject().isBlank()
                ? belge.subject()
                : belge.title();
        byte[] eml = emlBuilder.uret(html, konu, renderer.tema(themeKey));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("message/rfc822"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(dosyaAdi(belge.title()), StandardCharsets.UTF_8)
                        .build().toString())
                .contentLength(eml.length)
                .body(new ByteArrayResource(eml));
    }

    /**
     * Turkce karakterleri ve bosluklari dosya adindan temizler.
     * Bazi istemciler UTF-8 dosya adini yanlis cozup adi bozuyor; ASCII'ye
     * indirgemek indirmeyi her yerde ongorulebilir kiliyor.
     */
    private String dosyaAdi(String baslik) {
        String temiz = Normalizer.normalize(baslik == null ? "mail" : baslik, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("ı", "i").replace("İ", "I")
                .replace("ş", "s").replace("Ş", "S")
                .replace("ğ", "g").replace("Ğ", "G")
                .replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return (temiz.isBlank() ? "sprint-maili" : temiz) + ".eml";
    }
}
