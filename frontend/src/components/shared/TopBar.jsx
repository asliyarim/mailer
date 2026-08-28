// Üst şerit: uygulama adı + kayıt durumu + giriş yapmış kullanıcı.
// Sprint 0'ın bitiş şartı bu bileşenden geçiyor — kabuk içinde açıldığında
// kullanıcının adı burada görünmeli.
//
// durum: editörden gelir ("Kaydedildi" / "Kaydedilmemiş değişiklik" gibi).
// Verilmezse rozet hiç çizilmez; liste ekranlarında kaydedilecek bir şey yok.

export default function TopBar({ user, durum, durumUyari }) {
  return (
    <header className="ust-serit">
      <div className="ust-serit__marka">
        <div className="ust-serit__isaret" aria-hidden="true">
          {/* Zarf işareti. Emoji değil: platforma göre farklı çizilir ve
              kurumsal ekranda tutarsız durur. */}
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <rect x="2.5" y="4.5" width="19" height="15" rx="2.5" stroke="#fff" strokeWidth="1.7" />
            <path d="M3.5 6.5 12 13l8.5-6.5" stroke="#fff" strokeWidth="1.7" strokeLinecap="round" />
          </svg>
        </div>
        <div>
          <div className="ust-serit__ad">Aksa Mailer</div>
          <div className="ust-serit__alt">Sprint bilgilendirme maili üretici</div>
        </div>
      </div>

      <div className="ust-serit__sag">
        {durum && (
          <span className={durumUyari ? 'rozet rozet--uyari' : 'rozet'}>{durum}</span>
        )}
        <span className="ust-serit__kullanici">
          {user?.fullName ?? user?.sicil ?? 'Bilinmeyen kullanıcı'}
        </span>
      </div>
    </header>
  )
}
