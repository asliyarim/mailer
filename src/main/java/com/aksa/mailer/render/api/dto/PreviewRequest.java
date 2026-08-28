package com.aksa.mailer.render.api.dto;

import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.TemplateType;
import jakarta.validation.constraints.NotNull;

/**
 * docs/api.md §8. Kaydedilmemis icerik de onizlenebilsin diye govdede
 * geliyor - her tusa basista kaydetmek istemiyoruz ama kullanici yazdigini
 * gormeli.
 */
public record PreviewRequest(
        @NotNull(message = "teamId zorunlu") Long teamId,
        @NotNull(message = "templateType zorunlu") TemplateType templateType,
        @NotNull(message = "content zorunlu") MailContent content) {
}
