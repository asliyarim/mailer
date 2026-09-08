// YEREL TEST ARACI - odyssey-auth ile ayni sirla imzalanmis JWT uretir.
//
//   node scripts/mint-jwt.js <secret> [sicil] [role] [takimId...]
//
// Neden var: duman testinin calisan bir kullanici hesabina ihtiyaci olmasin.
// Backend token'i uretmez, sadece dogrular; ayni sirla imzalanmis token
// gercek oturumdan ayirt edilemez.
//
// UYARI: sadece yerel gelistirme. Uretim sirri asla buraya girmemeli.
const crypto = require('crypto');

const [, , secret, sicil = '10234', role = 'PO', ...takimlar] = process.argv;

// Takim kimlikleri verilmezse 1 ve 2 - duman testinin varsayilan senaryosu.
const teamIds = takimlar.length > 0 ? takimlar.map(Number) : [1, 2];

if (!secret) {
  console.error('Kullanım: node scripts/mint-jwt.js <secret> [sicil] [role]');
  process.exit(1);
}

const b64 = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');

/**
 * ALGORITMA SIRRIN UZUNLUGUNDAN SECILIR - jjwt ile AYNI kural.
 *
 * odyssey-auth token'i `builder.signWith(key)` ile imzaliyor ve algoritmayi
 * ACIKCA VERMIYOR; jjwt bu durumda anahtarin bayt uzunluguna bakip seciyor:
 * >=64 -> HS512, >=48 -> HS384, >=32 -> HS256. Aksa'nin sirri 64 bayt, yani
 * uretimdeki butun token'lar HS512.
 *
 * NEDEN SABIT 'HS256' DEGIL (eskiden oyleydi): mailer dogrularken
 * `Jwts.parser().verifyWith(key)` diyor ve ALGORITMA KISITLAMASI KOYMUYOR -
 * basliкta ne yaziyorsa onunla dogruluyor. Yani HS256 uretsek de kabul
 * ediliyordu ve duman testi (110 kontrol) yesil yaniyordu. Sonuc: test
 * ortami uretimde kullanilan algoritmayi HIC denemiyordu.
 *
 * Bu, .NET donusumunde sahte bir yesil uretirdi: HS256'ya sabitlenmis bir
 * port butun duman testini gecer, sonra canlida her istekte sessizce 401
 * doner. Simdi ayni sirla ayni algoritma uretiliyor.
 *
 * KURAL: test sirri uretimdekiyle AYNI BAYT UZUNLUGUNDA olmali. Kisa sirla
 * yapilan test yesil yanar ve hicbir sey kanitlamaz.
 */
const sirUzunlugu = Buffer.byteLength(secret, 'utf8');
const [alg, hash] =
  sirUzunlugu >= 64 ? ['HS512', 'sha512']
  : sirUzunlugu >= 48 ? ['HS384', 'sha384']
  : ['HS256', 'sha256'];

// jjwt'nin Keys.hmacShaKeyFor'u 32 baytin altinda WeakKeyException atiyor.
// Sessizce zayif token uretmektense burada duruyoruz - sunucu zaten reddeder.
if (sirUzunlugu < 32) {
  console.error(`HATA: sir ${sirUzunlugu} bayt. jjwt en az 32 bayt istiyor.`);
  process.exit(1);
}

const now = Math.floor(Date.now() / 1000);
const govde = `${b64({ alg, typ: 'JWT' })}.${b64({
  sub: sicil,
  role,
  fullName: 'Aslı Yarım',
  department: 'Dijital Uygulamalar',
  teamId: teamIds[0] ?? null,
  teamIds,
  iat: now,
  exp: now + 3600,
})}`;

const imza = crypto.createHmac(hash, secret).update(govde).digest('base64url');

process.stdout.write(`${govde}.${imza}`);
