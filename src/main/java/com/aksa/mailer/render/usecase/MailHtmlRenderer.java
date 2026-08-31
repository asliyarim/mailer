package com.aksa.mailer.render.usecase;

import com.aksa.mailer.common.domain.DomainValidationException;
import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailContentValidator;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeRegistry;
import com.aksa.mailer.render.template.MailTemplate;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * MAIL HTML'ININ URETILDIGI TEK YER.
 *
 * Onizleme de, .eml de buradan cikar - ikisi ayni metindir. Ikinci bir uretim
 * (ozellikle frontend'de) YAZILMAZ: ilk prototipte mail iki kez yazilmisti,
 * onizlemede duzeltilen hata mailde duzelmiyordu (Mimari Kural 1).
 *
 * Kod incelemesinin standart sorusu: "Ikinci bir HTML uretimi eklendi mi?"
 */
@Component
public class MailHtmlRenderer {

    private final Map<TemplateType, MailTemplate> sablonlar = new EnumMap<>(TemplateType.class);

    public MailHtmlRenderer(List<MailTemplate> bulunanSablonlar) {
        for (MailTemplate sablon : bulunanSablonlar) {
            sablonlar.put(sablon.tip(), sablon);
        }
    }

    /**
     * Icerik + tema -> Outlook-guvenli HTML.
     *
     * @param themeKey mail_teams.theme_key; karsiligi ThemeRegistry'de
     */
    public String uret(MailContent content, TemplateType tip, String themeKey) {
        MailContentValidator.dogrula(content);
        MailTheme tema = ThemeRegistry.tema(themeKey);
        return sablon(tip).uret(content, tema);
    }

    /** .eml uretimi icin: HTML ile birlikte kullanilacak temayi da dondurur. */
    public MailTheme tema(String themeKey) {
        return ThemeRegistry.tema(themeKey);
    }

    private MailTemplate sablon(TemplateType tip) {
        MailTemplate sablon = sablonlar.get(tip);
        if (sablon == null) {
            // Uc tipin de sablonu var; buraya ancak yeni bir TemplateType
            // eklenip sablonu yazilmadan duserse gelinir. Sessizce yanlis
            // sablon uretmek yerine acikca soyle.
            throw new DomainValidationException(
                    "Bu mail tipi henüz üretilemiyor: " + tip + ". Hazır tipler: " + sablonlar.keySet());
        }
        return sablon;
    }
}
