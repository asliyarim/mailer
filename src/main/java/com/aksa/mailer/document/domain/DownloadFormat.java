package com.aksa.mailer.document.domain;

/**
 * Indirme bicimi. V1__init.sql'deki CHECK kisitiyla ayni degerler -
 * buraya deger eklenirse yeni bir migrasyon gerekir.
 */
public enum DownloadFormat {
    EML,
    PDF,
    /** "Outlook İçin Kopyala" - mail panoya alindi (V5). */
    KOPYALA
}
