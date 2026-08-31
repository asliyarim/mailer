// Uygulamanın giriş sayfası: üç mail tipi kartı + "Son Taslaklarım".
//
// Neden burada tip seçiliyor: tip, belgenin şeklini belirleyen tek karar
// (bölümler, sütunlar, sayaçlar ona bağlı) ve kaydedildikten sonra
// değiştirilemiyor. Kullanıcının ilk işi bu seçim olsun — sonra doğrudan
// yazmaya başlıyor.
//
// Son taslaklar TİPE GÖRE gruplu geliyor: sunucu tek liste dönüyor
// (en yeni önce), gruplamayı burada yapıyoruz. Sunucu üç ayrı liste
// dönseydi "son N kayıt" anlamını kaybederdi.

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchRecentDocuments, fetchTeams } from '../../lib/apiClient.js'
import { TEMPLATE_LABELS, TEMPLATE_TYPES } from '../../lib/mailContent.js'
import Button from './Button.jsx'

const TIPLER = [
  {
    tip: TEMPLATE_TYPES.KAPANIS,
    ton: 'blue',
    aciklama:
      'Biten sprintin analiz ve geliştirme çalışmaları. Satırlar sektöre göre gruplanır.',
    icerik: 'Analiz · Geliştirme · Kritik notlar',
  },
  {
    tip: TEMPLATE_TYPES.PLANLAMA,
    ton: 'green',
    aciklama:
      'Gelecek sprintin konuları: konu anahtarı, özet, durum ve beklenen işler.',
    icerik: 'Konular · Durum · Beklenen işler',
  },
  {
    tip: TEMPLATE_TYPES.YONETICI_OZETI,
    ton: 'orange',
    aciklama:
      'Toplantı çıktısı: görüşülen konular, alınan kararlar, bekleyen aksiyonlar.',
    icerik: 'Sayaçlar · Kararlar · Aksiyonlar · Bağlantılar',
  },
]

function tarihBicimle(isoTarih) {
  if (!isoTarih) return ''
  const t = new Date(isoTarih)
  return Number.isNaN(t.getTime())
    ? isoTarih
    : t.toLocaleString('tr-TR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function GirisSayfasi({ user }) {
  const navigate = useNavigate()

  const [belgeler, setBelgeler] = useState([])
  const [teams, setTeams] = useState([])
  const [yukleniyor, setYukleniyor] = useState(true)
  const [hata, setHata] = useState(null)

  useEffect(() => {
    Promise.all([fetchRecentDocuments(12), fetchTeams()])
      .then(([liste, takimlar]) => {
        setBelgeler(liste)
        setTeams(takimlar)
        setHata(null)
      })
      .catch((e) => setHata(e.message))
      .finally(() => setYukleniyor(false))
  }, [])

  function takimAdi(teamId) {
    return teams.find((t) => t.id === teamId)?.name ?? `Takım #${teamId}`
  }

  // İlk adı kullan: "Ece Sena Salan" → "Ece". Tam ad selamlamada uzun kaçıyor.
  const ilkAd = user?.fullName?.trim().split(/\s+/)[0]

  return (
    <main className="sayfa">
      <div className="sayfa__ic">
        <div>
          <h1 className="sayfa__baslik">
            {ilkAd ? `Merhaba ${ilkAd},` : 'Merhaba,'} hangi maili hazırlayalım?
          </h1>
          <p className="sayfa__alt">
            Tipi seçin — bölümler, sütunlar ve takımınızın renkleri hazır gelsin.
          </p>
        </div>

        <div className="tip-kartlari">
          {TIPLER.map((t) => (
            <button
              key={t.tip}
              type="button"
              className={`tip-karti tip-karti--${t.ton}`}
              onClick={() => navigate(`/editor/yeni?tip=${t.tip}`)}
            >
              <span className="tip-karti__ton" aria-hidden="true" />
              <span className="tip-karti__ad">{TEMPLATE_LABELS[t.tip]}</span>
              <span className="tip-karti__aciklama">{t.aciklama}</span>
              <span className="tip-karti__icerik">{t.icerik}</span>
              <span className="tip-karti__eylem">Başla →</span>
            </button>
          ))}
        </div>

        {hata && <div className="uyari uyari--hata">{hata}</div>}

        <div>
          <div className="satir-arasi" style={{ marginBottom: 12 }}>
            <h2 className="sayfa__baslik" style={{ fontSize: 17 }}>
              Son taslaklarım
            </h2>
            <span className="sag-yasla">
              <Button varyant="sessiz" boyut="kucuk" onClick={() => navigate('/belgeler')}>
                Tümünü gör →
              </Button>
            </span>
          </div>

          {yukleniyor && <p className="sessiz-metin">Yükleniyor…</p>}

          {!yukleniyor && belgeler.length === 0 && !hata && (
            <div className="card">
              <div className="bos-durum">
                <p className="bos-durum__baslik">Henüz mail hazırlamadınız</p>
                <p className="bos-durum__metin">
                  Yukarıdan bir tip seçin; sağda mail şablonu hazır gelir, siz
                  sadece doldurursunuz.
                </p>
              </div>
            </div>
          )}

          {/* Tipi olmayan grup hiç çizilmiyor - boş başlık göstermek listeyi
              uzatıp hiçbir şey söylemez. */}
          {!yukleniyor &&
            TIPLER.map((t) => {
              const grup = belgeler.filter((b) => b.templateType === t.tip)
              if (grup.length === 0) return null
              return (
                <div className="card" key={t.tip} style={{ marginBottom: 12, padding: 0 }}>
                  <div className={`grup-baslik grup-baslik--${t.ton}`}>
                    {TEMPLATE_LABELS[t.tip]}
                    <span className="grup-baslik__sayi sayi">{grup.length}</span>
                  </div>
                  <table className="liste-tablo">
                    <tbody>
                      {grup.map((b) => (
                        <tr
                          key={b.id}
                          className="tiklanabilir"
                          onClick={() => navigate(`/editor/${b.id}`)}
                        >
                          <td>
                            <strong>{b.title}</strong>
                          </td>
                          <td className="sessiz-metin">{takimAdi(b.teamId)}</td>
                          <td className="sayi sessiz-metin">v{b.currentVersion}</td>
                          <td className="sessiz-metin" style={{ textAlign: 'right' }}>
                            {tarihBicimle(b.updatedAt)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )
            })}
        </div>
      </div>
    </main>
  )
}
