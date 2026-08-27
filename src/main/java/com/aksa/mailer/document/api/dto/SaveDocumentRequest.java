package com.aksa.mailer.document.api.dto;

import com.aksa.mailer.document.domain.MailContent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * docs/api.md §5.
 *
 * expectedVersion ZORUNLU. Opsiyonel yapilsaydi istemciler onu gondermemeyi
 * secer ve iyimser kilit ise yaramaz hale gelirdi.
 */
public record SaveDocumentRequest(
        @NotBlank(message = "title boş olamaz") String title,
        String subject,
        @NotNull(message = "content zorunlu") MailContent content,
        @NotNull(message = "expectedVersion zorunlu") Integer expectedVersion) {
}
