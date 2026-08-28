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
      <h2 className="card__baslik">1 · Belge ayarları</h2>

      <div style={{ display: 'grid', gap: 12 }}>
        <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', fontSize: 14 }}>
          <span>
            <strong>Mail tipi:</strong> {TEMPLATE_LABELS[belge.templateType] ?? belge.templateType}
          </span>
          <span>
            <strong>Tema:</strong> {belge.themeKey}
          </span>
          <span>
            <strong>Sürüm:</strong> {belge.currentVersion}
          </span>
        </div>

        <label style={{ display: 'grid', gap: 4 }}>
          <span>Başlık <small style={{ color: '#6d8296' }}>(listede görünen ad)</small></span>
          <input
            type="text"
            value={belge.title ?? ''}
            onChange={(e) => onDegisti({ title: e.target.value })}
          />
        </label>

        <label style={{ display: 'grid', gap: 4 }}>
          <span>Konu <small style={{ color: '#6d8296' }}>(Outlook'ta görünen konu satırı)</small></span>
          <input
            type="text"
            value={belge.subject ?? ''}
            placeholder="RPA Sprint Kapanış Bilgilendirme"
            onChange={(e) => onDegisti({ subject: e.target.value })}
          />
        </label>

        <div>
          <Button varyant="sessiz" onClick={onVersiyonlar}>
            Sürüm geçmişi
          </Button>
        </div>
      </div>
    </section>
  )
}
