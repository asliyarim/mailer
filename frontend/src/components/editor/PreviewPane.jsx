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
  satirAdresiMi,
  secenekleriCoz,
  SECENEK_NITELIGI,
  BOLUM_NITELIGI,
} from '../../lib/icerikYolu.js'
import SatirIciDuzenleyici from './SatirIciDuzenleyici.jsx'
import OnizlemeKatmani from './OnizlemeKatmani.jsx'

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
  yenileme,
  onAlanDegisti,
  onSatirIslemi,
  onBolumeEkle,
}) {
  const [html, setHtml] = useState('')
  const [hata, setHata] = useState(null)
  const [yenileniyor, setYenileniyor] = useState(false)
  const [yukseklik, setYukseklik] = useState(VARSAYILAN_YUKSEKLIK)
  // Acik duzenleme kutusu: { adres, kutu, taslakDeger, secenekler }
  const [duzenlenen, setDuzenlenen] = useState(null)
  // Mailin ustundeki arac katmani: satir araclari + "+ satir ekle".
  const [katman, setKatman] = useState({ satirlar: [], bolumler: [] })
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

  /**
   * Arac katmanini olcer: her satirin dikdortgeni + gorsel komsulari,
   * her bolumun dikdortgeni.
   *
   * KOMSU NEDEN DOM'DAN: Sprint Kapanis satirlari sektore gore gruplaniyor,
   * yani icerikteki sira ile ekrandaki sira farkli (icerik 0,1,2 -> ekran
   * 0,2,1). "Bir asagi" icerikte degil EKRANDA bir asagi demek.
   *
   * Gruplar ayri tablo DEGIL, baslik satirlariyla ayrilmis bloklar. Bu
   * yuzden komsu araniyorsa kural su: hemen yanindaki <tr> de veri satiri
   * ise komsudur, degilse (baslik satiri) grup siniridir. Boylece gruplama
   * kuralini istemcide tekrarlamiyoruz - yapiyi oldugu gibi okuyoruz.
   */
  const katmaniOlc = useCallback(() => {
    const belge = cerceveRef.current?.contentDocument
    if (!belge || !onSatirIslemi) return

    function komsuAdres(tr, yon) {
      const komsu = yon === 'yukari' ? tr.previousElementSibling : tr.nextElementSibling
      if (!komsu || komsu.tagName !== 'TR') return null
      const adres = komsu.getAttribute(ALAN_NITELIGI)
      return adres && satirAdresiMi(adres) ? adres : null
    }

    // Panelden tasan araclar diger bilesenlerin ustune binmesin.
    const panel = cerceveRef.current?.closest('.panel--onizleme')
    const sinir = panel?.getBoundingClientRect()
    const gorunur = (kutu) =>
      !sinir || (kutu.top >= sinir.top - 8 && kutu.top <= sinir.bottom - 12)

    const satirlar = []
    let sira = 0
    for (const el of belge.querySelectorAll(`[${ALAN_NITELIGI}]`)) {
      const adres = el.getAttribute(ALAN_NITELIGI)
      if (!satirAdresiMi(adres)) continue
      sira += 1
      const kutu = kutuyuOlc(el)
      if (!kutu || !gorunur(kutu)) continue
      satirlar.push({
        adres,
        kutu,
        gorunenSira: sira,
        yukari: komsuAdres(el, 'yukari'),
        asagi: komsuAdres(el, 'asagi'),
      })
    }

    // Ayni bolum icin iki capa var: renkli BASLIK SERIDI ve (satir varsa)
    // tablo. ILKINI aliyoruz - "+ satir ekle" basligin sag ucuna oturuyor.
    //
    // Onceden sonuncusu aliniyordu ve dugme bolumun ALTINDA duruyordu;
    // orada bir sonraki bolumun basligina daha yakin goruunuyor ve hangi
    // bolume satir ekledigi anlasilmiyordu (yonetici geri bildirimi).
    const ilkCapa = new Map()
    for (const el of belge.querySelectorAll(`[${BOLUM_NITELIGI}]`)) {
      const key = el.getAttribute(BOLUM_NITELIGI)
      if (!ilkCapa.has(key)) ilkCapa.set(key, el)
    }
    const bolumler = []
    for (const [key, el] of ilkCapa) {
      const kutu = kutuyuOlc(el)
      if (!kutu || !gorunur(kutu)) continue
      // Baslik metni dugmenin erisilebilirlik etiketinde kullaniliyor -
      // "hangi bolume ekliyorum" sorusu ekran okuyucuda da cevapli olsun.
      bolumler.push({ key, kutu, baslik: el.textContent?.trim().slice(0, 60) ?? '' })
    }

    setKatman({ satirlar, bolumler })
  }, [kutuyuOlc, onSatirIslemi])

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
        })
        .catch((e) => {
          if (iptal) return
          setHata(e.message)
          setHtml('')
        })
        .finally(() => {
          if (!iptal) setYenileniyor(false)
        })
    }, GECIKME_MS)

    return () => {
      iptal = true
      clearTimeout(zamanlayici)
    }
  }, [teamId, templateType, content, onizlenebilir, duzenlenen, yenileme])

  // --- tiklama: alani yakala ------------------------------------------------

  const cerceveYuklendi = useCallback(() => {
    yuksekligiOlc()
    katmaniOlc()

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

      // SATIRIN KENDISI: <tr> de adresli. Bu adres bir NESNEYI gosterir;
      // duzenleme kutusu acilirsa "[object Object]" yazar ve kaydedilirse
      // satirin tamami bir metinle ezilirdi. Satir islemleri, satirin
      // yanindaki arac cubugundan yapiliyor - tiklama bir sey yapmiyor.
      if (satirAdresiMi(adres)) return

      const kutu = kutuyuOlc(hedef)
      if (!kutu) return
      hedefRef.current = hedef

      // Gizli alanlar (karar numarasi) mailde CIZILIRKEN uretiliyor -
      // duzenlenirse yazilan deger bir sonraki cizimde ezilir. Sunucu artik
      // adres basmiyor ama filtre duruyor: yarin baska bir uretilen sutun
      // eklenirse burasi hazir.
      const sonParca = adres.split('.').pop()
      if (GIZLI_ALANLAR.includes(sonParca)) return

      // Beklenmedik bir adres metin disinda bir sey gosteriyorsa dokunma.
      const deger = alandanOku(content, adres)
      if (deger !== undefined && typeof deger !== 'string') return

      setDuzenlenen({
        adres,
        kutu,
        taslakDeger: deger ?? '',
        // Secenek listesi SUNUCUDAN geliyor - istemcide ikinci kez yazilmiyor.
        secenekler: secenekleriCoz(hedef.getAttribute(SECENEK_NITELIGI)),
      })
    })
  }, [yuksekligiOlc, katmaniOlc, kutuyuOlc, onAlanDegisti, content])

  // Panel kaydirilinca hem duzenleme kutusu hem arac katmani elemanlarla
  // birlikte gitsin. Kaydirma sik tetikleniyor - olcumu bir sonraki cizim
  // karesine biraktik, yoksa her piksel icin yeniden olculurdu.
  useEffect(() => {
    let bekleyen = 0
    function yenidenKonumla() {
      if (bekleyen) return
      bekleyen = window.requestAnimationFrame(() => {
        bekleyen = 0
        const kutu = kutuyuOlc(hedefRef.current)
        if (kutu) setDuzenlenen((o) => (o ? { ...o, kutu } : o))
        katmaniOlc()
      })
    }
    window.addEventListener('resize', yenidenKonumla)
    window.addEventListener('scroll', yenidenKonumla, true)
    return () => {
      if (bekleyen) window.cancelAnimationFrame(bekleyen)
      window.removeEventListener('resize', yenidenKonumla)
      window.removeEventListener('scroll', yenidenKonumla, true)
    }
  }, [kutuyuOlc, katmaniOlc])

  /**
   * Satir islemi: yukari / asagi / ekle / kopyala / sil.
   *
   * Yukari-asagi EKRANDAKI komsuyla takas ediliyor (satir.yukari/asagi),
   * icerikteki komsuyla degil: Kapanis tablosu sektore gore grupladigi icin
   * ikisi ayni sey degil.
   */
  function satirIslemiYap(satir, islem) {
    if (islem === 'sil') {
      const onay = window.confirm('Bu satır silinecek. Devam edilsin mi?')
      if (!onay) return
    }
    if (islem === 'yukari' || islem === 'asagi') {
      const hedefAdres = islem === 'yukari' ? satir.yukari : satir.asagi
      if (!hedefAdres) return
      onSatirIslemi?.(satir.adres, 'takas', hedefAdres)
      return
    }
    onSatirIslemi?.(satir.adres, islem)
  }

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

      {onizlenebilir && onSatirIslemi && (
        <OnizlemeKatmani
          satirlar={katman.satirlar}
          bolumler={katman.bolumler}
          onSatirIslemi={satirIslemiYap}
          onBolumeEkle={onBolumeEkle}
        />
      )}

      {duzenlenen && (
        <SatirIciDuzenleyici
          etiket={alanEtiketi(duzenlenen.adres)}
          deger={duzenlenen.taslakDeger}
          kutu={duzenlenen.kutu}
          secenekler={duzenlenen.secenekler}
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
