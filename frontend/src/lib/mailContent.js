// content jsonb semasinin (v1) istemci tarafi karsiligi.
// Sema tanimi: docs/BRIEF.md §6. Sunucudaki karsiligi document/domain/MailContent.
//
// BURADA MAIL HTML'I URETILMEZ. Bu dosya sadece bos iskelet uretir, alan
// ekler/siler ve dogrular. HTML uretimi yalnizca sunucuda, MailHtmlRenderer
// icinde olur (Mimari Kural 1).

export const SCHEMA_VERSION = 1;

export const TEMPLATE_TYPES = {
  KAPANIS: "KAPANIS",
  PLANLAMA: "PLANLAMA",
  YONETICI_OZETI: "YONETICI_OZETI",
};

export const TEMPLATE_LABELS = {
  KAPANIS: "Sprint Kapanış",
  PLANLAMA: "Sprint Planlama",
  YONETICI_OZETI: "Yönetici Özeti",
};

/** Bolum ve not kutularinin renk tonu. Gercek renk degeri TEMADAN gelir. */
export const TONES = ["blue", "green"];

/**
 * Satir alanlari. DIKKAT: satirlar dizi degil NESNE tutar - prototipte
 * data[0], data[1] diye indeksle gezilirdi ve bir sutun eklenince her sey
 * kayardi (bkz. docs/BRIEF.md §6).
 */
export const ROW_FIELDS = ["sector", "jira", "ci", "process", "stage", "stake", "note"];

export const ROW_FIELD_LABELS = {
  sector: "Sektör",
  jira: "Jira",
  ci: "CI",
  process: "Süreç",
  stage: "Aşama",
  stake: "Paydaş",
  note: "Not",
};

export function emptyRow() {
  return Object.fromEntries(ROW_FIELDS.map((field) => [field, ""]));
}

export function emptySection(key, title, tone = "blue") {
  return { key, title, tone, columns: [...ROW_FIELDS], rows: [] };
}

/** Yeni belgenin bos icerigi. Sunucu da ayni iskeleti uretir - tek dogru kaynak odur. */
export function emptyContent() {
  return {
    schemaVersion: SCHEMA_VERSION,
    header: { title: "", period: "", teamLabel: "" },
    meeting: { date: "", time: "", place: "" },
    intro: ["", "", ""],
    sections: [],
    notes: [],
    footer: { line1: "", line2: "" },
  };
}

/**
 * Sayaclar SAKLANMAZ, satir sayisindan hesaplanir - manuel sayac butonu
 * kapsam disi oldugu icin saklanacak bir sey yok (bkz. docs/BRIEF.md §6).
 */
export function countRows(content) {
  return (content?.sections ?? []).reduce((total, section) => total + (section.rows?.length ?? 0), 0);
}

/**
 * Kaydetmeden once cagrilir. Hata mesajlarindan olusan dizi doner; bos dizi
 * = gecerli. Nihai dogrulama SUNUCUDA yapilir, bu sadece kullaniciya erken
 * geri bildirim icindir.
 */
export function validateContent(content) {
  const errors = [];
  if (!content) return ["İçerik boş."];
  if (content.schemaVersion !== SCHEMA_VERSION) {
    errors.push(`Desteklenmeyen şema sürümü: ${content.schemaVersion}`);
  }
  if (!content.header?.title?.trim()) errors.push("Başlık boş olamaz.");
  if (!content.header?.teamLabel?.trim()) errors.push("Takım adı boş olamaz.");
  (content.sections ?? []).forEach((section, index) => {
    if (!section.key?.trim()) errors.push(`${index + 1}. bölümün anahtarı boş.`);
    if (!section.title?.trim()) errors.push(`${index + 1}. bölümün başlığı boş.`);
    if (!TONES.includes(section.tone)) errors.push(`${index + 1}. bölümün tonu geçersiz: ${section.tone}`);
  });
  return errors;
}
