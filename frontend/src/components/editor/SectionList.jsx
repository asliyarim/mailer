// Bolumler ve iclerindeki satir kartlari.
//
// sections SABIT "analiz + gelistirme" ikilisi DEGIL, bir dizidir - baska bir
// takimin uc bolumu olabilir. Bolum sayisini koda gomme (docs/BRIEF.md §6).

import RowCard from './RowCard.jsx'
import Button from '../shared/Button.jsx'
import { countRows, emptyRow, emptySection, ROW_FIELDS } from '../../lib/mailContent.js'

export default function SectionList({ sections, onChange }) {
  function bolumGuncelle(index, yeniBolum) {
    onChange(sections.map((b, i) => (i === index ? yeniBolum : b)))
  }

  function satirEkle(index) {
    const bolum = sections[index]
    bolumGuncelle(index, { ...bolum, rows: [...bolum.rows, emptyRow()] })
  }

  function satirGuncelle(bolumIndex, satirIndex, yeniSatir) {
    const bolum = sections[bolumIndex]
    bolumGuncelle(bolumIndex, {
      ...bolum,
      rows: bolum.rows.map((s, i) => (i === satirIndex ? yeniSatir : s)),
    })
  }

  function satirSil(bolumIndex, satirIndex) {
    const bolum = sections[bolumIndex]
    bolumGuncelle(bolumIndex, { ...bolum, rows: bolum.rows.filter((_, i) => i !== satirIndex) })
  }

  function bolumEkle() {
    onChange([...sections, emptySection(`bolum-${sections.length + 1}`, 'YENİ BÖLÜM')])
  }

  return (
    <section className="card">
      {/* Sayac SAKLANMAZ, satir sayisindan hesaplanir (docs/BRIEF.md §6). */}
      <h2 className="card__baslik">
        Bölümler ve satırlar · toplam {countRows({ sections })} satır
      </h2>

      {sections.map((bolum, bolumIndex) => (
        <div key={bolum.key} style={{ marginBottom: 20 }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
            <input
              type="text"
              value={bolum.title}
              onChange={(e) => bolumGuncelle(bolumIndex, { ...bolum, title: e.target.value })}
            />
            {/* Ton, temadaki renge cevrilir - burada renk kodu yazilmaz. */}
            <select
              value={bolum.tone}
              onChange={(e) => bolumGuncelle(bolumIndex, { ...bolum, tone: e.target.value })}
            >
              <option value="blue">Mavi</option>
              <option value="green">Yeşil</option>
            </select>
          </div>

          {bolum.rows.map((satir, satirIndex) => (
            <RowCard
              key={satirIndex}
              row={satir}
              columns={bolum.columns ?? ROW_FIELDS}
              sira={satirIndex + 1}
              onChange={(yeni) => satirGuncelle(bolumIndex, satirIndex, yeni)}
              onSil={() => satirSil(bolumIndex, satirIndex)}
            />
          ))}

          <Button onClick={() => satirEkle(bolumIndex)}>Satır ekle</Button>
        </div>
      ))}

      <Button varyant="sessiz" onClick={bolumEkle}>
        Bölüm ekle
      </Button>
    </section>
  )
}
