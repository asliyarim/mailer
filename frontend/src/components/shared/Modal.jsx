// Basit modal. Sürüm geçmişi, silme onayı gibi akışlar bunu kullanır.
// Kontrollü bileşen: açık/kapalı durumu ÇAĞIRANDA tutulur.

import { useEffect } from 'react'

export default function Modal({ acik, baslik, onKapat, children }) {
  // ESC ile kapanmalı: fare zorunluluğu olan bir modal klavyeyle çalışan
  // kullanıcıyı içeride kilitler.
  useEffect(() => {
    if (!acik) return
    function tusaBasildi(e) {
      if (e.key === 'Escape') onKapat()
    }
    document.addEventListener('keydown', tusaBasildi)
    return () => document.removeEventListener('keydown', tusaBasildi)
  }, [acik, onKapat])

  if (!acik) return null

  return (
    <div className="modal-zemin" onClick={onKapat}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={baslik}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__baslik">{baslik}</h2>
        {children}
      </div>
    </div>
  )
}
