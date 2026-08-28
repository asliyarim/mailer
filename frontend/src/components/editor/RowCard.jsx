// TEK BIR SATIR KARTI - sol paneldeki "bir tablo satiri" bunun karsiligi.
// Tablo hucresine tiklayip yazma yok; her satir acilip doldurulan bir kart.
//
// Bu dosya, Yardimci 1'in kopyalayacagi ORNEK KART'tir (yol haritasi,
// Sprint 1). Planlama satir karti ve yonetici ozeti alanlari bu desenden
// turetilecek: kontrollu bilesen, durum yukarida (EditorPage), asagi sadece
// deger + onChange iner.
//
// Satirlar NESNE tutar, dizi degil - alanlara anahtarla erisilir (row.sector),
// indeksle degil (bkz. docs/BRIEF.md §6).

import { KONU_TURLERI, ROW_FIELD_LABELS } from '../../lib/mailContent.js'
import Button from '../shared/Button.jsx'

// Uzun metin alan alanlar tek satirlik input yerine textarea alir.
const COK_SATIRLI = ['note', 'expected', 'summary', 'stake']

export default function RowCard({ row, columns, sira, onChange, onSil }) {
  function alanDegisti(alan, deger) {
    onChange({ ...row, [alan]: deger })
  }

  return (
    <div className="card" style={{ background: '#fbfcfd' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
        <strong>{sira}. satır</strong>
        <Button varyant="sessiz" onClick={onSil}>
          Sil
        </Button>
      </div>

      <div style={{ display: 'grid', gap: 8 }}>
        {columns.map((alan) => (
          <label key={alan} style={{ display: 'grid', gap: 4 }}>
            <span>{ROW_FIELD_LABELS[alan] ?? alan}</span>
            {alan === 'topicType' ? (
              <select value={row[alan] ?? ''} onChange={(e) => alanDegisti(alan, e.target.value)}>
                <option value="">—</option>
                {KONU_TURLERI.map((tur) => (
                  <option key={tur} value={tur}>
                    {tur}
                  </option>
                ))}
              </select>
            ) : COK_SATIRLI.includes(alan) ? (
              // Cok satirli metin: satir sonlari KORUNUR - mailde tek satira
              // yapismamasi sunucudaki renderer'in isi, ama girdiyi burada
              // kirpma (bkz. docs/BRIEF.md, Kural 2 sonundaki not).
              <textarea
                rows={3}
                value={row[alan] ?? ''}
                onChange={(e) => alanDegisti(alan, e.target.value)}
              />
            ) : (
              <input
                type="text"
                value={row[alan] ?? ''}
                onChange={(e) => alanDegisti(alan, e.target.value)}
              />
            )}
          </label>
        ))}
      </div>
    </div>
  )
}
