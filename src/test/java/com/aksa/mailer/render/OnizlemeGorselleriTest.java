package com.aksa.mailer.render;

import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeRegistry;
import com.aksa.mailer.render.template.KapanisTemplate;
import com.aksa.mailer.render.theme.RpaTheme;
import com.aksa.mailer.render.usecase.CidImageResolver;
import com.aksa.mailer.render.usecase.MailHtmlRenderer;
import com.aksa.mailer.render.usecase.OnizlemeGorselleri;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Onizleme HTML'i, mail HTML'inin AYNISI olmali - tek fark gorsel
 * referanslarinin cozulmus olmasi.
 *
 * Bu testin isi, o "tek fark"in tek fark olarak kalmasini korumak: birileri
 * onizleme yolunda duzen degistirmeye kalkarsa uzunluk/icerik farki buradan
 * gorunur (Mimari Kural 1).
 */
class OnizlemeGorselleriTest {

    private String mailHtml;
    private String onizlemeHtml;
    private MailTheme tema;

    @BeforeEach
    void kurulum() {
        MailHtmlRenderer renderer = new MailHtmlRenderer(List.of(new KapanisTemplate()));
        tema = ThemeRegistry.tema(RpaTheme.KEY);
        mailHtml = renderer.uret(OrnekIcerik.kapanis(), TemplateType.KAPANIS, RpaTheme.KEY);
        onizlemeHtml = new OnizlemeGorselleri(new CidImageResolver()).gomulu(mailHtml, tema);
    }

    @Test
    @DisplayName("Tarayicinin cozemeyecegi cid: referansi kalmaz")
    void cidKalmaz() {
        // Mail HTML'inde cid: OLMALI - .eml'e giden budur.
        assertThat(mailHtml).contains("src=\"cid:");
        // Onizlemede ise hicbiri kalmamali, yoksa iframe'de kirik gorsel cikar.
        assertThat(onizlemeHtml).doesNotContain("src=\"cid:");
    }

    @Test
    @DisplayName("Temanin bes gorseli de gomulur")
    void besGorsel() {
        assertThat(tema.images()).hasSize(5);
        int gomulen = onizlemeHtml.split("src=\"data:image/", -1).length - 1;
        assertThat(gomulen).isEqualTo(5);
    }

    @Test
    @DisplayName("Gorsel disinda tek karakter degismez")
    void duzenAynidir() {
        // Her iki metinde de gorsel adresini ayni yer tutucuya indirgeyip
        // karsilastiriyoruz: base64 govdesi disinda tek fark kalmamali.
        String onizlemeSade = onizlemeHtml.replaceAll(
                "src=\"data:image/[a-z]+;base64,[^\"]+\"", "src=\"@@\"");
        String mailSade = mailHtml.replaceAll("src=\"cid:[^\"]+\"", "src=\"@@\"");
        assertThat(onizlemeSade).isEqualTo(mailSade);
    }
}
