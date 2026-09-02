// Editorun TEK durum sahibi. Alt bilesenlerin hepsi kontrolludur: kendi
// icinde icerik tutmaz, deger + onChange alir. Yeni bir form alani eklerken
// durumu buraya koy, bilesenin icine degil.
//
// IKI KIP:
//
//   /editor/yeni TASLAK. Giris sayfasindan tip secilince gelinen hal.
//                Ortada BELGE YOK,
//                veritabanina hicbir sey yazilmadi. Icerik sunucudan
//                varsayilan olarak aliniyor (GET /documents/default), sag
//                panelde sablonun tamami bastan gorunuyor. Belge ancak
//                "Kaydet" dendiginde dogar.
//
//   /editor/:id  KAYITLI belge.
//
// Neden taslak kipi var: acilista eski bir belgenin acilmasi kullaniciya
// mantiksiz geldi (Aslı) - maili yazmaya gelen kisi bos bir form bekliyor.
// Her acilista belge yaratmak ise DB'de cop taslak biriktirirdi.
//
// Sol panel numarali kartlardan olusur:
//   1 · Belge ayarlari    2 · Mail bilgileri
//   3 · Bolumler          4 · Alt notlar
//
// Sag panel canli onizleme. Yazdirma ve panoya kopyalama HTML'i sunucudan
// AYRICA istiyor (/render/clipboard) - duzenleme nitelikleri olmadan.

import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  createDocument,
  fetchDefaultContent,
  fetchDocument,
  fetchTeams,
  saveDocument,
} from '../../lib/apiClient.js'
import { emptyContent, emptyRow, TEMPLATE_TYPES, validateContent } from '../../lib/mailContent.js'
import { alanaYaz, bolumeSatirEkle, satirIslemi, satirTakas } from '../../lib/icerikYolu.js'
import BelgeAyarlari from './BelgeAyarlari.jsx'
import MetaForm from './MetaForm.jsx'
import SectionList from './SectionList.jsx'
import NotesForm from './NotesForm.jsx'
import PreviewPane from './PreviewPane.jsx'
import TopActions from './TopActions.jsx'
import VersiyonGecmisi from './VersiyonGecmisi.jsx'
import Button from '../shared/Button.jsx'

// Son kullanilan takim tarayicida hatirlanir: RPA'da calisan biri her
// acilista listenin basindaki takimi degil kendi takimini bulsun.
// Sunucuda tutulacak bir tercih degil - kisiye ve tarayiciya ozel kolaylik.
const SON_TAKIM_ANAHTARI = 'aksa-mailer.son-takim'

function sonTakimiOku() {
  try {
    const deger = window.localStorage.getItem(SON_TAKIM_ANAHTARI)
    return deger ? Number(deger) : null
  } catch {
    // gizli sekme / site verisi kapali - listenin ilk takimina duseriz
    return null
  }
}

function sonTakimiYaz(teamId) {
  try {
    window.localStorage.setItem(SON_TAKIM_ANAHTARI, String(teamId))
  } catch {
    // yazamiyorsak hatirlamayiz, akis bozulmaz
  }
}

export default function EditorPage({ onDurum }) {
  const { id } = useParams()
  const navigate = useNavigate()
  const [aramaParametreleri] = useSearchParams()

  const [teams, setTeams] = useState([])
  const [belge, setBelge] = useState(null)
  const [content, setContent] = useState(emptyContent())
  // Taslak kipinde belge alanlarinin karsiligi. Kayitli kipte kullanilmaz.
  const [taslak, setTaslak] = useState({
    teamId: null,
    templateType: TEMPLATE_TYPES.KAPANIS,
    title: '',
    subject: '',
  })
  const [hatalar, setHatalar] = useState([])
  const [kaydediliyor, setKaydediliyor] = useState(false)
  const [kaydedilmemis, setKaydedilmemis] = useState(false)
  const [gecmisAcik, setGecmisAcik] = useState(false)
  const [aciliyor, setAciliyor] = useState(true)
  // "Tekrar dene" sayaci: artinca yukleme effect'leri yeniden calisir.
  const [yenidenDeneme, setYenidenDeneme] = useState(0)

  const taslakKipi = !id

  // --- kayitli belge yukleme ------------------------------------------------

  useEffect(() => {
    if (!id) return
    setAciliyor(true)
    fetchDocument(id)
      .then((d) => {
        setBelge(d)
        setContent(d.content)
        setKaydedilmemis(false)
        setHatalar([])
        sonTakimiYaz(d.teamId)
      })
      .catch((e) => setHatalar([e.message]))
      .finally(() => setAciliyor(false))
  }, [id, yenidenDeneme])

  // --- taslak kipi: takimlar + varsayilan icerik ----------------------------

  useEffect(() => {
    if (id) return undefined
    let iptal = false

    async function taslakKur() {
      setAciliyor(true)
      setBelge(null)
      try {
        const takimlar = await fetchTeams()
        if (iptal) return
        setTeams(takimlar)

        if (takimlar.length === 0) {
          setHatalar(['Erişebileceğiniz bir takım yok. Yöneticinize danışın.'])
          return
        }

        const hatirlanan = sonTakimiOku()
        const teamId = takimlar.some((t) => t.id === hatirlanan) ? hatirlanan : takimlar[0].id

        // Tip giris sayfasindan geliyor (/editor/yeni?tip=...). Adres cubugu
        // elle degistirilebilir, o yuzden dogruluyoruz - taninmayan bir tip
        // sunucuda 400'e duserdi.
        const istenen = aramaParametreleri.get('tip')
        const templateType = Object.values(TEMPLATE_TYPES).includes(istenen)
          ? istenen
          : TEMPLATE_TYPES.KAPANIS

        // Bos iskelet SUNUCUDAN gelir - istemcide kurulmaz (Mimari Kural 4).
        const varsayilan = await fetchDefaultContent(teamId, templateType)
        if (iptal) return

        setTaslak({
          teamId,
          templateType,
          title: '',
          subject: '',
        })
        setContent(varsayilan)
        setKaydedilmemis(false)
        setHatalar([])
      } catch (e) {
        if (!iptal) setHatalar([e.message])
      } finally {
        if (!iptal) setAciliyor(false)
      }
    }

    taslakKur()
    return () => {
      iptal = true
    }
  }, [id, aramaParametreleri, yenidenDeneme])

  // Kayitli belge kipinde de takimlara ihtiyac var (kunye etiketi).
  useEffect(() => {
    if (!id) return
    fetchTeams()
      .then(setTeams)
      .catch(() => {
        // kunye etiketi eksik kalir, editor calismaya devam eder
      })
  }, [id])

  // --- ust seritteki kayit rozeti ------------------------------------------

  useEffect(() => {
    if (!onDurum) return undefined
    if (kaydediliyor) onDurum({ metin: 'Kaydediliyor…' })
    else if (taslakKipi) onDurum({ metin: 'Kaydedilmemiş taslak', uyari: true })
    else if (kaydedilmemis) onDurum({ metin: 'Kaydedilmemiş değişiklik', uyari: true })
    else if (belge) onDurum({ metin: 'Kaydedildi' })
    else onDurum(null)
    return () => onDurum(null)
  }, [onDurum, belge, kaydediliyor, kaydedilmemis, taslakKipi])

  // Tarayici sekmesi kapatilirken uyar. Doldurulmus ama kaydedilmemis bir
  // mail sessizce kaybolmasin.
  useEffect(() => {
    if (!kaydedilmemis) return undefined
    function ayrilmadanOnce(e) {
      e.preventDefault()
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', ayrilmadanOnce)
    return () => window.removeEventListener('beforeunload', ayrilmadanOnce)
  }, [kaydedilmemis])

  // --- icerik guncelleme ----------------------------------------------------

  // Icerigin bir dalini gunceller: patch('header', { title: '...' })
  function patch(alan, deger) {
    setContent((onceki) => ({ ...onceki, [alan]: { ...onceki[alan], ...deger } }))
    setKaydedilmemis(true)
  }

  function icerikDegisti(guncelleyici) {
    setContent(guncelleyici)
    setKaydedilmemis(true)
  }

  /**
   * Önizlemede tıklanan alana yazılan değer.
   *
   * Soldaki formla AYNI state'e gidiyor - iki giriş yolu var ama tek kaynak
   * var. Mail HTML'i yine yalnızca sunucuda üretiliyor; burada değişen şey
   * içerik, çizim değil.
   */
  const alanDegisti = useCallback((adres, deger) => {
    setContent((onceki) => alanaYaz(onceki, adres, deger))
    setKaydedilmemis(true)
  }, [])

  /**
   * Önizlemede seçilen satır üzerinde işlem: ekle · çoğalt · yukarı · aşağı · sil.
   *
   * Soldaki kartlardaki "+ Satır ekle" / "Sil" ile AYNI veriyi değiştiriyor;
   * yalnızca giriş noktası farklı.
   */
  const satirIslemiYapildi = useCallback((adres, islem, hedefAdres) => {
    setContent((onceki) =>
      islem === 'takas'
        ? satirTakas(onceki, adres, hedefAdres)
        : satirIslemi(onceki, adres, islem, emptyRow())
    )
    setKaydedilmemis(true)
  }, [])

  /** Önizlemedeki "+ satır ekle": bölümün sonuna boş satır. */
  const bolumeEkle = useCallback((bolumKey) => {
    setContent((onceki) => bolumeSatirEkle(onceki, bolumKey, emptyRow()))
    setKaydedilmemis(true)
  }, [])

  // --- taslak: takim / tip degisimi ----------------------------------------

  // Takim ya da tip degisince icerik iskeleti de degisiyor (baslik takimdan
  // turetiliyor, bolumler tipe gore). Sunucudan yeniden aliyoruz - istemcide
  // "su alani su yap" diye yamamak iki tanim demek olurdu.
  async function taslakYapisiDegisti(yeniAlanlar) {
    const yeniTeamId = yeniAlanlar.teamId ?? taslak.teamId
    const yeniTip = yeniAlanlar.templateType ?? taslak.templateType

    if (kaydedilmemis) {
      const onay = window.confirm(
        'Bu maile girdikleriniz sıfırlanacak; şablon yeniden kurulacak. Devam edilsin mi?'
      )
      if (!onay) return
    }

    setHatalar([])
    try {
      const varsayilan = await fetchDefaultContent(yeniTeamId, yeniTip)
      setTaslak((o) => ({ ...o, ...yeniAlanlar }))
      setContent(varsayilan)
      setKaydedilmemis(false)
      sonTakimiYaz(yeniTeamId)
    } catch (e) {
      setHatalar([e.message])
    }
  }

  // --- kaydetme -------------------------------------------------------------

  async function kaydet() {
    const bulunanHatalar = validateContent(content)
    setHatalar(bulunanHatalar)
    if (bulunanHatalar.length > 0) return

    setKaydediliyor(true)
    try {
      if (taslakKipi) {
        // Belge BURADA doguyor. Once POST (sunucu varsayilan icerikle
        // yaratir), hemen ardindan PUT ile kullanicinin yazdigi icerik.
        // Baslik bos birakilmissa mailin kendi basligini kullaniyoruz -
        // POST title'i @NotBlank istiyor ve kullaniciyi bos bir zorunlu
        // alanla karsilamak istemiyoruz.
        const ad = taslak.title.trim() || content.header?.title?.trim() || 'Yeni mail'
        const yeni = await createDocument({
          teamId: taslak.teamId,
          templateType: taslak.templateType,
          title: ad,
        })
        const kaydedilen = await saveDocument(yeni.id, {
          title: ad,
          subject: taslak.subject,
          content,
          expectedVersion: yeni.currentVersion,
        })
        setKaydedilmemis(false)
        setBelge(kaydedilen)
        navigate(`/editor/${kaydedilen.id}`)
        return
      }

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

  function yeniMail() {
    if (kaydedilmemis) {
      const onay = window.confirm(
        'Kaydedilmemiş değişiklikler kaybolacak. Yeni maile geçilsin mi?'
      )
      if (!onay) return
    }
    setKaydedilmemis(false)
    // Giris sayfasina: yeni mailin tipi orada seciliyor.
    navigate('/')
  }

  function geriAlindi(guncelBelge) {
    setBelge(guncelBelge)
    setContent(guncelBelge.content)
    setKaydedilmemis(false)
    setHatalar([])
  }

  // --- cizim ----------------------------------------------------------------

  const teamId = taslakKipi ? taslak.teamId : belge?.teamId
  const templateType = taslakKipi
    ? taslak.templateType
    : (belge?.templateType ?? TEMPLATE_TYPES.KAPANIS)
  const takim = teams.find((t) => t.id === teamId)
  const formGoster = taslakKipi ? taslak.teamId != null : Boolean(belge)

  return (
    <div className="uygulama">
      <section className="panel panel--editor">
        <TopActions
          taslak={taslakKipi}
          belge={belge}
          teamId={teamId}
          templateType={templateType}
          content={content}
          kaydediliyor={kaydediliyor}
          kaydedilmemis={kaydedilmemis}
          hazir={formGoster}
          onKaydet={kaydet}
        />

        {/* Künye: aracın hangi birime ait olduğunu söyleyen tek koyu yüzey.
            Mailin hero şeridiyle aynı dili konuşur, panel baştan sona beyaz
            kart dizisi gibi durmasın. */}
        <div className="kunye">
          <p className="kunye__baslik">Dijital Uygulamalar &amp; Ürün Geliştirme</p>
          <p className="kunye__alt">
            Sprint kapanış ve planlama maillerini elle HTML yazmadan hazırlayın.
            Soldaki kartları doldurun, sağdaki önizleme Outlook'ta göreceğiniz
            mailin ta kendisidir.
          </p>
          {takim && (
            <div className="kunye__etiketler">
              <span className="kunye__etiket">{takim.name}</span>
              <span className="kunye__etiket">Tema: {takim.themeKey}</span>
            </div>
          )}
        </div>

        {hatalar.length > 0 && (
          <div className="uyari uyari--hata">
            <ul>
              {hatalar.map((h) => (
                <li key={h}>{h}</li>
              ))}
            </ul>
            {/* Sunucuya ulasilamadiginda sayfa kendi kendine toparlanmiyordu:
                istek bir kez basarisiz olunca yeniden denenmiyor ve kullanici
                sayfayi yenilemek zorunda kaliyordu. Sunucu yeniden baslatilinca
                (deploy sirasinda oluyor) tam olarak bu yasaniyor. */}
            <div style={{ marginTop: 10 }}>
              <Button
                varyant="ikincil"
                boyut="kucuk"
                onClick={() => {
                  setHatalar([])
                  setYenidenDeneme((n) => n + 1)
                }}
              >
                Tekrar dene
              </Button>
            </div>
          </div>
        )}

        {aciliyor && !formGoster && <p className="sessiz-metin">Yükleniyor…</p>}

        {formGoster && (
          <>
            <BelgeAyarlari
              taslak={taslakKipi}
              teams={teams}
              teamId={teamId}
              templateType={templateType}
              title={taslakKipi ? taslak.title : belge.title}
              subject={taslakKipi ? taslak.subject : belge.subject}
              surum={belge?.currentVersion}
              onTeamId={(yeni) => taslakYapisiDegisti({ teamId: yeni })}
              onTemplateType={(yeni) => taslakYapisiDegisti({ templateType: yeni })}
              onTitle={(deger) => {
                if (taslakKipi) setTaslak((o) => ({ ...o, title: deger }))
                else setBelge((o) => ({ ...o, title: deger }))
                setKaydedilmemis(true)
              }}
              onSubject={(deger) => {
                if (taslakKipi) setTaslak((o) => ({ ...o, subject: deger }))
                else setBelge((o) => ({ ...o, subject: deger }))
                setKaydedilmemis(true)
              }}
              onVersiyonlar={() => setGecmisAcik(true)}
              onYeniMail={yeniMail}
            />

            <MetaForm
              takimAdlari={teams.map((t) => t.name)}
              toplantiKutusu={templateType === TEMPLATE_TYPES.TOPLANTI_CIKTILARI}
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
          </>
        )}
      </section>

      <section className="panel panel--onizleme">
        <PreviewPane
          teamId={teamId}
          templateType={templateType}
          content={content}
          hazirlaniyor={!formGoster}
          onAlanDegisti={alanDegisti}
          yenileme={yenidenDeneme}
          onSatirIslemi={satirIslemiYapildi}
          onBolumeEkle={bolumeEkle}
        />
      </section>

      {belge && (
        <VersiyonGecmisi
          acik={gecmisAcik}
          documentId={belge.id}
          guncelSurum={belge.currentVersion}
          onKapat={() => setGecmisAcik(false)}
          onGeriAlindi={geriAlindi}
        />
      )}
    </div>
  )
}
