package com.aksa.mailer.common.domain;

/**
 * Istemci, sunucudakinden farkli bir surum bekliyordu - araya baska bir kayit
 * girmis. GlobalExceptionHandler bunu 409'a cevirir.
 *
 * Bu istisna olmadan ikinci sekmede yapilan kayit birincisini SESSIZCE siler
 * ve versiyon gecmisi yalan soyler (bkz. docs/api.md §5).
 */
public class VersionConflictException extends RuntimeException {

    public VersionConflictException(String message) {
        super(message);
    }
}
