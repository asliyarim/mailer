// 1. kart: belge ayarları.
//
// İKİ HÂLİ VAR:
//
// TASLAK (belge henüz kaydedilmemiş) — takım ve tip SEÇİLEBİLİR. Kullanıcı
// burada neyi yazacağına karar veriyor; seçim değişince sunucudan o tipin
// varsayılan içeriği yeniden alınır.
//
// KAYITLI belge — takım ve tip SALT OKUNUR. İkisi de içeriğin şeklini
// belirliyor: tip bölüm/sütun yapısını, takım temayı. Sonradan değiştirmek
// girilen satırları anlamsız kılardı; yanlış seçildiyse yeni mail açılır.
//
// Başlık ve konu her iki hâlde de düzenlenebilir:
//   title   → listede görünen ad, .eml dosya adı
//   subject → mailin konu satırı (Outlook'ta görünen)

import Button from '../shared/Button.jsx'
import { TEMPLATE_LABELS, TEMPLATE_TYPES } from '../../lib/mailContent.js'

const TIP_ACIKLAMALARI = {
  KAPANIS: 'Biten sprintin analiz ve geliştirme çalışmaları, sektöre göre gruplu tablolar.',
  PLANLAMA: 'Gelecek sprintin konuları: konu anahtarı, özet, durum, beklenen işler.',
  YONETICI_OZETI:
    'Görüşülen konular, alınan kararlar, bekleyen aksiyonlar ve erişim bağlantıları.',
  TOPLANTI_CIKTILARI:
    'Sprint dışı toplantının tutanağı: künye, konuşulanlar, resmî kararlar, sorumlular.',
}

// Sunucuda henüz şablonu olmayan tipler. Şablon geldiğinde bu liste boşalır;
// seçilebilir hâle getirmek için başka bir yere dokunmak gerekmez.
const HAZIR_OLMAYAN_TIPLER = []

export default function BelgeAyarlari({
  taslak,
  teams,
  teamId,
  templateType,
  title,
  subject,
  surum,
  onTeamId,
  onTemplateType,
  onTitle,
  onSubject,
  onVersiyonlar,
  onYeniMail,
}) {
  const tipSinifi = templateType === TEMPLATE_TYPES.PLANLAMA ? 'etiket etiket--yesil' : 'etiket'

  return (
    <section className="card">
      <h2 className="card__baslik">
        <span className="card__no">1</span>
        {taslak ? 'Yeni mail' : 'Belge ayarları'}
        <span className="card__sag">
          {!taslak && (
            <>
              <Button varyant="sessiz" boyut="kucuk" onClick={onYeniMail}>
                + Yeni mail
              </Button>
              <Button varyant="ikincil" boyut="kucuk" onClick={onVersiyonlar}>
                Sürüm geçmişi
              </Button>
            </>
          )}
        </span>
      </h2>

      {taslak ? (
        <>
          <p className="card__aciklama">
            Henüz kaydedilmedi. Takım ve tipi seçin, sağdaki önizleme anında
            oluşsun; "Kaydet" dediğinizde mail listenize eklenir.
          </p>

          <div className="alan">
            <span className="alan__etiket">Takım</span>
            <div className="seg-grup">
              {teams.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  className={t.id === teamId ? 'seg seg--secili' : 'seg'}
                  onClick={() => onTeamId(t.id)}
                >
                  {t.name}
                </button>
              ))}
            </div>
            {/* Tema takımdan geliyor - renk, maskot, logo. Kullanıcı ayrıca seçmez. */}
            <span className="alan__ipucu">
              Mailin rengi, maskotu ve logosu takıma göre belirlenir.
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
                      onChange={(e) => onTemplateType(e.target.value)}
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
            <span className="alan__ipucu">
              Tip kaydettikten sonra değiştirilemez; bölüm ve sütun yapısı ona bağlı.
            </span>
          </div>
        </>
      ) : (
        <div className="satir-arasi" style={{ marginBottom: 16 }}>
          {/* Takım ve tema künye kartında duruyor - burada tekrar edilmiyor. */}
          <span className={tipSinifi}>{TEMPLATE_LABELS[templateType] ?? templateType}</span>
          <span className="etiket etiket--notr sayi">Sürüm {surum}</span>
        </div>
      )}

      <label className="alan">
        <span className="alan__etiket">Başlık</span>
        <input
          type="text"
          value={title ?? ''}
          placeholder="Ağustos 2026 Sprint Kapanışı"
          onChange={(e) => onTitle(e.target.value)}
        />
        <span className="alan__ipucu">Listede görünen ad ve indirilen dosyanın adı.</span>
      </label>

      <label className="alan" style={{ marginBottom: 0 }}>
        <span className="alan__etiket">Mail konusu</span>
        <input
          type="text"
          value={subject ?? ''}
          placeholder="RPA Sprint Kapanış Bilgilendirme"
          onChange={(e) => onSubject(e.target.value)}
        />
        <span className="alan__ipucu">Outlook'ta konu satırında görünür.</span>
      </label>
    </section>
  )
}
