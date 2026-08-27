// Backend ile konusan TEK yer. Bilesenler dogrudan fetch() cagirmaz -
// yeni bir uc nokta gerekiyorsa buraya fonksiyon eklenir.
//
// "??" kasten kullanilir: prod'da VITE_API_BASE_URL bilerek BOS STRING olarak
// build edilir (Odyssey ile ayni origin, bkz. docs/BRIEF.md §1). "||" olsaydi
// bos string falsy oldugundan yanlislikla localhost'a duserdi.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8082";

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
async function authFetch(path, buildInit) {
  let response = await fetch(`${API_BASE_URL}${path}`, buildInit());
  if (response.status === 401 && !isAuthEndpoint(path)) {
    const refreshed = await tryRefreshSession();
    if (refreshed) {
      response = await fetch(`${API_BASE_URL}${path}`, buildInit());
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

async function ensureOk(response, fallbackMessage) {
  if (response.ok) return response;
  let message = fallbackMessage;
  try {
    const body = await response.json();
    if (body?.message) message = body.message;
  } catch {
    // govde JSON degilse fallback mesaj kalir
  }
  throw new ApiError(`${message} (HTTP ${response.status})`, response.status);
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

export async function fetchDocuments(teamId) {
  const response = await authFetch(`/api/mailer/documents?teamId=${encodeURIComponent(teamId)}`, getInit);
  await ensureOk(response, "Belgeler alınamadı.");
  return response.json();
}

export async function fetchDocument(id) {
  const response = await authFetch(`/api/mailer/documents/${id}`, getInit);
  await ensureOk(response, "Belge alınamadı.");
  return response.json();
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

/** .eml dosyasini indirilebilir Blob olarak getirir. */
export async function fetchEml(id) {
  const response = await authFetch(`/api/mailer/documents/${id}/export.eml`, getInit);
  await ensureOk(response, "Mail dosyası indirilemedi.");
  return response.blob();
}

export async function logDownload(id, format) {
  const response = await authFetch(`/api/mailer/documents/${id}/downloads`, jsonInit("POST", { format }));
  await ensureOk(response, "İndirme kaydedilemedi.");
}
