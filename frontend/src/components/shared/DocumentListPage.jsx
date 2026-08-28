// "Belgelerim" ekranı: takım seçimi + o takımın mail listesi + yeni taslak.

import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { fetchDocuments, fetchTeams } from '../../lib/apiClient.js'
import { TEMPLATE_LABELS } from '../../lib/mailContent.js'
import Button from './Button.jsx'

function tarihBicimle(isoTarih) {
  if (!isoTarih) return ''
  const t = new Date(isoTarih)
  return Number.isNaN(t.getTime())
    ? isoTarih
    : t.toLocaleString('tr-TR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function DocumentListPage() {
  const navigate = useNavigate()

  const [teams, setTeams] = useState([])
  const [seciliTeamId, setSeciliTeamId] = useState(null)
  const [belgeler, setBelgeler] = useState([])
  const [yukleniyor, setYukleniyor] = useState(true)
  const [hata, setHata] = useState(null)

  useEffect(() => {
    fetchTeams()
      .then((liste) => {
        setTeams(liste)
        if (liste.length > 0) setSeciliTeamId(liste[0].id)
        else setYukleniyor(false)
      })
      .catch((e) => {
        setHata(e.message)
        setYukleniyor(false)
      })
  }, [])

  useEffect(() => {
    if (seciliTeamId == null) return
    setYukleniyor(true)
    fetchDocuments(seciliTeamId)
      .then((liste) => {
        setBelgeler(liste)
        setHata(null)
      })
      .catch((e) => setHata(e.message))
      .finally(() => setYukleniyor(false))
  }, [seciliTeamId])

  return (
    <main style={{ padding: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <label htmlFor="takim">Takım</label>
        <select
          id="takim"
          value={seciliTeamId ?? ''}
          onChange={(e) => setSeciliTeamId(Number(e.target.value))}
        >
          {teams.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
        <Link to="/editor/new">
          <Button varyant="birincil">Yeni mail</Button>
        </Link>
      </div>

      {hata && <p style={{ color: '#9c3226' }}>{hata}</p>}

      {yukleniyor && <p>Yükleniyor…</p>}

      {!yukleniyor && belgeler.length === 0 && !hata && (
        <div className="card">
          <p style={{ margin: 0 }}>Bu takımda henüz mail yok.</p>
          <p style={{ margin: '6px 0 0', color: '#6d8296', fontSize: 14 }}>
            "Yeni mail" ile başlayın — tipi seçtiğinizde bölümler hazır gelir.
          </p>
        </div>
      )}

      {!yukleniyor && belgeler.length > 0 && (
        <div className="card" style={{ padding: 0, overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
            <thead>
              <tr style={{ background: '#f2f5f8', textAlign: 'left' }}>
                <th style={{ padding: '10px 14px' }}>Başlık</th>
                <th style={{ padding: '10px 14px' }}>Tip</th>
                <th style={{ padding: '10px 14px' }}>Sürüm</th>
                <th style={{ padding: '10px 14px' }}>Güncelleyen</th>
                <th style={{ padding: '10px 14px' }}>Güncelleme</th>
              </tr>
            </thead>
            <tbody>
              {belgeler.map((b) => (
                <tr
                  key={b.id}
                  onClick={() => navigate(`/editor/${b.id}`)}
                  style={{ borderTop: '1px solid #e2e9ef', cursor: 'pointer' }}
                >
                  <td style={{ padding: '10px 14px' }}>
                    <Link to={`/editor/${b.id}`} onClick={(e) => e.stopPropagation()}>
                      {b.title}
                    </Link>
                  </td>
                  <td style={{ padding: '10px 14px' }}>
                    {TEMPLATE_LABELS[b.templateType] ?? b.templateType}
                  </td>
                  <td style={{ padding: '10px 14px' }}>v{b.currentVersion}</td>
                  <td style={{ padding: '10px 14px', color: '#3f5265' }}>{b.updatedBy}</td>
                  <td style={{ padding: '10px 14px', color: '#6d8296' }}>
                    {tarihBicimle(b.updatedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}
