package com.aksa.mailer.document.api.dto;

import com.aksa.mailer.document.domain.DownloadFormat;
import jakarta.validation.constraints.NotNull;

/**
 * docs/api.md §10. teamId ALINMAZ - belgeden okunur; istemcinin gonderdigi
 * takim bilgisine guvenmek, olcumu carpitmanin kolay yolu olurdu.
 */
public record DownloadLogRequest(
        @NotNull(message = "format zorunlu") DownloadFormat format) {
}
