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

// Tip kartlarının açıklamaları - kullanıcı neyi seçtiğini bilerek seçsin,
// çünkü tip sonradan değiştirilemiyor.
const TIP_ACIKLAMALARI = {
  KAPANIS: 'Biten sprintin analiz ve geliştirme çalışmaları, sektöre göre gruplu tablolar.',
  PLANLAMA: 'Gelecek sprintin konuları: konu anahtarı, özet, durum, beklenen işler.',
  YONETICI_OZETI: 'Sayaçlar, sektör özeti ve dikkat gerektiren konular.',
}

// Sunucuda henüz şablonu olmayan tipler. Şablon geldiğinde bu liste boşalır;
// seçilebilir hâle getirmek için başka bir yere dokunmak gerekmez.
// (render/template/ altında YoneticiOzetiTemplate yok — belge oluşturulabilir
// ama önizleme ve .eml üretilemez, o yüzden baştan engelliyoruz.)
const HAZIR_OLMAYAN_TIPLER = [TEMPLATE_TYPES.YONETICI_OZETI]

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
  const seciliTakim = teams.find((t) => String(t.id) === teamId)

  return (
    <main className="sayfa">
      <div className="sayfa__ic sayfa__ic--dar">
        <div>
          <h1 className="sayfa__baslik">Yeni mail</h1>
          <p className="sayfa__alt">
            Üç bilgi yeter; bölümler ve sütunlar tipe göre hazır gelir.
          </p>
        </div>

        <form className="card" onSubmit={olustur}>
          {hata && (
            <div className="uyari uyari--hata" style={{ marginBottom: 14 }}>
              {hata}
            </div>
          )}

          <div className="izgara" style={{ gap: 18 }}>
            <div className="alan">
              <span className="alan__etiket">Takım</span>
              <div className="seg-grup">
                {teams.map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    className={String(t.id) === teamId ? 'seg seg--secili' : 'seg'}
                    onClick={() => setTeamId(String(t.id))}
                  >
                    {t.name}
                  </button>
                ))}
              </div>
              {/* Tema takımdan geliyor - renk, maskot, logo. Kullanıcı ayrıca seçmez. */}
              <span className="alan__ipucu">
                Mailin rengi, maskotu ve logosu takıma göre belirlenir
                {seciliTakim ? ` (tema: ${seciliTakim.themeKey})` : ''}.
              </span>
            </div>

            <div className="alan">
              <span className="alan__etiket">Mail tipi</span>
              <div className="tip-secim">
                {Object.values(TEMPLATE_TYPES).map((tip) => {
                  const hazirDegil = HAZIR_OLMAYAN_TIPLER.includes(tip)
                  const secili = templateType === tip
                  const siniflar = ['tip-kutu']
                  if (secili) siniflar.push('tip-kutu--secili')
                  if (hazirDegil) siniflar.push('tip-kutu--kapali')

                  return (
                    <label key={tip} className={siniflar.join(' ')}>
                      <input
                        type="radio"
                        name="templateType"
                        value={tip}
                        checked={secili}
                        disabled={hazirDegil}
                        onChange={(e) => setTemplateType(e.target.value)}
                      />
                      <span className="tip-kutu__ad">
                        {TEMPLATE_LABELS[tip]}
                        {hazirDegil && (
                          <span className="etiket etiket--notr" style={{ marginLeft: 8 }}>
                            hazırlanıyor
                          </span>
                        )}
                      </span>
                      <span className="tip-kutu__aciklama">
                        {hazirDegil
                          ? 'Şablonu henüz yazılmadı; seçilirse mail üretilemez.'
                          : TIP_ACIKLAMALARI[tip]}
                      </span>
                    </label>
                  )
                })}
              </div>
              {/* Tip sonradan değiştirilemiyor: bölüm ve sütun yapısı tipe bağlı,
                  değiştirmek girilen satırları anlamsız kılardı. */}
              <span className="alan__ipucu">
                Tip sonradan değiştirilemez; yanlış seçerseniz yeni bir mail oluşturun.
              </span>
            </div>

            <label className="alan">
              <span className="alan__etiket">Başlık</span>
              <input
                type="text"
                value={title}
                placeholder="Ağustos 2026 Sprint Kapanışı"
                onChange={(e) => setTitle(e.target.value)}
              />
              <span className="alan__ipucu">
                Bu ad yalnızca listede görünür; mailin içindeki başlığı sonra yazacaksınız.
              </span>
            </label>

            <div className="satir-arasi">
              <Button varyant="birincil" type="submit" disabled={!gecerli || olusturuluyor}>
                {olusturuluyor ? 'Oluşturuluyor…' : 'Oluştur'}
              </Button>
              <Button varyant="sessiz" onClick={() => navigate('/belgeler')}>
                Vazgeç
              </Button>
            </div>
          </div>
        </form>
      </div>
    </main>
  )
}
