package com.aksa.mailer.document.domain;

/**
 * Mail tipi. Veritabaninda CHECK kisitiyla ayni degerler tutuluyor
 * (V1__init.sql) - buraya deger eklenirse yeni bir migrasyon gerekir.
 */
public enum TemplateType {
    KAPANIS,
    PLANLAMA,
    YONETICI_OZETI
}
