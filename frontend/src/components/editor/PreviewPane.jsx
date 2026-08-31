// Sag panel: canli mail onizlemesi.
//
// ================== BU DOSYADA HTML URETILMEZ ==================
// Gosterilen HTML sunucudan HAZIR gelir (POST /api/mailer/render/preview) ve
// oldugu gibi iframe srcdoc'una basilir. Onizleme, .eml icine giren HTML'in
// TA KENDISIDIR - baska bir sey degil.
//
// Buraya string birlestirme, sablon doldurma, "sadece onizleme icin" ufak bir
// duzeltme eklersen onizleme mailden ayrisir ve iki prototipte de yasanan
// hataya geri donulur (bkz. docs/BRIEF.md, Mimari Kural 1-2).
// Mailin gorunumunu degistirmek gerekiyorsa backend'deki tema/sablon
// siniflari duzenlenir.
// ===============================================================

import { useEffect, useRef, useState } from 'react'
import { renderPreview } from '../../lib/apiClient.js'
import { validateContent } from '../../lib/mailContent.js'

// Her tusa basista istek atmamak icin bekleme suresi (docs/BRIEF.md §7).
const GECIKME_MS = 300

// Onizleme cercevesi olculemezse dusulecek yukseklik.
const VARSAYILAN_YUKSEKLIK = 900

export default function PreviewPane({ teamId, templateType, content, hazirlaniyor, onHtml }) {
  const [html, setHtml] = useState('')
  const [hata, setHata] = useState(null)
  const [yenileniyor, setYenileniyor] = useState(false)
  const [yukseklik, setYukseklik] = useState(VARSAYILAN_YUKSEKLIK)
  const cerceveRef = useRef(null)

  /**
   * Mail ne kadar uzunsa cerceve o kadar uzasin - ic ice iki kaydirma
   * cubugu olmasin. Kullanici maili sayfayla birlikte kaydiriyor.
   *
   * Olcum icin iframe'in belgesini okumak gerekiyor, o yuzden sandbox
   * "allow-same-origin". SCRIPT CALISTIRMA IZNI VERILMIYOR (allow-scripts
   * yok): mail HTML'i zaten Outlook-guvenli, script icermiyor ve
   * icermemeli. Ikisi birlikte verilseydi onizlenen HTML uygulamanin
   * DOM'una ve oturumuna erisebilirdi - o yuzden ikisi birlikte ASLA.
   */
  function yuksekligiOlc() {
    const cerceve = cerceveRef.current
    try {
      const belge = cerceve?.contentDocument
      if (!belge) return
      const olculen = Math.max(
        belge.documentElement?.scrollHeight ?? 0,
        belge.body?.scrollHeight ?? 0
      )
      // +4: kenar yuvarlamalari yuzunden cikan 1-2 piksellik kaydirma cubugu.
      if (olculen > 0) setYukseklik(olculen + 4)
    } catch {
      // Olculemedi (tarayici kisitlamasi) - varsayilan yukseklikte kalir.
    }
  }

  // Onizleme ucu baslik ve takim adini zorunlu tutuyor (MailContentValidator).
  // Ayni kurali ISTEMCIDE onceden isletip eksikken istegi hic atmiyoruz:
  // yoksa kullanici daha hicbir sey yazmadan 400 yiyoruz ve ekranda kirmizi
  // hata kutusu cikiyor. Ortada ariza yok - form henuz eksik.
  //
  // Kurali burada TEKRAR YAZMIYORUZ: validateContent tek kaynak, sunucudaki
  // dogrulayicinin istemci karsiligi. Nihai soz yine sunucunun.
  const eksikler = teamId == null ? [] : validateContent(content)
  const hazir = teamId != null && eksikler.length === 0

  useEffect(() => {
    if (!hazir) {
      // Yarim icerigi gostermiyoruz; yazdirma da bosalsin ki formda
      // OLMAYAN bir mail yazdirilmasin.
      setHtml('')
      setHata(null)
      setYenileniyor(false)
      onHtml?.('')
      return undefined
    }

    let iptal = false
    setYenileniyor(true)
    const zamanlayici = setTimeout(() => {
      renderPreview({ teamId, templateType, content })
        .then((gelen) => {
          if (iptal) return
          setHtml(gelen)
          setHata(null)
          // Yazdirma ayni metni kullanir - ikinci uretim yok.
          onHtml?.(gelen)
        })
        .catch((e) => {
          if (iptal) return
          setHata(e.message)
          // Onizleme BOSALTILIYOR. Eski HTML'i ekranda birakmak, formda
          // OLMAYAN bir maili gostermek demek - onizlemenin yalan soylemesi.
          // Somut senaryo: surum gecmisinden bos bir surume geri alindiginda
          // form bosaliyor ama onizlemede eski dolu mail duruyordu.
          //
          // onHtml de temizleniyor: "PDF / Yazdir" ayni metni kullaniyor,
          // temizlenmezse formda olmayan mail YAZDIRILIR.
          setHtml('')
          onHtml?.('')
        })
        .finally(() => {
          if (!iptal) setYenileniyor(false)
        })
    }, GECIKME_MS)

    return () => {
      iptal = true
      clearTimeout(zamanlayici)
    }
  }, [teamId, templateType, content, onHtml, hazir])

  const durumMetni = hazirlaniyor
    ? 'Mail seçilmedi'
    : !hazir
      ? 'Alanlar dolduruldukça belirir'
      : yenileniyor
        ? 'Güncelleniyor…'
        : 'Outlook’ta göreceğiniz hâli'

  return (
    <>
      <div className="onizleme-ust">
        <span>Canlı mail önizlemesi</span>
        <span className="onizleme-ust__durum">{durumMetni}</span>
      </div>

      {/* Onizleme uretilemediginde cerceve BOSALIR (bkz. yukaridaki catch) -
          hatanin ne oldugunu burada soyluyoruz, yoksa kullanici bos ekranin
          sebebini bilemez. */}
      {hata && (
        <div className="uyari uyari--hata" style={{ maxWidth: 820, margin: '0 auto 12px' }}>
          Önizleme üretilemedi: {hata}
        </div>
      )}

      {/* Cerceve HER ZAMAN yerinde: mail alani ekranin sabit bir parcasi,
          icerik geldikce dolar. Alanlar dolunca duzen ziplamaz. */}
      <div className="mail-cerceve">
        {hazir ? (
          <iframe
            ref={cerceveRef}
            title="Mail önizlemesi"
            srcDoc={html}
            // allow-same-origin YALNIZCA yuksekligi olcebilmek icin.
            // allow-scripts BILEREK YOK - bkz. yuksekligiOlc().
            sandbox="allow-same-origin"
            style={{ height: yukseklik }}
            onLoad={yuksekligiOlc}
          />
        ) : (
          <div className="bos-durum" style={{ padding: '72px 24px' }}>
            <p className="bos-durum__baslik">
              {hazirlaniyor ? 'Mail önizlemesi burada görünecek' : 'Önizleme birkaç alan bekliyor'}
            </p>
            <p className="bos-durum__metin">
              {hazirlaniyor
                ? 'Soldan bir mail oluşturun ya da "Belgelerim"den mevcut bir maili açın.'
                : 'Mail bu alanlar olmadan üretilemiyor. Soldaki kartları doldurdukça önizleme kendiliğinden belirir.'}
            </p>
            {eksikler.length > 0 && (
              <ul
                style={{
                  display: 'inline-block',
                  textAlign: 'left',
                  margin: 0,
                  paddingLeft: 18,
                  fontSize: 13,
                  lineHeight: 1.7,
                  color: 'var(--soluk)',
                }}
              >
                {eksikler.map((eksik) => (
                  <li key={eksik}>{eksik}</li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>
    </>
  )
}
