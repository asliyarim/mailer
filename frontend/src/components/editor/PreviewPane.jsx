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

// Her tusa basista istek atmamak icin bekleme suresi (docs/BRIEF.md §7).
const GECIKME_MS = 300

export default function PreviewPane({ teamId, templateType, content }) {
  const [html, setHtml] = useState('')
  const [hata, setHata] = useState(null)

  useEffect(() => {
    if (teamId == null) return

    let iptal = false
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
          // Onizleme BOSALTILIYOR. Eski HTML'i birakmak, formda olmayan bir
          // maili gostermek demek - onizlemenin yalan soylemesi. Projenin
          // butun mimarisi bunu onlemek uzerine kurulu (Mimari Kural 1);
          // hata durumunda da ayni ilke gecerli.
          setHtml('')
        })
    }, GECIKME_MS)

    return () => {
      iptal = true
      clearTimeout(zamanlayici)
    }
  }, [teamId, templateType, content])

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      {hata && (
        <div style={{ padding: 12, borderBottom: '1px solid #d9e0e7', background: '#f5dedb' }}>
          <p style={{ color: '#9c3226', margin: 0, fontWeight: 600 }}>Önizleme üretilemedi</p>
          <p style={{ color: '#9c3226', margin: '4px 0 0', fontSize: 13.5 }}>{hata}</p>
        </div>
      )}
      <iframe
        title="Mail önizlemesi"
        srcDoc={html}
        // sandbox: onizlenen HTML'in uygulamanin oturumuna erisememesi icin.
        sandbox=""
        style={{ width: '100%', height: 'calc(100vh - 120px)', border: 0, background: '#fff' }}
      />
    </div>
  )
}
