// Önizlemede seçilen satırın yanında beliren araç çubuğu:
// ekle · kopyala · yukarı · aşağı · sil.
//
// Referans uygulamada bu düğmeler mail HTML'inin İÇİNE gömülüydü. Bizde
// olamaz: mail HTML'i .eml'e olduğu gibi giriyor ve Outlook'a düzenleme
// düğmeleri taşıyan bir mail gitmesi kabul edilemez. Bu yüzden düğmeler
// iframe'in DIŞINDA, ana pencerede, satırın hizasına konumlanıyor.

const ARACLAR = [
  { islem: 'ekle', isaret: '＋', baslik: 'Altına satır ekle' },
  { islem: 'kopyala', isaret: '⧉', baslik: 'Satırı çoğalt' },
  { islem: 'yukari', isaret: '↑', baslik: 'Yukarı taşı' },
  { islem: 'asagi', isaret: '↓', baslik: 'Aşağı taşı' },
  { islem: 'sil', isaret: '×', baslik: 'Satırı sil' },
]

export default function SatirAraclari({ kutu, index, toplam, onIslem, onKapat }) {
  return (
    <div
      className="satir-arac"
      style={{ top: kutu.top, left: kutu.left + kutu.width + 8 }}
      role="toolbar"
      aria-label={`${index + 1}. satır araçları`}
    >
      <span className="satir-arac__no">{index + 1}</span>
      {ARACLAR.map((arac) => {
        // İlk satır yukarı, son satır aşağı gidemez - düğme kapalı olsun ki
        // kullanıcı tıklayıp "çalışmıyor" sanmasın.
        const kapali =
          (arac.islem === 'yukari' && index === 0) ||
          (arac.islem === 'asagi' && index >= toplam - 1)
        return (
          <button
            key={arac.islem}
            type="button"
            className={arac.islem === 'sil' ? 'satir-arac__dugme satir-arac__dugme--sil' : 'satir-arac__dugme'}
            title={arac.baslik}
            aria-label={arac.baslik}
            disabled={kapali}
            onClick={() => onIslem(arac.islem)}
          >
            {arac.isaret}
          </button>
        )
      })}
      <button
        type="button"
        className="satir-arac__dugme"
        title="Seçimi kapat"
        aria-label="Seçimi kapat"
        onClick={onKapat}
      >
        ✕
      </button>
    </div>
  )
}
