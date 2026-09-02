package com.aksa.mailer.render.template;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.MailTheme;
import com.aksa.mailer.render.domain.ThemeImage;

import java.util.List;

/**
 * Bir mail tipinin HTML uretimi.
 *
 * Gerceklemeler YALNIZCA Outlook'un anladigi CSS uretir: tablo duzeni, satir
 * ici style, sabit piksel genislik. flexbox, grid, position, border-radius,
 * linear-gradient, background-image, max-width, yuzde genislikli ic ice kutu
 * ve web fontu YASAK - Outlook masaustu HTML'i Word'un motoruyla cizer ve
 * bunlarin hicbirini desteklemez (Mimari Kural 2).
 *
 * Yasak listesi MailHtmlRendererTest icinde test olarak da duruyor; kimse
 * yanlislikla ekleyemesin diye.
 */
public interface MailTemplate {

    TemplateType tip();

    String uret(MailContent content, MailTheme tema);

    /**
     * Bu sablonun hero gorseli. Varsayilan: takimin kendi logosu.
     *
     * Iki toplanti sablonu bunu ezer ve butun takimlarda AYNI logoyu
     * kullanir (bolumun cark logosu / toplanti logosu) - zemin yine takimin
     * renginde kalir.
     */
    default ThemeImage heroGorseli(MailTheme tema) {
        return tema.hero();
    }

    /**
     * Maile GOMULECEK gorseller.
     *
     * HTML'in gosterdigiyle BIREBIR ayni olmak zorunda: hero sablona gore
     * degistigi icin gomulen liste de ona gore degismeli. Sabit
     * tema.images() kullanilsaydi Yonetici Ozeti mailinde HTML bir hero'yu
     * isaret eder, .eml baskasini gomer ve Outlook'ta KIRIK GORSEL cikardi.
     */
    default List<ThemeImage> gorseller(MailTheme tema) {
        return tema.images(heroGorseli(tema));
    }
}
