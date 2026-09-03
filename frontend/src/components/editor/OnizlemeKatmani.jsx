// Mailin ÜSTÜNDE duran düzenleme katmanı: her satırın yanında sıralama
// araçları, her bölümün altında "+ satır ekle".
//
// Neden mailin üstünde: referans uygulamada bu düğmeler mail HTML'inin
// İÇİNE gömülüydü. Bizde olamaz - o HTML .eml'e olduğu gibi giriyor ve
// Outlook'a düzenleme düğmesi taşıyan bir mail gitmesi kabul edilemez.
// Düğmeler iframe'in dışında, ana pencerede, ölçülen konumlara yerleşiyor.

// "+" düğmesinin ölçüsü ve şeridin sağ kenarından boşluğu. CSS ile aynı
// olmak zorunda: konum JS'te hesaplanıyor, görünüm CSS'te.
const DUGME_OLCUSU = 28
const SAG_BOSLUK = 14

const SATIR_ARACLARI = [
  { islem: 'yukari', isaret: '↑', baslik: 'Yukarı taşı' },
  { islem: 'asagi', isaret: '↓', baslik: 'Aşağı taşı' },
  { islem: 'ekle', isaret: '＋', baslik: 'Altına satır ekle' },
  { islem: 'kopyala', isaret: '⧉', baslik: 'Satırı çoğalt' },
  { islem: 'sil', isaret: '×', baslik: 'Satırı sil' },
]

export default function OnizlemeKatmani({ satirlar, bolumler, onSatirIslemi, onBolumeEkle }) {
  return (
    <>
      {satirlar.map((satir) => (
        <div
          key={satir.adres}
          className="satir-arac"
          style={{ top: satir.kutu.top + 2, left: satir.kutu.left + satir.kutu.width + 6 }}
          role="toolbar"
          aria-label={`${satir.gorunenSira}. satır araçları`}
        >
          {SATIR_ARACLARI.map((arac) => {
            // Gruplanmış tabloda satır yalnızca KENDİ GRUBU içinde taşınabilir.
            // Grup sınırındaki satırda düğme kapalı - basılıp hiçbir şey
            // olmaması, kapalı olmasından daha kötü.
            const kapali =
              (arac.islem === 'yukari' && !satir.yukari) ||
              (arac.islem === 'asagi' && !satir.asagi)
            return (
              <button
                key={arac.islem}
                type="button"
                className={
                  arac.islem === 'sil'
                    ? 'satir-arac__dugme satir-arac__dugme--sil'
                    : 'satir-arac__dugme'
                }
                title={arac.baslik}
                aria-label={arac.baslik}
                disabled={kapali}
                onClick={() => onSatirIslemi(satir, arac.islem)}
              >
                {arac.isaret}
              </button>
            )
          })}
        </div>
      ))}

      {/* Bölüm BOŞ olsa da başlığı çiziliyor, yani bu düğme her zaman var:
          hiç satırı olmayan bölüme de buradan satır eklenebiliyor.

          Renkli BAŞLIK ŞERİDİNİN sağ ucunda, başlıkla aynı hizada duruyor.
          Önceden bölümün altındaydı ve bir sonraki bölümün başlığına daha
          yakın görünüyordu - hangi bölüme eklediği anlaşılmıyordu. */}
      {bolumler.map((bolum) => (
        <button
          key={bolum.key}
          type="button"
          className="bolume-ekle"
          title={bolum.baslik ? `${bolum.baslik} bölümüne satır ekle` : 'Satır ekle'}
          aria-label={bolum.baslik ? `${bolum.baslik} bölümüne satır ekle` : 'Satır ekle'}
          style={{
            // Şeridin dikey ortasına: (yükseklik - düğme) / 2
            top: bolum.kutu.top + (bolum.kutu.height - DUGME_OLCUSU) / 2,
            left: bolum.kutu.left + bolum.kutu.width - DUGME_OLCUSU - SAG_BOSLUK,
          }}
          onClick={() => onBolumeEkle(bolum.key)}
        >
          +
        </button>
      ))}
    </>
  )
}
