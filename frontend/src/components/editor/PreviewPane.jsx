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

import { useEffect, useState } from 'react'
import { renderPreview } from '../../lib/apiClient.js'
import { validateContent } from '../../lib/mailContent.js'

// Her tusa basista istek atmamak icin bekleme suresi (docs/BRIEF.md §7).
const GECIKME_MS = 300

export default function PreviewPane({ teamId, templateType, content, onHtml }) {
  const [html, setHtml] = useState('')
  const [hata, setHata] = useState(null)
  const [yenileniyor, setYenileniyor] = useState(false)

  // Yeni belge sunucuda BOS header ile dogar (VarsayilanIcerik -> MailContent.bos()),
  // onizleme ucu ise baslik ve takim adini zorunlu tutar. Yani belge acilir
  // acilmaz istek atarsak, kullanici daha hicbir sey yazmadan 400 yiyoruz ve
  // ekranda kirmizi hata kutusu cikiyor. Oysa ortada ariza yok - form henuz
  // bos. Ayni kurali ISTEMCIDE onceden isletip istegi hic atmiyoruz.
  //
  // Kurali burada TEKRAR YAZMIYORUZ: validateContent tek kaynak, sunucudaki
  // MailContentValidator'in istemci karsiligi. Nihai soz yine sunucunun.
  const eksikler = validateContent(content)
  const hazir = eksikler.length === 0

  useEffect(() => {
    if (teamId == null) return undefined

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
          //
          // Hata mesaji zaten ustteki kirmizi kutuda; eski maili tutmak
          // bilgi katmiyor, yaniltiyor (Mimari Kural 1).
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

  return (
    <>
      <div className="onizleme-ust">
        <span>Canlı mail önizlemesi</span>
        <span className="onizleme-ust__durum">
          {!hazir
            ? 'Alanlar dolduruldukça belirir'
            : yenileniyor
              ? 'Güncelleniyor…'
              : 'Outlook’ta göreceğiniz hâli'}
        </span>
      </div>

      {/* Onizleme uretilemediginde cerceve BOSALIR (bkz. yukaridaki catch) -
          hatanin ne oldugunu burada soyluyoruz, yoksa kullanici bos ekranin
          sebebini bilemez. */}
      {hata && (
        <div className="uyari uyari--hata" style={{ maxWidth: 820, margin: '0 auto 10px' }}>
          Önizleme üretilemedi: {hata}
        </div>
      )}

      {/* Icerik henuz eksikken kirmizi hata yerine ne gerektigini soyluyoruz.
          Cerceve yerinde kaliyor ki alanlar dolunca duzen zıplamasin. */}
      {!hazir && (
        <div className="mail-cerceve">
          <div className="bos-durum" style={{ padding: '56px 24px' }}>
            <p className="bos-durum__baslik">Önizleme birkaç alan bekliyor</p>
            <p className="bos-durum__metin">
              Mail bu alanlar olmadan üretilemiyor. Soldaki kartları doldurdukça
              önizleme kendiliğinden belirir.
            </p>
            <ul
              style={{
                display: 'inline-block',
                textAlign: 'left',
                margin: 0,
                paddingLeft: 18,
                fontSize: 13,
                lineHeight: 1.7,
              }}
            >
              {eksikler.map((eksik) => (
                <li key={eksik}>{eksik}</li>
              ))}
            </ul>
          </div>
        </div>
      )}

      <div className="mail-cerceve" style={hazir ? undefined : { display: 'none' }}>
        <iframe
          title="Mail önizlemesi"
          srcDoc={html}
          // sandbox: onizlenen HTML'in uygulamanin oturumuna erisememesi icin.
          sandbox=""
        />
      </div>
    </>
  )
}
