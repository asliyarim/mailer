// /editor/new — yeni taslak oluşturma.
//
// İçerik BURADA kurulmaz. Sunucu, tipin ve temanın varsayılan içeriğiyle
// belgeyi doğurur (docs/api.md §3); biz sadece üç bilgiyi sorup POST atarız.
// İstemcinin boş iskelet göndermesine izin verilseydi iki ayrı "boş içerik"
// tanımı oluşurdu ve zamanla ayrışırlardı.

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createDocument, fetchTeams } from '../../lib/apiClient.js'
import { TEMPLATE_LABELS, TEMPLATE_TYPES } from '../../lib/mailContent.js'
import Button from '../shared/Button.jsx'

export default function YeniBelgeForm() {
  const navigate = useNavigate()

  const [teams, setTeams] = useState([])
  const [teamId, setTeamId] = useState('')
  const [templateType, setTemplateType] = useState(TEMPLATE_TYPES.KAPANIS)
  const [title, setTitle] = useState('')
  const [hata, setHata] = useState(null)
  const [olusturuluyor, setOlusturuluyor] = useState(false)

  useEffect(() => {
    fetchTeams()
      .then((liste) => {
        setTeams(liste)
        if (liste.length > 0) setTeamId(String(liste[0].id))
      })
      .catch((e) => setHata(e.message))
  }, [])

  async function olustur(e) {
    e.preventDefault()
    setHata(null)
    setOlusturuluyor(true)
    try {
      const belge = await createDocument({
        teamId: Number(teamId),
        templateType,
        title: title.trim(),
      })
      navigate(`/editor/${belge.id}`)
    } catch (err) {
      setHata(err.message)
      setOlusturuluyor(false)
    }
  }

  const gecerli = teamId !== '' && title.trim() !== ''

  return (
    <main style={{ padding: 16, maxWidth: 560 }}>
      <form className="card" onSubmit={olustur}>
        <h2 className="card__baslik">Yeni mail</h2>

        {hata && (
          <p style={{ color: '#9c3226', marginTop: 0 }}>{hata}</p>
        )}

        <div style={{ display: 'grid', gap: 12 }}>
          <label style={{ display: 'grid', gap: 4 }}>
            <span>Takım</span>
            <select value={teamId} onChange={(e) => setTeamId(e.target.value)}>
              {teams.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
            {/* Tema takımdan geliyor - renk, maskot, logo. Kullanıcı ayrıca seçmez. */}
            <small style={{ color: '#6d8296' }}>
              Mailin rengi, maskotu ve logosu takıma göre belirlenir.
            </small>
          </label>

          <fieldset style={{ border: '1px solid #d9e0e7', borderRadius: 4, padding: 12 }}>
            <legend>Mail tipi</legend>
            <div style={{ display: 'grid', gap: 6 }}>
              {Object.values(TEMPLATE_TYPES).map((tip) => (
                <label key={tip} style={{ display: 'flex', gap: 8, alignItems: 'baseline' }}>
                  <input
                    type="radio"
                    name="templateType"
                    value={tip}
                    checked={templateType === tip}
                    onChange={(e) => setTemplateType(e.target.value)}
                  />
                  <span>{TEMPLATE_LABELS[tip]}</span>
                  {tip === TEMPLATE_TYPES.YONETICI_OZETI && (
                    <small style={{ color: '#8a5a09' }}>henüz hazır değil</small>
                  )}
                </label>
              ))}
            </div>
            {/* Tip sonradan degistirilemiyor: bolum ve sutun yapisi tipe bagli,
                degistirmek girilen satirlari anlamsiz kilardi. */}
            <small style={{ color: '#6d8296' }}>
              Tip sonradan değiştirilemez; yanlış seçerseniz yeni bir mail oluşturun.
            </small>
          </fieldset>

          <label style={{ display: 'grid', gap: 4 }}>
            <span>Başlık</span>
            <input
              type="text"
              value={title}
              placeholder="Ağustos 2026 Sprint Kapanışı"
              onChange={(e) => setTitle(e.target.value)}
            />
            <small style={{ color: '#6d8296' }}>
              Bu ad yalnızca listede görünür; mailin içindeki başlığı sonra yazacaksınız.
            </small>
          </label>

          <div style={{ display: 'flex', gap: 8 }}>
            <Button varyant="birincil" type="submit" disabled={!gecerli || olusturuluyor}>
              {olusturuluyor ? 'Oluşturuluyor…' : 'Oluştur'}
            </Button>
            <Button varyant="sessiz" onClick={() => navigate('/belgeler')}>
              Vazgeç
            </Button>
          </div>
        </div>
      </form>
    </main>
  )
}
