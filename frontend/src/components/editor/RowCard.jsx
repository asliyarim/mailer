// TEK BİR SATIR KARTI - sol paneldeki "bir tablo satırı" bunun karşılığı.
// Tablo hücresine tıklayıp yazma yok; her satır açılıp doldurulan bir kart.
//
// Bu dosya, Yardımcı 1'in kopyalayacağı ÖRNEK KART'tır (yol haritası,
// Sprint 1). Planlama satır kartı ve yönetici özeti alanları bu desenden
// türetilecek: kontrollü bileşen, durum yukarıda (EditorPage), aşağı sadece
// değer + onChange iner.
//
// Satırlar NESNE tutar, dizi değil - alanlara anahtarla erişilir (row.sector),
// indeksle değil (bkz. docs/BRIEF.md §6).
//
// Açık/kapalı durumu BURADA tutulur çünkü içerik değil, görünüm durumu:
// kaydedilmez, mailde bir karşılığı yoktur. İçeriğe ait her şey yukarıda.

import { useState } from 'react'
import { KONU_TURLERI, ROW_FIELD_LABELS, SEKTORLER } from '../../lib/mailContent.js'
import Button from '../shared/Button.jsx'

// Uzun metin alan alanlar tek satırlık input yerine textarea alır.
const COK_SATIRLI = ['note', 'expected', 'summary', 'stake']

// Sabit listeden seçilen alanlar. Serbest metin olsalardı aynı şeyin iki
// yazımı iki ayrı değer olurdu (sektör gruplaması buna bakıyor).
const LISTELI = {
  sector: SEKTORLER,
  topicType: KONU_TURLERI,
}

// Kapalı kartta hangi alan özet olarak gösterilsin - ilk dolu olan kazanır.
const OZET_SIRASI = ['jira', 'process', 'summary', 'sector', 'topicType']

function ozetMetni(row, columns) {
  const alan = OZET_SIRASI.find((a) => columns.includes(a) && row[a]?.trim())
  if (!alan) return 'Boş satır'
  const metin = row[alan].trim().replace(/\s+/g, ' ')
  return metin.length > 60 ? `${metin.slice(0, 60)}…` : metin
}

export default function RowCard({ row, columns, sira, tone = 'blue', onChange, onSil }) {
  const [acik, setAcik] = useState(true)

  function alanDegisti(alan, deger) {
    onChange({ ...row, [alan]: deger })
  }

  return (
    <div className={tone === 'green' ? 'satir-karti satir-karti--green' : 'satir-karti'}>
      <div className="satir-karti__ust">
        <span className={`nokta nokta--${tone}`} aria-hidden="true" />
        <span className="satir-karti__no">{sira}. satır</span>

        {!acik && <span className="satir-karti__ozet">{ozetMetni(row, columns)}</span>}

        <span className={acik ? 'sag-yasla' : ''}>
          <Button varyant="sessiz" boyut="kucuk" onClick={() => setAcik((o) => !o)}>
            {acik ? 'Daralt' : 'Genişlet'}
          </Button>
        </span>
        <Button varyant="tehlike" boyut="kucuk" onClick={onSil} baslik="Satırı sil">
          Sil
        </Button>
      </div>

      {acik && (
        <div className="izgara izgara--2">
          {columns.map((alan) => {
            const cokSatirli = COK_SATIRLI.includes(alan)
            const secenekler = LISTELI[alan]

            return (
              <label
                className="alan"
                key={alan}
                style={cokSatirli ? { gridColumn: '1 / -1' } : undefined}
              >
                <span className="alan__etiket">{ROW_FIELD_LABELS[alan] ?? alan}</span>

                {secenekler ? (
                  <select value={row[alan] ?? ''} onChange={(e) => alanDegisti(alan, e.target.value)}>
                    <option value="">—</option>
                    {/* Kayıtlı değer listede yoksa kaybolmasın: başa eklenir. */}
                    {row[alan] && !secenekler.includes(row[alan]) && (
                      <option value={row[alan]}>{row[alan]}</option>
                    )}
                    {secenekler.map((secenek) => (
                      <option key={secenek} value={secenek}>
                        {secenek}
                      </option>
                    ))}
                  </select>
                ) : cokSatirli ? (
                  // Çok satırlı metin: satır sonları KORUNUR - mailde tek satıra
                  // yapışmaması sunucudaki renderer'ın işi, ama girdiyi burada
                  // kırpma (bkz. docs/BRIEF.md, Kural 2 sonundaki not).
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
            )
          })}
        </div>
      )}
    </div>
  )
}
