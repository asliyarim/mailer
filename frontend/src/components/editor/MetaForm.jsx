// Mailin üst bilgisi: başlık, dönem, takım etiketi, toplantı bilgisi ve
// giriş paragrafları. Kontrollü bileşen - durum EditorPage'de.
//
// Yeni alan eklerken önce docs/BRIEF.md §6'daki şemaya ekle, sonra buraya.
//
// Giriş paragrafları ÜÇ TANE ve sayısı sabit: şema `intro: [ilk, orta,
// kapanış]` diyor. Sayıyı değiştirmek şema değişikliği demek - önce
// docs/api.md konuşulur.

const PARAGRAF_ETIKETLERI = ['İlk paragraf', 'Orta paragraf', 'Kapanış paragrafı']

const PARAGRAF_IPUCLARI = [
  'Sprintin genel çerçevesi — mailin ilk cümlesi.',
  'Tabloya giriş: aşağıda ne göreceklerini söyleyin.',
  'Toplantı çağrısı veya beklenen aksiyon.',
]

export default function MetaForm({
  takimAdlari = [],
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
          <div className="alan__etiket" style={{ marginBottom: 6 }}>
            Giriş paragrafları
          </div>
          <div className="izgara">
            {intro.map((paragraf, index) => (
              <label className="alan" key={index}>
                {/* Satır sonları KORUNUR - kırpma. Renderer paragrafa çevirir. */}
                <textarea
                  rows={2}
                  aria-label={PARAGRAF_ETIKETLERI[index] ?? `${index + 1}. paragraf`}
                  placeholder={PARAGRAF_ETIKETLERI[index] ?? ''}
                  value={paragraf}
                  onChange={(e) => paragrafGuncelle(index, e.target.value)}
                />
                {PARAGRAF_IPUCLARI[index] && (
                  <span className="alan__ipucu">{PARAGRAF_IPUCLARI[index]}</span>
                )}
              </label>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
