package com.aksa.mailer.document.domain;

/**
 * Mail tipi. Veritabaninda CHECK kisitiyla ayni degerler tutuluyor
 * (V1__init.sql) - buraya deger eklenirse yeni bir migrasyon gerekir.
 */
public enum TemplateType {
    KAPANIS,
    PLANLAMA,
    YONETICI_OZETI,
    /**
     * Sprint disi toplantilar icin. Yonetici Ozeti'yle ortusuyor ama farki
     * su: Yonetici Ozeti bir TAKIMIN SPRINT'ine bagli, bu ise HERHANGI BIR
     * TOPLANTIYA ve takimlar ustu. Birlestirilseydi her iki durumda da
     * yarisi bos kalan bir form cikardi (V6).
     */
    TOPLANTI_CIKTILARI
}
