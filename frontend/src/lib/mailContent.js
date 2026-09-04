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
  TOPLANTI_CIKTILARI: "TOPLANTI_CIKTILARI",
};

/**
 * Kullaniciya GORUNEN adlar. Anahtarlar (TOPLANTI_CIKTILARI gibi) sozlesmenin
 * parcasi ve veritabaninda yazili - onlar DEGISMEZ, yalnizca bu metinler
 * degisir. "Toplanti Ciktilari" -> "Toplanti Notlari" boyle yapildi.
 */
export const TEMPLATE_LABELS = {
  KAPANIS: "Sprint Kapanış",
  PLANLAMA: "Sprint Planlama",
  YONETICI_OZETI: "Yönetici Özeti",
  TOPLANTI_CIKTILARI: "Toplantı Notları",
};

/**
 * Bolum ve not kutularinin renk tonu. Gercek renk degeri TEMADAN gelir -
 * icerik JSON'u bir ROL yazar, renk kodu degil (Mimari Kural 4).
 *
 * "orange" Yonetici Ozeti ile geldi: bekleyen/dikkat gerektiren konular.
 * Bu liste BACKEND'DEN ONCE genisletildi - tersi olsaydi turuncu bolumle
 * dogan yeni belgeyi validateContent "gecersiz ton" diye reddeder ve
 * kullanici belgesini kaydedemezdi.
 */
export const TONES = ["blue", "green", "orange", "pink", "yellow", "purple"];

/**
 * Ton seçicinin gösterdiği liste. TEK NOKTADA: daha önce SectionList ve
 * NotesForm içinde ikiz olarak duruyordu; biri güncellenip öteki unutulunca
 * bölüm seçicisi ile not seçicisi farklı seçenek gösterirdi.
 *
 * Sıra önemli: ilk üçü ROL taşıyor (takım rengi / tamamlandı / bekliyor),
 * son üçü yalnızca görsel tercih.
 */
export const TON_SECENEKLERI = [
  { deger: "blue", etiket: "Mavi" },
  { deger: "green", etiket: "Yeşil" },
  { deger: "orange", etiket: "Turuncu" },
  { deger: "pink", etiket: "Pembe" },
  { deger: "yellow", etiket: "Sarı" },
  { deger: "purple", etiket: "Mor" },
];

/**
 * Tüm satır alanları, tek sözlük. Hangi alanın hangi mail tipinde
 * kullanılacağını bölümün `columns` dizisi söyler — şema tek, seçim tipe göre.
 *
 * DİKKAT: satırlar dizi değil NESNE tutar. Prototipte `data[0]`, `data[1]`
 * diye indeksle geziliyordu ve bir sütun eklenince her şey kayıyordu
 * (bkz. docs/api.md, content şeması kural 1).
 */
export const ROW_FIELDS = [
  // Sprint Kapanış
  "sector", "jira", "ci", "process", "stage", "stake", "note", "gain",
  // Sprint Planlama
  "topicType", "summary", "status", "sprint", "expected", "department",
  // Yönetici Özeti
  "team", "topic", "detail", "no", "decision", "pending", "owner", "due",
  "linkType", "title", "description", "button", "url",
  // Toplantı Çıktıları
  "scope",
];

export const ROW_FIELD_LABELS = {
  sector: "Sektör",
  jira: "Jira / Konu Anahtarı",
  ci: "CI",
  process: "Süreç",
  stage: "Aşama",
  stake: "Paydaşlar",
  note: "Kritik Not",
  gain: "Kazanç (Saat/Yıl)",
  topicType: "Konu Türü",
  summary: "Özet",
  status: "Durum",
  sprint: "Sprint",
  expected: "Beklenen Konular",
  department: "Departman",
  team: "Ekip",
  topic: "Konu",
  detail: "Detay",
  no: "Karar No",
  decision: "Karar",
  pending: "Bekleyen Konu",
  owner: "Sorumlu",
  due: "Termin",
  linkType: "Bağlantı Türü",
  title: "Başlık",
  description: "Açıklama",
  button: "Buton Metni",
  url: "Adres (URL)",
  scope: "Kapsam / Alan",
};

/**
 * Editörde GÖSTERİLMEYEN alanlar.
 *
 * `no` (karar numarası) içerikte boş kalır, mail çizilirken K-01, K-02 diye
 * üretilir. Böylece ortadaki karar silinince kalanlar kendiliğinden yeniden
 * numaralanır. Kullanıcıya gösterilseydi elle yazdığı numara ile mailde
 * çıkan numara ayrışırdı.
 */
export const GIZLI_ALANLAR = ["no"];

/** Yönetici Özeti'ndeki "Durum" seçenekleri. */
export const DURUMLAR = ["Bekliyor", "Devam Ediyor", "Karar Bekliyor", "Tamamlandı"];

/**
 * Bağlantı kartları mailde TEK SATIRDA yan yana çiziliyor; Outlook alta
 * kaydırmıyor. Dörtten fazlası dar kartta okunmaz hale gelir.
 */
export const EN_FAZLA_BAGLANTI = 4;

/** Konu Türü seçenekleri (v6.1'den). */
export const KONU_TURLERI = ["Hikaye", "Görev", "Bug", "İyileştirme", "Diğer"];

/**
 * Sektörler (v6.1'den).
 *
 * Serbest metin DEĞİL, liste: Sprint Kapanış tablosu satırları SEKTÖRE GÖRE
 * GRUPLANIYOR. "Elektrik" ile "elektrik" serbest metin olsaydı mailde iki
 * ayrı grup başlığı çıkardı ve kimse sebebini anlamazdı.
 *
 * Listede olmayan bir değer (eski kayıt, elle girilmiş) SİLİNMEZ: RowCard
 * onu seçeneklere ekleyip gösterir.
 */
export const SEKTORLER = [
  "Holding",
  "Doğalgaz",
  "Elektrik",
  "Enerji",
  "Jeneratör",
  "Hospitality",
  "Diğer",
];

/**
 * Mail tipine göre varsayılan sütunlar. Gövde 760px sabit; altı sütundan
 * fazlası Outlook'ta okunmaz hale geliyor, o yüzden geri kalanlar varsayılan
 * olarak KAPALI. İhtiyacı olan takım `columns`'a ekler — satırda olup
 * `columns`'ta olmayan alan çizilmez ama silinmez.
 */
export const VARSAYILAN_SUTUNLAR = {
  KAPANIS: ["sector", "jira", "ci", "process", "stage", "stake", "note"],
  PLANLAMA: ["topicType", "jira", "summary", "status", "expected", "stake"],
  // Yönetici Özeti'nde bölümlerin sütunları TİPE DEĞİL BÖLÜME bağlı
  // (görüşülen / kararlar / aksiyonlar / bağlantılar birbirinden farklı).
  // Buradaki liste yalnızca kullanıcı elle yeni bölüm eklerse kullanılır;
  // sunucudan gelen bölümler kendi sütunlarıyla doğuyor.
  YONETICI_OZETI: ["team", "topic", "detail"],
  TOPLANTI_CIKTILARI: ["team", "topic", "detail"],
};

export function emptyRow() {
  return Object.fromEntries(ROW_FIELDS.map((field) => [field, ""]));
}

export function emptySection(key, title, tone = "blue", templateType = TEMPLATE_TYPES.KAPANIS) {
  const columns = VARSAYILAN_SUTUNLAR[templateType] ?? VARSAYILAN_SUTUNLAR.KAPANIS;
  return { key, title, tone, columns: [...columns], rows: [] };
}

/** Yeni belgenin bos icerigi. Sunucu da ayni iskeleti uretir - tek dogru kaynak odur. */
export function emptyContent() {
  return {
    schemaVersion: SCHEMA_VERSION,
    header: { title: "", period: "", teamLabel: "" },
    // title / moderator / attendees Toplanti Ciktilari ile geldi; eski
    // kayitlarda yoklar ve null okunurlar - sema surumu degismedi.
    meeting: { date: "", time: "", place: "", title: "", moderator: "", attendees: "" },
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
