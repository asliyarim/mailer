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
      {/* Marka giriş sayfasına dönüyor - uygulamalarda beklenen davranış bu
          ve kullanıcı tip seçimine dönmek için başka bir yol aramasın. */}
      <Link className="ust-serit__marka" to="/" aria-label="Giriş sayfasına dön">
        {/* Kurumsal logo. Dosya 128px üretildi, 44px'lik kutuda çiziliyor -
            2x ekranda net kalsın diye. Zemin beyaz: logonun kendi zemini
            beyaz (saydam değil), koyu şeridin üstünde doğrudan durursa
            kenarları kirli görünür. */}
        <div className="ust-serit__isaret">
          <img
            src={`${import.meta.env.BASE_URL}aksa-mailler.png`}
            width="36"
            height="36"
            alt=""
          />
        </div>
        <div>
          <div className="ust-serit__ad">Aksa Mailler</div>
          <div className="ust-serit__alt">Sprint bilgilendirme maili üretici</div>
        </div>
      </Link>

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
