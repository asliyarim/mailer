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

const now = Math.floor(Date.now() / 1000);
const govde = `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({
  sub: sicil,
  role,
  fullName: 'Aslı Yarım',
  department: 'Dijital Uygulamalar',
  teamId: teamIds[0] ?? null,
  teamIds,
  iat: now,
  exp: now + 3600,
})}`;

const imza = crypto.createHmac('sha256', secret).update(govde).digest('base64url');

process.stdout.write(`${govde}.${imza}`);
