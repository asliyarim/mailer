// "Belgelerim" ekrani: takim secimi + o takimin mail listesi + yeni taslak.
// Sprint 2'de tamamlanacak (Yardimci 1). Su an iskelet: veri akisi kurulu,
// liste gorunumu doldurulacak.

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchDocuments, fetchTeams } from '../../lib/apiClient.js'
import { TEMPLATE_LABELS } from '../../lib/mailContent.js'
import Button from './Button.jsx'

export default function DocumentListPage() {
  const [teams, setTeams] = useState([])
  const [seciliTeamId, setSeciliTeamId] = useState(null)
  const [belgeler, setBelgeler] = useState([])
  const [hata, setHata] = useState(null)

  useEffect(() => {
    fetchTeams()
      .then((liste) => {
        setTeams(liste)
        if (liste.length > 0) setSeciliTeamId(liste[0].id)
      })
      .catch((e) => setHata(e.message))
  }, [])

  useEffect(() => {
    if (seciliTeamId == null) return
    fetchDocuments(seciliTeamId)
      .then(setBelgeler)
      .catch((e) => setHata(e.message))
  }, [seciliTeamId])

  return (
    <main style={{ padding: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
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

      {/* TODO (Yrd 1, Sprint 2): liste gorunumu - baslik, tip, guncelleme
          tarihi, versiyon. Satira tiklayinca /editor/:id acilir. */}
      <ul>
        {belgeler.map((b) => (
          <li key={b.id}>
            <Link to={`/editor/${b.id}`}>
              {b.title} · {TEMPLATE_LABELS[b.templateType] ?? b.templateType}
            </Link>
          </li>
        ))}
      </ul>

      {belgeler.length === 0 && !hata && <p>Bu takımda henüz mail yok.</p>}
    </main>
  )
}
