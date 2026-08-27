package com.aksa.mailer.document.domain;

import com.aksa.mailer.common.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailContentValidatorTest {

    /** BRIEF: test verisine şğıİçöü koy. */
    private static final String TURKCE = "Şebeke Operasyonları Aylık Hakediş Faturaları · İĞÜÇÖ";

    private static MailContent gecerliIcerik(List<MailSection> sections) {
        return new MailContent(
                1,
                new MailContent.Header("DİJİTAL UYGULAMALAR", "Ağustos 2026", "RPA Takımı"),
                new MailContent.Meeting("03.09.2026", "10:00", "Toplantı Salonu"),
                List.of("ilk paragraf", "orta", "kapanış"),
                sections,
                List.of(new MailContent.Note(Tone.BLUE, TURKCE)),
                new MailContent.Footer("Teşekkür ederiz.", "Başarılar dileriz!"));
    }

    private static MailSection bolum(String key) {
        return new MailSection(key, "ANALİZ ÇALIŞMALARI", Tone.BLUE,
                List.of("sector", "process"),
                List.of(Map.of("sector", "Elektrik", "process", TURKCE)));
    }

    @Test
    @DisplayName("geçerli içerik hata vermez")
    void gecerliIcerikGecer() {
        assertThatCode(() -> MailContentValidator.dogrula(gecerliIcerik(List.of(bolum("analysis")))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("başlık boşsa reddedilir")
    void bosBaslik() {
        MailContent icerik = new MailContent(
                1,
                new MailContent.Header("   ", "Ağustos", "RPA Takımı"),
                new MailContent.Meeting("", "", ""),
                List.of(), List.of(), List.of(),
                new MailContent.Footer("", ""));

        assertThatThrownBy(() -> MailContentValidator.dogrula(icerik))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Başlık boş olamaz");
    }

    @Test
    @DisplayName("desteklenmeyen şema sürümü reddedilir")
    void yanlisSemaSurumu() {
        MailContent icerik = new MailContent(
                99,
                new MailContent.Header("Başlık", "", "RPA Takımı"),
                new MailContent.Meeting("", "", ""),
                List.of(), List.of(), List.of(),
                new MailContent.Footer("", ""));

        assertThatThrownBy(() -> MailContentValidator.dogrula(icerik))
                .hasMessageContaining("Desteklenmeyen şema sürümü: 99");
    }

    @Test
    @DisplayName("tekrar eden bölüm anahtarı reddedilir")
    void tekrarEdenAnahtar() {
        MailContent icerik = gecerliIcerik(List.of(bolum("analysis"), bolum("analysis")));

        assertThatThrownBy(() -> MailContentValidator.dogrula(icerik))
                .hasMessageContaining("Bölüm anahtarı tekrar ediyor: analysis");
    }

    @Test
    @DisplayName("satır var ama sütun yoksa reddedilir - mailde boş tablo çıkardı")
    void sutunsuzSatir() {
        MailSection sutunsuz = new MailSection("analysis", "ANALİZ", Tone.BLUE,
                List.of(), List.of(Map.of("sector", "Elektrik")));

        assertThatThrownBy(() -> MailContentValidator.dogrula(gecerliIcerik(List.of(sutunsuz))))
                .hasMessageContaining("satır var ama sütun tanımlı değil");
    }

    @Test
    @DisplayName("sayaç saklanmaz, satır sayısından hesaplanır")
    void sayacHesaplanir() {
        MailContent icerik = gecerliIcerik(List.of(bolum("analysis"), bolum("development")));

        assertThat(icerik.toplamSatirSayisi()).isEqualTo(2);
    }

    @Test
    @DisplayName("bölümü olmayan içerik geçerlidir - sections sabit ikili değil")
    void bolumsuzIcerikGecerli() {
        assertThatCode(() -> MailContentValidator.dogrula(gecerliIcerik(List.of())))
                .doesNotThrowAnyException();
    }
}
