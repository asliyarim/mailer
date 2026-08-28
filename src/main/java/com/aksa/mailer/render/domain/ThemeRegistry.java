package com.aksa.mailer.render.domain;

import com.aksa.mailer.common.domain.NotFoundException;
import com.aksa.mailer.render.theme.Temalar;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kodda tanimli temalarin listesi. mail_teams.theme_key buraya isaret eder -
 * veritabaninda yabanci anahtar YOK, karsilik burasi.
 *
 * Yeni takim eklemek: bir tema sinifi yaz, asagiya kaydet, gorsel klasorunu
 * ekle. Baska hicbir yere dokunulmaz.
 */
public final class ThemeRegistry {

    private static final Map<String, MailTheme> TEMALAR = new LinkedHashMap<>();

    static {
        Temalar.hepsi().forEach(ThemeRegistry::kaydet);
    }

    private ThemeRegistry() {
    }

    private static void kaydet(MailTheme tema) {
        TEMALAR.put(tema.key(), tema);
    }

    /**
     * Anahtari verilen tema. Yoksa NotFoundException - sessizce varsayilan
     * temaya DUSMEZ: yanlis takimin kimligiyle mail gitmesindense hata versin.
     */
    public static MailTheme tema(String key) {
        MailTheme tema = TEMALAR.get(key);
        if (tema == null) {
            throw new NotFoundException(
                    "Tanımlı olmayan tema: " + key + ". Tanımlı temalar: " + TEMALAR.keySet());
        }
        return tema;
    }

    public static boolean tanimliMi(String key) {
        return TEMALAR.containsKey(key);
    }

    public static Map<String, MailTheme> hepsi() {
        return Map.copyOf(TEMALAR);
    }
}
