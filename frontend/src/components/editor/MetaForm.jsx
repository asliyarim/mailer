// Mailin ust bilgisi: baslik, donem, takim etiketi, toplanti bilgisi ve
// giris paragraflari. Kontrollu bilesen - durum EditorPage'de.
//
// TODO (Yrd 1, Sprint 1): alanlarin gorunumu ve dogrulama geri bildirimi.
// Yeni alan eklerken once docs/BRIEF.md §6'daki semaya ekle, sonra buraya.

export default function MetaForm({ header, meeting, intro, onHeaderChange, onMeetingChange, onIntroChange }) {
  function paragrafGuncelle(index, deger) {
    onIntroChange(intro.map((p, i) => (i === index ? deger : p)))
  }

  return (
    <section className="card">
      <h2 className="card__baslik">2 · Mail bilgileri</h2>

      <div style={{ display: 'grid', gap: 8 }}>
        <label style={{ display: 'grid', gap: 4 }}>
          <span>Başlık</span>
          <input
            type="text"
            value={header.title}
            onChange={(e) => onHeaderChange({ title: e.target.value })}
          />
        </label>

        <label style={{ display: 'grid', gap: 4 }}>
          <span>Dönem</span>
          <input
            type="text"
            value={header.period}
            onChange={(e) => onHeaderChange({ period: e.target.value })}
          />
        </label>

        <label style={{ display: 'grid', gap: 4 }}>
          <span>Takım etiketi</span>
          <input
            type="text"
            value={header.teamLabel}
            onChange={(e) => onHeaderChange({ teamLabel: e.target.value })}
          />
        </label>

        <fieldset style={{ display: 'grid', gap: 8, border: '1px solid #d9e0e7', borderRadius: 4 }}>
          <legend>Toplantı</legend>
          <input
            type="text"
            placeholder="Tarih (03.09.2026)"
            value={meeting.date}
            onChange={(e) => onMeetingChange({ date: e.target.value })}
          />
          <input
            type="text"
            placeholder="Saat (10:00)"
            value={meeting.time}
            onChange={(e) => onMeetingChange({ time: e.target.value })}
          />
          <input
            type="text"
            placeholder="Yer"
            value={meeting.place}
            onChange={(e) => onMeetingChange({ place: e.target.value })}
          />
        </fieldset>

        <fieldset style={{ display: 'grid', gap: 8, border: '1px solid #d9e0e7', borderRadius: 4 }}>
          <legend>Giriş paragrafları</legend>
          {intro.map((paragraf, index) => (
            <textarea
              key={index}
              rows={2}
              value={paragraf}
              onChange={(e) => paragrafGuncelle(index, e.target.value)}
            />
          ))}
        </fieldset>
      </div>
    </section>
  )
}
