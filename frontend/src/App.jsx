import { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { fetchCurrentUser } from './lib/apiClient.js'
import TopBar from './components/shared/TopBar.jsx'
import DocumentListPage from './components/shared/DocumentListPage.jsx'
import EditorPage from './components/editor/EditorPage.jsx'

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

  useEffect(() => {
    fetchCurrentUser()
      .then((u) => {
        setUser(u)
        setDurum('hazir')
      })
      .catch(() => setDurum('oturumYok'))
  }, [])

  if (durum === 'yukleniyor') {
    return <p style={{ padding: 24 }}>Yükleniyor…</p>
  }

  if (durum === 'oturumYok') {
    // Kabuk disinda acildiysa Odyssey'in giris sayfasina gonder.
    const odysseyUrl = import.meta.env.VITE_ODYSSEY_URL || '/'
    return (
      <div style={{ padding: 24 }}>
        <p>Oturumunuz bulunamadı.</p>
        <a href={odysseyUrl}>Odyssey üzerinden giriş yapın</a>
      </div>
    )
  }

  return (
    <BrowserRouter basename={BASENAME}>
      <TopBar user={user} />
      <Routes>
        <Route path="/" element={<Navigate to="/belgeler" replace />} />
        <Route path="/belgeler" element={<DocumentListPage user={user} />} />
        <Route path="/editor/new" element={<EditorPage user={user} />} />
        <Route path="/editor/:id" element={<EditorPage user={user} />} />
      </Routes>
    </BrowserRouter>
  )
}
