package com.aksa.mailer.render.template;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.render.domain.MailTheme;

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
}
