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
    <div style={{ marginBottom: 14 }}>
      <div className="alan__etiket" style={{ marginBottom: 7 }}>
        Mailde gösterilecek alanlar
      </div>

      <div className="secim-izgara">
        {secilebilir.map((alan) => {
          const acik = secili.includes(alan)
          return (
            <label key={alan} className={acik ? 'secim secim--acik' : 'secim'}>
              <input
                type="checkbox"
                checked={acik}
                onChange={(e) => degistir(alan, e.target.checked)}
              />
              <span>{ROW_FIELD_LABELS[alan] ?? alan}</span>
            </label>
          )
        })}
      </div>

      {cizilen > ONERILEN_UST_SINIR && (
        <div className="uyari uyari--dikkat" style={{ marginTop: 8 }}>
          Tabloda {cizilen} sütun çizilecek. Mail gövdesi 760 piksel sabit —
          {' '}{ONERILEN_UST_SINIR}'dan fazlası Outlook'ta okunmakta zorlanır.
        </div>
      )}

      {/* Sektör artık varsayılan KAPALI geliyor. Açmak yalnızca bir sütun
          eklemiyor, tablonun DÜZENİNİ değiştiriyor - kullanıcı bunu tıklamadan
          önce bilsin. Eskiden açıktı ve sütun çizilmediği için insanlar
          davranışı gördükleri bir şeye bağlayamıyordu. */}
      {templateType === 'KAPANIS' && (
        <p className="alan__ipucu" style={{ marginTop: 8 }}>
          {sektoreGoreGruplu
            ? 'Sektör seçili: tablo sektöre göre gruplanır. Grup başlığı zaten sektörü söylediği için ayrı bir sütun olarak çizilmez.'
            : 'Sektörü açarsanız tablo sektöre göre gruplanır — sütun olarak değil, grup başlığı olarak çizilir.'}
        </p>
      )}

      {secili.length === 0 && (
        <div className="uyari uyari--hata" style={{ marginTop: 8 }}>
          Hiç sütun seçili değil — bu bölüm mailde çizilmez.
        </div>
      )}
    </div>
  )
}
