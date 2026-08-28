import { useCallback, useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { fetchCurrentUser } from './lib/apiClient.js'
import TopBar from './components/shared/TopBar.jsx'
import DocumentListPage from './components/shared/DocumentListPage.jsx'
import EditorPage from './components/editor/EditorPage.jsx'
import YeniBelgeForm from './components/editor/YeniBelgeForm.jsx'

// Rotalar: / · /belgeler · /editor/new · /editor/:id
//
// basename normalde Vite'in base'i (/mailer/) - kabuk icinde o onekten
// servis ediliyoruz. Ama uygulamaya DOGRUDAN kokten girildiginde (yerelde
// localhost:5174) tarayicinin yolu "/" olur, basename "/mailer/" ile
// eslesmez ve router HICBIR SEY cizmez - ekran bombos kalir, konsolda hata
// da yoktur. O yuzden onek gercekten yoksa "/"e duseriz.
const VITE_BASE = import.meta.env.BASE_URL || '/'
const BASENAME = window.location.pathname.startsWith(VITE_BASE) ? VITE_BASE : '/'

export default function App() {
  const [user, setUser] = useState(null)
  const [durum, setDurum] = useState('yukleniyor') // yukleniyor | hazir | oturumYok

  // Üst şeritteki kayıt rozeti. Editör kendi durumunu buraya bildirir;
  // rozet uygulamanın tepesinde olduğu için form kaydırılsa da görünür.
  const [kayitDurumu, setKayitDurumu] = useState(null)

  useEffect(() => {
    fetchCurrentUser()
      .then((u) => {
        setUser(u)
        setDurum('hazir')
      })
      .catch(() => setDurum('oturumYok'))
  }, [])

  // useCallback: EditorPage bunu useEffect bağımlılığı olarak kullanıyor,
  // her çizimde yeni fonksiyon üretilirse effect sonsuz döner.
  const durumBildir = useCallback((yeni) => setKayitDurumu(yeni), [])

  if (durum === 'yukleniyor') {
    return (
      <div className="durum-ekrani">
        <p>Yükleniyor…</p>
      </div>
    )
  }

  if (durum === 'oturumYok') {
    // Kabuk disinda acildiysa Odyssey'in giris sayfasina gonder.
    const odysseyUrl = import.meta.env.VITE_ODYSSEY_URL || '/'
    return (
      <div className="durum-ekrani">
        <div>
          <p className="bos-durum__baslik">Oturumunuz bulunamadı</p>
          <p className="bos-durum__metin">
            Aksa Mailer, Odyssey oturumuyla çalışır. Giriş yaptıktan sonra
            uygulamayı katalogdan yeniden açın.
          </p>
          <a className="btn btn--birincil" href={odysseyUrl}>
            Odyssey'e git
          </a>
        </div>
      </div>
    )
  }

  return (
    <BrowserRouter basename={BASENAME}>
      <TopBar user={user} durum={kayitDurumu?.metin} durumUyari={kayitDurumu?.uyari} />
      <Routes>
        <Route path="/" element={<Navigate to="/belgeler" replace />} />
        <Route path="/belgeler" element={<DocumentListPage user={user} />} />
        <Route path="/editor/new" element={<YeniBelgeForm />} />
        <Route path="/editor/:id" element={<EditorPage user={user} onDurum={durumBildir} />} />
      </Routes>
    </BrowserRouter>
  )
}
