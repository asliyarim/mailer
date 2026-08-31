// Önizlemede tıklanan alanın ÜSTÜNE açılan düzenleme kutusu.
//
// Kutu iframe'in İÇİNDE değil, ana pencerede duruyor ve mailin üstüne
// konumlanıyor. Sebep: iframe'in içine yazmak, mail HTML'ini istemcide
// değiştirmek olurdu (Mimari Kural 1). contenteditable de kullanılmıyor.
//
// Yazılan değer aynı React state'ine gidiyor — soldaki form da oraya
// yazıyor. İki giriş yolu var, tek kaynak var.

import { useEffect, useRef } from 'react'

export default function SatirIciDuzenleyici({
  etiket,
  deger,
  kutu,
  cokSatirli,
  onDegisti,
  onBitti,
  onVazgec,
}) {
  const girisRef = useRef(null)

  // Kutu açılır açılmaz odak ve metin seçimi: kullanıcı tıkladığı yerde
  // hemen yazmaya başlayabilsin.
  useEffect(() => {
    const giris = girisRef.current
    if (!giris) return
    giris.focus()
    giris.select()
  }, [])

  function tusaBasildi(e) {
    if (e.key === 'Escape') {
      e.preventDefault()
      onVazgec()
      return
    }
    // Tek satırlıkta Enter kaydeder. Çok satırlıda Enter yeni satır açar,
    // kaydetmek için Ctrl+Enter ya da kutudan çıkmak gerekir.
    if (e.key === 'Enter' && (!cokSatirli || e.ctrlKey || e.metaKey)) {
      e.preventDefault()
      onBitti()
    }
  }

  const ortak = {
    ref: girisRef,
    value: deger,
    onChange: (e) => onDegisti(e.target.value),
    onKeyDown: tusaBasildi,
    onBlur: onBitti,
    'aria-label': etiket,
  }

  return (
    <div
      className="satir-ici"
      style={{
        top: kutu.top,
        left: kutu.left,
        width: Math.max(kutu.width, 220),
      }}
    >
      <div className="satir-ici__etiket">
        {etiket}
        <span className="satir-ici__ipucu">
          {cokSatirli ? 'Ctrl+Enter kaydeder · Esc vazgeçer' : 'Enter kaydeder · Esc vazgeçer'}
        </span>
      </div>
      {cokSatirli ? (
        <textarea rows={3} {...ortak} />
      ) : (
        <input type="text" {...ortak} />
      )}
    </div>
  )
}
