package com.aksa.mailer.common.config;

import com.aksa.mailer.auth.security.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Uygulama ayaga kalkarken YANLIS AYARI YAKALAR.
 *
 * Iki tur sorun var ve ikisine de farkli davraniyoruz:
 *
 * DURDURAN: JWT sirri hala gelistirme varsayilaniysa uygulama BASLAMAZ.
 * O sir depoda yaziyor; uretimde kalirsa herkes kendine ADMIN token'i
 * uretebilir. Sessizce calismasindansa acikca durmasi gerekir.
 *
 * UYARAN: CORS listesi hala localhost'sa yalnizca log'a yaziyoruz.
 * Yanlis olabilir ama gercekten localhost'ta calisiyor da olabiliriz -
 * durdurmak yanlis olurdu. Yasanan hata tam buydu: kabuk adresi listede
 * olmadigi icin butun YAZMA islemleri "403 Invalid CORS request" ile
 * dusuyordu, okuma calistigi icin fark edilmesi saatler aldi. Etkin liste
 * artik acilista log'a basiliyor - bir daha tahmin etmeye gerek yok.
 */
@Component
public class BaslangicDenetimi {

    private static final Logger log = LoggerFactory.getLogger(BaslangicDenetimi.class);

    /** JwtProperties'teki varsayilanla AYNI olmak zorunda. */
    private static final String GELISTIRME_SIRRI = "dev-only-secret-change-me-please-32-bytes-min";

    private static final int EN_AZ_SIR_UZUNLUGU = 32;

    private final JwtProperties jwt;
    private final List<String> corsOriginleri;
    private final boolean uretim;

    public BaslangicDenetimi(JwtProperties jwt,
                             @Value("${app.cors.allowed-origins}") List<String> corsOriginleri,
                             @Value("${app.uretim:false}") boolean uretim) {
        this.jwt = jwt;
        this.corsOriginleri = corsOriginleri;
        this.uretim = uretim;
    }

    /**
     * @PostConstruct - ApplicationReadyEvent DEGIL. Fark onemli: hazir
     * olayi sunucu ayaga kalktiktan SONRA tetikleniyor, yani yanlis sirla
     * calisan uygulama bir an istek kabul edebilirdi. Burada baglam kurulurken
     * patliyor, port hic acilmiyor.
     */
    @PostConstruct
    public void denetle() {
        sirriDogrula();
        corsuBildir();
    }

    private void sirriDogrula() {
        String sir = jwt.getSecret();

        if (GELISTIRME_SIRRI.equals(sir)) {
            if (uretim) {
                throw new IllegalStateException("""
                        APP_JWT_SECRET verilmemis - uygulama gelistirme sirriyla calisiyor olurdu.
                        O sir depoda acikca yaziyor; uretimde kalirsa herkes kendine ADMIN token'i \
                        uretebilir. APP_JWT_SECRET'i ayarla (odyssey-auth'takiyle AYNI olmali) \
                        ve yeniden baslat.""");
            }
            log.warn("APP_JWT_SECRET verilmedi, GELISTIRME sirri kullaniliyor. "
                    + "Uretimde app.uretim=true ile bu durum uygulamayi durdurur.");
            return;
        }

        if (sir == null || sir.length() < EN_AZ_SIR_UZUNLUGU) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET en az " + EN_AZ_SIR_UZUNLUGU + " karakter olmali (HMAC-SHA256), "
                            + "verilen: " + (sir == null ? 0 : sir.length()) + " karakter.");
        }
    }

    /**
     * Etkin CORS listesini acilista yaz.
     *
     * Tarayici AYNI ORIGIN'de bile POST/PUT/DELETE isteklerine Origin basligi
     * ekler; kabugun adresi listede yoksa okuma calisir ama yazma duser.
     */
    private void corsuBildir() {
        log.info("CORS izin verilen origin'ler: {}", corsOriginleri);
        if (corsOriginleri.stream().allMatch(o -> o.contains("localhost"))) {
            log.warn("CORS listesi yalnizca localhost iceriyor. Uygulama baska bir adresten "
                    + "sunuluyorsa BUTUN yazma islemleri '403 Invalid CORS request' ile duser. "
                    + "APP_CORS_ALLOWED_ORIGINS'i kontrol et.");
        }
    }
}
