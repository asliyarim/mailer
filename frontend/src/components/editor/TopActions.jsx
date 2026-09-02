// Editorun ust eylem seridi: kaydet, "Outlook Maili İndir", "Outlook İçin
// Kopyala", "PDF İndir".
//
// UC CIKTININ UCU DE SUNUCUDAN GELIR - istemcide ne mail dosyasi kurulur ne
// PDF cizilir (Mimari Kural 1):
//   .eml  GET  /documents/{id}/export.eml   gorseller cid: ile gomulu
//   pano  POST /render/clipboard            duzenleme nitelikleri soyulmus
//   PDF   POST /render/pdf                  yazi tipi gomulu (Turkce harfler)

import { useState } from 'react'
import { fetchEml, logDownload, renderClipboard, renderPdf } from '../../lib/apiClient.js'
import Button from '../shared/Button.jsx'

/**
 * Blob'u dosya olarak indirtir.
 *
 * revokeObjectURL HEMEN cagrilmaz: bazi tarayicilarda indirme daha
 * baslamadan adres gecersiz olup dosya yarim kaliyor.
 */
function dosyaIndir(blob, ad) {
  const url = URL.createObjectURL(blob)
  const baglanti = document.createElement('a')
  baglanti.href = url
  baglanti.download = ad
  document.body.appendChild(baglanti)
  baglanti.click()
  baglanti.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/**
 * Panonun DUZ METIN karsiligi.
 *
 * HTML kabul etmeyen hedeflere (Not Defteri, sohbet kutulari) yapistirinca
 * hicbir sey cikmasin diye. Tarayicinin kendi ayristiricisini kullaniyoruz -
 * elle regex ile etiket soymak ic ice tablolarda yanlis sonuc verir.
 */
function duzMetin(html) {
  try {
    const belge = new DOMParser().parseFromString(html, 'text/html')
    return (belge.body?.innerText ?? belge.body?.textContent ?? '')
      .replace(/\n{3,}/g, '\n\n')
      .trim()
  } catch {
    return ''
  }
}

export default function TopActions({
  taslak,
  belge,
  teamId,
  templateType,
  content,
  kaydediliyor,
  kaydedilmemis,
  hazir,
  onKaydet,
}) {
  const [indiriliyor, setIndiriliyor] = useState(false)
  const [kopyalaniyor, setKopyalaniyor] = useState(false)
  const [kopyalandi, setKopyalandi] = useState(false)
  const [pdfHazirlaniyor, setPdfHazirlaniyor] = useState(false)
  const [hata, setHata] = useState(null)

  /**
   * "Outlook İçin Kopyala" - maili PANOYA koyar, kullanıcı Outlook'ta
   * yeni mail açıp yapıştırır.
   *
   * Panoya HTML olarak konuyor (text/html); düz metin karşılığı da
   * ekleniyor, çünkü bazı hedefler (Not Defteri, sohbet kutuları) HTML
   * kabul etmiyor ve o zaman hiçbir şey yapışmıyor.
   *
   * DİKKAT: kaydedilmemiş değişiklikleri de kopyalar - .eml'den farklı
   * olarak sunucudaki kayıtlı sürümü değil, ekranda gördüğünü verir.
   */
  async function panoyaKopyala() {
    setHata(null)
    setKopyalandi(false)

    if (!navigator.clipboard?.write || typeof ClipboardItem === 'undefined') {
      setHata('Tarayıcınız panoya HTML kopyalamayı desteklemiyor. "Outlook Maili İndir" kullanın.')
      return
    }

    setKopyalaniyor(true)
    try {
      const html = await renderClipboard({ teamId, templateType, content })
      await navigator.clipboard.write([
        new ClipboardItem({
          'text/html': new Blob([html], { type: 'text/html' }),
          'text/plain': new Blob([duzMetin(html)], { type: 'text/plain' }),
        }),
      ])
      setKopyalandi(true)
      // Onay iki saniye gorunsun, sonra dugme normale donsun.
      setTimeout(() => setKopyalandi(false), 2000)

      if (belge) {
        logDownload(belge.id, 'KOPYALA').catch(() => {
          // olcum kaydi; kullaniciyi ilgilendirmiyor
        })
      }
    } catch (e) {
      // Pano izni reddedilmis olabilir - kullaniciya yolu gosterelim.
      setHata(
        e.name === 'NotAllowedError'
          ? 'Tarayıcı panoya erişime izin vermedi. Sayfaya bir kez tıklayıp tekrar deneyin.'
          : e.message
      )
    } finally {
      setKopyalaniyor(false)
    }
  }

  async function emlIndir() {
    setHata(null)
    setIndiriliyor(true)
    try {
      const { blob, dosyaAdi } = await fetchEml(belge.id)
      // Ad SUNUCUDAN gelir: orada bilerek ASCII'ye indirgeniyor, cunku bazi
      // istemciler UTF-8 dosya adini bozuyor. Buradan "belge.title" yazmak
      // o karari bosa cikarirdi. Baslik okunamazsa yedege duseriz.
      dosyaIndir(blob, dosyaAdi ?? `${belge.title || 'sprint-maili'}.eml`)

      // Log basarisiz olsa da indirme bozulmamali - ayri try.
      try {
        await logDownload(belge.id, 'EML')
      } catch {
        // olcum kaydi; kullaniciyi ilgilendirmiyor
      }
    } catch (e) {
      setHata(e.message)
    } finally {
      setIndiriliyor(false)
    }
  }

  /**
   * PDF İndir.
   *
   * Yazdırma penceresi AÇILMIYOR: dosya doğrudan iniyor. Önceden
   * window.print() ile tarayıcının yazdırma penceresi açılıyordu ve
   * kullanıcının oradan "PDF olarak kaydet"i seçmesi gerekiyordu; ayrıca
   * arka plan renkleri o pencerede kullanıcı ayarına kalıyordu.
   *
   * PDF'i SUNUCU üretiyor (aynı HTML'den, yazı tipi gömülü). İstemcide
   * üretilseydi mail rasterize edilirdi - bulanık yazı ve ikinci bir çizim.
   *
   * .eml'den farkı: kaydedilmemiş içerikten üretilir, yani ekranda ne
   * görüyorsan o iner. Önizleme ve pano da böyle çalışıyor.
   */
  async function pdfIndir() {
    setHata(null)
    if (!hazir) return

    setPdfHazirlaniyor(true)
    try {
      const { blob, dosyaAdi } = await renderPdf({ teamId, templateType, content })
      // Ad sunucudan gelir (ASCII'ye indirgenmiş). Gelmezse yedek.
      dosyaIndir(blob, dosyaAdi ?? `${belge?.title || 'sprint-maili'}.pdf`)

      // Taslakta belge yok - ölçülecek kayıt da yok.
      if (belge) {
        logDownload(belge.id, 'PDF').catch(() => {
          // olcum kaydi; kullaniciyi ilgilendirmiyor
        })
      }
    } catch (e) {
      setHata(e.message)
    } finally {
      setPdfHazirlaniyor(false)
    }
  }

  return (
    <div className="arac-serit">
      <div className="arac-serit__satir">
        {/* "Belgelerim" ust seritte - burada yeri yok, bu serit belgenin
            kendisiyle ilgili eylemleri tasiyor. */}
        <span className="arac-serit__ad" title={belge?.title}>
          {belge?.title ?? 'Yeni mail'}
        </span>

        {/* Taslakta da Kaydet acik: belge zaten o an dogacak. */}
        <Button varyant="birincil" onClick={onKaydet} disabled={kaydediliyor || !hazir}>
          {kaydediliyor ? 'Kaydediliyor…' : taslak ? 'Kaydet ve oluştur' : 'Kaydet'}
        </Button>

        {/* Indirilen .eml SUNUCUDAKI kayitli surumden uretilir - ekrandaki
            kaydedilmemis degisiklikleri icermez. Kullaniciyi uyariyoruz. */}
        <Button
          onClick={emlIndir}
          disabled={!belge || indiriliyor || kaydedilmemis}
          baslik={
            taslak
              ? 'Mail dosyası kayıtlı sürümden üretilir - önce kaydedin.'
              : kaydedilmemis
                ? 'Önce kaydedin: mail dosyası sunucudaki kayıtlı sürümden üretilir.'
                : undefined
          }
        >
          {indiriliyor ? 'Hazırlanıyor…' : 'Outlook Maili İndir'}
        </Button>

        <Button onClick={panoyaKopyala} disabled={!hazir || kopyalaniyor}>
          {kopyalandi ? '✓ Kopyalandı' : kopyalaniyor ? 'Hazırlanıyor…' : 'Outlook İçin Kopyala'}
        </Button>

        {/* Ilk PDF istegi yavas olabilir (yazi tipi ve PDF motoru isiniyor) -
            durum gostermezsek kullanici ikinci kez basiyor. */}
        <Button onClick={pdfIndir} disabled={!hazir || pdfHazirlaniyor}>
          {pdfHazirlaniyor ? 'PDF hazırlanıyor…' : 'PDF İndir'}
        </Button>
      </div>

      {taslak ? (
        <p className="alan__ipucu" style={{ marginTop: 8 }}>
          Bu mail henüz kaydedilmedi. Kaydettiğinizde mail listenize eklenir ve
          Outlook dosyası indirilebilir hâle gelir.
        </p>
      ) : (
        kaydedilmemis && (
          <p className="alan__ipucu" style={{ marginTop: 8 }}>
            Kaydedilmemiş değişiklikleriniz var. Önizleme ve yazdırma bunları gösterir;
            Outlook maili ise kayıtlı sürümden üretilir.
          </p>
        )
      )}

      {hata && (
        <div className="uyari uyari--hata" style={{ marginTop: 8 }}>
          {hata}
        </div>
      )}
    </div>
  )
}
