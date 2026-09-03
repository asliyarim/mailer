// Sekiz takimin HERO ve FOOTER LOGO gorsellerini uretir.
//
//   hero.png  takim renginde zemin, ustte aksa | KAZANCI HOLDING bandi,
//             altinda takim damgasi
//   logo.png  footer seridi icin, zemini TAKIMIN KENDI RENGI
//
//   node scripts/tema-gorselleri-uret.js
//
// NEDEN URETIYORUZ, elle cizmiyoruz:
//   - Yedi takim RPA'nin lacivert gorselini yer tutucu kullaniyordu; bordo
//     bir mailin icinde lacivert bir robot duruyordu.
//   - Maskot kaldirildi, yerine takim damgasi geldi (yonetici istegi).
// Renkler Temalar.java ile AYNI olmak zorunda - orada degisirse burada da.
//
// Damga tasarimi OZGUNDUR. Ic ice halka fikri yaygin bir gorsel arac;
// herhangi bir markanin cizimi kopyalanmadi.
//
// Gereksinim: sharp. Yoksa: npm i sharp

const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const KOK = path.resolve(__dirname, '..');
const CIKIS = path.join(KOK, 'src/main/resources/themes');
const LOGO_BANDI = path.join(__dirname, 'varliklar/logo-bandi.png'); // beyaz, seffaf zeminli

const HERO_EN = 315, HERO_BOY = 235;
const AKSA_YESIL = '#8ccc4f';

/**
 * Temalar.java ile AYNI olmak zorunda: anahtar, logo dosyasi, koyu renk.
 * Logolar Aslı'nin gonderdigi kurumsal dosyalar - cizim degil, kaynak.
 */
const LOGO_KLASORU = path.join(__dirname, 'varliklar/takim-logolari');
const TAKIMLAR = [
  ['rpa',                 'rpa.jpg',                 '#003b78'],
  ['is-zekasi',           'is-zekasi.jpg',           '#7a4405'],
  ['urun-gelistirme',     'urun-gelistirme.jpg',     '#701a3a'],
  ['yapay-zeka',          'yapay-zeka.jpg',          '#3b2f8f'],
  ['dijital-uygulamalar', 'dijital-uygulamalar.jpg', '#052c59'],
  ['dokuman',             'dokuman.jpg',             '#2f3b4a'],
  ['cbs',                 'cbs.jpg',                 '#0a4f49'],
  ['mobil',               'mobil.jpg',               '#1a3a8f'],
];

// --- takim logosu ------------------------------------------------------------

/**
 * Takimin logosunu daire olarak kirpar.
 *
 * Kaynak dosyalar BEYAZ ZEMINLI kare. Oldugu gibi konsaydi takim renginde
 * hero'nun uzerinde beyaz bir KARE gorunurdu. Once icerigin gercek sinirlari
 * bulunup kare kirpiliyor, sonra daireye maskeleniyor: beyaz gobek kaliyor
 * ama koseler gidiyor, sonuc renkli zeminde bir rozet gibi duruyor.
 */
async function logoDairesi(dosya, olcu, dolgu = 1.0) {
  const { data, info } = await sharp(dosya).ensureAlpha().raw()
    .toBuffer({ resolveWithObject: true });
  const W = info.width, H = info.height, C = info.channels;

  // Beyaz olmayan pikseller = logonun kendisi
  let x0 = W, y0 = H, x1 = 0, y1 = 0;
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const i = (y * W + x) * C;
    if ((data[i] + data[i + 1] + data[i + 2]) / 3 < 235) {
      if (x < x0) x0 = x; if (y < y0) y0 = y;
      if (x > x1) x1 = x; if (y > y1) y1 = y;
    }
  }
  const kenar = Math.max(x1 - x0, y1 - y0) + 8;
  const cx = Math.round((x0 + x1) / 2), cy = Math.round((y0 + y1) / 2);
  const sol = Math.max(0, cx - Math.round(kenar / 2));
  const ust = Math.max(0, cy - Math.round(kenar / 2));

  // dolgu > 1 ise cizim rozetten BUYUK cizilip ortadan kirpiliyor.
  // Neden gerekiyor: cark logosunun KENDI cizimi ic bosluk tasiyor, oldugu
  // gibi konunca rozetin icinde belirgin bir beyaz pay kaliyor ve logo
  // digerlerine gore kucuk gorunuyordu. Uc oran denenip gozle karsilastirildi:
  // 1.08 rozeti dolduruyor, 1.14'te dis etiketler kesilmeye basliyor.
  const ciz = Math.round(olcu * dolgu);
  let tuval = await sharp(dosya)
    .extract({ left: sol, top: ust,
               width: Math.min(kenar, W - sol), height: Math.min(kenar, H - ust) })
    .resize({ width: ciz, height: ciz, fit: 'cover' }).png().toBuffer();

  if (ciz > olcu) {
    const k = Math.round((ciz - olcu) / 2);
    tuval = await sharp(tuval).extract({ left: k, top: k, width: olcu, height: olcu })
      .png().toBuffer();
  }

  const maske = Buffer.from(
    `<svg width="${olcu}" height="${olcu}"><circle cx="${olcu / 2}" cy="${olcu / 2}" r="${olcu / 2}" fill="#fff"/></svg>`);
  return sharp(tuval).composite([{ input: maske, blend: 'dest-in' }]).png().toBuffer();
}

// --- hero -------------------------------------------------------------------

async function heroUret(anahtar, logoDosyasi, koyu, ciktiAdi = 'hero.png', dolgu = 1.0) {
  const DAMGA = 152;
  const damga = await logoDairesi(logoDosyasi, DAMGA, dolgu);

  const logo = await sharp(LOGO_BANDI).resize({ width: 196 }).png().toBuffer();
  const logoBoy = (await sharp(logo).metadata()).height;

  const klasor = path.join(CIKIS, anahtar);
  fs.mkdirSync(klasor, { recursive: true });

  await sharp({ create: { width: HERO_EN, height: HERO_BOY, channels: 4,
                          background: koyu } })
    .composite([
      { input: logo,  left: Math.round((HERO_EN - 196) / 2), top: 20 },
      { input: damga, left: Math.round((HERO_EN - DAMGA) / 2), top: 20 + logoBoy + 16 },
    ])
    .png({ compressionLevel: 9 })
    .toFile(path.join(klasor, ciktiAdi));

  return path.join(klasor, ciktiAdi);
}

/**
 * Footer logosu. Zemin TAKIMIN KENDI RENGI - saydam DEGIL.
 *
 * Ikisi de denendi ve ikisi de kirildi:
 *   - Zemini lacivert sabit birakinca bordo footer'da mavi bir dikdortgen
 *     goruluyordu (tema basina renk farkli).
 *   - Saydam yapinca Outlook'a YAPISTIRMADA kayboldu: Word saydam PNG'yi
 *     beyaza duzlestiriyor, beyaz yazi beyaz zeminde gorunmez oluyor.
 *
 * Zemini takimin renginde basmak ikisini de cozuyor: footer'la birebir
 * ayni renk oldugu icin blok gorunmuyor, opak oldugu icin duzlestirme
 * bozamiyor.
 */
async function logoUret(anahtar, koyu) {
  const EN = 285, BOY = 34, BANT = 232;

  const bant = await sharp(LOGO_BANDI).resize({ width: BANT }).png().toBuffer();
  const bantBoy = (await sharp(bant).metadata()).height;

  const klasor = path.join(CIKIS, anahtar);
  fs.mkdirSync(klasor, { recursive: true });

  await sharp({ create: { width: EN, height: BOY, channels: 4, background: koyu } })
    .composite([{ input: bant, left: Math.round((EN - BANT) / 2),
                  top: Math.round((BOY - bantBoy) / 2) }])
    .png({ compressionLevel: 9 })
    .toFile(path.join(klasor, 'logo.png'));

  return path.join(klasor, 'logo.png');
}

(async () => {
  if (!fs.existsSync(LOGO_BANDI)) {
    console.error('HATA: logo bandi yok -> ' + LOGO_BANDI);
    process.exit(1);
  }
  for (const [anahtar, logoDosyasi, koyu] of TAKIMLAR) {
    // Hero SABLONA GORE degisiyor: iki toplanti sablonunda logo butun
    // takimlarda AYNI, ama ZEMIN takimin rengi kaliyor. Ortak tek dosya
    // uretilseydi bordo bir mailde lacivert panel gorunurdu.
    const hero = await heroUret(anahtar, path.join(LOGO_KLASORU, logoDosyasi), koyu);
    // Yeni yonetici logosu halka bicimli ve kareyi zaten dolduruyor:
    // carkta gereken %108 buyutme burada gerekmiyor (dolgu = 1.0).
    const yonetici = await heroUret(anahtar,
      path.join(LOGO_KLASORU, 'yonetici-ozeti.png'), koyu, 'hero-yonetici.png');
    const toplanti = await heroUret(anahtar,
      path.join(LOGO_KLASORU, 'toplanti-ciktilari.png'), koyu, 'hero-toplanti.png');
    const logo = await logoUret(anahtar, koyu);
    const kb = (n) => (fs.statSync(n).size / 1024).toFixed(1);
    console.log(`  ${anahtar.padEnd(22)} hero ${kb(hero)} · yonetici ${kb(yonetici)} · toplanti ${kb(toplanti)} · logo ${kb(logo)} KB`);
  }
  console.log('\nHero ve footer logolari uretildi.');
})();
