// Sag panel: canli mail onizlemesi + satir ici duzenleme.
//
// ================== BU DOSYADA HTML URETILMEZ ==================
// Gosterilen HTML sunucudan HAZIR gelir (POST /api/mailer/render/preview) ve
// oldugu gibi iframe srcdoc'una basilir. Onizleme, .eml icine giren HTML'in
// TA KENDISIDIR - tek fark gorsellerin gomulu olmasi ve duzenleme
// adreslerinin eklenmis olmasi; ikisi de sunucuda, kilitli.
//
// Buraya string birlestirme, sablon doldurma, "sadece onizleme icin" ufak bir
// duzeltme eklersen onizleme mailden ayrisir ve iki prototipte de yasanan
// hataya geri donulur (bkz. docs/BRIEF.md, Mimari Kural 1-2).
// ===============================================================
//
// SATIR ICI DUZENLEME: sunucu her duzenlenebilir alana data-alan basiyor.
// Kullanici onizlemede bir alana tikliyor, biz alanin ustune bir kutu
// aciyoruz ve yazilan deger AYNI React state'ine gidiyor. iframe'in icine
// yazilmiyor, contenteditable kullanilmiyor, script izni verilmiyor.

import { useCallback, useEffect, useRef, useState } from 'react'
import { renderPreview } from '../../lib/apiClient.js'
import { validateContent, GIZLI_ALANLAR } from '../../lib/mailContent.js'
import {
  alandanOku,
  alanEtiketi,
  ALAN_NITELIGI,
  cokSatirliMi,
} from '../../lib/icerikYolu.js'
import SatirIciDuzenleyici from './SatirIciDuzenleyici.jsx'

// Her tusa basista istek atmamak icin bekleme suresi (docs/BRIEF.md §7).
const GECIKME_MS = 300

// Onizleme cercevesi olculemezse dusulecek yukseklik.
const VARSAYILAN_YUKSEKLIK = 900

/**
 * Onizlemenin icine enjekte edilen stil.
 *
 * Mail HTML'ini DEGISTIRMIYOR: yalnizca duzenlenebilir alanlarin uzerine
 * gelince belli olmasini sagliyor. Bu olmadan ozellik kesfedilemez -
 * kullanici tiklanabilecegini bilemez. .eml'e giden HTML'e dokunulmuyor,
 * bu stil yalnizca tarayicidaki onizleme belgesinde yasiyor.
 *
 * ================= BURAYA YALNIZCA GORUNUM IPUCU GIRER =================
 * Izin verilen: cursor, outline, outline-offset - DUZENI ETKILEMEZLER.
 *
 * YASAK: margin, padding, font, line-height, width, display, visibility ve
 * duzeni etkileyen her sey. Boyle bir sey eklenirse kullanici onizlemede
 * bir sey gorup Outlook'ta baskasini alir - yani onizleme yalan soyler.
 * Sunucudaki iki kontrollu fark (gomulu gorseller, duzenleme adresleri)
 * testlerle kilitli; bu dosya o kilidin disinda kalan tek yer, o yuzden
 * sinir burada YAZIYLA duruyor.
 * =======================================================================
 */
const DUZENLEME_STILI = `
  [${ALAN_NITELIGI}] { cursor: text; outline-offset: 2px; }
  [${ALAN_NITELIGI}]:hover { outline: 2px dashed rgba(11,74,162,.55); }
`

export default function PreviewPane({
  teamId,
  templateType,
  content,
  hazirlaniyor,
  onHtml,
  onAlanDegisti,
}) {
  const [html, setHtml] = useState('')
  const [hata, setHata] = useState(null)
  const [yenileniyor, setYenileniyor] = useState(false)
  const [yukseklik, setYukseklik] = useState(VARSAYILAN_YUKSEKLIK)
  // Acik duzenleme kutusu: { adres, kutu, taslakDeger }
  const [duzenlenen, setDuzenlenen] = useState(null)
  const cerceveRef = useRef(null)
  // Duzenlenen elemanin iframe icindeki dugumu - kaydirmada yeniden olcmek icin.
  const hedefRef = useRef(null)

  const eksikler = validateContent(content)
  const onizlenebilir = eksikler.length === 0 && teamId != null && !hazirlaniyor

  /**
   * Mail ne kadar uzunsa cerceve o kadar uzasin - ic ice iki kaydirma
   * cubugu olmasin. Kullanici maili sayfayla birlikte kaydiriyor.
   *
   * Olcum icin iframe'in belgesini okumak gerekiyor, o yuzden sandbox
   * "allow-same-origin". SCRIPT CALISTIRMA IZNI VERILMIYOR (allow-scripts
   * yok): ikisi birlikte verilseydi onizlenen HTML uygulamanin DOM'una ve
   * oturumuna erisebilirdi.
   */
  const yuksekligiOlc = useCallback(() => {
    const cerceve = cerceveRef.current
    try {
      const belge = cerceve?.contentDocument
      if (!belge) return
      const olculen = Math.max(
        belge.documentElement?.scrollHeight ?? 0,
        belge.body?.scrollHeight ?? 0
      )
      if (olculen > 0) setYukseklik(olculen + 4)
    } catch {
      // Olculemedi (tarayici kisitlamasi) - varsayilan yukseklikte kalir.
    }
  }, [])

  /** Iframe icindeki elemanin ana penceredeki yerini hesaplar. */
  const kutuyuOlc = useCallback((eleman) => {
    const cerceve = cerceveRef.current
    if (!cerceve || !eleman) return null
    const c = cerceve.getBoundingClientRect()
    const e = eleman.getBoundingClientRect()
    return { top: c.top + e.top, left: c.left + e.left, width: e.width, height: e.height }
  }, [])

  // --- onizlemeyi getir -----------------------------------------------------

  useEffect(() => {
    if (!onizlenebilir) return undefined
    // Duzenleme kutusu acikken YENILEME YOK: iframe yeniden yuklenirse
    // kutunun altindaki eleman kaybolur ve kutu bosluga bakar.
    if (duzenlenen) return undefined

    let iptal = false
    setYenileniyor(true)
    const zamanlayici = setTimeout(() => {
      renderPreview({ teamId, templateType, content })
        .then((gelen) => {
          if (iptal) return
          setHtml(gelen)
          setHata(null)
          onHtml?.(gelen)
        })
        .catch((e) => {
          if (iptal) return
          setHata(e.message)
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
  }, [teamId, templateType, content, onHtml, onizlenebilir, duzenlenen])

  // --- tiklama: alani yakala ------------------------------------------------

  const cerceveYuklendi = useCallback(() => {
    yuksekligiOlc()

    const belge = cerceveRef.current?.contentDocument
    if (!belge || !onAlanDegisti) return

    // Duzenlenebilir alanlari gorunur kil (yalnizca onizleme belgesinde).
    const stil = belge.createElement('style')
    stil.textContent = DUZENLEME_STILI
    belge.head?.appendChild(stil)

    belge.addEventListener('click', (e) => {
      const hedef = e.target?.closest?.(`[${ALAN_NITELIGI}]`)
      if (!hedef) return
      const adres = hedef.getAttribute(ALAN_NITELIGI)
      if (!adres) return

      // Bagimsiz sekmeye gitmesin, kart butonu tiklaninca gezinmesin.
      e.preventDefault()

      // Gizli alanlar (karar numarasi) mailde CIZILIRKEN uretiliyor -
      // duzenlenirse yazilan deger bir sonraki cizimde ezilir.
      const sonParca = adres.split('.').pop()
      if (GIZLI_ALANLAR.includes(sonParca)) return

      const kutu = kutuyuOlc(hedef)
      if (!kutu) return
      hedefRef.current = hedef
      setDuzenlenen({ adres, kutu, taslakDeger: alandanOku(content, adres) ?? '' })
    })
  }, [yuksekligiOlc, kutuyuOlc, onAlanDegisti, content])

  // Panel kaydirilinca kutu elemanla birlikte gitsin.
  useEffect(() => {
    if (!duzenlenen) return undefined
    function yenidenKonumla() {
      const kutu = kutuyuOlc(hedefRef.current)
      if (kutu) setDuzenlenen((o) => (o ? { ...o, kutu } : o))
    }
    window.addEventListener('resize', yenidenKonumla)
    window.addEventListener('scroll', yenidenKonumla, true)
    return () => {
      window.removeEventListener('resize', yenidenKonumla)
      window.removeEventListener('scroll', yenidenKonumla, true)
    }
  }, [duzenlenen, kutuyuOlc])

  function duzenlemeyiBitir() {
    if (!duzenlenen) return
    const oncekiDeger = alandanOku(content, duzenlenen.adres) ?? ''
    if (duzenlenen.taslakDeger !== oncekiDeger) {
      onAlanDegisti(duzenlenen.adres, duzenlenen.taslakDeger)
    }
    hedefRef.current = null
    setDuzenlenen(null)
  }

  return (
    <>
      <div className="onizleme-ust">
        <span>Canlı mail önizlemesi</span>
        <span className="onizleme-ust__durum">
          {yenileniyor
            ? 'Güncelleniyor…'
            : onAlanDegisti
              ? 'Düzenlemek için mailin üstüne tıklayın'
              : 'Outlook’ta göreceğiniz hâli'}
        </span>
      </div>

      {hata && (
        <div className="uyari uyari--hata" style={{ maxWidth: 900, margin: '0 auto 10px' }}>
          Önizleme üretilemedi: {hata}
        </div>
      )}

      {!onizlenebilir && !hazirlaniyor && (
        <div className="mail-cerceve">
          <div className="bos-durum" style={{ padding: 40 }}>
            <p className="bos-durum__baslik">Önizleme birkaç alan bekliyor</p>
            <p className="bos-durum__metin">
              Mail bu alanlar olmadan üretilemiyor. Soldaki kartları doldurdukça
              önizleme kendiliğinden belirir.
            </p>
            <ul style={{ textAlign: 'left', maxWidth: 320, margin: '0 auto', color: '#68778a' }}>
              {eksikler.map((e) => (
                <li key={e}>{e}</li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {onizlenebilir && (
        <div className="mail-cerceve">
          <iframe
            ref={cerceveRef}
            title="Mail önizlemesi"
            srcDoc={html}
            // allow-same-origin: yukseklik olcumu ve tiklama yakalama icin.
            // allow-scripts BILEREK YOK - onizlenen HTML kod calistiramaz.
            sandbox="allow-same-origin"
            style={{ height: yukseklik }}
            onLoad={cerceveYuklendi}
          />
        </div>
      )}

      {duzenlenen && (
        <SatirIciDuzenleyici
          etiket={alanEtiketi(duzenlenen.adres)}
          deger={duzenlenen.taslakDeger}
          kutu={duzenlenen.kutu}
          cokSatirli={cokSatirliMi(duzenlenen.adres)}
          onDegisti={(deger) => setDuzenlenen((o) => ({ ...o, taslakDeger: deger }))}
          onBitti={duzenlemeyiBitir}
          onVazgec={() => {
            hedefRef.current = null
            setDuzenlenen(null)
          }}
        />
      )}
    </>
  )
}
