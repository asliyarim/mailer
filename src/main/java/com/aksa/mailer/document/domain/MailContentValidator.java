package com.aksa.mailer.document.domain;

import com.aksa.mailer.common.domain.DomainValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Icerik dogrulamasi. NIHAI kaynak burasidir - frontend'deki
 * mailContent.js sadece kullaniciya erken geri bildirim verir; istemciye
 * guvenilmez.
 *
 * Saf domain: Spring, JPA, HTTP yok.
 */
public final class MailContentValidator {

    private MailContentValidator() {
    }

    public static void dogrula(MailContent content) {
        List<String> hatalar = hatalari(content);
        if (!hatalar.isEmpty()) {
            throw new DomainValidationException(String.join(" ", hatalar));
        }
    }

    static List<String> hatalari(MailContent content) {
        List<String> hatalar = new ArrayList<>();
        if (content == null) {
            return List.of("İçerik boş olamaz.");
        }
        if (content.schemaVersion() != MailContent.GECERLI_SEMA_SURUMU) {
            hatalar.add("Desteklenmeyen şema sürümü: " + content.schemaVersion() + ".");
        }
        if (content.header() == null || bos(content.header().title())) {
            hatalar.add("Başlık boş olamaz.");
        }
        if (content.header() == null || bos(content.header().teamLabel())) {
            hatalar.add("Takım adı boş olamaz.");
        }

        Set<String> gorulenAnahtarlar = new HashSet<>();
        List<MailSection> sections = content.sections();
        for (int i = 0; i < sections.size(); i++) {
            MailSection bolum = sections.get(i);
            int sira = i + 1;
            if (bos(bolum.key())) {
                hatalar.add(sira + ". bölümün anahtarı boş.");
            } else if (!gorulenAnahtarlar.add(bolum.key())) {
                // Anahtar tekrarlanirsa render tarafinda hangi bolumun
                // hangisi oldugu belirsizlesir.
                hatalar.add("Bölüm anahtarı tekrar ediyor: " + bolum.key() + ".");
            }
            if (bos(bolum.title())) {
                hatalar.add(sira + ". bölümün başlığı boş.");
            }
            if (bolum.tone() == null) {
                hatalar.add(sira + ". bölümün tonu belirtilmemiş.");
            }
            if (bolum.columns().isEmpty() && !bolum.rows().isEmpty()) {
                // Satir var ama gosterilecek sutun yok - mailde bos tablo cikar.
                hatalar.add(sira + ". bölümde satır var ama sütun tanımlı değil.");
            }
        }

        for (int i = 0; i < content.notes().size(); i++) {
            if (content.notes().get(i).tone() == null) {
                hatalar.add((i + 1) + ". notun tonu belirtilmemiş.");
            }
        }

        return hatalar;
    }

    private static boolean bos(String deger) {
        return deger == null || deger.isBlank();
    }
}
