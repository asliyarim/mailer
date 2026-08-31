// Önizlemedeki bir alanın ADRESİNİ içerik nesnesindeki yere çevirir.
//
// Satır içi düzenleme için var: sunucu mail HTML'ine her düzenlenebilir
// alanın adresini basıyor (bkz. ALAN_NITELIGI), kullanıcı önizlemede o
// alana tıklayınca biz adresi burada çözüp aynı React state'ine yazıyoruz.
//
// TEK YAZMA YOLU: önizlemedeki kutu da soldaki form da aynı state'e yazar.
// iframe'in içine hiçbir şey yazılmaz, contenteditable kullanılmaz - o yol
// mail HTML'ini istemcide değiştirmek olurdu (Mimari Kural 1).
//
// Adres biçimi (içerik JSON'undaki nokta yolu):
//   header.title · header.period · header.teamLabel
//   meeting.date · meeting.time · meeting.place
//   intro.0 · intro.1 · intro.2
//   sections.<key>.title
//   sections.<key>.rows.<index>.<field>
//   notes.<index>.text
//   footer.line1 · footer.line2
//
// Bölümde İNDEKS DEĞİL key kullanılıyor: kullanıcı bölümleri yeniden
// sıralarsa indeks kayar ve adres yanlış alana yazardı.

import { ROW_FIELD_LABELS } from "./mailContent.js";

/** Sunucunun mail HTML'ine bastığı nitelik. Sözleşme değişirse burası. */
export const ALAN_NITELIGI = "data-alan";

/** Tek satırlık input yerine textarea açılacak alanlar. */
const COK_SATIRLI_ALANLAR = ["note", "expected", "summary", "stake", "text"];

const SABIT_ETIKETLER = {
  "header.title": "Başlık",
  "header.period": "Dönem",
  "header.teamLabel": "Takım etiketi",
  "meeting.date": "Toplantı tarihi",
  "meeting.time": "Saat",
  "meeting.place": "Yer",
  "intro.0": "İlk paragraf",
  "intro.1": "Orta paragraf",
  "intro.2": "Kapanış paragrafı",
  "footer.line1": "Kapanış 1. satır",
  "footer.line2": "Kapanış 2. satır",
};

/**
 * Adresi, içerik nesnesinde gezilebilecek somut bir yola çevirir.
 * Bölüm anahtarı burada indekse dönüşür. Bulunamazsa null.
 */
function yolaCevir(content, adres) {
  const parcalar = adres.split(".");
  if (parcalar[0] !== "sections") return parcalar;

  const bolumler = content?.sections ?? [];
  const index = bolumler.findIndex((b) => b.key === parcalar[1]);
  if (index < 0) return null;
  return ["sections", index, ...parcalar.slice(2)];
}

/** Adresteki değeri okur. Alan yoksa undefined. */
export function alandanOku(content, adres) {
  const yol = yolaCevir(content, adres);
  if (!yol) return undefined;

  let dugum = content;
  for (const parca of yol) {
    if (dugum == null) return undefined;
    dugum = Array.isArray(dugum) ? dugum[Number(parca)] : dugum[parca];
  }
  return dugum;
}

/** Diziyi/nesneyi kopyalayarak yazar - React state'i yerinde değişmez. */
function derinYaz(dugum, yol, deger) {
  const [bas, ...kalan] = yol;
  if (Array.isArray(dugum)) {
    const index = Number(bas);
    const kopya = [...dugum];
    kopya[index] = kalan.length === 0 ? deger : derinYaz(dugum[index], kalan, deger);
    return kopya;
  }
  const kopya = { ...dugum };
  kopya[bas] = kalan.length === 0 ? deger : derinYaz(dugum?.[bas], kalan, deger);
  return kopya;
}

/**
 * Adrese yeni değeri yazıp YENİ içerik nesnesi döndürür.
 * Adres çözülemezse içerik OLDUĞU GİBİ döner - bilinmeyen bir adres
 * yüzünden kullanıcının içeriği bozulmasın.
 */
export function alanaYaz(content, adres, deger) {
  const yol = yolaCevir(content, adres);
  if (!yol) return content;
  return derinYaz(content, yol, deger);
}

/** Düzenleme kutusunun başlığı - kullanıcı neyi düzenlediğini görsün. */
export function alanEtiketi(adres) {
  if (SABIT_ETIKETLER[adres]) return SABIT_ETIKETLER[adres];

  const parcalar = adres.split(".");
  const son = parcalar[parcalar.length - 1];

  if (adres.startsWith("sections.") && son === "title") return "Bölüm başlığı";
  if (adres.startsWith("notes.")) return "Alt not";
  if (adres.includes(".rows.")) {
    const satirNo = Number(parcalar[3]) + 1;
    const alanAdi = ROW_FIELD_LABELS[son] ?? son;
    return `${satirNo}. satır · ${alanAdi}`;
  }
  return ROW_FIELD_LABELS[son] ?? son;
}

/** Bu alan çok satırlı mı - textarea mı input mu açılacak. */
export function cokSatirliMi(adres) {
  const son = adres.split(".").pop();
  return COK_SATIRLI_ALANLAR.includes(son) || adres.startsWith("intro.");
}
