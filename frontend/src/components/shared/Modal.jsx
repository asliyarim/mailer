// Basit modal. Versiyon gecmisi, silme onayi gibi akislar bunu kullanir.
// Kontrollu bilesen: acik/kapali durumu CAGIRANDA tutulur.

export default function Modal({ acik, baslik, onKapat, children }) {
  if (!acik) return null

  return (
    <div
      onClick={onKapat}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(14, 28, 45, 0.45)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 100,
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: '#fff',
          borderRadius: 6,
          padding: 20,
          minWidth: 420,
          maxWidth: '90vw',
          maxHeight: '85vh',
          overflow: 'auto',
        }}
      >
        <h2 style={{ margin: '0 0 14px', fontSize: 16 }}>{baslik}</h2>
        {children}
      </div>
    </div>
  )
}
