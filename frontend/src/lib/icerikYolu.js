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

/**
 * Sabit seçenekli alanlarda seçenek listesi: "Bekliyor|Devam Ediyor|..."
 * Liste SUNUCUDAN geliyor - burada ikinci kez yazılmıyor, yoksa iki liste
 * zamanla ayrışır (Yönetici Özeti sayacı "Tamamlandı" metnine bakıyor).
 */
export const SECENEK_NITELIGI = "data-secenekler";

/**
 * Bölümün mail içindeki yeri: <table data-bolum="discussed">.
 *
 * data-alan'dan AYRI bir soru cevaplıyor. data-alan bir içerik yolu -
 * "şu metni düzenle" der. Bu ise "bölüm ekranda nerede" der; "+ satır ekle"
 * düğmesini oraya koyabilmek için gerekiyor.
 *
 * Bölüm BOŞ olsa bile başlık çizildiği için bu çapa her zaman var - yani
 * hiç satırı olmayan bölüme de satır eklenebiliyor.
 */
export const BOLUM_NITELIGI = "data-bolum";

/** data-secenekler niteliğini diziye çevirir. Yoksa null. */
export function secenekleriCoz(deger) {
  if (!deger) return null;
  const liste = deger.split("|").filter(Boolean);
  return liste.length > 0 ? liste : null;
}

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

/**
 * Adres bir SATIRIN kendisini mi gösteriyor?
 *
 * Sunucu her satırın <tr>'sine de adres basıyor (sections.<key>.rows.<i>),
 * hücrelere ise alan adresi (sections.<key>.rows.<i>.<field>). Satır adresi
 * bir NESNEYİ gösterir; düzenleme kutusu açılırsa "[object Object]" yazar ve
 * kaydedilirse satırın tamamı bir metinle ezilir. O yüzden satır adresi
 * düzenlemeye değil, satır araçlarına (ekle/sil/sırala) gider.
 */
export function satirAdresiMi(adres) {
  return /^sections\.[^.]+\.rows\.\d+$/.test(adres);
}

/** Satır adresini bölüm adresi + indeks olarak ayırır. */
function satirAyristir(adres) {
  const parcalar = adres.split(".");
  return { bolumAdresi: `${parcalar[0]}.${parcalar[1]}.rows`, index: Number(parcalar[3]) };
}

/**
 * Satır işlemleri - hepsi YENİ içerik döndürür, yerinde değiştirmez.
 * islem: 'ekle' | 'kopyala' | 'yukari' | 'asagi' | 'sil'
 */
export function satirIslemi(content, adres, islem, bosSatir) {
  if (!satirAdresiMi(adres)) return content;
  const { bolumAdresi, index } = satirAyristir(adres);
  const satirlar = alandanOku(content, bolumAdresi);
  if (!Array.isArray(satirlar)) return content;

  const yeni = [...satirlar];
  switch (islem) {
    case "ekle":
      yeni.splice(index + 1, 0, bosSatir);
      break;
    case "kopyala":
      yeni.splice(index + 1, 0, { ...satirlar[index] });
      break;
    case "yukari":
      if (index === 0) return content;
      [yeni[index - 1], yeni[index]] = [yeni[index], yeni[index - 1]];
      break;
    case "asagi":
      if (index >= satirlar.length - 1) return content;
      [yeni[index], yeni[index + 1]] = [yeni[index + 1], yeni[index]];
      break;
    case "sil":
      yeni.splice(index, 1);
      break;
    default:
      return content;
  }
  return alanaYaz(content, bolumAdresi, yeni);
}

/**
 * İki satırın yerini değiştirir.
 *
 * Neden komşu takası değil de İKİ ADRES: Sprint Kapanış tablosu satırları
 * sektöre göre grupluyor, yani ekrandaki sıra içerikteki sıra DEĞİL
 * (içerik 0,1,2 → ekran 0,2,1 olabiliyor). "Bir aşağı" demek, içerikte
 * bir sonraki değil EKRANDA bir sonraki satırla takas etmek demek; hangi
 * satır olduğunu çağıran taraf DOM'dan buluyor.
 */
export function satirTakas(content, adresA, adresB) {
  if (!satirAdresiMi(adresA) || !satirAdresiMi(adresB)) return content;
  const a = satirAyristir(adresA);
  const b = satirAyristir(adresB);
  if (a.bolumAdresi !== b.bolumAdresi) return content;

  const satirlar = alandanOku(content, a.bolumAdresi);
  if (!Array.isArray(satirlar)) return content;
  if (satirlar[a.index] === undefined || satirlar[b.index] === undefined) return content;

  const yeni = [...satirlar];
  [yeni[a.index], yeni[b.index]] = [yeni[b.index], yeni[a.index]];
  return alanaYaz(content, a.bolumAdresi, yeni);
}

/** Bölümün SONUNA satır ekler. Önizlemedeki "+ satır ekle" bunu kullanır. */
export function bolumeSatirEkle(content, bolumKey, bosSatir) {
  const bolumAdresi = `sections.${bolumKey}.rows`;
  const satirlar = alandanOku(content, bolumAdresi);
  if (!Array.isArray(satirlar)) return content;
  return alanaYaz(content, bolumAdresi, [...satirlar, bosSatir]);
}

/** Satırın bulunduğu bölümdeki satır sayısı - ilk/son satırı anlamak için. */
export function satirSayisi(content, adres) {
  if (!satirAdresiMi(adres)) return 0;
  const { bolumAdresi } = satirAyristir(adres);
  const satirlar = alandanOku(content, bolumAdresi);
  return Array.isArray(satirlar) ? satirlar.length : 0;
}

/** Satır adresindeki indeks. */
export function satirIndeksi(adres) {
  return satirAdresiMi(adres) ? satirAyristir(adres).index : -1;
}

/** Bu alan çok satırlı mı - textarea mı input mu açılacak. */
export function cokSatirliMi(adres) {
  const son = adres.split(".").pop();
  return COK_SATIRLI_ALANLAR.includes(son) || adres.startsWith("intro.");
}
