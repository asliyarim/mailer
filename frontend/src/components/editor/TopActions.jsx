// Editorun ust eylem seridi: kaydet, "Outlook Maili İndir", PDF/Yazdır.
//
// .eml dosyasi SUNUCUDAN indirilir (GET .../export.eml) - istemcide mail
// dosyasi kurulmaz. Gorseller cid: ile mailin icine gomulu geldigi icin bu
// tek guvenilir yol (docs/BRIEF.md, Kural 3).

import { fetchEml, logDownload } from '../../lib/apiClient.js'
import Button from '../shared/Button.jsx'

export default function TopActions({ belge, kaydediliyor, onKaydet, onListe }) {
  async function emlIndir() {
    const blob = await fetchEml(belge.id)
    const url = URL.createObjectURL(blob)
    const baglanti = document.createElement('a')
    baglanti.href = url
    baglanti.download = `${belge.title || 'sprint-maili'}.eml`
    baglanti.click()
    URL.revokeObjectURL(url)
    await logDownload(belge.id, 'EML')
  }

  return (
    <div className="card" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <Button varyant="sessiz" onClick={onListe}>
        ← Belgelerim
      </Button>
      <span style={{ flex: 1 }}>{belge?.title ?? 'Yeni mail'}</span>
      <Button varyant="birincil" onClick={onKaydet} disabled={kaydediliyor || !belge}>
        {kaydediliyor ? 'Kaydediliyor…' : 'Kaydet'}
      </Button>
      <Button onClick={emlIndir} disabled={!belge}>
        Outlook Maili İndir
      </Button>
      {/* TODO (Sprint 2): PDF / Yazdır - onizleme iframe'inin print'i. */}
      <Button disabled>PDF / Yazdır</Button>
    </div>
  )
}
