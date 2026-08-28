// Editorun ust eylem seridi: kaydet, "Outlook Maili İndir", PDF/Yazdır.
//
// .eml dosyasi SUNUCUDAN indirilir (GET .../export.eml) - istemcide mail
// dosyasi kurulmaz. Gorseller cid: ile mailin icine gomulu geldigi icin bu
// tek guvenilir yol (docs/BRIEF.md, Kural 3).

import { useState } from 'react'
import { fetchEml, logDownload } from '../../lib/apiClient.js'
import Button from '../shared/Button.jsx'

export default function TopActions({ belge, kaydediliyor, kaydedilmemis, onKaydet, onListe }) {
  const [indiriliyor, setIndiriliyor] = useState(false)
  const [hata, setHata] = useState(null)

  async function emlIndir() {
    setHata(null)
    setIndiriliyor(true)
    let url
    try {
      const blob = await fetchEml(belge.id)
      url = URL.createObjectURL(blob)
      const baglanti = document.createElement('a')
      baglanti.href = url
      baglanti.download = `${belge.title || 'sprint-maili'}.eml`
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

  return (
    <div className="card">
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
        <Button varyant="sessiz" onClick={onListe}>
          ← Belgelerim
        </Button>

        <span style={{ flex: 1, minWidth: 120, fontWeight: 600 }}>
          {belge?.title ?? 'Yeni mail'}
          {kaydedilmemis && (
            <span style={{ fontWeight: 400, color: '#8a5a09' }}> · kaydedilmemiş değişiklik</span>
          )}
        </span>

        <Button varyant="birincil" onClick={onKaydet} disabled={kaydediliyor || !belge}>
          {kaydediliyor ? 'Kaydediliyor…' : 'Kaydet'}
        </Button>

        {/* Indirilen .eml SUNUCUDAKI kayitli surumden uretilir - ekrandaki
            kaydedilmemis degisiklikleri icermez. Kullaniciyi uyariyoruz. */}
        <Button onClick={emlIndir} disabled={!belge || indiriliyor || kaydedilmemis}>
          {indiriliyor ? 'Hazırlanıyor…' : 'Outlook Maili İndir'}
        </Button>

        {/* TODO (Sprint 2): PDF / Yazdır - önizleme iframe'inin print'i. */}
        <Button disabled>PDF / Yazdır</Button>
      </div>

      {kaydedilmemis && (
        <p style={{ margin: '8px 0 0', fontSize: 12.5, color: '#8a5a09' }}>
          İndirmeden önce kaydedin — mail dosyası sunucudaki kayıtlı sürümden üretilir.
        </p>
      )}

      {hata && <p style={{ margin: '8px 0 0', color: '#9c3226' }}>{hata}</p>}
    </div>
  )
}
