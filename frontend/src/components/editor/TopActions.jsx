// Editorun ust eylem seridi: kaydet, "Outlook Maili İndir", PDF/Yazdır.
//
// .eml dosyasi SUNUCUDAN indirilir (GET .../export.eml) - istemcide mail
// dosyasi kurulmaz. Gorseller cid: ile mailin icine gomulu geldigi icin bu
// tek guvenilir yol (docs/BRIEF.md, Kural 3).
//
// Yazdirma da AYNI HTML'i kullanir: onizleme panelinden gelen, sunucunun
// urettigi metin. Yazdirmak icin ikinci bir HTML kurulmaz (Kural 1).

import { useState } from 'react'
import { fetchEml, logDownload, renderClipboard } from '../../lib/apiClient.js'
import Button from '../shared/Button.jsx'

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
  onizlemeHtml,
  onKaydet,
}) {
  const [indiriliyor, setIndiriliyor] = useState(false)
  const [kopyalaniyor, setKopyalaniyor] = useState(false)
  const [kopyalandi, setKopyalandi] = useState(false)
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
    let url
    try {
      const { blob, dosyaAdi } = await fetchEml(belge.id)
      url = URL.createObjectURL(blob)
      const baglanti = document.createElement('a')
      baglanti.href = url
      // Ad SUNUCUDAN gelir: orada bilerek ASCII'ye indirgeniyor, cunku bazi
      // istemciler UTF-8 dosya adini bozuyor. Buradan "belge.title" yazmak
      // o karari bosa cikarirdi. Baslik okunamazsa yedege duseriz.
      baglanti.download = dosyaAdi ?? `${belge.title || 'sprint-maili'}.eml`
      document.body.appendChild(baglanti)
      baglanti.click()
      baglanti.remove()
      // Log basarisiz olsa da indirme bozulmamali - ayri try.
      try {
        await logDownload(belge.id, 'EML')
      } catch {
        // olcum kaydi; kullaniciyi ilgilendirmiyor
      }
    } catch (e) {
      setHata(e.message)
    } finally {
      // revokeObjectURL'i hemen cagirmak bazi tarayicilarda indirmeyi iptal
      // ediyor; tarayicinin dosyayi almasi icin kisa bir sure biraktik.
      if (url) setTimeout(() => URL.revokeObjectURL(url), 1000)
      setIndiriliyor(false)
    }
  }

  /**
   * PDF / Yazdır.
   *
   * Onizleme iframe'inin uzerinden print() CAGRILAMAZ: iframe sandbox=""
   * ile calisiyor, yani script yok ve ayni kaynak erisimi yok - bu bilerek
   * boyle, onizlenen HTML uygulamanin oturumuna erisemesin diye.
   * O yuzden ayni HTML yeni bir pencereye yazilip orada yazdiriliyor.
   *
   * Onizleme kaydedilmemis icerigi de gosterdigi icin cikti EKRANDA GORULEN
   * mailin aynisidir - .eml'den farkli olarak sunucudaki kayitli surumu
   * degil, o anki hali yazdirir.
   */
  function yazdir() {
    setHata(null)
    if (!onizlemeHtml) {
      setHata('Önizleme henüz hazır değil. Birkaç saniye sonra tekrar deneyin.')
      return
    }

    const pencere = window.open('', '_blank', 'width=900,height=1000')
    if (!pencere) {
      setHata('Tarayıcı yeni pencereyi engelledi. Bu site için açılır pencerelere izin verin.')
      return
    }

    pencere.document.write(onizlemeHtml)
    pencere.document.close()
    pencere.focus()

    // Gorseller cozulmeden print() cagirilirsa cikti bos kutularla gelir.
    // Yukleme bittiyse hemen, bitmediyse load olayinda yazdir.
    function yazdirmayiBaslat() {
      pencere.print()
    }
    if (pencere.document.readyState === 'complete') yazdirmayiBaslat()
    else pencere.addEventListener('load', yazdirmayiBaslat, { once: true })

    // Pencereyi kapatmiyoruz: kullanici yazdirmayi iptal edip tekrar
    // deneyebilir ya da PDF olarak kaydetme yerini secebilir.
    // Taslakta belge yok - olculecek bir kayit da yok.
    if (belge) {
      logDownload(belge.id, 'PDF').catch(() => {
        // olcum kaydi; kullaniciyi ilgilendirmiyor
      })
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

        <Button onClick={yazdir} disabled={!onizlemeHtml}>
          PDF / Yazdır
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
