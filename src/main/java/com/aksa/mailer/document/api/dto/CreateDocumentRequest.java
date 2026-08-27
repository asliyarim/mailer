package com.aksa.mailer.document.api.dto;

import com.aksa.mailer.document.domain.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * docs/api.md §3. content ALINMAZ - taslak, tipin varsayilan icerigiyle
 * SUNUCUDA dogar. Istemcinin bos iskelet gondermesine izin verilse iki ayri
 * "bos icerik" tanimi olusurdu.
 */
public record CreateDocumentRequest(
        @NotNull(message = "teamId zorunlu") Long teamId,
        @NotNull(message = "templateType zorunlu") TemplateType templateType,
        @NotBlank(message = "title boş olamaz") String title) {
}
