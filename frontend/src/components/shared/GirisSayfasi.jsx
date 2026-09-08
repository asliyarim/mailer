// Uygulamanın giriş sayfası: mail tipi seçimi.
//
// Neden burada tip seçiliyor: tip, belgenin şeklini belirleyen tek karar
// (bölümler, sütunlar, sayaçlar ona bağlı) ve kaydedildikten sonra
// değiştirilemiyor. Kullanıcının ilk işi bu seçim olsun — sonra doğrudan
// yazmaya başlıyor.
//
// "SON TASLAKLAR" BURADAN KALDIRILDI (Aslı'nın kararı): aynı belgeler
// "Belgelerim"de takım takım daha düzenli duruyor ve iki liste bakımı
// gereksizdi. Bedeli: buradaki arama BÜTÜN takımlarda birden arıyordu,
// Belgelerim'deki tek takımda arıyor.

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { copyDocument, fetchRecentDocuments } from '../../lib/apiClient.js'
import { TEMPLATE_LABELS, TEMPLATE_TYPES } from '../../lib/mailContent.js'
import Button from './Button.jsx'
import SablonOnizleme from './SablonOnizleme.jsx'

/**
 * "Geçen sprintten devam et" şeridi YALNIZCA PO'lara gösterilir.
 *
 * Neden admin'e değil: /documents/recent ucu ADMIN icin BUTUN aktif
 * takimlarin belgelerini dondurur (bkz. MailerDocumentController). Sekiz
 * takimin son mailleri karisik bir yigin olur; ustelik "donemi bir artir"
 * fikri tek bir takimin sprint ritmi icinde anlamli - takimlar arasinda
 * degil. Admin'in bu ekrandaki isi zaten uretim degil gozetim, ona
 * "Belgelerim" takim takim daha dogru cevap veriyor.
 */
const SERIT_ROLU = 'PO'

/**
 * Şeritte HER TİPTEN YALNIZCA EN YENİSİ görünür.
 *
 * Önce "en son güncellenen 4 mail" gösteriliyordu ve tip önemsizdi; art arda
 * iki Yönetici Özeti hazırlayan biri şeritte iki Yönetici Özeti görüyor,
 * Sprint Kapanışı ise listeden düşüyordu. Oysa devam edilecek şey TİPTİR:
 * "geçen Kapanış'tan devam et", "geçen Planlama'dan devam et".
 *
 * Böylece şerit üstteki dört şablon kartıyla birebir eşleşiyor ve en fazla
 * dört satır oluyor.
 */

/**
 * Sunucudan çekilen mail sayısı - içinden her tipin en yenisi seçilecek.
 *
 * Neden 4 değil: 4 istenseydi ve son dört mail de aynı tipten olsaydı,
 * eleme sonrası şeritte tek satır kalırdı. 20, bir PO'nun yakın geçmişinde
 * dört tipin de bulunmasına fazlasıyla yeter.
 *
 * Bu sınır aynı zamanda DOĞRU olan: son Toplantı Notları bir yıl öncesindeyse
 * onu "geçen sprintten devam et" diye önermek zaten yanlış olurdu.
 */
const CEKILEN_ADET = 20

/**
 * "2 saat önce". Tam tarih başlık (title) olarak duruyor - göreli zaman
 * hızlı okunur ama kesin bilgi de kaybolmasın.
 */
function goreliZaman(isoTarih) {
  if (!isoTarih) return ''
  const t = new Date(isoTarih)
  if (Number.isNaN(t.getTime())) return ''

  const saniye = Math.round((Date.now() - t.getTime()) / 1000)
  if (saniye < 60) return 'az önce'

  const olculer = [
    [60, 'dakika'],
    [24, 'saat'],
    [7, 'gün'],
    [4.35, 'hafta'],
    [12, 'ay'],
  ]

  let deger = saniye
  let birim = 'saniye'
  for (const [bolen, ad] of olculer) {
    if (deger < bolen) break
    deger = deger / bolen
    birim = ad
  }

  return `${Math.floor(deger)} ${birim} önce`
}

/**
 * Her tipten en yenisini seçer, sıra bozulmadan.
 *
 * Liste sunucudan zaten "en yeni önce" geliyor; ilk karşılaşılan kayıt o
 * tipin en yenisidir. Yeniden sıralamıyoruz - şeridin sırası "en son neye
 * dokundun" olarak kalsın.
 */
function tipBasinaEnYeni(belgeler) {
  const gorulen = new Set()
  return belgeler.filter((b) => {
    if (gorulen.has(b.templateType)) return false
    gorulen.add(b.templateType)
    return true
  })
}

function tamTarih(isoTarih) {
  const t = new Date(isoTarih)
  return Number.isNaN(t.getTime())
    ? ''
    : t.toLocaleString('tr-TR', { dateStyle: 'long', timeStyle: 'short' })
}

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

export default function GirisSayfasi({ user }) {
  const navigate = useNavigate()

  // İlk adı kullan: "Ece Sena Salan" → "Ece". Tam ad selamlamada uzun kaçıyor.
  const ilkAd = user?.fullName?.trim().split(/\s+/)[0]

  const seritGoster = user?.role === SERIT_ROLU
  const [sonMailler, setSonMailler] = useState([])
  const [kopyalanan, setKopyalanan] = useState(null)
  const [seritHatasi, setSeritHatasi] = useState(null)

  useEffect(() => {
    if (!seritGoster) return undefined

    let iptal = false
    fetchRecentDocuments(CEKILEN_ADET)
      .then((liste) => {
        if (!iptal) setSonMailler(tipBasinaEnYeni(liste))
      })
      // Şerit bir KOLAYLIK; alınamazsa sayfanın asıl işi (şablon seçimi)
      // etkilenmesin. Hata sessizce yutulmuyor ama ekranı da kaplamıyor.
      .catch(() => {
        if (!iptal) setSeritHatasi('Son mailler alınamadı.')
      })

    return () => {
      iptal = true
    }
  }, [seritGoster])

  async function kopyala(belge) {
    if (kopyalanan) return
    setKopyalanan(belge.id)
    setSeritHatasi(null)
    try {
      const yeni = await copyDocument(belge.id)
      navigate(`/editor/${yeni.id}`)
    } catch (e) {
      setSeritHatasi(e.message)
      setKopyalanan(null)
    }
  }

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
              {/* Mailin şekli - hangi tipin ne ürettiği yazıdan önce
                  buradan anlaşılsın. */}
              <SablonOnizleme tip={t.tip} />
              <span className="tip-karti__icerik">{t.icerik}</span>
              <span className="tip-karti__eylem">Başla →</span>
            </button>
          ))}
        </div>

        {/* "Geçen sprintten devam et" - yalnızca PO'da ve yalnızca gerçekten
            devam edilecek bir mail varsa. Boş bir başlık göstermek, olmayan
            bir şeyi vaat etmek olurdu. */}
        {seritGoster && sonMailler.length > 0 && (
          <section className="devam">
            <div className="devam__ust">
              <h2 className="devam__baslik">Geçen sprintten devam et</h2>
              <p className="devam__alt">
                Her tipin en son maili. Kopya yeni bir taslak olarak açılır,
                dönem numarası bir artar; kaynak mail olduğu gibi kalır.
              </p>
            </div>

            <ul className="devam__liste">
              {sonMailler.map((belge) => (
                <li key={belge.id} className="devam__kart">
                  <span className="devam__tip">{TEMPLATE_LABELS[belge.templateType]}</span>
                  <span className="devam__ad" title={belge.title}>
                    {belge.title}
                  </span>
                  {belge.period && <span className="devam__donem">{belge.period}</span>}
                  <span className="devam__zaman" title={tamTarih(belge.updatedAt)}>
                    {goreliZaman(belge.updatedAt)}
                  </span>
                  <span className="devam__eylem">
                    <Button
                      varyant="ikincil"
                      boyut="kucuk"
                      onClick={() => kopyala(belge)}
                      disabled={kopyalanan != null}
                      baslik={`${belge.title} maili kopyalanıp yeni taslak açılır`}
                    >
                      {kopyalanan === belge.id ? 'Kopyalanıyor…' : 'Kopyala'}
                    </Button>
                  </span>
                </li>
              ))}
            </ul>

            {seritHatasi && <p className="devam__hata">{seritHatasi}</p>}
          </section>
        )}

        {/* Kaydedilmiş maillere giden yol: üst şeritte de "Belgelerim" var,
            ama yeni gelen kullanıcı oraya bakmayabilir. */}
        <div className="card">
          <div className="satir-arasi">
            <div>
              <p className="bos-durum__baslik" style={{ margin: 0 }}>
                Daha önce hazırladıklarınız
              </p>
              <p className="alan__ipucu" style={{ margin: '4px 0 0' }}>
                Kayıtlı mailler takım takım listeleniyor; başlık veya döneme
                göre arayabilirsiniz.
              </p>
            </div>
            <span className="sag-yasla">
              <Button varyant="ikincil" onClick={() => navigate('/belgeler')}>
                Belgelerim →
              </Button>
            </span>
          </div>
        </div>
      </div>
    </main>
  )
}
