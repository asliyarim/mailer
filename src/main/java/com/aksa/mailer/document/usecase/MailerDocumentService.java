package com.aksa.mailer.document.usecase;

import com.aksa.mailer.common.domain.NotFoundException;
import com.aksa.mailer.common.domain.VersionConflictException;
import com.aksa.mailer.document.domain.DonemArtirici;
import com.aksa.mailer.document.domain.DownloadFormat;
import com.aksa.mailer.document.domain.MailContent;
import com.aksa.mailer.document.domain.MailContentValidator;
import com.aksa.mailer.document.domain.MailerDocument;
import com.aksa.mailer.document.domain.TemplateType;
import com.aksa.mailer.document.port.in.ManageMailerDocumentsUseCase;
import com.aksa.mailer.document.port.out.MailerDocumentRepository;
import com.aksa.mailer.document.port.out.MailerDocumentVersionRepository;
import com.aksa.mailer.document.port.out.MailerDownloadLogRepository;
import com.aksa.mailer.team.domain.MailTeam;
import com.aksa.mailer.team.port.in.GetTeamsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Belge is mantigi. JPA veya HTTP sinifi import ETMEZ.
 *
 * Kaydetme ve geri alma iki tabloya birden yazdigi icin @Transactional:
 * belge guncellenip versiyon satiri yazilamazsa gecmis eksik kalirdi.
 */
@Service
public class MailerDocumentService implements ManageMailerDocumentsUseCase {

    private final MailerDocumentRepository documentRepository;
    private final MailerDocumentVersionRepository versionRepository;
    private final MailerDownloadLogRepository downloadLogRepository;
    /**
     * Yeni taslagin basligi ve takim etiketi takimin ADINDAN turetiliyor.
     * Baska bir modulun port/in'i - port/out'una veya JPA repository'sine
     * gidilmez (bkz. GetTeamsUseCase javadoc'u).
     */
    private final GetTeamsUseCase teams;

    public MailerDocumentService(MailerDocumentRepository documentRepository,
                                 MailerDocumentVersionRepository versionRepository,
                                 MailerDownloadLogRepository downloadLogRepository,
                                 GetTeamsUseCase teams) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.downloadLogRepository = downloadLogRepository;
        this.teams = teams;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummary> takimBelgeleri(Long teamId, String arama) {
        Map<Long, String> adlar = takimAdlari(List.of(teamId));
        String aranan = arama == null ? "" : sadelestir(arama.trim());
        return documentRepository.takimBelgeleri(teamId).stream()
                .map(d -> ozet(d, adlar))
                // Bos arama SUZMEZ: kullanici kutuyu temizleyince liste geri gelir.
                .filter(o -> aranan.isEmpty() || eslesiyorTekTakim(o, aranan))
                .toList();
    }

    /**
     * Tek takim listesinde eslesme: baslik VEYA donem.
     *
     * sonBelgeler()'deki eslesme takim adini da tariyor cunku orada birden
     * cok takimin belgesi karisik duruyor. Burada takim ZATEN secili, yani
     * butun satirlarda ayni: takim adi da taransaydi kullanici takiminin
     * adini yazdiginda HICBIR SEYI suzmeyen bir filtre calisirdi.
     */
    private static boolean eslesiyorTekTakim(DocumentSummary ozet, String aranan) {
        return icerir(ozet.title(), aranan) || icerir(ozet.period(), aranan);
    }

    /**
     * teamId -> takim adi. TEK sorguda cozuluyor.
     *
     * Her belge icin ayri ayri takim sorulsaydi elli belgelik bir listede
     * elli sorgu olurdu; takim sayisi zaten sekiz.
     */
    private Map<Long, String> takimAdlari(List<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return teams.erisilebilirTakimlar(false, teamIds).stream()
                .collect(Collectors.toMap(MailTeam::id, MailTeam::name));
    }

    private static DocumentSummary ozet(MailerDocument d, Map<Long, String> takimAdlari) {
        // Donem icerikten okunuyor; icerik veya header bos olabilir.
        String period = d.content() == null || d.content().header() == null
                ? null
                : d.content().header().period();
        return new DocumentSummary(
                d.id(), d.teamId(), takimAdlari.get(d.teamId()),
                d.templateType(), d.title(), period,
                d.status().name(), d.currentVersion(), d.updatedBy(), d.updatedAt());
    }

    /** Giris sayfasi bir ekrana bu kadarini sigdirabiliyor. */
    private static final int EN_FAZLA_SON_BELGE = 50;

    /**
     * Arama yapilirken kac kaydin taranacagi.
     *
     * Arama kutusunun isi son 50 taslakla sinirli kalmamak: kullanici eski bir
     * sprinti arayabilmeli. Ama sinirsiz da olamaz - her tuşta butun tabloyu
     * jsonb icerigiyle birlikte yuklemek olur. Bu pencere disinda kalan cok
     * eski bir taslak aramada CIKMAZ; bilinerek verilmis bir sinir.
     */
    private static final int ARAMA_PENCERESI = 500;

    private static final Locale TURKCE = Locale.forLanguageTag("tr");

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummary> sonBelgeler(List<Long> teamIds, int limit, String arama) {
        // Ust sinir SUNUCUDA: istemci limit=100000 yollayip butun tabloyu
        // cekemesin. Alt sinir da var - limit=0 sessizce bos liste dondururdu.
        int sinir = Math.max(1, Math.min(limit, EN_FAZLA_SON_BELGE));

        Map<Long, String> adlar = takimAdlari(teamIds);
        // Arananı da AYNI sadelestirmeden gecir - yoksa kullanici sapkali
        // yazdiginda bu sefer o eslesmezdi.
        String aranan = arama == null ? "" : sadelestir(arama.trim());

        if (aranan.isEmpty()) {
            return documentRepository.sonBelgeler(teamIds, sinir).stream()
                    .map(d -> ozet(d, adlar))
                    .toList();
        }

        return documentRepository.sonBelgeler(teamIds, ARAMA_PENCERESI).stream()
                .map(d -> ozet(d, adlar))
                .filter(o -> eslesiyor(o, aranan))
                .limit(sinir)
                .toList();
    }

    /**
     * Baslikta, donemde VEYA TAKIM ADINDA geciyor mu.
     *
     * Takim adi sonradan eklendi: ADMIN butun takimlarin belgelerini goruyor
     * ve "iş" yazip İş Zekâsı'nin belgelerini suzmek en dogal beklenti.
     * Once yalnizca baslik ve donem taraniyordu, o yuzden hicbir sey
     * bulunmuyordu.
     *
     * Kucuk harfe cevirim TURKCE kurallariyla: varsayilan locale'de "İŞ"
     * -> "i̇ş" olur ve kullanicinin yazdigi "iş" ile eslesmez.
     */
    private static boolean eslesiyor(DocumentSummary ozet, String aranan) {
        return icerir(ozet.title(), aranan)
                || icerir(ozet.period(), aranan)
                || icerir(ozet.teamName(), aranan);
    }

    private static boolean icerir(String deger, String aranan) {
        return deger != null && sadelestir(deger).contains(aranan);
    }

    /**
     * Aramaya hazirlar: Turkce kucuk harf + SAPKALI sesli katlama.
     *
     * Sapka meselesi: takimin kayitli adi "İş Zekâsı Takımı" (â ile) ama
     * kullanici "İş Zekası" yaziyor - klavyede sapkali a yok denecek kadar
     * az kullaniliyor. Katlamasaydik hicbir sey bulunmuyordu.
     *
     * YALNIZCA sapkali sesliler katlaniyor (â î û). ı, ş, ğ, ü, ö, ç
     * Turkce'de AYRI HARFLERDIR ve katlanmaz: "is" yazan biri "İş"i
     * bulmamali, baska kelime.
     */
    private static String sadelestir(String deger) {
        return deger.toLowerCase(TURKCE)
                .replace('â', 'a')
                .replace('î', 'i')
                .replace('û', 'u');
    }

    @Override
    @Transactional(readOnly = true)
    public MailerDocument getir(Long id) {
        return documentRepository.bul(id)
                .orElseThrow(() -> new NotFoundException("Belge bulunamadı: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public MailContent varsayilanIcerik(Long teamId, TemplateType templateType) {
        // Takimi cozmek iki isi birden goruyor: baslik/etiket adindan
        // turetiliyor, ve olmayan bir teamId burada temiz 404 doner - eskiden
        // olusturmada yabanci anahtar ihlaline dusup 500 olurdu.
        MailTeam takim = teams.takim(teamId);
        return VarsayilanIcerik.uret(templateType, takim.name());
    }

    @Override
    @Transactional
    public MailerDocument olustur(NewDocumentCommand komut) {
        // Taslak, tipin varsayilan icerigiyle DOGAR - istemci bos content
        // gondermez. Tek dogru kaynak sunucu (bkz. docs/api.md §3).
        //
        // GET /documents/default ile AYNI metot: arayuzun on izlemede
        // gosterdigi iskelet ile kaydedilen belgenin icerigi ayrisamaz.
        MailContent varsayilan = varsayilanIcerik(komut.teamId(), komut.templateType());

        MailerDocument taslak = MailerDocument.yeniTaslak(
                komut.teamId(), komut.templateType(), komut.title(), varsayilan, komut.sicil());

        MailerDocument kaydedilen = documentRepository.kaydet(taslak);
        versionRepository.ekle(kaydedilen.id(), 1, varsayilan, komut.sicil());
        return kaydedilen;
    }

    /**
     * "Geçen sprintten devam et".
     *
     * NEDEN SUNUCUDA: istemci bunu "yeni belge oluştur, sonra içeriği kaydet"
     * diye iki istekle de yapabilirdi. Ama ikinci istek düşerse geriye BOŞ bir
     * belge kalırdı - kullanıcının silemediği bir çöp kayıt. Burada tek işlem:
     * ya belge içeriğiyle birlikte doğar ya da hiç doğmaz.
     *
     * Kopyalanmayan üç şey var, üçü de bilerek:
     *   - subject: mailin konu satırı sprint'e özel yazılıyor, taşınırsa
     *     yanlış konuyla gönderilme riski var. Boş doğsun, kullanıcı yazsın.
     *   - status/currentVersion: kopya YENI bir taslak, geçmişi kaynağınki
     *     değil. Sürüm 1'den başlar.
     *   - createdBy: kopyayı çıkaran kişi yazılır, kaynağın sahibi değil.
     */
    @Override
    @Transactional
    public MailerDocument kopyala(Long kaynakId, String sicil) {
        MailerDocument kaynak = getir(kaynakId);

        MailContent yeniIcerik = donemiArtirilmis(kaynak.content());
        // Baslikta artirilacak sayi yoksa ad aynen kalirdi ve listede iki
        // ayni isim yan yana dururdu - "(kopya)" o durumu ayirt ettiriyor.
        String yeniBaslik = DonemArtirici.artir(kaynak.title())
                .orElseGet(() -> kaynak.title() + " (kopya)");

        MailerDocument taslak = MailerDocument.yeniTaslak(
                kaynak.teamId(), kaynak.templateType(), yeniBaslik, yeniIcerik, sicil);

        MailerDocument kaydedilen = documentRepository.kaydet(taslak);
        versionRepository.ekle(kaydedilen.id(), 1, yeniIcerik, sicil);
        return kaydedilen;
    }

    /**
     * İçeriğin başlık bloğundaki dönem ve başlık metinlerini bir artırır.
     * Geri kalan her şey (bölümler, satırlar, notlar) OLDUĞU GIBI taşınır -
     * kopyalamanın bütün değeri zaten orada.
     */
    private MailContent donemiArtirilmis(MailContent icerik) {
        MailContent.Header baslik = icerik.header();
        if (baslik == null) {
            return icerik;
        }

        MailContent.Header yeniBaslik = new MailContent.Header(
                DonemArtirici.artir(baslik.title()).orElse(baslik.title()),
                DonemArtirici.artir(baslik.period()).orElse(baslik.period()),
                baslik.teamLabel());

        return new MailContent(
                icerik.schemaVersion(), yeniBaslik, icerik.meeting(),
                icerik.intro(), icerik.sections(), icerik.notes(), icerik.footer());
    }

    @Override
    @Transactional
    public MailerDocument kaydet(SaveDocumentCommand komut) {
        MailerDocument mevcut = getir(komut.documentId());

        if (mevcut.currentVersion() != komut.expectedVersion()) {
            throw new VersionConflictException(
                    "Belge siz düzenlerken değişti. Sunucudaki sürüm: " + mevcut.currentVersion()
                            + ", gönderilen: " + komut.expectedVersion() + ".");
        }

        // Istemciye GUVENILMEZ - nihai dogrulama burada.
        MailContentValidator.dogrula(komut.content());

        MailerDocument guncel = mevcut.guncellenmis(
                komut.title(), komut.subject(), komut.content(), komut.sicil());

        MailerDocument kaydedilen = documentRepository.kaydet(guncel);
        versionRepository.ekle(
                kaydedilen.id(), kaydedilen.currentVersion(), komut.content(), komut.sicil());
        return kaydedilen;
    }

    @Override
    @Transactional
    public void indirmeKaydet(Long documentId, DownloadFormat format, String sicil) {
        // teamId'yi belgeden aliyoruz - istemcinin gonderdigine guvenmiyoruz.
        MailerDocument belge = getir(documentId);
        downloadLogRepository.ekle(belge.id(), belge.teamId(), format, sicil);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionSummary> versiyonlar(Long documentId) {
        getir(documentId); // yoksa 404
        return versionRepository.gecmis(documentId).stream()
                .map(v -> new VersionSummary(v.version(), v.createdBy(), v.createdAt()))
                .toList();
    }

    @Override
    @Transactional
    public MailerDocument geriAl(Long documentId, int version, String sicil) {
        MailerDocument mevcut = getir(documentId);

        MailContent eskiIcerik = versionRepository.icerik(documentId, version)
                .orElseThrow(() -> new NotFoundException(
                        "Belge " + documentId + " için " + version + ". sürüm bulunamadı."));

        // Gecmis SILINMEZ: eski icerik YENI bir surum olarak yazilir, boylece
        // geri alma da geri alinabilir (bkz. docs/api.md §7).
        MailerDocument geriAlinmis = mevcut.guncellenmis(
                mevcut.title(), mevcut.subject(), eskiIcerik, sicil);

        MailerDocument kaydedilen = documentRepository.kaydet(geriAlinmis);
        versionRepository.ekle(
                kaydedilen.id(), kaydedilen.currentVersion(), eskiIcerik, sicil);
        return kaydedilen;
    }
}
