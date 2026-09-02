// Mailin üst bilgisi: başlık, dönem, takım etiketi, toplantı bilgisi ve
// giriş paragrafları. Kontrollü bileşen - durum EditorPage'de.
//
// Yeni alan eklerken önce docs/BRIEF.md §6'daki şemaya ekle, sonra buraya.
//
// Giriş paragraflarının SAYISI SABİT DEĞİL. Sunucu tipe göre 1-2 dolu
// paragrafla doğuruyor ve dizideki kaç öğe varsa hepsini çiziyor (ölçüldü).
// O yüzden burada ekleme/silme var - aksi hâlde Toplantı Çıktıları tek
// paragrafa hapsolurdu.

import Button from '../shared/Button.jsx'

export default function MetaForm({
  takimAdlari = [],
  toplantiKutusu = false,
  header,
  meeting,
  intro,
  onHeaderChange,
  onMeetingChange,
  onIntroChange,
}) {
  function paragrafGuncelle(index, deger) {
    onIntroChange(intro.map((p, i) => (i === index ? deger : p)))
  }

  return (
    <section className="card">
      <h2 className="card__baslik">
        <span className="card__no">2</span>
        Mail bilgileri
      </h2>

      <div className="izgara">
        <label className="alan">
          <span className="alan__etiket">Başlık</span>
          <input
            type="text"
            placeholder="DİJİTAL UYGULAMALAR SPRINT BİLGİLENDİRME"
            value={header.title}
            onChange={(e) => onHeaderChange({ title: e.target.value })}
          />
        </label>

        <div className="izgara izgara--2">
          <label className="alan">
            <span className="alan__etiket">Dönem</span>
            <input
              type="text"
              placeholder="Ağustos 2026 Sprint Kapanışı"
              value={header.period}
              onChange={(e) => onHeaderChange({ period: e.target.value })}
            />
          </label>

          <label className="alan">
            <span className="alan__etiket">Takım etiketi</span>
            {/* Elle yazılmıyor, listeden seçiliyor: bu metin mailin hero
                şeridinde çıkıyor ve "RPA Takimi" gibi bir yazım hatası
                düzeltilmeden dışarı gider. Takım adları sunucudan geliyor. */}
            <select
              value={header.teamLabel}
              onChange={(e) => onHeaderChange({ teamLabel: e.target.value })}
            >
              <option value="">—</option>
              {/* Kayıtlı değer listede yoksa kaybolmasın (eski belge, takım
                  adı değişmiş olabilir): başa eklenir. */}
              {header.teamLabel && !takimAdlari.includes(header.teamLabel) && (
                <option value={header.teamLabel}>{header.teamLabel}</option>
              )}
              {takimAdlari.map((ad) => (
                <option key={ad} value={ad}>
                  {ad}
                </option>
              ))}
            </select>
          </label>
        </div>

        {/* Toplantı Çıktıları'nda mailin en üstünde bir toplantı kutusu var:
            adı, moderatörü, katılımcıları. Kutu HİÇBİRİ dolu değilse
            çizilmiyor - yani önizlemeden tıklanamıyor. Bu alanlar buradan
            doldurulmazsa kutuya hiç ulaşılamaz. */}
        {toplantiKutusu && (
          <div className="izgara">
            <label className="alan">
              <span className="alan__etiket">Toplantı adı</span>
              <input
                type="text"
                placeholder="Dijital Uygulamalar Değerlendirme Toplantısı"
                value={meeting.title ?? ''}
                onChange={(e) => onMeetingChange({ title: e.target.value })}
              />
            </label>

            <div className="izgara izgara--2">
              <label className="alan">
                <span className="alan__etiket">Moderatör / not alan</span>
                <input
                  type="text"
                  placeholder="Aslı Yarım"
                  value={meeting.moderator ?? ''}
                  onChange={(e) => onMeetingChange({ moderator: e.target.value })}
                />
              </label>

              <label className="alan">
                <span className="alan__etiket">Katılımcı ekipler</span>
                <input
                  type="text"
                  placeholder="RPA, İş Zekâsı, CBS"
                  value={meeting.attendees ?? ''}
                  onChange={(e) => onMeetingChange({ attendees: e.target.value })}
                />
              </label>
            </div>
            <span className="alan__ipucu">
              Bu üçü boşken mailin üstündeki toplantı kutusu hiç çizilmez.
            </span>
          </div>
        )}

        <div>
          <div className="alan__etiket" style={{ marginBottom: 6 }}>
            Toplantı
          </div>
          <div className="izgara izgara--3">
            <input
              type="text"
              aria-label="Toplantı tarihi"
              placeholder="03.09.2026"
              value={meeting.date}
              onChange={(e) => onMeetingChange({ date: e.target.value })}
            />
            <input
              type="text"
              aria-label="Toplantı saati"
              placeholder="10:00"
              value={meeting.time}
              onChange={(e) => onMeetingChange({ time: e.target.value })}
            />
            <input
              type="text"
              aria-label="Toplantı yeri"
              placeholder="Toplantı Salonu"
              value={meeting.place}
              onChange={(e) => onMeetingChange({ place: e.target.value })}
            />
          </div>
        </div>

        <div>
          <div className="satir-arasi" style={{ marginBottom: 6 }}>
            <span className="alan__etiket">Giriş paragrafları</span>
            <span className="sag-yasla">
              <Button varyant="sessiz" boyut="kucuk" onClick={() => onIntroChange([...intro, ''])}>
                + Paragraf ekle
              </Button>
            </span>
          </div>

          <div className="izgara">
            {intro.map((paragraf, index) => (
              <div className="alan" key={index}>
                <div className="satir-arasi">
                  <span className="alan__etiket">{index + 1}. paragraf</span>
                  {/* Son paragraf silinemez: mail giriş metinsiz kalmasın. */}
                  {intro.length > 1 && (
                    <span className="sag-yasla">
                      <Button
                        varyant="tehlike"
                        boyut="kucuk"
                        baslik="Paragrafı sil"
                        onClick={() => onIntroChange(intro.filter((_, i) => i !== index))}
                      >
                        Sil
                      </Button>
                    </span>
                  )}
                </div>
                {/* Satır sonları KORUNUR - kırpma. Renderer paragrafa çevirir. */}
                <textarea
                  rows={2}
                  aria-label={`${index + 1}. paragraf`}
                  value={paragraf}
                  onChange={(e) => paragrafGuncelle(index, e.target.value)}
                />
              </div>
            ))}
          </div>
          <span className="alan__ipucu">
            Mailin açılış metni. Yazdığınız sırayla çizilir; boş bırakılan
            paragraf mailde görünmez.
          </span>
        </div>
      </div>
    </section>
  )
}
