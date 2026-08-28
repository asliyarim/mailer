// Bölümler ve içlerindeki satır kartları.
//
// sections SABİT "analiz + geliştirme" ikilisi DEĞİL, bir dizidir - başka bir
// takımın üç bölümü olabilir. Bölüm sayısını koda gömme (docs/BRIEF.md §6).

import RowCard from './RowCard.jsx'
import SutunSecimi from './SutunSecimi.jsx'
import Button from '../shared/Button.jsx'
import { countRows, emptyRow, emptySection, VARSAYILAN_SUTUNLAR } from '../../lib/mailContent.js'

const TON_SECENEKLERI = [
  { deger: 'blue', etiket: 'Mavi' },
  { deger: 'green', etiket: 'Yeşil' },
]

export default function SectionList({ sections, templateType, onChange }) {
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

  function bolumSil(index) {
    const bolum = sections[index]
    // Dolu bir bölümü tek tıkla silmek, 10 satırlık emeği geri alınamaz
    // şekilde götürür. Boşsa sormaya gerek yok.
    if (bolum.rows.length > 0) {
      const onay = window.confirm(
        `"${bolum.title}" bölümü ${bolum.rows.length} satırla birlikte silinecek. Devam edilsin mi?`
      )
      if (!onay) return
    }
    onChange(sections.filter((_, i) => i !== index))
  }

  function bolumEkle() {
    onChange([
      ...sections,
      emptySection(`bolum-${sections.length + 1}`, 'YENİ BÖLÜM', 'blue', templateType),
    ])
  }

  const toplamSatir = countRows({ sections })

  return (
    <section className="card">
      <h2 className="card__baslik">
        <span className="card__no">3</span>
        Bölümler ve satırlar
      </h2>

      {/* Sayaçlar SAKLANMAZ, satır sayısından hesaplanır (docs/BRIEF.md §6).
          Mailde de aynı rakamlar çıkar; burada göstermek kullanıcıya maili
          açmadan "kaç kalem gidiyor" sorusunun cevabını veriyor. */}
      {sections.length > 0 && (
        <div className="kpi-serit">
          <div className="kpi">
            <span className="kpi__sayi">{toplamSatir}</span>
            <span className="kpi__etiket">Toplam satır</span>
          </div>
          {sections.map((bolum) => (
            <div
              key={bolum.key}
              className={bolum.tone === 'green' ? 'kpi kpi--yesil' : 'kpi'}
            >
              <span className="kpi__sayi">{bolum.rows.length}</span>
              <span className="kpi__etiket">{bolum.title || 'Adsız bölüm'}</span>
            </div>
          ))}
        </div>
      )}

      {sections.length === 0 && (
        <div className="bos-durum">
          <p className="bos-durum__baslik">Henüz bölüm yok</p>
          <p className="bos-durum__metin">
            Bölüm, mailde bir tablo demek — "Analiz Çalışmaları" gibi. Satırlar
            bölümün içine girer.
          </p>
          <Button varyant="birincil" onClick={bolumEkle}>
            İlk bölümü ekle
          </Button>
        </div>
      )}

      {sections.map((bolum, bolumIndex) => {
        const sutunlar =
          bolum.columns ?? VARSAYILAN_SUTUNLAR[templateType] ?? VARSAYILAN_SUTUNLAR.KAPANIS
        return (
          <div className="bolum" key={bolum.key}>
            <div className="bolum__ust">
              <span className={`nokta nokta--${bolum.tone}`} aria-hidden="true" />
              <input
                type="text"
                aria-label="Bölüm başlığı"
                value={bolum.title}
                onChange={(e) => bolumGuncelle(bolumIndex, { ...bolum, title: e.target.value })}
              />
              <Button
                varyant="tehlike"
                boyut="kucuk"
                onClick={() => bolumSil(bolumIndex)}
                baslik="Bölümü sil"
              >
                Sil
              </Button>
            </div>

            {/* Ton, temadaki renge çevrilir - burada renk kodu yazılmaz. */}
            <div className="satir-arasi" style={{ marginBottom: 12 }}>
              <span className="alan__etiket">Renk tonu</span>
              <div className="seg-grup">
                {TON_SECENEKLERI.map((secenek) => (
                  <button
                    key={secenek.deger}
                    type="button"
                    className={bolum.tone === secenek.deger ? 'seg seg--secili' : 'seg'}
                    onClick={() => bolumGuncelle(bolumIndex, { ...bolum, tone: secenek.deger })}
                  >
                    {secenek.etiket}
                  </button>
                ))}
              </div>
            </div>

            <SutunSecimi
              templateType={templateType}
              columns={sutunlar}
              onChange={(yeniSutunlar) =>
                bolumGuncelle(bolumIndex, { ...bolum, columns: yeniSutunlar })
              }
            />

            {bolum.rows.map((satir, satirIndex) => (
              <RowCard
                key={satirIndex}
                row={satir}
                columns={sutunlar}
                sira={satirIndex + 1}
                onChange={(yeni) => satirGuncelle(bolumIndex, satirIndex, yeni)}
                onSil={() => satirSil(bolumIndex, satirIndex)}
              />
            ))}

            <Button
              varyant={bolum.tone === 'green' ? 'yesil' : 'ikincil'}
              boyut="kucuk"
              onClick={() => satirEkle(bolumIndex)}
            >
              + Satır ekle
            </Button>
          </div>
        )
      })}

      {sections.length > 0 && (
        <div style={{ marginTop: 14 }}>
          <Button varyant="sessiz" onClick={bolumEkle}>
            + Bölüm ekle
          </Button>
        </div>
      )}
    </section>
  )
}
