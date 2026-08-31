// Editorun ust eylem seridi: kaydet, "Outlook Maili İndir", PDF/Yazdır.
//
// .eml dosyasi SUNUCUDAN indirilir (GET .../export.eml) - istemcide mail
// dosyasi kurulmaz. Gorseller cid: ile mailin icine gomulu geldigi icin bu
// tek guvenilir yol (docs/BRIEF.md, Kural 3).
//
// Yazdirma da AYNI HTML'i kullanir: onizleme panelinden gelen, sunucunun
// urettigi metin. Yazdirmak icin ikinci bir HTML kurulmaz (Kural 1).

import { useState } from 'react'
import { fetchEml, logDownload } from '../../lib/apiClient.js'
import Button from '../shared/Button.jsx'

export default function TopActions({
  taslak,
  belge,
  kaydediliyor,
  kaydedilmemis,
  hazir,
  onizlemeHtml,
  onKaydet,
}) {
  const [indiriliyor, setIndiriliyor] = useState(false)
  const [hata, setHata] = useState(null)

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
    logDownload(belge.id, 'PDF').catch(() => {
      // olcum kaydi; kullaniciyi ilgilendirmiyor
    })
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
