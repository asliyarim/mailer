// Editorun TEK durum sahibi. Alt bilesenlerin hepsi kontrolludur: kendi
// icinde icerik tutmaz, deger + onChange alir. Yeni bir form alani eklerken
// durumu buraya koy, bilesenin icine degil.
//
// Sol panel numarali kartlardan olusur (docs/BRIEF.md §2):
//   1 · Belge ayarlari    2 · Mail bilgileri
//   3 · Bolumler          4 · Alt notlar

import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { fetchDocument, saveDocument } from '../../lib/apiClient.js'
import { emptyContent, TEMPLATE_TYPES, validateContent } from '../../lib/mailContent.js'
import BelgeAyarlari from './BelgeAyarlari.jsx'
import MetaForm from './MetaForm.jsx'
import SectionList from './SectionList.jsx'
import NotesForm from './NotesForm.jsx'
import PreviewPane from './PreviewPane.jsx'
import TopActions from './TopActions.jsx'
import VersiyonGecmisi from './VersiyonGecmisi.jsx'

export default function EditorPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [belge, setBelge] = useState(null)
  const [content, setContent] = useState(emptyContent())
  const [hatalar, setHatalar] = useState([])
  const [kaydediliyor, setKaydediliyor] = useState(false)
  const [kaydedilmemis, setKaydedilmemis] = useState(false)
  const [gecmisAcik, setGecmisAcik] = useState(false)

  useEffect(() => {
    if (!id) return
    fetchDocument(id)
      .then((d) => {
        setBelge(d)
        setContent(d.content)
        setKaydedilmemis(false)
      })
      .catch((e) => setHatalar([e.message]))
  }, [id])

  // Icerigin bir dalini gunceller: patch('header', { title: '...' })
  function patch(alan, deger) {
    setContent((onceki) => ({ ...onceki, [alan]: { ...onceki[alan], ...deger } }))
    setKaydedilmemis(true)
  }

  function icerikDegisti(guncelleyici) {
    setContent(guncelleyici)
    setKaydedilmemis(true)
  }

  function belgeDegisti(alanlar) {
    setBelge((o) => ({ ...o, ...alanlar }))
    setKaydedilmemis(true)
  }

  async function kaydet() {
    const bulunanHatalar = validateContent(content)
    setHatalar(bulunanHatalar)
    if (bulunanHatalar.length > 0) return

    setKaydediliyor(true)
    try {
      const kaydedilen = await saveDocument(belge.id, {
        title: belge.title,
        subject: belge.subject,
        content,
        expectedVersion: belge.currentVersion,
      })
      setBelge(kaydedilen)
      setContent(kaydedilen.content)
      setKaydedilmemis(false)
    } catch (e) {
      // 409 = araya baskasi kaydetmis. Korlemesine uzerine YAZMA.
      if (e.status === 409) {
        setHatalar(['Bu belge siz düzenlerken başkası tarafından kaydedildi. Sayfayı yenileyin.'])
      } else {
        setHatalar([e.message])
      }
    } finally {
      setKaydediliyor(false)
    }
  }

  function geriAlindi(guncelBelge) {
    setBelge(guncelBelge)
    setContent(guncelBelge.content)
    setKaydedilmemis(false)
    setHatalar([])
  }

  if (!belge) {
    return (
      <main style={{ padding: 16 }}>
        {hatalar.length > 0 ? (
          <p style={{ color: '#9c3226' }}>{hatalar[0]}</p>
        ) : (
          <p>Yükleniyor…</p>
        )}
      </main>
    )
  }

  const templateType = belge.templateType ?? TEMPLATE_TYPES.KAPANIS

  return (
    <div className="editor-layout">
      <div className="editor-form">
        <TopActions
          belge={belge}
          kaydediliyor={kaydediliyor}
          kaydedilmemis={kaydedilmemis}
          onKaydet={kaydet}
          onListe={() => navigate('/belgeler')}
        />

        {hatalar.length > 0 && (
          <div className="card" style={{ borderColor: '#9c3226', color: '#9c3226' }}>
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              {hatalar.map((h) => (
                <li key={h}>{h}</li>
              ))}
            </ul>
          </div>
        )}

        <BelgeAyarlari
          belge={belge}
          onDegisti={belgeDegisti}
          onVersiyonlar={() => setGecmisAcik(true)}
        />

        <MetaForm
          header={content.header}
          meeting={content.meeting}
          intro={content.intro}
          onHeaderChange={(d) => patch('header', d)}
          onMeetingChange={(d) => patch('meeting', d)}
          onIntroChange={(intro) => icerikDegisti((o) => ({ ...o, intro }))}
        />

        <SectionList
          sections={content.sections}
          templateType={templateType}
          onChange={(sections) => icerikDegisti((o) => ({ ...o, sections }))}
        />

        <NotesForm
          notes={content.notes}
          footer={content.footer}
          onNotesChange={(notes) => icerikDegisti((o) => ({ ...o, notes }))}
          onFooterChange={(d) => patch('footer', d)}
        />
      </div>

      <PreviewPane teamId={belge.teamId} templateType={templateType} content={content} />

      <VersiyonGecmisi
        acik={gecmisAcik}
        documentId={belge.id}
        guncelSurum={belge.currentVersion}
        onKapat={() => setGecmisAcik(false)}
        onGeriAlindi={geriAlindi}
      />
    </div>
  )
}
