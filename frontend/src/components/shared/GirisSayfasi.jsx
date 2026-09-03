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

import { useNavigate } from 'react-router-dom'
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

export default function GirisSayfasi({ user }) {
  const navigate = useNavigate()

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
