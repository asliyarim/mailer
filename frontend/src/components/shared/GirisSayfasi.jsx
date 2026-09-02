// Uygulamanın giriş sayfası: üç mail tipi kartı + "Son Taslaklar".
//
// Neden burada tip seçiliyor: tip, belgenin şeklini belirleyen tek karar
// (bölümler, sütunlar, sayaçlar ona bağlı) ve kaydedildikten sonra
// değiştirilemiyor. Kullanıcının ilk işi bu seçim olsun — sonra doğrudan
// yazmaya başlıyor.
//
// Son taslaklar TİPE GÖRE gruplu ve gruplar KAPALI başlıyor: liste uzadıkça
// sayfa taslak yığınına dönüşüyordu. Başlıklar sayıyı gösteriyor, tıklayınca
// açılıyor.
//
// ARAMA SUNUCUDA: q parametresi TAKIM ADINDA, başlıkta ve dönemde arıyor
// (şapkalı â/î/û katlanarak - "İş Zekası" yazan "İş Zekâsı"yı bulsun).
// Burada ikinci bir filtre yok: olsaydı iki arama mantığı olurdu ve
// özellikle şapka katlama gibi ince kurallarda zamanla ayrışırlardı.

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchRecentDocuments } from '../../lib/apiClient.js'
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
      'Sprint değerlendirmesinin yönetici özeti: sayaçlar, kararlar, bekleyen aksiyonlar.',
    icerik: 'Sayaçlar · Kararlar · Aksiyonlar · Bağlantılar',
  },
  {
    tip: TEMPLATE_TYPES.TOPLANTI_CIKTILARI,
    ton: 'blue',
    aciklama:
      'Sprint dışı bir toplantının tutanağı: konuşulanlar, resmî kararlar, sorumlular.',
    icerik: 'Toplantı künyesi · Kararlar · Aksiyonlar',
  },
]

// Her tuşa basışta istek atmamak için bekleme (önizlemedekiyle aynı mantık).
const ARAMA_GECIKMESI_MS = 300

// Sunucu son 500 kaydı tarıyor, daha eskisi bu aramaya girmiyor.
const ARAMA_KAPSAMI = 500

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
  const [yukleniyor, setYukleniyor] = useState(true)
  const [hata, setHata] = useState(null)
  const [arama, setArama] = useState('')
  // Açık gruplar. Boş küme = hepsi kapalı (varsayılan).
  const [acikGruplar, setAcikGruplar] = useState(new Set())

  useEffect(() => {
    let iptal = false
    setYukleniyor(true)
    const zamanlayici = setTimeout(() => {
      fetchRecentDocuments(12, arama)
        .then((liste) => {
          if (iptal) return
          setBelgeler(liste)
          setHata(null)
        })
        .catch((e) => {
          if (!iptal) setHata(e.message)
        })
        .finally(() => {
          if (!iptal) setYukleniyor(false)
        })
    }, ARAMA_GECIKMESI_MS)

    return () => {
      iptal = true
      clearTimeout(zamanlayici)
    }
  }, [arama])

  function grubuDegistir(tip) {
    setAcikGruplar((onceki) => {
      const yeni = new Set(onceki)
      if (yeni.has(tip)) yeni.delete(tip)
      else yeni.add(tip)
      return yeni
    })
  }

  /**
   * Takım adı SUNUCUDAN geliyor (DocumentSummary.teamName).
   *
   * Önceden takım listesinden id ile bulunuyordu; pasif ya da listede
   * olmayan bir takımın belgesinde "Takım #3" yazıyordu. Ayrıca bu, giriş
   * sayfasında gereksiz ikinci bir istek demekti.
   */
  function takimAdi(belge) {
    return belge.teamName ?? `Takım #${belge.teamId}`
  }

  // İlk adı kullan: "Ece Sena Salan" → "Ece". Tam ad selamlamada uzun kaçıyor.
  const ilkAd = user?.fullName?.trim().split(/\s+/)[0]
  const aramaVar = arama.trim().length > 0

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
          <div className="liste-basligi">
            <h2 className="sayfa__baslik" style={{ fontSize: 17 }}>
              Son taslaklar
            </h2>

            <input
              type="search"
              className="arama-kutusu"
              placeholder="Takım, başlık veya dönem ara — “İş Zekası”, “Ağustos 2026”"
              aria-label="Taslaklarda ara"
              value={arama}
              onChange={(e) => setArama(e.target.value)}
            />

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
                <p className="bos-durum__baslik">
                  {aramaVar ? 'Son kayıtlarda bulunamadı' : 'Henüz mail hazırlamadınız'}
                </p>
                <p className="bos-durum__metin">
                  {aramaVar
                    ? // Sunucu son 500 kaydı tarıyor. "Hiç yok" demek yanlış
                      // olurdu - daha eski bir kayıt var olabilir.
                      `Arama son ${ARAMA_KAPSAMI} kaydı tarar. Daha eski bir mail arıyorsanız
                       "Tümünü gör" ile takım listesine bakın.`
                    : 'Yukarıdan bir tip seçin; sağda mail şablonu hazır gelir, siz sadece doldurursunuz.'}
                </p>
              </div>
            </div>
          )}

          {/* Gruplar KAPALI başlar. Arama yapılıyorsa açık gelir - aksi hâlde
              kullanıcı arar, sonuç bulunur ama ekranda kapalı başlıklardan
              başka bir şey görmez. */}
          {!yukleniyor &&
            TIPLER.map((t) => {
              const grup = belgeler.filter((b) => b.templateType === t.tip)
              if (grup.length === 0) return null
              const acik = aramaVar || acikGruplar.has(t.tip)

              return (
                <div className="card" key={t.tip} style={{ marginBottom: 12, padding: 0 }}>
                  <button
                    type="button"
                    className={`grup-baslik grup-baslik--${t.ton}${acik ? '' : ' grup-baslik--kapali'}`}
                    aria-expanded={acik}
                    onClick={() => grubuDegistir(t.tip)}
                  >
                    <span className="grup-baslik__ok" aria-hidden="true">
                      {acik ? '▾' : '▸'}
                    </span>
                    {TEMPLATE_LABELS[t.tip]}
                    <span className="grup-baslik__sayi sayi">{grup.length}</span>
                  </button>

                  {acik && (
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
                              {/* Dönem, sprintin kendisini söylüyor; başlık
                                  çoğu zaman dosya adı gibi kullanılıyor. */}
                              {b.period && (
                                <span className="sessiz-metin"> · {b.period}</span>
                              )}
                            </td>
                            {/* Takım rozet olarak: yönetici sekiz takımın
                                belgelerini bir arada görüyor ve "ddd" gibi
                                iki belge farklı takımlardan olabiliyor. */}
                            <td>
                              <span className="etiket etiket--notr">{takimAdi(b)}</span>
                            </td>
                            <td className="sayi sessiz-metin">v{b.currentVersion}</td>
                            <td className="sessiz-metin" style={{ textAlign: 'right' }}>
                              {tarihBicimle(b.updatedAt)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              )
            })}
        </div>
      </div>
    </main>
  )
}
