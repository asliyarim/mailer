// Tek dugme bileseni. Yeni bir dugme gorunumu gerekiyorsa BURAYA varyant
// eklenir - bilesenlerin icine dagilmis inline stiller birikmesin.

const VARYANTLAR = {
  birincil: { background: '#12467f', color: '#fff', border: '1px solid #12467f' },
  ikincil: { background: '#fff', color: '#12467f', border: '1px solid #12467f' },
  sessiz: { background: 'transparent', color: '#3d4f64', border: '1px solid transparent' },
}

export default function Button({ varyant = 'ikincil', disabled, onClick, type = 'button', children }) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      style={{
        ...VARYANTLAR[varyant],
        padding: '7px 14px',
        borderRadius: 4,
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.55 : 1,
        font: 'inherit',
      }}
    >
      {children}
    </button>
  )
}
