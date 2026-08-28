// Tek düğme bileşeni. Yeni bir düğme görünümü gerekiyorsa BURAYA varyant
// eklenir — bileşenlerin içine dağılmış inline stiller birikmesin.
//
// Görünümün kendisi index.css'te (.btn ve .btn--*). Burada sadece hangi
// sınıfın seçileceği var; renk kodu bu dosyada yazılmaz.

const VARYANTLAR = {
  birincil: 'btn--birincil', // ana eylem: Kaydet, Oluştur
  ikincil: 'btn--ikincil', // ikinci sıra: İndir, Sürüm geçmişi
  yesil: 'btn--yesil', // olumlu/ekleme: Satır ekle
  sessiz: 'btn--sessiz', // düşük vurgu: Vazgeç, geri dön
  tehlike: 'btn--tehlike', // silme
}

export default function Button({
  varyant = 'ikincil',
  boyut,
  disabled,
  onClick,
  type = 'button',
  baslik,
  children,
}) {
  const siniflar = ['btn', VARYANTLAR[varyant] ?? VARYANTLAR.ikincil]
  if (boyut === 'kucuk') siniflar.push('btn--kucuk')

  return (
    <button
      type={type}
      className={siniflar.join(' ')}
      onClick={onClick}
      disabled={disabled}
      title={baslik}
    >
      {children}
    </button>
  )
}
