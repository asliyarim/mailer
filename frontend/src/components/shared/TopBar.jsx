// Ust serit: uygulama adi + giris yapmis kullanici.
// Sprint 0'in bitis sarti bu bilesenden geciyor - kabuk icinde acildiginda
// kullanicinin adi burada gorunmeli.

export default function TopBar({ user }) {
  return (
    <header
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '10px 16px',
        background: '#12467f',
        color: '#fff',
      }}
    >
      <strong>Aksa Mailer</strong>
      <span>{user?.fullName ?? user?.sicil ?? 'Bilinmeyen kullanıcı'}</span>
    </header>
  )
}
