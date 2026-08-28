// Sürüm geçmişi ve geri alma.
//
// Geri alma GEÇMİŞİ SİLMEZ: eski içerik yeni bir sürüm olarak yazılır, yani
// geri alma da geri alınabilir (docs/api.md §7). Listedeki sürüm sayısı
// geri aldıkça artar - beklenen davranış.

import { useEffect, useState } from 'react'
import { fetchVersions, rollbackToVersion } from '../../lib/apiClient.js'
import Button from '../shared/Button.jsx'
import Modal from '../shared/Modal.jsx'

function tarihBicimle(isoTarih) {
  if (!isoTarih) return ''
  const t = new Date(isoTarih)
  return Number.isNaN(t.getTime())
    ? isoTarih
    : t.toLocaleString('tr-TR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function VersiyonGecmisi({ acik, documentId, guncelSurum, onKapat, onGeriAlindi }) {
  const [surumler, setSurumler] = useState([])
  const [hata, setHata] = useState(null)
  const [islemdeki, setIslemdeki] = useState(null)

  useEffect(() => {
    if (!acik || !documentId) return
    setHata(null)
    fetchVersions(documentId)
      .then(setSurumler)
      .catch((e) => setHata(e.message))
  }, [acik, documentId])

  async function geriAl(surum) {
    setIslemdeki(surum)
    setHata(null)
    try {
      const guncel = await rollbackToVersion(documentId, surum)
      onGeriAlindi(guncel)
      onKapat()
    } catch (e) {
      setHata(e.message)
    } finally {
      setIslemdeki(null)
    }
  }

  return (
    <Modal acik={acik} baslik="Sürüm geçmişi" onKapat={onKapat}>
      {hata && <div className="uyari uyari--hata">{hata}</div>}

      {surumler.length === 0 && !hata && (
        <p className="sessiz-metin">Henüz kayıtlı sürüm yok.</p>
      )}

      {surumler.length > 0 && (
        <table className="liste-tablo">
          <tbody>
            {surumler.map((s) => (
              <tr key={s.version}>
                <td className="sayi" style={{ width: 92 }}>
                  <strong>v{s.version}</strong>
                  {s.version === guncelSurum && (
                    <span className="sessiz-metin"> · güncel</span>
                  )}
                </td>
                <td>{s.createdBy}</td>
                <td className="sessiz-metin">{tarihBicimle(s.createdAt)}</td>
                <td style={{ textAlign: 'right' }}>
                  {s.version !== guncelSurum && (
                    <Button
                      boyut="kucuk"
                      onClick={() => geriAl(s.version)}
                      disabled={islemdeki !== null}
                    >
                      {islemdeki === s.version ? 'Alınıyor…' : 'Bu sürüme dön'}
                    </Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <p className="alan__ipucu" style={{ marginTop: 14 }}>
        Geri alma geçmişi silmez; eski içerik yeni bir sürüm olarak kaydedilir.
      </p>

      <div style={{ marginTop: 14, textAlign: 'right' }}>
        <Button varyant="sessiz" onClick={onKapat}>
          Kapat
        </Button>
      </div>
    </Modal>
  )
}
