// Üst şerit: uygulama adı + kayıt durumu + "Belgelerim" + kullanıcı.
//
// "Belgelerim" burada duruyor çünkü uygulama artık doğrudan editörle açılıyor;
// belge listesi karşılama ekranı değil, ihtiyaç oldukça gidilen bir yer.
//
// durum: editörden gelir ("Kaydedildi" / "Kaydedilmemiş değişiklik" gibi).
// Verilmezse rozet hiç çizilmez.

import { Link, useLocation } from 'react-router-dom'

export default function TopBar({ user, durum, durumUyari }) {
  const { pathname } = useLocation()
  const listedeyiz = pathname.startsWith('/belgeler')

  return (
    <header className="ust-serit">
      <div className="ust-serit__marka">
        <div className="ust-serit__isaret" aria-hidden="true">
          {/* Zarf işareti. Emoji değil: platforma göre farklı çizilir ve
              kurumsal ekranda tutarsız durur. */}
          <svg width="21" height="21" viewBox="0 0 24 24" fill="none" aria-hidden="true">
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
        {durum && <span className={durumUyari ? 'rozet rozet--uyari' : 'rozet'}>{durum}</span>}

        <Link className="ust-serit__eylem" to={listedeyiz ? '/' : '/belgeler'}>
          {listedeyiz ? (
            'Editöre dön'
          ) : (
            <>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path
                  d="M4 5.5h6l1.5 2H20v11H4z"
                  stroke="#fff"
                  strokeWidth="1.7"
                  strokeLinejoin="round"
                />
              </svg>
              Belgelerim
            </>
          )}
        </Link>

        <span className="ust-serit__kullanici">
          {user?.fullName ?? user?.sicil ?? 'Bilinmeyen kullanıcı'}
        </span>
      </div>
    </header>
  )
}
