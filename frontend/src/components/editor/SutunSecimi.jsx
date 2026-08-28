// Bölümün hangi alanlarının mailde çizileceği.
//
// Sütunu kapatmak VERİYİ SİLMEZ: satırda kalır, sadece çizilmez. Yeniden
// açınca eski değerler geri gelir (docs/api.md, satır alanları).
//
// Sıra da buradan geliyor - columns dizisinin sırası mailde soldan sağa
// sütun sırasıdır. Kutuyu işaretlemek alanı SONA ekler.

import { ROW_FIELD_LABELS, VARSAYILAN_SUTUNLAR } from '../../lib/mailContent.js'

/** Mail tipine göre seçilebilecek alanlar. */
const SECILEBILIR = {
  KAPANIS: ['sector', 'jira', 'ci', 'process', 'stage', 'stake', 'note', 'gain'],
  PLANLAMA: ['topicType', 'jira', 'summary', 'status', 'sprint', 'expected', 'stake', 'sector', 'department'],
  YONETICI_OZETI: ['sector', 'process', 'note', 'gain'],
}

// Gövde 760px sabit; altıdan fazla sütun Outlook'ta okunmaz hale geliyor.
const ONERILEN_UST_SINIR = 6

export default function SutunSecimi({ templateType, columns, onChange }) {
  const secilebilir = SECILEBILIR[templateType] ?? VARSAYILAN_SUTUNLAR.KAPANIS
  const secili = columns ?? []

  // Kapanış'ta Sektör seçiliyse tablo ona göre gruplanır ve sütun olarak
  // ÇİZİLMEZ - genişlik uyarısı seçilen değil, çizilen sütuna bakmalı.
  const sektoreGoreGruplu = templateType === 'KAPANIS' && secili.includes('sector')
  const cizilen = secili.length - (sektoreGoreGruplu ? 1 : 0)

  function degistir(alan, isaretli) {
    onChange(isaretli ? [...secili, alan] : secili.filter((s) => s !== alan))
  }

  return (
    <div style={{ margin: '10px 0 12px' }}>
      <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 6 }}>
        Mailde gösterilecek alanlar
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px 14px' }}>
        {secilebilir.map((alan) => (
          <label key={alan} style={{ display: 'flex', gap: 5, alignItems: 'center', fontSize: 13 }}>
            <input
              type="checkbox"
              checked={secili.includes(alan)}
              onChange={(e) => degistir(alan, e.target.checked)}
            />
            <span>{ROW_FIELD_LABELS[alan] ?? alan}</span>
          </label>
        ))}
      </div>

      {cizilen > ONERILEN_UST_SINIR && (
        <p style={{ margin: '6px 0 0', fontSize: 12.5, color: '#8a5a09' }}>
          Tabloda {cizilen} sütun çizilecek. Mail gövdesi 760 piksel sabit —
          {' '}{ONERILEN_UST_SINIR}'dan fazlası Outlook'ta okunmakta zorlanır.
        </p>
      )}

      {sektoreGoreGruplu && (
        <p style={{ margin: '6px 0 0', fontSize: 12.5, color: '#6d8296' }}>
          Sektör seçili: tablo sektöre göre gruplanır. Grup başlığı zaten sektörü
          söylediği için ayrı bir sütun olarak çizilmez.
        </p>
      )}

      {secili.length === 0 && (
        <p style={{ margin: '6px 0 0', fontSize: 12.5, color: '#9c3226' }}>
          Hiç sütun seçili değil — bu bölüm mailde çizilmez.
        </p>
      )}
    </div>
  )
}
