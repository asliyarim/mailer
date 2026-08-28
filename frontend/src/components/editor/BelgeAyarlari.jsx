// 1. kart: belge ayarları.
//
// Takım ve mail tipi SALT OKUNUR. İkisi de içeriğin şeklini belirliyor -
// tip bölüm/sütun yapısını, takım temayı. Sonradan değiştirmek girilen
// satırları anlamsız kılardı; kullanıcı yanlış seçtiyse yeni mail oluşturur.
//
// Başlık ve konu düzenlenebilir:
//   title   → listede görünen ad, .eml dosya adı
//   subject → mailin konu satırı (Outlook'ta görünen)

import Button from '../shared/Button.jsx'
import { TEMPLATE_LABELS } from '../../lib/mailContent.js'

export default function BelgeAyarlari({ belge, onDegisti, onVersiyonlar }) {
  if (!belge) return null

  return (
    <section className="card">
      <h2 className="card__baslik">
        <span className="card__no">1</span>
        Belge ayarları
        <span className="card__sag">
          <Button varyant="sessiz" boyut="kucuk" onClick={onVersiyonlar}>
            Sürüm geçmişi
          </Button>
        </span>
      </h2>

      {/* Değiştirilemeyen üç bilgi etiket olarak duruyor: form alanı gibi
          görünürlerse kullanıcı düzenlemeyi dener. */}
      <div className="satir-arasi" style={{ marginBottom: 14 }}>
        <span className="etiket">{TEMPLATE_LABELS[belge.templateType] ?? belge.templateType}</span>
        <span className="etiket etiket--notr">Tema: {belge.themeKey}</span>
        <span className="etiket etiket--notr sayi">v{belge.currentVersion}</span>
      </div>

      <div className="izgara">
        <label className="alan">
          <span className="alan__etiket">Başlık</span>
          <input
            type="text"
            value={belge.title ?? ''}
            onChange={(e) => onDegisti({ title: e.target.value })}
          />
          <span className="alan__ipucu">Listede görünen ad ve indirilen dosyanın adı.</span>
        </label>

        <label className="alan">
          <span className="alan__etiket">Mail konusu</span>
          <input
            type="text"
            value={belge.subject ?? ''}
            placeholder="RPA Sprint Kapanış Bilgilendirme"
            onChange={(e) => onDegisti({ subject: e.target.value })}
          />
          <span className="alan__ipucu">Outlook'ta konu satırında görünür.</span>
        </label>
      </div>
    </section>
  )
}
