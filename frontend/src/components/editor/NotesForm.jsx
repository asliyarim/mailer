// Alt not kutuları ve footer metinleri.
// notes: [{ tone, text }] - ton temadaki renge çevrilir, burada renk yazılmaz.

import Button from '../shared/Button.jsx'

const TON_SECENEKLERI = [
  { deger: 'blue', etiket: 'Mavi' },
  { deger: 'green', etiket: 'Yeşil' },
]

export default function NotesForm({ notes, footer, onNotesChange, onFooterChange }) {
  function notGuncelle(index, yeniNot) {
    onNotesChange(notes.map((n, i) => (i === index ? yeniNot : n)))
  }

  return (
    <section className="card">
      <h2 className="card__baslik">
        <span className="card__no">4</span>
        Alt notlar ve kapanış
      </h2>

      <p className="card__aciklama">
        Alt notlar, mailin sonundaki renkli kutulardır. Tablodaki hiçbir satıra
        girmeyen ama söylenmesi gereken şeyler buraya yazılır.
      </p>

      {notes.length === 0 && (
        <p className="alan__ipucu" style={{ marginBottom: 10 }}>
          Not eklenmezse mailde alt not kutusu çizilmez.
        </p>
      )}

      {notes.map((not, index) => (
        <div className="satir-karti" key={index}>
          <div className="satir-karti__ust">
            <span className={`nokta nokta--${not.tone}`} aria-hidden="true" />
            <span className="satir-karti__no">{index + 1}. not</span>

            <span className="sag-yasla">
              <div className="seg-grup">
                {TON_SECENEKLERI.map((secenek) => (
                  <button
                    key={secenek.deger}
                    type="button"
                    className={not.tone === secenek.deger ? 'seg seg--secili' : 'seg'}
                    onClick={() => notGuncelle(index, { ...not, tone: secenek.deger })}
                  >
                    {secenek.etiket}
                  </button>
                ))}
              </div>
            </span>

            <Button
              varyant="tehlike"
              boyut="kucuk"
              onClick={() => onNotesChange(notes.filter((_, i) => i !== index))}
              baslik="Notu sil"
            >
              Sil
            </Button>
          </div>

          {/* Satır sonları korunur - kırpma. */}
          <textarea
            rows={3}
            aria-label={`${index + 1}. not metni`}
            value={not.text}
            onChange={(e) => notGuncelle(index, { ...not, text: e.target.value })}
          />
        </div>
      ))}

      <Button
        varyant="ikincil"
        boyut="kucuk"
        onClick={() => onNotesChange([...notes, { tone: 'blue', text: '' }])}
      >
        + Not ekle
      </Button>

      <div className="izgara izgara--2" style={{ marginTop: 16 }}>
        <label className="alan">
          <span className="alan__etiket">Kapanış 1. satır</span>
          <input
            type="text"
            value={footer.line1}
            placeholder="Teşekkür ederiz."
            onChange={(e) => onFooterChange({ line1: e.target.value })}
          />
        </label>

        <label className="alan">
          <span className="alan__etiket">Kapanış 2. satır</span>
          <input
            type="text"
            value={footer.line2}
            placeholder="Başarılar dileriz!"
            onChange={(e) => onFooterChange({ line2: e.target.value })}
          />
        </label>
      </div>
    </section>
  )
}
