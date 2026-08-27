// Alt not kutulari ve footer metinleri.
// notes: [{ tone, text }] - ton temadaki renge cevrilir, burada renk yazilmaz.

import Button from '../shared/Button.jsx'

export default function NotesForm({ notes, footer, onNotesChange, onFooterChange }) {
  function notGuncelle(index, yeniNot) {
    onNotesChange(notes.map((n, i) => (i === index ? yeniNot : n)))
  }

  return (
    <section className="card">
      <h2 className="card__baslik">Alt notlar ve kapanış</h2>

      {notes.map((not, index) => (
        <div key={index} style={{ display: 'grid', gap: 6, marginBottom: 10 }}>
          <select value={not.tone} onChange={(e) => notGuncelle(index, { ...not, tone: e.target.value })}>
            <option value="blue">Mavi</option>
            <option value="green">Yeşil</option>
          </select>
          {/* Satir sonlari korunur - kirpma. */}
          <textarea
            rows={3}
            value={not.text}
            onChange={(e) => notGuncelle(index, { ...not, text: e.target.value })}
          />
          <Button varyant="sessiz" onClick={() => onNotesChange(notes.filter((_, i) => i !== index))}>
            Notu sil
          </Button>
        </div>
      ))}

      <Button onClick={() => onNotesChange([...notes, { tone: 'blue', text: '' }])}>Not ekle</Button>

      <div style={{ display: 'grid', gap: 8, marginTop: 14 }}>
        <input
          type="text"
          value={footer.line1}
          placeholder="Footer 1. satır"
          onChange={(e) => onFooterChange({ line1: e.target.value })}
        />
        <input
          type="text"
          value={footer.line2}
          placeholder="Footer 2. satır"
          onChange={(e) => onFooterChange({ line2: e.target.value })}
        />
      </div>
    </section>
  )
}
