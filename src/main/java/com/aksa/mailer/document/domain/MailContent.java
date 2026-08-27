package com.aksa.mailer.document.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * mailer_documents.content (jsonb) govdesinin Java karsiligi. Sema v1;
 * baglayici tanim docs/api.md icinde.
 *
 * Bu nesne KULLANICININ DOLDURDUGU her seyi tasir. Tema (renk, logo, maskot,
 * blok sirasi) BURADA DEGIL, kodda yasar (render/theme/). Bu sinir bulanirsa
 * sablon yonetim paneli yazmak gerekir; o kapsam disi (Mimari Kural 4).
 *
 * @JsonIgnoreProperties: ileride sema v2 alan eklerse eski surum okunurken
 * patlamasin. Sema surumu yukseltilirken gocurme kodu ayrica yazilir.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MailContent(
        int schemaVersion,
        Header header,
        Meeting meeting,
        List<String> intro,
        List<MailSection> sections,
        List<Note> notes,
        Footer footer) {

    public static final int GECERLI_SEMA_SURUMU = 1;

    public MailContent {
        intro = intro == null ? List.of() : List.copyOf(intro);
        sections = sections == null ? List.of() : List.copyOf(sections);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String title, String period, String teamLabel) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meeting(String date, String time, String place) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Note(Tone tone, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Footer(String line1, String line2) {
    }

    /**
     * Toplam satir sayisi. SAKLANMAZ, her cagrida hesaplanir - manuel sayac
     * butonu kapsam disi oldugu icin saklanacak bir sey yok.
     *
     * @JsonIgnore: hesaplanan deger jsonb govdesine YAZILMAMALI, yoksa
     * saklanmis sayac haline gelir ve bayatlar.
     */
    @JsonIgnore
    public int toplamSatirSayisi() {
        return sections.stream().mapToInt(MailSection::satirSayisi).sum();
    }

    /** Bos iskelet - yeni taslak bununla dogar. */
    public static MailContent bos() {
        return new MailContent(
                GECERLI_SEMA_SURUMU,
                new Header("", "", ""),
                new Meeting("", "", ""),
                List.of("", "", ""),
                List.of(),
                List.of(),
                new Footer("", ""));
    }
}
