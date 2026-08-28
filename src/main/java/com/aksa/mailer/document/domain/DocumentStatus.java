package com.aksa.mailer.document.domain;

/**
 * Belge durumu. V1__init.sql'deki CHECK kisitiyla ayni degerler.
 *
 * DIKKAT - FINAL SU AN KULLANILMIYOR.
 *
 * Her belge DRAFT dogar ve oyle kalir; FINAL'e geciren bir uc YOK, docs/api.md
 * icinde de yok. Sutun semada duruyor ama isletilmiyor - tabloya bakip
 * "demek bir onay akisi var" sanilmasin diye buraya yaziyoruz.
 *
 * Bilerek boyle: bir onay/gonderim akisi ne yol haritasinda ne de kullanici
 * talebinde var. Kimsenin istemedigi bir akis eklemek kapsami sessizce
 * genisletmek olurdu - ustelik FINAL belgeyi kilitlersek "neden
 * duzenleyemiyorum" diye geri gelir.
 *
 * Ileride "gonderildi olarak isaretle" istenirse POST /documents/{id}/finalize
 * ucu ve listede bir filtre yeterli; sema hazir, migrasyon gerekmiyor.
 */
public enum DocumentStatus {
    DRAFT,
    FINAL
}
