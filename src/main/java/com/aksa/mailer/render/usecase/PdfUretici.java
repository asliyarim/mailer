package com.aksa.mailer.render.usecase;

import com.aksa.mailer.render.domain.ThemeImage;
import java.util.List;
import com.aksa.mailer.common.domain.DomainValidationException;
import com.aksa.mailer.render.domain.MailTheme;
import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Mail HTML'ini PDF'e cevirir. HTML URETMEZ.
 *
 * "PDF İndir" dugmesi buradan besleniyor. Once tarayicinin yazdirma
 * penceresi aciliyordu; kullanici oradan "PDF olarak kaydet" secmek ya da
 * PDFCreator gibi bir yardimci kurmak zorundaydi.
 *
 * NEDEN SUNUCUDA: istemcide (jsPDF/html2canvas) uretmek maili ekran
 * goruntusu gibi RASTERIZE eder - yazilar bulaniklasir, tablolar bozulur,
 * dosya sisar. Daha kotusu, mailin IKINCI bir cizimi olur ve zamanla
 * asil maille ayrisir (Mimari Kural 1). Burada kaynak yine
 * MailHtmlRenderer'in urettigi TEK HTML.
 */
@Component
public class PdfUretici {

    /**
     * Sablonlar Arial ve Segoe UI istiyor; ikisi de sunucuda yok ve PDF'in
     * yerlesik fontlari Turkce harfleri (s, g, i, I) TASIMIYOR - onlarla
     * uretilen PDF'te o harfler bos kutu cikar.
     *
     * Droid Sans'i BU ADLARLA kaydediyoruz: boylece sablon HTML'ine hic
     * dokunmadan dogru yazi tipi kullaniliyor. Lisansi Apache 2.0, dagitimi
     * serbest (src/main/resources/fonts/LISANS.txt).
     */
    private static final String[] AILE_ADLARI = {"Arial", "Segoe UI", "Helvetica", "sans-serif"};

    private static final String NORMAL = "fonts/DroidSans.ttf";
    private static final String KALIN = "fonts/DroidSans-Bold.ttf";

    private final CidImageResolver cozucu;

    public PdfUretici(CidImageResolver cozucu) {
        this.cozucu = cozucu;
    }

    public byte[] uret(String html, List<ThemeImage> gorselListesi) {
        Map<String, byte[]> gorseller = cozucu.gorseller(gorselListesi);

        try (ByteArrayOutputStream cikti = new ByteArrayOutputStream()) {
            PdfRendererBuilder olusturucu = new PdfRendererBuilder();
            olusturucu.useFastMode();
            olusturucu.withW3cDocument(w3c(html), "/");

            // cid: adreslerini dosya baytlarina cevir. Aksi halde PDF'te
            // gorsellerin yerinde bos kutu kalir - .eml'de calisan cid:
            // burada bir anlam ifade etmiyor.
            olusturucu.useProtocolsStreamImplementation(url -> {
                String cid = url.startsWith("cid:") ? url.substring(4) : url;
                byte[] veri = gorseller.get(cid);
                if (veri == null) {
                    throw new IllegalStateException("PDF icin gorsel bulunamadi: " + url);
                }
                return new FSStream() {
                    @Override
                    public InputStream getStream() {
                        return new ByteArrayInputStream(veri);
                    }

                    @Override
                    public Reader getReader() {
                        return new InputStreamReader(new ByteArrayInputStream(veri), StandardCharsets.UTF_8);
                    }
                };
            }, "cid");

            yaziTipleriniKaydet(olusturucu);

            olusturucu.toStream(cikti);
            olusturucu.run();
            return cikti.toByteArray();
        } catch (Exception e) {
            throw new DomainValidationException("PDF üretilemedi: " + e.getMessage());
        }
    }

    /**
     * Sablon HTML'i TARAYICI toleransina gore yaziliyor: kapanmayan &lt;img&gt;,
     * &lt;br&gt;, &lt;col&gt;. openhtmltopdf ise XML bekliyor ve bunlarda
     * patliyor. jsoup araya girip duzgun bir DOM kuruyor.
     */
    /**
     * Sayfa kutusu. Mail govdesi 760px SABIT (MailIskeleti.GOVDE_GENISLIK) ve
     * govde <td align="center"> icinde ortali duruyor.
     *
     * @page verilmezse openhtmltopdf her yandan 0.5 inc (36pt) birakiyor:
     * A4'un 595.275pt genisliginden geriye 523.275pt = 698px kaliyor. 760px'lik
     * govde oraya sigmiyor, 62px tasiyor ve ORTALI oldugu icin tasma ikiye
     * bolunup HER IKI YANDAN ~31px kirpiliyor. (Uretilen PDF'in kirpma yolu
     * olculdu: x ekseninde 36 -> 559.275pt.)
     *
     * 10pt yan bosluk: 595.275 - 20 = 575.275pt = 767px. 760px sigiyor,
     * 7px pay kaliyor. Daha genis bosluk birakilamaz - govde genisligi
     * Outlook kurali geregi sabit, kucultulemez (Mimari Kural 2).
     *
     * Kural YALNIZCA PDF yolunda: paylasilan mail HTML'ine eklenmiyor, yani
     * onizleme/.eml arasindaki iki farkli nokta artmiyor (Mimari Kural 1).
     */
    private static final String SAYFA_KURALI =
            "@page { size: A4; margin: 24pt 10pt; }";

    private org.w3c.dom.Document w3c(String html) {
        Document jsoupBelgesi = Jsoup.parse(html);
        jsoupBelgesi.head().append("<style>" + SAYFA_KURALI + "</style>");
        jsoupBelgesi.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return new W3CDom().fromJsoup(jsoupBelgesi);
    }

    /**
     * Fontu GECICI DOSYAYA yaziyoruz.
     *
     * openhtmltopdf font icin bir File istiyor; JAR icindeki kaynak ise
     * dosya sisteminde yok. Kopya bir kez cikariliyor ve JVM kapanirken
     * siliniyor.
     */
    private void yaziTipleriniKaydet(PdfRendererBuilder olusturucu) throws IOException {
        Path normal = geciciKopya(NORMAL, "droid-sans");
        Path kalin = geciciKopya(KALIN, "droid-sans-bold");

        for (String aile : AILE_ADLARI) {
            olusturucu.useFont(normal.toFile(), aile, 400,
                    BaseRendererBuilder.FontStyle.NORMAL, true);
            olusturucu.useFont(kalin.toFile(), aile, 700,
                    BaseRendererBuilder.FontStyle.NORMAL, true);
        }
    }

    private Path geciciKopya(String kaynakYolu, String ad) throws IOException {
        Path hedef = Path.of(System.getProperty("java.io.tmpdir"), "aksa-mailer-" + ad + ".ttf");
        if (Files.exists(hedef) && Files.size(hedef) > 0) {
            return hedef;   // ilk istekten sonra yeniden yazilmiyor
        }
        try (InputStream in = new ClassPathResource(kaynakYolu).getInputStream()) {
            Files.copy(in, hedef, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        hedef.toFile().deleteOnExit();
        return hedef;
    }
}
