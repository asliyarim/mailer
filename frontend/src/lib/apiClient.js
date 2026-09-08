// Backend ile konusan TEK yer. Bilesenler dogrudan fetch() cagirmaz -
// yeni bir uc nokta gerekiyorsa buraya fonksiyon eklenir.
//
// "??" kasten kullanilir: prod'da VITE_API_BASE_URL bilerek BOS STRING olarak
// build edilir (Odyssey ile ayni origin, bkz. docs/BRIEF.md §1). "||" olsaydi
// bos string falsy oldugundan yedege duserdi.
//
// YEDEK DE BOS STRING, "localhost:8082" DEGIL. Sebep: degisken verilmeden
// build alinirsa (Docker disinda elle "npm run build" gibi) localhost adresi
// pakete GOMULUR ve uretimde her istek kullanicinin kendi makinesine gider.
// Sessizce ve herkeste ayni sekilde bozulur. Bos string ise "ayni origin"
// demek - gelistirmede vite proxy'si, uretimde Odyssey'in nginx'i karsilar.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

/**
 * Auth cerezleri (access_token/refresh_token) httpOnly oldugu icin JS'ten
 * okunamaz - "credentials: include" ile otomatik gonderilir. XSRF-TOKEN
 * cerezi ise bilerek httpOnly DEGIL: degerini okuyup X-CSRF-Token basligina
 * yansitiyoruz (double-submit deseni, bkz. backend CsrfCookieFilter).
 */
function readCookie(name) {
  const match = document.cookie.match(new RegExp("(?:^|; )" + name + "=([^;]*)"));
  return match ? decodeURIComponent(match[1]) : null;
}

function csrfHeaders() {
  const token = readCookie("XSRF-TOKEN");
  return token ? { "X-CSRF-Token": token } : {};
}

// access_token'in omru kisa. refresh_token hala gecerliyken kullanici sessizce
// 401'lere dusmesin diye: her istekte 401 alinirsa BIR KEZ refresh denenir,
// basariliysa istek tekrar edilir. Ayni anda birden fazla istek 401 alirsa
// tek bir refresh cagrisinda birlesir (refreshPromise).
let refreshPromise = null;

function isAuthEndpoint(path) {
  return path.startsWith("/api/auth/");
}

async function tryRefreshSession() {
  if (!refreshPromise) {
    refreshPromise = fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: "POST",
      credentials: "include",
      headers: csrfHeaders(),
    })
      .then((r) => r.ok)
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

/**
 * Mutasyon istekleri icin `buildInit` FONKSIYON olarak verilir ki tekrar
 * denemede X-CSRF-Token taze cerez degerinden yeniden okunsun (refresh,
 * XSRF-TOKEN cerezini rotate edebilir).
 */
/**
 * Sunucuya hic ulasilamadiginda tarayici "Failed to fetch" diye ham bir
 * TypeError atiyor. Kullaniciya bunu gostermek "bir sey oldu ama ne
 * bilmiyorum" demek; hangi durumda oldugunu ve ne yapacagini soyluyoruz.
 * Sunucu yeniden baslatilirken (deploy) tam olarak bu yasaniyor.
 */
async function agaSor(path, init) {
  try {
    return await fetch(`${API_BASE_URL}${path}`, init);
  } catch {
    throw new ApiError(
      "Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.",
      0
    );
  }
}

async function authFetch(path, buildInit) {
  let response = await agaSor(path, buildInit());
  if (response.status === 401 && !isAuthEndpoint(path)) {
    const refreshed = await tryRefreshSession();
    if (refreshed) {
      response = await agaSor(path, buildInit());
    }
  }
  return response;
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

/**
 * Govdesi olmayan hatalar icin duruma ozel mesajlar.
 *
 * 401 ve 403 KARISTIRILMAMALI: 401 oturumla ilgilidir ve yenilemekle gecer
 * (authFetch zaten bir kez dener), 403 ise yetkiyle ilgilidir - kullanici
 * ne kadar yenilerse yenilesin gecmez. "Tekrar giris yapin" demek, o takima
 * erisimi olmayan kullaniciyi bos yere giris cikis dongusune sokar.
 * 401'in govdesi zaten yoktur (docs/api.md, Ortak kurallar).
 */
const DURUM_MESAJLARI = {
  401: "Oturumunuz sona ermiş. Odyssey üzerinden yeniden giriş yapın.",
  403: "Bu işlem için yetkiniz yok. Takım erişiminizi kontrol edin.",
};

/**
 * JSON olmayan hata govdesini teknik ipucu olarak hazirlar.
 *
 * Neden: DURUM_MESAJLARI bir TAHMINDIR. Gercek bir vakada sunucu 403 ile
 * duz metin "Invalid CORS request" dondu (Spring'in CORS filtresi, gövde
 * JSON degil) ve biz kullaniciya "yetkiniz yok" dedik - yetkiyle hicbir
 * ilgisi yoktu, kabuk portu CORS listesinde degildi. Sunucu sebebi
 * SOYLEMISTI, biz atiyorduk.
 *
 * Artik atmiyoruz: tahmini kullanicinin okuyacagi cumle olarak tutup,
 * sunucunun ham sozunu yaninda tasiyoruz. Sorunu bildiren kisi ekrandaki
 * metni okudugunda teshis elimizde oluyor.
 */
function hamIpucu(metin) {
  const temiz = metin?.trim();
  if (!temiz) return null;
  // nginx/proxy HTML hata sayfasi - kullaniciya gosterilecek bir sey degil.
  if (temiz.startsWith("<")) return null;
  return temiz.length > 120 ? `${temiz.slice(0, 120)}…` : temiz;
}

async function ensureOk(response, fallbackMessage) {
  if (response.ok) return response;
  let message = DURUM_MESAJLARI[response.status] ?? fallbackMessage;
  let ipucu = null;

  try {
    // Govdeyi metin olarak okuyup JSON'u KENDIMIZ deniyoruz; response.json()
    // basarisiz olunca govde tamamen kayboluyordu.
    const metin = await response.text();
    let govde = null;
    try {
      govde = JSON.parse(metin);
    } catch {
      // JSON degil - ham metni ipucu olarak sakla
    }
    if (govde?.message) {
      // Sunucunun kendi mesaji her zaman daha spesifik - o kazanir.
      message = govde.message;
    } else {
      ipucu = hamIpucu(metin);
    }
  } catch {
    // govde hic okunamadi - yukaridaki mesaj tek basina kalir
  }

  const detay = ipucu ? ` — sunucu: "${ipucu}"` : "";
  throw new ApiError(`${message}${detay} (HTTP ${response.status})`, response.status);
}

function getInit() {
  return { method: "GET", credentials: "include" };
}

function jsonInit(method, body) {
  return () => ({
    method,
    credentials: "include",
    headers: { "Content-Type": "application/json", ...csrfHeaders() },
    body: JSON.stringify(body),
  });
}

// --- Oturum (odyssey-auth servisi) ------------------------------------------

/** Giris yapmis kullanici: { sicil, fullName, role, ... }. 401 ise oturum yok. */
export async function fetchCurrentUser() {
  const response = await authFetch("/api/auth/me", getInit);
  await ensureOk(response, "Oturum bilgisi alınamadı.");
  return response.json();
}

// --- Takimlar ---------------------------------------------------------------

export async function fetchTeams() {
  const response = await authFetch("/api/mailer/teams", getInit);
  await ensureOk(response, "Takımlar alınamadı.");
  return response.json();
}

// --- Belgeler ---------------------------------------------------------------

/**
 * Bir takimin belgeleri. `q` verilirse SUNUCU suzuyor - basliкta ve donemde,
 * Turkce harf kurallariyla (docs/api.md).
 *
 * Istemcide ikinci bir filtre YOK: iki arama mantigi olsaydi ozellikle
 * sapkali sesli katlamasi gibi ince kurallarda zamanla ayrisirlardi.
 */
export async function fetchDocuments(teamId, q = "") {
  const parametreler = new URLSearchParams({ teamId: String(teamId) });
  if (q.trim()) parametreler.set("q", q.trim());
  const response = await authFetch(`/api/mailer/documents?${parametreler}`, getInit);
  await ensureOk(response, "Belgeler alınamadı.");
  return response.json();
}

/**
 * Bir tipin/takimin VARSAYILAN icerigi - belge OLUSTURMADAN (docs/api.md §3b).
 *
 * Neden var: uygulama acilinca sagda mail sablonunun tamami gorunsun
 * isteniyor, ama o an ortada belge yok. Iki alternatif elendi:
 *   - her acilista taslak yaratmak  -> DB'de cop taslak birikir
 *   - iskeleti istemcide kurmak     -> ikinci bir "bos icerik" tanimi
 *     olusur ve VarsayilanIcerik'ten ayrisir (Mimari Kural 4)
 *
 * Uc yan etkisiz: hicbir sey yazmaz. Dondugu sey POST /documents'in
 * uretecegi content'in AYNISI - sunucuda ikisi ayni metodu cagiriyor.
 */
export async function fetchDefaultContent(teamId, templateType) {
  const response = await authFetch(
    `/api/mailer/documents/default?teamId=${encodeURIComponent(teamId)}` +
      `&templateType=${encodeURIComponent(templateType)}`,
    getInit
  );
  await ensureOk(response, "Varsayılan içerik alınamadı.");
  return response.json();
}

/**
 * Giris sayfasindaki "Son Taslaklarım" - kullanicinin erisebildigi BUTUN
 * takimlarin belgeleri, en yeni once (docs/api.md).
 *
 * teamId parametresi YOK: sunucu oturumun kendi takimlarindan turetiyor.
 * documents?teamId= tek takim istiyor; PO'nun birden cok takimi olabildigi
 * icin giris sayfasi orada N ayri istek atmak zorunda kalirdi.
 */
export async function fetchRecentDocuments(limit = 12, arama = "") {
  // Arama SUNUCUDA yapiliyor (baslik + donem, Turkce harf kurallariyla).
  // Istemcide ikinci bir filtre YAZILMAZ: iki arama mantigi zamanla ayrisir
  // ve "neden bu kayit cikmadi" sorusunun cevabi kaybolur.
  const parametreler = new URLSearchParams({ limit: String(limit) });
  if (arama.trim()) parametreler.set("q", arama.trim());

  const response = await authFetch(
    `/api/mailer/documents/recent?${parametreler}`,
    getInit
  );
  await ensureOk(response, "Son belgeler alınamadı.");
  return response.json();
}

export async function fetchDocument(id) {
  const response = await authFetch(`/api/mailer/documents/${id}`, getInit);
  await ensureOk(response, "Belge alınamadı.");
  return response.json();
}

/**
 * "Geçen sprintten devam et": kaynağın kopyasını yeni taslak olarak açar.
 *
 * Kopyalama SUNUCUDA yapiliyor - burada "yeni belge yarat + icerigi kaydet"
 * diye iki istek atilsaydi, ikincisi dustugunde geriye kullanicinin
 * silemedigi BOS bir belge kalirdi.
 */
export async function copyDocument(id) {
  const response = await authFetch(`/api/mailer/documents/${id}/kopya`, jsonInit("POST", {}));
  await ensureOk(response, "Belge kopyalanamadı.");
  return response.json();
}

/**
 * Belgeyi KALICI olarak siler. Onay ARAYUZDE alinir - bu fonksiyon
 * cagrildiginda kullanici zaten onaylamis sayilir.
 */
export async function deleteDocument(id) {
  // buildInit FONKSIYON verilir: 401 sonrasi tekrar denemede X-CSRF-Token
  // taze cerezden yeniden okunsun (bkz. authFetch).
  const response = await authFetch(`/api/mailer/documents/${id}`, () => ({
    method: "DELETE",
    credentials: "include",
    headers: csrfHeaders(),
  }));
  await ensureOk(response, "Belge silinemedi.");
}

export async function createDocument({ teamId, templateType, title }) {
  const response = await authFetch("/api/mailer/documents", jsonInit("POST", { teamId, templateType, title }));
  await ensureOk(response, "Belge oluşturulamadı.");
  return response.json();
}

/**
 * expectedVersion zorunlu: sunucudaki currentVersion bundan farklıysa 409
 * döner (araya başkası kaydetmiş). Cagiran taraf 409'u yakalayip kullaniciya
 * sormali - korlemesine uzerine YAZMAMALI. bkz. docs/api.md §5.
 */
export async function saveDocument(id, { title, subject, content, expectedVersion }) {
  const response = await authFetch(
    `/api/mailer/documents/${id}`,
    jsonInit("PUT", { title, subject, content, expectedVersion })
  );
  await ensureOk(response, "Belge kaydedilemedi.");
  return response.json();
}

export async function fetchVersions(id) {
  const response = await authFetch(`/api/mailer/documents/${id}/versions`, getInit);
  await ensureOk(response, "Versiyon geçmişi alınamadı.");
  return response.json();
}

export async function rollbackToVersion(id, version) {
  const response = await authFetch(`/api/mailer/documents/${id}/versions/${version}/rollback`, jsonInit("POST", {}));
  await ensureOk(response, "Geri alma başarısız.");
  return response.json();
}

// --- Onizleme ve cikti ------------------------------------------------------

/**
 * MIMARI KURAL 1: mail HTML'i YALNIZCA sunucuda uretilir. Bu fonksiyon
 * kaydedilmemis icerigi gonderip HAZIR HTML alir; donen metin oldugu gibi
 * iframe srcdoc'una basilir. Burada veya baska bir yerde HTML kurma,
 * string birlestirme, sablon doldurma YAPILMAZ.
 */
export async function renderPreview({ teamId, templateType, content }) {
  const response = await authFetch(
    "/api/mailer/render/preview",
    jsonInit("POST", { teamId, templateType, content })
  );
  await ensureOk(response, "Önizleme üretilemedi.");
  return response.text();
}

/**
 * "Outlook Icin Kopyala" - panoya konacak HTML (docs/api.md).
 *
 * Onizlemeden tek farki duzenleme niteliklerinin (data-alan,
 * data-secenekler) soyulmus olmasi: panoya yapistirilan sey gercek mail
 * olmali, editor izleri tasimamali. Gorseller data: URI ile gomulu -
 * cid: pano uzerinden calismaz, MIME kabi yok.
 */
export async function renderClipboard({ teamId, templateType, content }) {
  const response = await authFetch(
    "/api/mailer/render/clipboard",
    jsonInit("POST", { teamId, templateType, content })
  );
  await ensureOk(response, "Kopyalanacak mail üretilemedi.");
  return response.text();
}

/**
 * "PDF İndir" - sunucu ayni HTML'den PDF uretir (docs/api.md).
 *
 * NEDEN SUNUCUDA: istemcide PDF uretmek maili ekran goruntusu gibi
 * rasterize etmek demekti - bulanik yazi, sisen dosya, bozulan tablo ve
 * mailin IKINCI bir cizimi (Mimari Kural 1). Sunucuda ayni HTML'den
 * uretiliyor; Turkce harfler icin yazi tipi PDF'e gomuluyor, cunku PDF'in
 * yerlesik fontlari s/g/i/I tasimiyor.
 *
 * Onizleme ve pano gibi KAYDEDILMEMIS icerikten uretir - ekranda ne
 * gorunuyorsa o iner.
 */
export async function renderPdf({ teamId, templateType, content }) {
  const response = await authFetch(
    "/api/mailer/render/pdf",
    jsonInit("POST", { teamId, templateType, content })
  );
  await ensureOk(response, "PDF üretilemedi.");
  return {
    blob: await response.blob(),
    dosyaAdi: dosyaAdiCoz(response.headers.get("Content-Disposition")),
  };
}

/**
 * Content-Disposition basligindaki dosya adini okur.
 *
 * Sunucu adi BILEREK ASCII'ye indirger (RenderController.dosyaAdi): bazi
 * istemciler UTF-8 dosya adini yanlis cozup adi bozuyor. Istemcide
 * belge.title'i kullanmak o karari bosa cikarir - "Ağustos Kapanışı.eml"
 * kimi Windows kurulumlarinda bozuk adla iner. O yuzden ad sunucudan gelir.
 *
 * Baslik okunamazsa null doner (farkli origin'de tarayici bu basligi
 * gizler); cagiran taraf o zaman kendi yedegini kullanir.
 */
function dosyaAdiCoz(contentDisposition) {
  if (!contentDisposition) return null;
  // Once RFC 5987 bicimi: filename*=UTF-8''...
  const genisletilmis = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (genisletilmis) {
    try {
      return decodeURIComponent(genisletilmis[1]);
    } catch {
      // bozuk yuzde kodlamasi - duz bicime dus
    }
  }
  const duz = contentDisposition.match(/filename="?([^";]+)"?/i);
  return duz ? duz[1] : null;
}

/** .eml dosyasini indirilebilir Blob + sunucunun verdigi dosya adiyla getirir. */
export async function fetchEml(id) {
  const response = await authFetch(`/api/mailer/documents/${id}/export.eml`, getInit);
  await ensureOk(response, "Mail dosyası indirilemedi.");
  return {
    blob: await response.blob(),
    dosyaAdi: dosyaAdiCoz(response.headers.get("Content-Disposition")),
  };
}

export async function logDownload(id, format) {
  const response = await authFetch(`/api/mailer/documents/${id}/downloads`, jsonInit("POST", { format }));
  await ensureOk(response, "İndirme kaydedilemedi.");
}
