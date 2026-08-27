package com.aksa.mailer.common.web;

import java.time.Instant;

/**
 * Tek tip hata govdesi. Frontend'deki apiClient.js bu govdedeki "message"
 * alanini okuyup kullaniciya gosterir - alan adi degistirilirse orasi da
 * guncellenmeli.
 */
public record ApiErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path);
    }
}
