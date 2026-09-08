// Şablon kartındaki minik mail çizimi.
//
// NE OLDUĞU: mailin ŞEMASI - ekran görüntüsü değil. Kartın ~230 piksellik
// genişliğinde metin zaten okunmaz; okunan şey MAİLİN ŞEKLİ: kaç bölüm var,
// sayaç şeridi var mı, kararlar numaralı mı. Dört tipi birbirinden ayıran
// da tam olarak bu.
//
// NEDEN GERÇEK EKRAN GÖRÜNTÜSÜ DEĞİL: PNG üretmek için başsız tarayıcı
// gerekiyor, bu makinede ve CI'da yok. Gerçek görüntü istenirse
// scripts/sablon-onizleme-uret.js mailin GERÇEK HTML'ini üretiyor; ondan
// alınan ekran görüntüleri bu çizimlerin yerine konabilir.
//
// ÇİZİMLER GERÇEK YAPIYI YANSITIR (sunucudaki varsayılan içerikten okundu):
//   KAPANIS             analysis + development             → iki tablo
//   PLANLAMA            topics                             → tek uzun tablo
//   YONETICI_OZETI      sayaçlar + discussed/decisions/...  → sayaç şeridi
//   TOPLANTI_CIKTILARI  1./2./3. numaralı üç bölüm          → numaralı bloklar
//
// GEOMETRI: viewBox oranı (200×98 ≈ 2.04) CSS kutusuyla (230×112 ≈ 2.05)
// bilerek eşleştirildi. Tutmadığı ilk sürümde çizimin altı kırpılıyordu ve
// dört kart da "koyu bir blok" gibi duruyordu.
//
// Renkler CSS değişkenlerinden gelir; palet değişirse çizim de değişir.

import { TEMPLATE_TYPES } from '../../lib/mailContent.js'

const G = 200
const Y = 98

const HERO_Y = 26

/** Tablo satırı - iki hücreli, ikinci sütun daha dar. */
function Satir({ y }) {
  return (
    <g>
      <rect x="14" y={y} width="46" height="2.5" rx="1.25" fill="var(--cizgi)" />
      <rect x="66" y={y} width="78" height="2.5" rx="1.25" fill="var(--cizgi)" />
      <rect x="150" y={y} width="36" height="2.5" rx="1.25" fill="var(--cizgi)" />
    </g>
  )
}

/** Bölüm başlığı bandı + tablo satırları. */
function Bolum({ y, bant, satirlar, numara }) {
  return (
    <g>
      <rect x="10" y={y} width="180" height="7" rx="1.5" fill={bant} />
      {numara && (
        <text x="13.5" y={y + 5.4} fontSize="5" fill="#ffffff" fontWeight="700">
          {numara}
        </text>
      )}
      {/* Sütun başlığı şeridi - mailde tablo başlıkları soluk mavi zeminde. */}
      <rect x="10" y={y + 8} width="180" height="4" fill="var(--mavi-soluk)" />
      {Array.from({ length: satirlar }, (_, i) => (
        <Satir key={i} y={y + 16 + i * 6} />
      ))}
    </g>
  )
}

/** Üç sayaç kutusu - Yönetici Özeti'ne özgü şerit. */
function Sayaclar({ y }) {
  return (
    <g>
      {[10, 74, 138].map((x) => (
        <g key={x}>
          <rect x={x} y={y} width="52" height="15" rx="2.5" fill="var(--panel)" stroke="var(--cizgi)" strokeWidth="0.8" />
          <rect x={x + 23} y={y + 3.5} width="6" height="4.5" rx="1" fill="var(--mavi)" />
          <rect x={x + 13} y={y + 10} width="26" height="2" rx="1" fill="var(--cizgi)" />
        </g>
      ))}
    </g>
  )
}

const CIZIMLER = {
  // İki tablo. İkinci bant yeşil: mailde "Geliştirme Çalışmaları" yeşil ton.
  [TEMPLATE_TYPES.KAPANIS]: (
    <>
      <Bolum y={31} bant="var(--lacivert)" satirlar={2} />
      <Bolum y={65} bant="var(--yesil)" satirlar={2} />
    </>
  ),

  // Tek ama uzun tablo: bütün sprint konuları tek listede.
  [TEMPLATE_TYPES.PLANLAMA]: <Bolum y={31} bant="var(--yesil)" satirlar={7} />,

  // Sayaç şeridi + bölümler. Sayaçlar yalnızca bu tipte var.
  [TEMPLATE_TYPES.YONETICI_OZETI]: (
    <>
      <Sayaclar y={30} />
      <Bolum y={51} bant="var(--lacivert)" satirlar={2} />
      <Bolum y={85} bant="var(--mavi)" satirlar={0} />
    </>
  ),

  // Numaralı bölümler: 1. konuşulanlar, 2. kararlar, 3. aksiyonlar.
  [TEMPLATE_TYPES.TOPLANTI_CIKTILARI]: (
    <>
      <Bolum y={31} bant="var(--lacivert)" satirlar={1} numara="1" />
      <Bolum y={57} bant="var(--lacivert)" satirlar={1} numara="2" />
      <Bolum y={83} bant="var(--mavi)" satirlar={0} numara="3" />
    </>
  ),
}

export default function SablonOnizleme({ tip }) {
  return (
    <svg
      className="tip-karti__onizleme"
      viewBox={`0 0 ${G} ${Y}`}
      role="img"
      aria-label="Bu şablonun mail düzeni"
      preserveAspectRatio="xMidYMid meet"
    >
      <rect x="0" y="0" width={G} height={Y} fill="var(--panel)" />

      {/* Hero: koyu bant, solda başlık satırları, sağda maskot yuvarlağı
          ve Aksa logosunun durduğu beyaz şerit. */}
      <rect x="0" y="0" width={G} height={HERO_Y} fill="var(--lacivert)" />
      <rect x="10" y="7" width="66" height="4.5" rx="2.25" fill="#ffffff" opacity="0.94" />
      <rect x="10" y="15" width="42" height="3.5" rx="1.75" fill="#ffffff" opacity="0.5" />
      <rect x="132" y="6" width="24" height="6" rx="1.5" fill="#ffffff" opacity="0.85" />
      <circle cx="176" cy="14" r="9.5" fill="#ffffff" opacity="0.15" />
      <circle cx="176" cy="14" r="6" fill="#ffffff" opacity="0.24" />

      {CIZIMLER[tip]}
    </svg>
  )
}
