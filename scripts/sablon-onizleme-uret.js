// ╔══════════════════════════════════════════════════════════════════════╗
// ║  Sablon karti onizlemeleri - giris sayfasindaki 4 kartin gorseli     ║
// ╚══════════════════════════════════════════════════════════════════════╝
//
//   node scripts/sablon-onizleme-uret.js
//
// NE YAPAR: her sablon tipi icin backend'e mailin GERCEK HTML'ini
// urettirip scratchpad'e yazar. Sonra tarayicida ekran goruntusu alinip
// frontend/public/sablon/ altina kucuk PNG olarak konur.
//
// NEDEN GERCEK MAIL: kart gorselini elle cizseydik mail sablonu
// degistiginde kart sessizce yalan soylemeye baslardi. Boyle uretilince
// kaynak tek: MailHtmlRenderer (Mimari Kural 1).
//
// ICERIK UYDURMA AMA GENEL: gercek bir belgenin icerigi kullanilmiyor.
// Uretilen PNG frontend paketiyle birlikte HERKESE gidiyor; icine kurum
// verisi koymak dogru olmaz. Satirlar asagidaki ORNEK_HUCRE'den geliyor.
//
// YEREL ARAC. Uretim sirri buraya girmez (bkz. scripts/mint-jwt.js).

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const KOK = path.resolve(__dirname, '..');
const CIKTI = process.argv[2] || path.join(KOK, 'target', 'sablon-onizleme');
const API = process.env.API || 'http://localhost:8082';
const TAKIM = Number(process.env.TAKIM || 1);

const TIPLER = ['KAPANIS', 'PLANLAMA', 'YONETICI_OZETI', 'TOPLANTI_CIKTILARI'];

/**
 * Sutun anahtarina gore ornek hucre metni. Anahtar taninmazsa kisa bir
 * cizgi konur - bos birakmak tabloyu cokertiyor, uydurma uzun metin de
 * kucultulunce lekeye donuyor.
 */
// Anahtarlar TAHMIN EDILMEDI, sunucudan okundu: her tipin varsayilan
// icerigindeki section.columns listeleri (KAPANIS jira/ci/process/stage/
// stake/note, PLANLAMA topicType/jira/summary/..., YONETICI_OZETI ve
// TOPLANTI_CIKTILARI team/topic/detail, no/decision/... ).
const ORNEK_HUCRE = {
  jira: ['CI-8420', 'CI-8174', 'CI-9195', 'CI-8354'],
  ci: ['CI-2201', 'CI-2198', 'CI-2185', 'CI-2177'],
  process: ['Fatura mutabakat analizi', 'Tahsilat raporu otomasyonu', 'Sözleşme kontrolü', 'Bordro aktarımı'],
  stage: ['Tamamlandı', 'Test ortamında', 'Analizde', 'Canlıda'],
  stake: ['Elektrik', 'Doğalgaz', 'Holding', 'Perakende'],
  note: ['Onay bekliyor', '—', 'Kapsam genişledi', '—'],

  topicType: ['Geliştirme', 'Analiz', 'Hata', 'İyileştirme'],
  summary: ['Bes girişleri süreci', '120 cari hesap raporu', 'ERKA fatura jeneratörü', 'UYAP veri karşılaştırma'],
  status: ['Devam ediyor', 'Planlandı', 'Beklemede', 'Başlamadı'],
  expected: ['Sprint sonunda canlı', 'Test ortamında', 'Analiz çıktısı', 'Onay sonrası'],

  team: ['RPA Ekibi', 'İş Zekâsı', 'Ürün Geliştirme', 'Doküman Yönetimi'],
  topic: ['Sprint kapasitesi', 'Yeni talep akışı', 'Test ortamı', 'Yetkilendirme'],
  detail: ['Ekip kapasitesi gözden geçirildi', 'Talep formu sadeleştirilecek', 'Ortam güncellendi', 'Roller netleştirildi'],

  no: ['1', '2', '3', '4'],
  decision: ['Süreç otomasyona alınacak', 'Kapsam ikiye bölünecek', 'Mevcut akış korunacak', 'Yeni ekip atanacak'],
  scope: ['Holding', 'Elektrik', 'Tüm şirketler', 'Perakende'],

  pending: ['Analiz dokümanı', 'Test senaryoları', 'Canlı geçiş planı', 'Yetki talebi'],
  owner: ['Ürün Geliştirme', 'RPA Ekibi', 'İş Zekâsı', 'Doküman Yönetimi'],
  due: ['12.09.2026', '19.09.2026', '26.09.2026', '03.10.2026'],

  linkType: ['Rapor', 'Pano', 'Doküman', 'Kayıt'],
  title: ['Sprint raporu', 'Kapasite panosu', 'Süreç dokümanı', 'Toplantı kaydı'],
  description: ['Dönem özeti ve sayaçlar', 'Ekip doluluk oranları', 'Güncel akış şeması', 'Video kaydı'],
  button: ['Aç', 'Aç', 'Aç', 'Aç'],
  url: ['https://intranet.aksa', 'https://intranet.aksa', 'https://intranet.aksa', 'https://intranet.aksa'],
};

const SATIR_ADEDI = 4;

function ornekHucre(sutun, sira) {
  const secenekler = ORNEK_HUCRE[sutun];
  if (!secenekler) return '—';
  return secenekler[sira % secenekler.length];
}

function cerez() {
  const env = fs.readFileSync(path.join(KOK, '.env'), 'utf8');
  const eslesme = env.match(/^APP_JWT_SECRET=(.+)$/m);
  if (!eslesme) throw new Error('.env icinde APP_JWT_SECRET yok');
  const token = execFileSync('node', [path.join(KOK, 'scripts', 'mint-jwt.js'), eslesme[1].trim(), '10234', 'PO', String(TAKIM)])
    .toString()
    .trim();
  return `access_token=${token}; XSRF-TOKEN=t`;
}

async function iste(yol, secenekler = {}) {
  const yanit = await fetch(API + yol, {
    ...secenekler,
    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': 't', Cookie: CEREZ, ...(secenekler.headers || {}) },
  });
  if (!yanit.ok) throw new Error(`${yol} -> ${yanit.status} ${await yanit.text()}`);
  return yanit;
}

/** Bos sablonu ornek satirlarla doldurur - govde tabloyla dolu gorunsun. */
function satirlariDoldur(icerik) {
  return {
    ...icerik,
    sections: (icerik.sections || []).map((bolum) => ({
      ...bolum,
      rows: Array.from({ length: SATIR_ADEDI }, (_, sira) =>
        Object.fromEntries((bolum.columns || []).map((sutun) => [sutun, ornekHucre(sutun, sira)]))
      ),
    })),
  };
}

const CEREZ = cerez();

(async () => {
  fs.mkdirSync(CIKTI, { recursive: true });

  for (const tip of TIPLER) {
    const varsayilan = await (
      await iste(`/api/mailer/documents/default?teamId=${TAKIM}&templateType=${tip}`)
    ).json();

    const icerik = satirlariDoldur(varsayilan);

    const html = await (
      await iste('/api/mailer/render/preview', {
        method: 'POST',
        body: JSON.stringify({ teamId: TAKIM, templateType: tip, content: icerik }),
      })
    ).text();

    const dosya = path.join(CIKTI, `${tip.toLowerCase()}.html`);
    fs.writeFileSync(dosya, html);
    console.log(`${tip.padEnd(20)} ${(html.length / 1024).toFixed(0)} KB  ${dosya}`);
  }

  console.log('\nSonraki adim: bu HTML dosyalarini tarayicida acip ekran goruntusu al.');
})();
