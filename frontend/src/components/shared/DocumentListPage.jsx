// "Belgelerim": takım seçimi + o takımın mailleri + arama.
//
// ARAMA SUNUCUDA: q parametresi başlıkta ve dönemde arıyor (Türkçe harf
// kurallarıyla, şapkalı sesliler katlanarak). Burada ikinci bir filtre yok -
// olsaydı iki arama mantığı olur ve zamanla ayrışırlardı.
//
// Arama SEÇİLİ TAKIM içinde arar. Giriş sayfasındaki "son taslaklar" bütün
// takımlarda arıyordu ama o liste kaldırıldı; takım bilinmiyorsa takımlar
// arasında gezmek gerekiyor.

import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { deleteDocument, fetchDocuments, fetchTeams } from '../../lib/apiClient.js'
import { TEMPLATE_LABELS, TEMPLATE_TYPES } from '../../lib/mailContent.js'
import Button from './Button.jsx'
import Modal from './Modal.jsx'

// Her tuşa basışta istek atmamak için bekleme (önizlemedekiyle aynı mantık).
const ARAMA_GECIKMESI_MS = 300

function tarihBicimle(isoTarih) {
  if (!isoTarih) return ''
  const t = new Date(isoTarih)
  return Number.isNaN(t.getTime())
    ? isoTarih
    : t.toLocaleString('tr-TR', { dateStyle: 'short', timeStyle: 'short' })
}

/** Tip etiketinin rengi - listede tipler renkle de ayrışsın. */
function etiketSinifi(templateType) {
  if (templateType === TEMPLATE_TYPES.PLANLAMA) return 'etiket etiket--yesil'
  if (templateType === TEMPLATE_TYPES.YONETICI_OZETI) return 'etiket etiket--notr'
  return 'etiket'
}

export default function DocumentListPage() {
  const navigate = useNavigate()

  const [teams, setTeams] = useState([])
  const [seciliTeamId, setSeciliTeamId] = useState(null)
  const [belgeler, setBelgeler] = useState([])
  const [arama, setArama] = useState('')
  const [yukleniyor, setYukleniyor] = useState(true)
  const [hata, setHata] = useState(null)

  /**
   * Silme onayı. Silme GERİ ALINAMAZ olduğu için tek tıkla yapılmıyor;
   * onay penceresi belgenin ADINI gösteriyor — "emin misiniz?" diye soran
   * ama neyi sildiğini söylemeyen bir pencere hiçbir şey doğrulatmaz.
   */
  const [silinecek, setSilinecek] = useState(null)
  const [siliniyor, setSiliniyor] = useState(false)

  useEffect(() => {
    fetchTeams()
      .then((liste) => {
        setTeams(liste)
        if (liste.length > 0) setSeciliTeamId(liste[0].id)
        else setYukleniyor(false)
      })
      .catch((e) => {
        setHata(e.message)
        setYukleniyor(false)
      })
  }, [])

  useEffect(() => {
    if (seciliTeamId == null) return undefined
    let iptal = false
    setYukleniyor(true)

    const zamanlayici = setTimeout(() => {
      fetchDocuments(seciliTeamId, arama)
        .then((liste) => {
          if (iptal) return
          setBelgeler(liste)
          setHata(null)
        })
        .catch((e) => {
          if (!iptal) setHata(e.message)
        })
        .finally(() => {
          if (!iptal) setYukleniyor(false)
        })
    }, ARAMA_GECIKMESI_MS)

    return () => {
      iptal = true
      clearTimeout(zamanlayici)
    }
  }, [seciliTeamId, arama])

  const aramaVar = arama.trim().length > 0
  const seciliTakim = teams.find((t) => t.id === seciliTeamId)

  async function silmeyiOnayla() {
    if (!silinecek) return
    setSiliniyor(true)
    try {
      await deleteDocument(silinecek.id)
      // Listeyi yeniden çekmek yerine yerelde çıkarıyoruz: silinen kayıt
      // zaten sunucuda yok, ikinci bir istek beklemek ekranı gereksiz
      // dondurur.
      setBelgeler((liste) => liste.filter((b) => b.id !== silinecek.id))
      setSilinecek(null)
      setHata(null)
    } catch (e) {
      setHata(e.message)
      setSilinecek(null)
    } finally {
      setSiliniyor(false)
    }
  }

  return (
    <main className="sayfa">
      <div className="sayfa__ic">
        <div className="satir-arasi">
          <div>
            <h1 className="sayfa__baslik">Belgelerim</h1>
            <p className="sayfa__alt">
              Takımın hazırladığı mailler. Bir satıra tıklayarak düzenleyin.
            </p>
          </div>
          <span className="sag-yasla">
            <Button varyant="birincil" onClick={() => navigate('/')}>
              + Yeni mail
            </Button>
          </span>
        </div>

        {/* Takım seçimi: kaç takım olduğu doğrudan görünsün diye açılır liste
            değil hap düğmeler. */}
        {teams.length > 1 && (
          <div className="satir-arasi">
            <span className="alan__etiket">Takım</span>
            <div className="seg-grup">
              {teams.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  className={t.id === seciliTeamId ? 'seg seg--secili' : 'seg'}
                  onClick={() => setSeciliTeamId(t.id)}
                >
                  {t.name}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="liste-basligi">
          <h2 className="sayfa__baslik" style={{ fontSize: 17 }}>
            {seciliTakim?.name ?? 'Belgeler'}
          </h2>

          <input
            type="search"
            className="arama-kutusu"
            placeholder="Başlık veya dönem ara — “Ağustos 2026”, “Sprint 42”"
            aria-label="Bu takımın belgelerinde ara"
            value={arama}
            onChange={(e) => setArama(e.target.value)}
          />
        </div>

        {hata && <div className="uyari uyari--hata">{hata}</div>}

        {yukleniyor && <p className="sessiz-metin">Yükleniyor…</p>}

        {!yukleniyor && belgeler.length === 0 && !hata && (
          <div className="card">
            <div className="bos-durum">
              <p className="bos-durum__baslik">
                {aramaVar ? 'Bu takımda eşleşen mail yok' : 'Bu takımda henüz mail yok'}
              </p>
              <p className="bos-durum__metin">
                {aramaVar
                  ? 'Aradığınız mail başka bir takımda olabilir — yukarıdan takımı değiştirin.'
                  : '"Yeni mail" ile başlayın — tipi seçtiğinizde bölümler ve sütunlar hazır gelir, siz sadece satırları doldurursunuz.'}
              </p>
              {!aramaVar && (
                <Button varyant="birincil" onClick={() => navigate('/')}>
                  + Yeni mail
                </Button>
              )}
            </div>
          </div>
        )}

        {!yukleniyor && belgeler.length > 0 && (
          <div className="card" style={{ padding: 0, overflowX: 'auto' }}>
            <table className="liste-tablo">
              <thead>
                <tr>
                  <th>Başlık</th>
                  <th>Tip</th>
                  <th>Sürüm</th>
                  <th>Güncelleyen</th>
                  <th>Güncelleme</th>
                  {/* Başlıksız sütun: içinde ne olduğu düğmenin kendisinden
                      belli, "İşlem" yazmak gürültü olurdu. */}
                  <th aria-label="İşlemler"></th>
                </tr>
              </thead>
              <tbody>
                {belgeler.map((b) => (
                  <tr
                    key={b.id}
                    className="tiklanabilir"
                    onClick={() => navigate(`/editor/${b.id}`)}
                  >
                    <td>
                      {/* Bağlantı klavyeyle gezenler için burada duruyor;
                          satır tıklaması yalnızca fare kolaylığı. */}
                      <Link to={`/editor/${b.id}`} onClick={(e) => e.stopPropagation()}>
                        {b.title}
                      </Link>
                    </td>
                    <td>
                      <span className={etiketSinifi(b.templateType)}>
                        {TEMPLATE_LABELS[b.templateType] ?? b.templateType}
                      </span>
                    </td>
                    <td className="sayi">v{b.currentVersion}</td>
                    <td>{b.updatedBy}</td>
                    <td className="sessiz-metin">{tarihBicimle(b.updatedAt)}</td>
                    <td className="satir-eylem">
                      {/* stopPropagation ŞART: satırın kendisi tıklanınca
                          editöre gidiyor. Olmasaydı "Sil" düğmesi hem onay
                          penceresini açar hem belgeyi açardı. */}
                      <button
                        type="button"
                        className="sil-dugmesi"
                        title={`${b.title} — kalıcı olarak sil`}
                        onClick={(e) => {
                          e.stopPropagation()
                          setSilinecek(b)
                        }}
                      >
                        Sil
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <Modal
          acik={silinecek != null}
          baslik="Maili sil"
          onKapat={() => (siliniyor ? null : setSilinecek(null))}
        >
          <p className="modal__metin">
            <strong>{silinecek?.title}</strong> kalıcı olarak silinecek. Sürüm
            geçmişi de gider ve <strong>geri alınamaz</strong>.
          </p>
          <div className="modal__eylemler">
            <Button varyant="sessiz" onClick={() => setSilinecek(null)} disabled={siliniyor}>
              Vazgeç
            </Button>
            <Button varyant="tehlike" onClick={silmeyiOnayla} disabled={siliniyor}>
              {siliniyor ? 'Siliniyor…' : 'Sil'}
            </Button>
          </div>
        </Modal>
      </div>
    </main>
  )
}
