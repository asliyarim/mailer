package com.aksa.mailer.render.api;

import com.aksa.mailer.auth.security.OturumKullanicisi;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.port.in.ManageMailerDocumentsUseCase;
import com.aksa.mailer.render.api.dto.PreviewRequest;
import com.aksa.mailer.render.usecase.EmlBuilder;
import com.aksa.mailer.render.usecase.MailHtmlRenderer;
import com.aksa.mailer.render.usecase.DuzenlemeAdresleri;
import com.aksa.mailer.render.usecase.OnizlemeGorselleri;
import com.aksa.mailer.team.port.in.GetTeamsUseCase;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final OnizlemeGorselleri onizlemeGorselleri;
    private final DuzenlemeAdresleri duzenlemeAdresleri;
    private final ManageMailerDocumentsUseCase documents;
    private final GetTeamsUseCase teams;

    public RenderController(MailHtmlRenderer renderer, EmlBuilder emlBuilder,
                            OnizlemeGorselleri onizlemeGorselleri,
                            DuzenlemeAdresleri duzenlemeAdresleri,
                            ManageMailerDocumentsUseCase documents, GetTeamsUseCase teams) {
        this.renderer = renderer;
        this.emlBuilder = emlBuilder;
        this.onizlemeGorselleri = onizlemeGorselleri;
        this.duzenlemeAdresleri = duzenlemeAdresleri;
        this.documents = documents;
        this.teams = teams;
    }

    /**
     * Govdedeki JSON'u HTML'e cevirir. Istemci bunu oldugu gibi iframe'e basar.
     *
     * Uretilen HTML .eml'e giden HTML'in AYNISIDIR; IKI fark var ve ikisi de
     * tek noktada, adlandirilmis ve duman testleriyle cift yonlu kilitli:
     *
     *   1. Gorsel referanslari - tarayici cid: adresini cozemedigi icin ayni
     *      dosyalarin base64 hali gomulur (OnizlemeGorselleri).
     *   2. Duzenleme adresleri - onizlemede kalir, .eml'de soyulur
     *      (DuzenlemeAdresleri, export ucunda).
     *
     * Ikisi de ikinci bir HTML URETIMI DEGIL: duzenin tek kaynagi hala
     * MailHtmlRenderer. Fark sayisi ikidir ve oyle kalmalidir - sessizce
     * artarsa iki prototipte de yasanan hataya donulur (Mimari Kural 1).
     */
    @PostMapping(value = "/render/preview", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public String onizleme(@Valid @RequestBody PreviewRequest istek, Authentication authentication) {
        // Onizleme de takim temasini kullaniyor - baska takimin kimligiyle
        // mail uretilmesin diye burada da yetki kontrolu var.
        OturumKullanicisi.of(authentication).dogrula(istek.teamId());
        String themeKey = teams.takim(istek.teamId()).themeKey();
        String html = renderer.uret(istek.content(), istek.templateType(), themeKey);
        return onizlemeGorselleri.gomulu(html, renderer.tema(themeKey));
    }

    /**
     * "Outlook İçin Kopyala" - panoya konacak HTML.
     *
     * Onizlemeden TEK farki duzenleme nitelikleri: burada soyuluyor. Kullanici
     * bunu Outlook taslagina yapistiracak; editor icin var olan nitelikler
     * oraya tasinmasin.
     *
     * Gorseller data: URI ile gomulu geliyor - cid: pano uzerinden calismaz,
     * MIME kabi yok. Outlook masaustunun data: URI'yi nasil ele aldigi
     * surume gore degisiyor; GARANTI yol .eml indirmektir (bir sonraki uc).
     * Bu uc kolaylik icin: acik bir taslaga yapistirmak istendiginde.
     */
    @PostMapping(value = "/render/clipboard", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public String panoyaKopyala(@Valid @RequestBody PreviewRequest istek, Authentication authentication) {
        OturumKullanicisi.of(authentication).dogrula(istek.teamId());
        String themeKey = teams.takim(istek.teamId()).themeKey();
        String html = duzenlemeAdresleri.soy(
                renderer.uret(istek.content(), istek.templateType(), themeKey));
        return onizlemeGorselleri.gomulu(html, renderer.tema(themeKey));
    }

    /** "Outlook Maili İndir" - message/rfc822, gorseller cid: ile gomulu. */
    @GetMapping("/documents/{id}/export.eml")
    public ResponseEntity<Resource> emlIndir(@PathVariable Long id, Authentication authentication) {
        MailerDocument belge = documents.getir(id);
        OturumKullanicisi.of(authentication).dogrula(belge.teamId());
        String themeKey = teams.takim(belge.teamId()).themeKey();

        // Duzenleme adresleri YALNIZCA onizleme icindir - Outlook'a giden
        // mailde yalnizca editor icin var olan nitelik tasinmasin.
        String html = duzenlemeAdresleri.soy(
                renderer.uret(belge.content(), belge.templateType(), themeKey));
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
