# Aksa Mailer — API sözleşmesi

> **Durum: DONDURULMUŞ.** Değiştirmek için PR ve Aslı'nın onayı gerekir.
> Frontend bu belgeye güvenerek yazılır — "bu veriyi nereden alacağım" diye
> sormaya gerek yok. Bir alan eksikse önce burada konuşulur, sonra kodlanır.
>
> İstemci karşılığı: [`frontend/src/lib/apiClient.js`](../frontend/src/lib/apiClient.js).
> Yeni uç eklenirse ikisi birlikte güncellenir.

Tüm yollar `/api/mailer/` altındadır. Odyssey'in nginx'i **en uzun öneki**
seçtiği için bu blok mevcut `/api/` bloğunu (Capacity Planner) yener; onun
bozulma riski yok.

---

## Ortak kurallar

### Kimlik doğrulama

Her istek `access_token` çereziyle gelir (httpOnly, odyssey-auth yazar).
Çerezsiz veya geçersiz çerezli istek **401** döner — gövde yoktur.

İstemci `credentials: "include"` kullanmak zorundadır.

### CSRF

`GET`, `HEAD`, `OPTIONS` dışındaki her istek `X-CSRF-Token` başlığı taşımalıdır.
Değeri `XSRF-TOKEN` çerezinden okunur (bu çerez httpOnly **değil**, bilerek).
Eşleşmezse **403** döner.

### İçerik tipi

Aksi belirtilmedikçe istek ve yanıt `application/json; charset=UTF-8`.
İki istisna var: `render/preview` → `text/html`, `export.eml` → `message/rfc822`.

### Hata gövdesi

Tüm hatalar aynı şekli taşır:

```json
{
  "timestamp": "2026-08-27T12:20:55.841Z",
  "status": 404,
  "error": "Not Found",
  "message": "Belge bulunamadı: 42",
  "path": "/api/mailer/documents/42"
}
```

Frontend `message` alanını kullanıcıya gösterir. Alan adı değişirse
`apiClient.js` de değişmeli.

| Kod | Anlamı |
|---|---|
| `400` | Geçersiz istek — şema hatası, zorunlu alan boş |
| `401` | Oturum yok veya süresi dolmuş |
| `403` | CSRF doğrulaması başarısız, ya da bu takıma yetkin yok |
| `404` | Kayıt yok |
| `409` | Sürüm çakışması (bkz. `PUT /documents/{id}`) |

---

## 1. `GET /api/mailer/teams`

Kullanıcının erişebildiği aktif takımlar.

**Yanıt `200`**

```json
[
  { "id": 1, "code": "RPA", "name": "RPA Takımı", "themeKey": "rpa" },
  { "id": 2, "code": "IS_ZEKASI", "name": "İş Zekâsı Takımı", "themeKey": "is-zekasi" }
]
```

`themeKey` kodda kayıtlı bir temayı işaret eder (`render/theme/`). Frontend
bunu **taşır ama yorumlamaz** — renk, logo, maskot sunucuda çözülür.

> Sprint 0'da herkes tüm aktif takımları görür. Takım bazlı yetki Sprint 2'de
> gelecek; uç noktanın şekli değişmeyecek.

---

## 2. `GET /api/mailer/documents?teamId={id}`

Takımın mail listesi — **özet**, `content` dönmez.

**Yanıt `200`**

```json
[
  {
    "id": 7,
    "teamId": 1,
    "templateType": "KAPANIS",
    "title": "Ağustos 2026 Sprint Kapanışı",
    "status": "DRAFT",
    "currentVersion": 3,
    "updatedBy": "10234",
    "updatedAt": "2026-08-27T09:14:00Z"
  }
]
```

En son güncellenen başta. `teamId` zorunlu; eksikse `400`.

---

## 3. `POST /api/mailer/documents`

Yeni taslak. **Tipin ve temanın varsayılan içeriğiyle doğar** — istemci boş
`content` göndermez, sunucu üretir. Tek doğru kaynak sunucudur.

**İstek**

```json
{ "teamId": 1, "templateType": "KAPANIS", "title": "Ağustos 2026 Sprint Kapanışı" }
```

`templateType`: `KAPANIS` | `PLANLAMA` | `YONETICI_OZETI`

**Yanıt `201`** — 4. uçtaki tam belge gövdesi. `currentVersion` = 1.

---

## 4. `GET /api/mailer/documents/{id}`

Tam belge.

**Yanıt `200`**

```json
{
  "id": 7,
  "teamId": 1,
  "themeKey": "rpa",
  "templateType": "KAPANIS",
  "title": "Ağustos 2026 Sprint Kapanışı",
  "subject": "Dijital Uygulamalar Sprint Bilgilendirme",
  "status": "DRAFT",
  "currentVersion": 3,
  "content": { "schemaVersion": 1, "...": "bkz. §content şeması" },
  "createdBy": "10234",
  "updatedBy": "10234",
  "createdAt": "2026-08-20T07:00:00Z",
  "updatedAt": "2026-08-27T09:14:00Z"
}
```

`themeKey` kolaylık için burada da döner — istemci ayrıca `/teams` çağırmasın.

---

## 5. `PUT /api/mailer/documents/{id}`

Kaydet. **Her kayıt yeni bir versiyon satırı yazar.**

**İstek**

```json
{
  "title": "Ağustos 2026 Sprint Kapanışı",
  "subject": "Dijital Uygulamalar Sprint Bilgilendirme",
  "content": { "schemaVersion": 1, "...": "..." },
  "expectedVersion": 3
}
```

`expectedVersion` zorunlu. Sunucudaki `currentVersion` bundan farklıysa
**409** döner — başkası (veya kendi ikinci sekmen) araya kaydetmiştir.
İstemci kullanıcıya sorar; körlemesine üzerine yazmaz.

**Yanıt `200`** — 4. uçtaki gövde, `currentVersion` bir artmış.

`content` sunucuda doğrulanır; şema hatası **400** döner.

---

## 6. `GET /api/mailer/documents/{id}/versions`

Versiyon geçmişi — `content` dönmez, liste hafif kalsın.

**Yanıt `200`**

```json
[
  { "version": 3, "createdBy": "10234", "createdAt": "2026-08-27T09:14:00Z" },
  { "version": 2, "createdBy": "10234", "createdAt": "2026-08-25T11:02:00Z" }
]
```

Yeniden eskiye sıralı.

---

## 7. `POST /api/mailer/documents/{id}/versions/{version}/rollback`

Geri al. Eski içeriği **yeni bir versiyon olarak** yazar — geçmiş silinmez,
geri alma da geri alınabilir.

Gövde yok.

**Yanıt `200`** — 4. uçtaki gövde. Örnek: v3'teyken v1'e dönersen
`currentVersion` **4** olur ve içeriği v1'in içeriğidir.

Olmayan versiyon → `404`.

---

## 8. `POST /api/mailer/render/preview`

Gövdedeki içeriği HTML'e çevirir. **Kaydedilmemiş düzenlemeler de önizlenir** —
her tuşta kaydetmek istemiyoruz ama kullanıcı yazdığını görmeli.

**İstek**

```json
{ "teamId": 1, "templateType": "KAPANIS", "content": { "schemaVersion": 1, "...": "..." } }
```

**Yanıt `200`**, `Content-Type: text/html; charset=UTF-8` — gövde ham HTML.

Bu HTML **`.eml` içine giren HTML'in ta kendisidir.** İstemci onu olduğu gibi
`iframe srcdoc`'una basar; üzerinde değişiklik yapmaz, ikinci bir sürüm
üretmez (Mimari Kural 1).

İstemci çağrıyı **300 ms geciktirir** ve yeni tuşa basıldığında öncekini iptal
eder.

> Görseller önizlemede `cid:` referansı taşır ve **görünmez** — normaldir.
> `cid:` ancak mail istemcisi içinde çözülür. Önizlemede görselleri görmek
> istiyorsan bu, Kural 2'yi delmek için bir gerekçe değil; Outlook'ta test et.

---

## 9. `GET /api/mailer/documents/{id}/export.eml`

"Outlook Maili İndir" butonu.

**Yanıt `200`**

```
Content-Type: message/rfc822
Content-Disposition: attachment; filename="sprint-kapanis.eml"
```

Gövde: `multipart/related` mail. Görseller `cid:` ile içine gömülü, konu
başlığı UTF-8 kodlu, `X-Unsent: 1` başlığı var (Outlook çift tıklayınca
**yeni mail** olarak açsın, okunmuş mail olarak değil).

Toplam boyut **300 KB altında** kalmalı.

---

## 10. `POST /api/mailer/documents/{id}/downloads`

İndirme logu. Ölçüm için; başarısız olması indirmeyi bozmamalı.

**İstek**

```json
{ "format": "EML" }
```

`format`: `EML` | `PDF`

**Yanıt `204`** — gövde yok.

---

## `content` şeması (v1)

Tam açıklama: [`BRIEF.md` §6](BRIEF.md). Burada bağlayıcı olan kısım:

```json
{
  "schemaVersion": 1,
  "header":  { "title": "…", "period": "…", "teamLabel": "…" },
  "meeting": { "date": "03.09.2026", "time": "10:00", "place": "…" },
  "intro":   ["…", "…", "…"],
  "sections": [
    {
      "key": "analysis",
      "title": "ANALİZ ÇALIŞMALARI",
      "tone": "blue",
      "columns": ["sector","jira","ci","process","stage","stake","note"],
      "rows": [
        { "sector": "…", "jira": "…", "ci": "…", "process": "…",
          "stage": "…", "stake": "…", "note": "…", "gain": "850" }
      ]
    }
  ],
  "notes":  [{ "tone": "blue", "text": "…" }],
  "footer": { "line1": "…", "line2": "…" }
}
```

Üç kural, üçü de eski prototiplerdeki somut hatalardan geliyor:

1. **Satırlar nesnedir, dizi değil.** Alanlara anahtarla erişilir (`row.sector`),
   indeksle değil. Prototipte `data[0]`, `data[1]` kullanılıyordu; bir sütun
   eklenince her şey kayıyordu.
2. **Sayaçlar saklanmaz.** Satır sayısından hesaplanır. Manuel sayaç butonu
   kapsam dışı, dolayısıyla saklanacak bir şey yok.
3. **`sections` bir dizidir**, sabit "analiz + geliştirme" ikilisi değil.
   Başka bir takımın üç bölümü olabilir; şema bunu kod değişikliği olmadan
   taşır.

`tone` değeri `blue` | `green`. Bu bir **renk adı değil, rol adı** — gerçek
renk temadan gelir. İstemci asla renk kodu göndermez.

### Satır alanları

| Alan | Başlık | Not |
|---|---|---|
| `sector` | Sektör | Görünür sütunsa tablolar buna göre **gruplanır** ve sütun olarak ayrıca çizilmez |
| `jira` | JIRA | Listedeki ilk sütun kalın ve renkli çizilir |
| `ci` | CI | |
| `process` | Süreç | |
| `stage` | Aşama | |
| `stake` | Paydaşlar | |
| `note` | Kritik Not | |
| `gain` | Kazanç (Saat/Yıl) | Varsayılan **kapalı**; kullanan takım `columns`'a ekler |

`columns` hangi alanın **hangi sırayla** çizileceğini söyler. Satırda olup
`columns`'ta olmayan alan çizilmez ama **silinmez** — sütunu kapatıp açmak
veri kaybettirmez.

---

## Kapsam dışı — sorulursa cevap "şimdilik hayır"

Excel/CSV içe aktarma · Jira'dan veri çekme · şablon yönetim paneli ·
manuel sayaç butonu.

İçerik şeması dördüne de uygun; yayından sonra konuşulur.
