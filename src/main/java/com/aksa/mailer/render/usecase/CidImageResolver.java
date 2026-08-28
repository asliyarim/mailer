package com.aksa.mailer.render.usecase;

import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Temanin gorsellerini classpath'ten okur.
 *
 * Gorseller src/main/resources/themes/<tema>/ altinda DOSYA olarak durur;
 * veritabaninda degil. Sebebi Mimari Kural 4: tema kodda yasar. Ayrica
 * .eml uretirken ham bayta ihtiyacimiz var - base64'e cevirip
 * multipart/related govdesine gomecegiz.
 *
 * Okunan icerik onbellege alinir: ayni gorsel her onizlemede diskten
 * okunmasin. Dosyalar imajin icinde ve calisma aninda degismez.
 */
@Component
public class CidImageResolver {

    private final Map<String, byte[]> onbellek = new LinkedHashMap<>();

    /** Temanin bes gorseli: cid -> ham bayt. Mailde gectikleri sirayla. */
    public synchronized Map<String, byte[]> gorseller(MailTheme tema) {
        Map<String, byte[]> sonuc = new LinkedHashMap<>();
        for (ThemeImage gorsel : tema.images()) {
            sonuc.put(gorsel.cid(), oku(gorsel.resourcePath()));
        }
        return sonuc;
    }

    private byte[] oku(String yol) {
        byte[] onbellekten = onbellek.get(yol);
        if (onbellekten != null) {
            return onbellekten;
        }
        try (InputStream akis = new ClassPathResource(yol).getInputStream()) {
            byte[] icerik = akis.readAllBytes();
            onbellek.put(yol, icerik);
            return icerik;
        } catch (IOException e) {
            // Tema gorseli eksikse mail yarim gider; sessizce gecmek yerine
            // acikca patlat - hangi dosyanin eksik oldugu mesajda yazsin.
            throw new UncheckedIOException("Tema görseli okunamadı: " + yol, e);
        }
    }
}
