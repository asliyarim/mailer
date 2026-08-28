// "Belgelerim" ekranı: takım seçimi + o takımın mail listesi + yeni taslak.

import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { fetchDocuments, fetchTeams } from '../../lib/apiClient.js'
import { TEMPLATE_LABELS, TEMPLATE_TYPES } from '../../lib/mailContent.js'
import Button from './Button.jsx'

function tarihBicimle(isoTarih) {
  if (!isoTarih) return ''
  const t = new Date(isoTarih)
  return Number.isNaN(t.getTime())
    ? isoTarih
    : t.toLocaleString('tr-TR', { dateStyle: 'short', timeStyle: 'short' })
}

/** Tip etiketinin rengi - listede tipler renkle de ayrışsın. */
function etiketSinifi(templateType) {
  if (templateType === TEMPLATE_TYPES.PLANLAMA) return 'etiket etiket--yesil'
  if (templateType === TEMPLATE_TYPES.YONETICI_OZETI) return 'etiket etiket--notr'
  return 'etiket'
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
    <main className="sayfa">
      <div className="sayfa__ic">
        <div className="satir-arasi">
          <div>
            <h1 className="sayfa__baslik">Belgelerim</h1>
            <p className="sayfa__alt">
              Takımın hazırladığı sprint mailleri. Bir satıra tıklayarak düzenleyin.
            </p>
          </div>
          <span className="sag-yasla">
            <Link to="/editor/new">
              <Button varyant="birincil">+ Yeni mail</Button>
            </Link>
          </span>
        </div>

        {/* Takım seçimi: iki takım için açılır liste fazla ağır kaçıyor,
            kaç takım olduğu doğrudan görünsün. */}
        {teams.length > 1 && (
          <div className="satir-arasi">
            <span className="alan__etiket">Takım</span>
            <div className="seg-grup">
              {teams.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  className={t.id === seciliTeamId ? 'seg seg--secili' : 'seg'}
                  onClick={() => setSeciliTeamId(t.id)}
                >
                  {t.name}
                </button>
              ))}
            </div>
          </div>
        )}

        {hata && <div className="uyari uyari--hata">{hata}</div>}

        {yukleniyor && <p className="sessiz-metin">Yükleniyor…</p>}

        {!yukleniyor && belgeler.length === 0 && !hata && (
          <div className="card">
            <div className="bos-durum">
              <p className="bos-durum__baslik">Bu takımda henüz mail yok</p>
              <p className="bos-durum__metin">
                "Yeni mail" ile başlayın — tipi seçtiğinizde bölümler ve sütunlar
                hazır gelir, siz sadece satırları doldurursunuz.
              </p>
              <Link to="/editor/new">
                <Button varyant="birincil">+ Yeni mail</Button>
              </Link>
            </div>
          </div>
        )}

        {!yukleniyor && belgeler.length > 0 && (
          <div className="card" style={{ padding: 0, overflowX: 'auto' }}>
            <table className="liste-tablo">
              <thead>
                <tr>
                  <th>Başlık</th>
                  <th>Tip</th>
                  <th>Sürüm</th>
                  <th>Güncelleyen</th>
                  <th>Güncelleme</th>
                </tr>
              </thead>
              <tbody>
                {belgeler.map((b) => (
                  <tr
                    key={b.id}
                    className="tiklanabilir"
                    onClick={() => navigate(`/editor/${b.id}`)}
                  >
                    <td>
                      {/* Bağlantı klavyeyle gezenler için burada duruyor;
                          satır tıklaması yalnızca fare kolaylığı. */}
                      <Link to={`/editor/${b.id}`} onClick={(e) => e.stopPropagation()}>
                        {b.title}
                      </Link>
                    </td>
                    <td>
                      <span className={etiketSinifi(b.templateType)}>
                        {TEMPLATE_LABELS[b.templateType] ?? b.templateType}
                      </span>
                    </td>
                    <td className="sayi">v{b.currentVersion}</td>
                    <td>{b.updatedBy}</td>
                    <td className="sessiz-metin">{tarihBicimle(b.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </main>
  )
}
