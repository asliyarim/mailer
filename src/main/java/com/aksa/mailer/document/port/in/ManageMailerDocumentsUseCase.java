package com.aksa.mailer.document.port.in;

import com.aksa.mailer.document.domain.DownloadFormat;
import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.domain.TemplateType;

import java.time.Instant;
import java.util.List;

/**
 * GIRIS portu. api/ katmani sadece bunu bilir; MailerDocumentService'i degil.
 */
public interface ManageMailerDocumentsUseCase {

    /**
     * Bir takimin belgeleri. arama bos/null ise SUZULMEZ.
     *
     * Arama sonBelgeler() ile AYNI davranisi tasir (baslik + donem, Turkce
     * kucuk harf ve sapkali katlamasiyla) - iki ekranda iki farkli arama
     * olsaydi kullanici hangisinin nasil calistigini kestiremezdi.
     */
    List<DocumentSummary> takimBelgeleri(Long teamId, String arama);

    /**
     * Kullanicinin erisebildigi BUTUN takimlarin belgeleri, en yeni once.
     *
     * Giris sayfasindaki "Son Taslaklarim" icin. takimBelgeleri() tek takim
     * istiyor ama bir PO'nun birden cok takimi olabiliyor; arayuz her takim
     * icin ayri istek atmasin diye tek uc.
     *
     * Gruplama (Kapanis / Planlama / Yonetici Ozeti) ISTEMCIDE yapilir -
     * templateType zaten ozetle geliyor ve sunucunun uc ayri liste dondurmesi
     * "son N kayit" anlamini bozardi.
     */
    /**
     * @param arama bos degilse basligi VE donemi bu metinle suzer
     *              (buyuk/kucuk harf duyarsiz, Turkce kurallariyla)
     */
    List<DocumentSummary> sonBelgeler(List<Long> teamIds, int limit, String arama);

    MailerDocument getir(Long id);

    MailerDocument olustur(NewDocumentCommand komut);

    /**
     * Bir belge OLUSTURULSAYDI icerigi ne olurdu - hicbir sey yazmadan.
     *
     * Arayuz, uygulama acilir acilmaz sag taraftaki mail sablonunu bosken de
     * cizebilsin diye var. Bunun yerine her acilista POST /documents
     * cagrilsaydi terk edilen "Yeni mail" taslaklari birikirdi; iskelet
     * istemcide kurulsaydi varsayilan icerigin IKINCI bir tanimi olur ve
     * zamanla bundan ayrisirdi.
     *
     * olustur() ile AYNI metodu cagirir - ikisinin ayrisma ihtimali yok.
     */
    MailContent varsayilanIcerik(Long teamId, TemplateType templateType);

    MailerDocument kaydet(SaveDocumentCommand komut);

    List<VersionSummary> versiyonlar(Long documentId);

    MailerDocument geriAl(Long documentId, int version, String sicil);

    /** Indirme olcumu. Basarisiz olmasi indirmeyi bozmamali (docs/api.md §10). */
    void indirmeKaydet(Long documentId, DownloadFormat format, String sicil);

    /** Liste ekrani icin hafif ozet - content TASIMAZ. */
    record DocumentSummary(
            Long id,
            Long teamId,
            /**
             * Takimin adi - "İş Zekâsı Takımı".
             *
             * teamId tek basina yetmiyordu: ADMIN butun takimlarin
             * belgelerini goruyor ve en dogal filtre "hangi takim". Arama
             * bunu da tariyor. Ad cozulemezse null.
             */
            String teamName,
            TemplateType templateType,
            String title,
            /**
             * content.header.period - "Ağustos 2026 Sprint Kapanışı" gibi.
             *
             * Ozette content TASINMAZ ama BU alan istisna: kullanici
             * taslaklarini sprint numarasi ve donemle ariyor, o bilgi yalnizca
             * burada. Tasinmasaydi arama kutusu baslikla sinirli kalirdi.
             * content zaten yuklu geliyor, ek maliyeti yok.
             */
            String period,
            String status,
            int currentVersion,
            String updatedBy,
            Instant updatedAt) {
    }

    /** Versiyon gecmisi satiri - content TASIMAZ, liste hafif kalsin. */
    record VersionSummary(int version, String createdBy, Instant createdAt) {
    }

    record NewDocumentCommand(Long teamId, TemplateType templateType, String title, String sicil) {
    }

    /**
     * expectedVersion iyimser kilit: sunucudaki currentVersion bundan
     * farkliysa VersionConflictException atilir (bkz. docs/api.md §5).
     */
    record SaveDocumentCommand(
            Long documentId,
            String title,
            String subject,
            MailContent content,
            int expectedVersion,
            String sicil) {
    }
}
