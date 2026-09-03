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

#### İki istisna: bu iki yanıtta JSON gövde yoktur

Denetlendi — geri kalan her hata yukarıdaki gövdeyi taşır.

| Durum | Gövde | Neden |
|---|---|---|
| `401` | **boş** | Spring Security'nin giriş noktası; oturum yoksa açıklayacak bir şey yok |
| `403` CORS | `Invalid CORS request` (düz metin) | Spring'in CORS işlemcisi yanıtı kendisi yazar |

İkisi de çerçeveden geliyor ve **bilerek değiştirilmedi**: CORS reddi yalnızca
gerçekten yabancı bir origin'den gelen isteklerde oluşur, orada da yanıtın
biçimi kimseye yardımcı olmaz. Özel bir `CorsProcessor` yazmak, kazancına
değmeyecek bir katman olurdu.

**İstemci tarafı için sonuç:** gövde JSON değilse `message` okunamaz. O durumda
kendi metnini göster ama sunucunun ham sözünü de taşı — yoksa yanlış teşhis
edersin. Yaşandı: kabuk portu CORS listesinde olmadığı için yazma işlemleri
düşüyordu, arayüz "yetkiniz yok" diyordu, gerçek sebep `Invalid CORS request`'ti.

CORS izin listesi `.env`'deki portlardan türetilir
(`APP_CORS_ALLOWED_ORIGINS`, bkz. `docker-compose.yml`) — elle yazılmaz.

| Kod | Anlamı |
|---|---|
| `400` | Geçersiz istek — şema hatası, zorunlu alan boş |
| `401` | Oturum yok veya süresi dolmuş |
| `403` | CSRF doğrulaması başarısız, ya da bu takıma yetkin yok |
| `404` | Kayıt yok |
| `409` | Sürüm çakışması (bkz. `PUT /documents/{id}`) |

### `status` alanı kullanılmıyor

Yanıtlarda `"status": "DRAFT"` görürsünüz. Şema `DRAFT | FINAL` tutuyor ama
**`FINAL`'e geçiren bir uç yok** — her belge `DRAFT` doğar ve öyle kalır.

Bilerek böyle: onay/gönderim akışı ne yol haritasında ne de talepte var.
İleride istenirse `POST /documents/{id}/finalize` yeterli; şema hazır.

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
    "period": "Ağustos 2026 Sprint Kapanışı",
    "status": "DRAFT",
    "currentVersion": 3,
    "updatedBy": "10234",
    "updatedAt": "2026-08-27T09:14:00Z"
  }
]
```

En son güncellenen başta. `teamId` zorunlu; eksikse `400`.

> **`period` neden burada?** Özet `content` taşımaz — bu alan **istisna**.
> Sprint numarası ve dönem yalnızca `content.header.period`'da yaşıyor;
> taşınmasaydı arama kutusu başlıkla sınırlı kalırdı (§2b). `content` zaten
> yüklü geldiği için ek maliyeti yok. İçeriği olmayan bir kayıtta `null`
> gelebilir.

---

## 3. `POST /api/mailer/documents`

Yeni taslak. **Tipin ve temanın varsayılan içeriğiyle doğar** — istemci boş
`content` göndermez, sunucu üretir. Tek doğru kaynak sunucudur.

**İstek**

```json
{ "teamId": 1, "templateType": "KAPANIS", "title": "Ağustos 2026 Sprint Kapanışı" }
```

`templateType`: `KAPANIS` | `PLANLAMA` | `YONETICI_OZETI` | `TOPLANTI_CIKTILARI`

**Yanıt `201`** — 4. uçtaki tam belge gövdesi. `currentVersion` = 1.

### Yeni belge **geçerli** doğar

Varsayılan içerik yalnızca bölüm başlıkları ve sütun listelerinden ibaret
değil: `header.title` ve `header.teamLabel` de dolu gelir. Sebebi tek cümle —
bu ikisi `PUT` ve önizlemenin **zorunlu** tuttuğu alanlar; boş doğsalardı belge
doğduğu anda kendi kuralını ihlal eder, kullanıcı tek harf yazmadan önizlemede
hata görürdü.

| Alan | Değer | Örnek (takım 5) |
|---|---|---|
| `header.title` | takım adı + tipe göre ibare | `DİJİTAL UYGULAMALAR SPRINT BİLGİLENDİRME` |
| `header.teamLabel` | takımın adı, **olduğu gibi** | `Dijital Uygulamalar Takımı` |
| `header.period` | boş | — |

Başlıkta takım adının sonundaki "Takımı"/"Ekibi" atılır ve Türkçe kurallarla
büyütülür (`Dijital` → `DİJİTAL`). Metin **sabit değil, türetilmiş**: sabit
olsaydı her takımın mailinde aynı takımın adı görünürdü.

`period` boş bırakılır — sprint numarası ve tarih aralığı tahmin edilemez.

Bunlar **başlangıç değeri**; kullanıcı üçünü de değiştirebilir.

> Bu uç artık `teamId`'yi de doğruluyor: olmayan takım `404` döner (eskiden
> yabancı anahtar ihlaline düşüp `500` olurdu).

---

## 2b. `GET /api/mailer/documents/recent`

Giriş sayfasındaki **"Son Taslaklarım"**. Kullanıcının erişebildiği **bütün**
takımların belgeleri, en yeni önce.

```
GET /api/mailer/documents/recent?limit=12&q=sprint%2042
```

**Yanıt `200`** — §2'dekiyle aynı özet dizisi.

### `q` — arama

Boş değilse **başlıkta veya dönemde** geçenleri süzer. Büyük/küçük harf
duyarsız, **Türkçe kurallarıyla**: `izmir` yazınca `İZMİR` bulunur.
Varsayılan locale kullanılsaydı bulunmazdı — `"İ".toLowerCase()` Latin `i`
vermiyor.

Boş veya yalnızca boşluk olan `q` süzmez.

> **Arama son 500 kaydı tarar**, gösterilen 12'yi değil — kullanıcı listede
> görünmeyen eski bir sprinti de bulabilsin diye. Ama 500'den eskisi çıkmaz:
> her tuşta bütün tabloyu `jsonb` içeriğiyle yüklememek için bilerek verilmiş
> bir sınır. Arayüz "bulunamadı" derken bunu ima etmeli.

**İstemci ikinci bir filtre yazmamalı** — iki arama mantığı zamanla ayrışır.

`teamId` **parametre değil**: oturumun kendi takımlarından türetilir. Yetkisiz
bir takım istenemez çünkü istenecek alan yok. `ADMIN` bütün aktif takımları
görür.

`limit` sunucuda **1–50** arasına kırpılır. `limit=100000` gönderirsen 50
alırsın; istemci bütün tabloyu çekemez.

> **Neden ayrı uç:** §2 tek takım ister, ama bir PO'nun birden çok takımı
> olabiliyor. Giriş sayfasının N istek atmasını istemiyoruz.

**Gruplama istemcide yapılır** — `templateType` zaten özette geliyor. Sunucu
üç ayrı liste döndürseydi "son N kayıt" anlamı bozulurdu.

---

## 3b. `GET /api/mailer/documents/default`

**Bir belge oluşturulsaydı içeriği ne olurdu** — hiçbir şey yazmadan.

```
GET /api/mailer/documents/default?teamId=1&templateType=KAPANIS
```

**Yanıt `200`** — doğrudan `content` nesnesi (§3'teki belgenin `content`
alanının aynısı, sarmalayıcı yok).

Arayüz, uygulama açılır açılmaz sağdaki mail şablonunu **belge oluşturmadan**
çizebilsin diye var. Alternatifleri şunlardı, ikisi de daha kötü:

| Alternatif | Neden değil |
|---|---|
| Açılışta `POST /documents` | Her açılışta bir taslak satırı düşer; terk edilmiş "Yeni mail" belgeleri birikip Belgelerim listesini kullanılmaz hale getirir |
| İskeleti frontend'de kurmak | Varsayılan içeriğin **ikinci** bir tanımı olur (bölüm başlıkları, sütun listeleri, başlık türetmesi) ve zamanla sunucudakinden ayrışır |

**Bu uç `POST /documents`'in üreteceğinin aynısını döndürür** — söz olarak
değil, yapısal olarak: ikisi sunucuda aynı metodu çağırır, ve bir test bunu
her derlemede karşılaştırıyor. Ayrışırlarsa kullanıcı ekranda bir şey görüp
başka bir şey kaydederdi.

**Yan etkisi yoktur.** Ne belge, ne sürüm, ne log satırı yazar.

| Durum | Kod |
|---|---|
| Yetkisiz takım | `403` |
| Olmayan takım (PO) | `403` — yetki kapısı önce kapanır |
| Olmayan takım (ADMIN) | `404` |
| Geçersiz `templateType` | `400` |

> Olmayan takım PO'ya neden `404` değil `403`? Yetki kontrolü takım
> aramasından **önce** çalışıyor. Tersi olsaydı yetkisiz biri, `404` ile
> `403` farkına bakarak hangi takım kimliklerinin var olduğunu çıkarabilirdi.

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

İstemci onu olduğu gibi `iframe srcdoc`'una basar; üzerinde değişiklik
yapmaz, ikinci bir sürüm üretmez (Mimari Kural 1).

İstemci çağrıyı **300 ms geciktirir** ve yeni tuşa basıldığında öncekini iptal
eder.

### Önizleme ile `.eml` arasındaki **iki** fark

Aynı HTML'dir; iki noktada, adlandırılmış biçimde ve çift yönlü kilitli olarak
ayrışır. **Sayı ikidir ve öyle kalmalıdır** — sessizce artarsa iki prototipte
de yaşanan "önizlemede düzelen mailde düzelmiyor" hatasına dönülür.

| # | Fark | Önizleme | `.eml` | Nerede |
|---|---|---|---|---|
| 1 | Görsel referansı | `data:` URI (gömülü) | `cid:` | `OnizlemeGorselleri` |
| 2 | Düzenleme adresi | `data-alan="..."` var | **yok** | `DuzenlemeAdresleri`, export ucunda |

İkisi de **ikinci bir HTML üretimi değil**: düzenin tek kaynağı hâlâ
`MailHtmlRenderer`.

> Eskiden burada "görseller önizlemede görünmez, normaldir" yazıyordu.
> Doğru değildi: tarayıcı `cid:` çözemez, o yüzden **aynı dosyaların** base64
> hâli gömülüyor. Düzen, renk, ölçü değişmiyor.

### Satır içi düzenleme: `data-alan`

Önizlemedeki her düzenlenebilir alan, içerik JSON'undaki karşılığının **nokta
yolunu** taşır. Kullanıcı sağdaki maile tıklayıp yazabilsin diye.

**Neden sunucu basıyor:** hangi hücrenin hangi veriye karşılık geldiğini
yalnızca sunucu bilir. Sütun sırası tipe ve kullanıcının sütun seçimine göre
değişir; istemci hücre sayarak eşleştirseydi ilk sütun değişikliğinde yanlış
alana yazardı.

```
header.title          header.period        header.teamLabel
meeting.date          meeting.time         meeting.place
intro.0
sections.<key>.title
sections.<key>.rows.<index>.<field>
notes.<index>.text
footer.line1          footer.line2
```

Üç kural, üçü de test altında:

**Bölüm `key` ile adreslenir, indeksle değil.** Kullanıcı bölüm ekleyip
silince indeks kayar, anahtar kaymaz.

**Satır indeksi ÇİZİM sırasını değil İÇERİK sırasını taşır.** Kapanış
satırları sektöre göre gruplanır; sektörler `A, B, A` ise çizim sırası
`0, 2, 1` olur ama ekrandaki ikinci satırın adresi `rows.2`'dir.

**`intro` indeksi de içeriktekidir.** Boş paragraflar çizilmez;
`["dolu", "", "dolu"]` içeriğinde çizilen ikinci paragraf `intro.2` der.

Adres, metni saran elemanda durur: metin tek başınaysa `<td>`/`<div>`
üzerinde, başka içerikle karışıyorsa bir `<span>` içinde (notlarda madde
işareti değil yalnızca yazı sarılır).

**Satırın kendisi de adreslenir:**

```html
<tr data-alan="sections.discussed.rows.0">
```

Satır ekleme / silme / sıralama düğmelerini bunun üzerine konumlandırmak
için. İstemci hücrelerden ön ek çıkarmak zorunda kalmasın. Yönetici
Özeti'nin bağlantı kartlarında aynı adres kartın `<td>`'sinde durur.

**Bölümün alanı ayrı bir nitelikle işaretlenir:**

```html
<table data-bolum="discussed">   <!-- bölüm başlığı şeridi -->
<table data-bolum="discussed">   <!-- bölümün tablosu, satır varsa -->
```

`data-alan` bir **içerik yoludur** — `sections.discussed.title` düzenlenecek
metni gösterir. `data-bolum` ise "bu DOM parçası şu bölüme ait" der; başka
soru, başka nitelik.

**Bölüm boş olsa bile başlık çizilir, yani çapa her zaman vardır.** Kullanıcı
hiçbir şey doldurmamışken de her başlığın altında satır ekleyebilsin diye.

> **Sprint Kapanış'ta sıralama tuzağı.** Kapanış satırları sektöre göre
> gruplanır; içerikteki sıra ile ekrandaki sıra aynı değildir.
> `[0]=Elektrik, [1]=Holding, [2]=Elektrik` içeriği ekranda
> `Elektrik: 0, 2` ve `Holding: 1` diye çizilir. Kullanıcı üstteki satırı
> "aşağı" taşırsa ve istemci dizide `0` ile `1`'i takas ederse satır başka
> bir gruba atlar veya hiç kıpırdamaz. Planlama ve Yönetici Özeti'nde
> gruplama yoktur, orada düz takas doğrudur.

**Üretilen sütun adres taşımaz.** Değeri kullanıcıdan değil şablondan gelen
bir sütun düzenlenebilir görünmemeli: içerikte karşılığı boş olduğu için
adres yanlış bir şey iddia ederdi, ve kullanıcı yazsa bile sonraki çizimde
üretilen değer onu ezerdi. Şu an tek örnek Yönetici Özeti'nin `no` sütunu.

**Sabit seçenekli sütun seçeneklerini bildirir:**

```html
<td data-alan="sections.actions.rows.0.status"
    data-secenekler="Bekliyor|Devam Ediyor|Karar Bekliyor|Tamamlandı">
```

Liste **tek yerde** kalsın diye. İstemci ikinci kez yazsaydı, biri diğerine
eklenen bir durumu kaçırır ve kullanıcı mailde geçerli ama listede olmayan
bir değer görürdü. Ayraç dikey çizgi.

> `data-secenekler` de `.eml`'e gitmez. Soyma listesi **adı adı sayılmış**
> bir listedir; yeni bir `data-*` niteliği eklenip listeye yazılmazsa
> sessizce Outlook'a giderdi. Test bunu "geriye **hiç** `data-` kalmaz"
> diye arayarak yakalar.

Yol kaçırılarak yazılır — bölüm anahtarı kullanıcı içeriğidir, tırnak
içerebilir.

---

## 8b. `POST /api/mailer/render/clipboard`

**Editör izi taşımayan mail HTML'i.** Gövde §8 ile aynı.

**Yanıt `200`**, `text/html`.

Önizlemeden **tek farkı** düzenleme niteliklerinin soyulmuş olması.

**İki yerde kullanılıyor:**

| Kullanım | Neden bu uç |
|---|---|
| "Outlook İçin Kopyala" | Kullanıcı Outlook taslağına yapıştıracak; editör nitelikleri oraya taşınmasın |
| "PDF / Yazdır" | Kâğıda giden şey de editör izi taşımamalı |

> **Adı yanıltıcı, farkındayız.** Uç yalnızca pano için yazılmıştı; yazdırma
> sonradan buraya bağlandı çünkü ihtiyaç aynıydı. Adını düzeltmek yerine
> burada anlatmayı seçtik: doğrulanmış iki akışı isim uğruna kırmak,
> yanlış isimden pahalıya gelirdi.

Böylece `data-*` nitelikleri **yalnızca ekranda** kalıyor: `.eml`'de yok,
panoda yok, PDF'te yok.

Görseller `data:` URI ile gömülü gelir — `cid:` pano üzerinden çalışmaz, MIME
kabı yok.

> **Garanti yol `.eml`'dir.** Outlook masaüstünün `data:` URI'yi nasıl ele
> aldığı sürüme göre değişiyor. Bu uç **kolaylık** içindir: açık bir taslağa
> yapıştırmak istendiğinde. Görseller gelmezse çözüm tema görsellerini
> kimlik doğrulamasız bir uçtan yayınlayıp mutlak URL'e geçmektir — henüz
> yapılmadı, çünkü yeni bir açık uç sessizce eklenmez.

---

## 8c. `POST /api/mailer/render/pdf`

**"PDF İndir"** — dosya doğrudan iner, **yazdırma penceresi açılmaz**.
Gövde §8 ile aynı.

**Yanıt `200`**, `application/pdf`, `Content-Disposition: attachment`.
Dosya adı başlıktan üretilir ve ASCII'ye indirgenir (`.eml`'deki mantık).

Kayıtlı sürümden değil **ekrandaki içerikten** üretilir — pano ve önizleme
de öyle çalışıyor; kullanıcı PDF'i genelde kaydetmeden alıyor.

> **Neden sunucuda üretiliyor:** istemcide (jsPDF/html2canvas) üretmek maili
> ekran görüntüsü gibi **rasterize eder** — yazılar bulanıklaşır, tablolar
> bozulur, dosya şişer. Daha önemlisi mailin **ikinci bir çizimi** olurdu ve
> zamanla asıl maille ayrışırdı (Mimari Kural 1). Burada kaynak yine
> `MailHtmlRenderer`'ın ürettiği tek HTML.

### Yazı tipi neden gömülüyor

PDF'in yerleşik (base-14) fontları **Türkçe'ye özgü harfleri taşımıyor** —
`ş ğ ı İ` yerine boş kutu çıkar. Arial'in lisansı gömmeye izin vermiyor.

Droid Sans (Apache 2.0) gömülüyor ve `Arial`, `Segoe UI`, `Helvetica`,
`sans-serif` **adlarıyla** kaydediliyor; böylece şablon HTML'ine hiç
dokunulmadan doğru yazı tipi kullanılıyor. Ayrıntı:
`src/main/resources/fonts/LISANS.txt`.

Görseller `cid:` ile geliyor ve PDF üreticisi onları dosya baytlarına
çeviriyor — `.eml`'de çalışan `cid:` burada bir anlam ifade etmezdi.

> **İlk istek yavaştır:** yazı tipi diske çıkarılıyor ve PDF motoru ısınıyor.
> Sonraki istekler hızlı. Arayüz bir "Hazırlanıyor…" durumu göstermeli.

---

## 9. `GET /api/mailer/documents/{id}/export.eml`

"Outlook Maili İndir" butonu.

**Yanıt `200`**

```
Content-Type: message/rfc822
Content-Disposition: attachment; filename="Agustos_2026_Sprint_Kapanisi.eml"; filename*=UTF-8''Agustos_2026_Sprint_Kapanisi.eml
```

**Dosya adını istemci `Content-Disposition`'dan okur, `title`'dan değil.**

Sunucu adı Türkçe karakterlerden ve boşluklardan arındırıp ASCII'ye indirger:
bazı istemciler UTF-8 dosya adını yanlış çözüp adı bozuyor. İstemci `title`'ı
kullanıp adı kendisi kurarsa bu indirgeme boşa çıkar ve dosya kimi Windows
kurulumlarında bozuk adla iner.

Başlık iki biçimi birden taşır (`filename` ve `filename*`); istemci ikisinden
birini okuyabilir. Başlık okunamıyorsa — farklı origin'de tarayıcı gizler —
istemci `title`'a düşebilir, ama aynı origin'de çalıştığımız için normalde
gizlenmez.

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

`format`: `EML` | `PDF` | `KOPYALA`

`KOPYALA` = "Outlook İçin Kopyala" (§8b). Panoya almak da bir dağıtım yolu;
ölçümde diğer ikisiyle aynı yerde durmalı, yoksa "kaç mail üretildi"
sorusunun cevabı eksik çıkar.

> Değer listesi `DownloadFormat` enum'u ile `mailer_download_logs`'un `CHECK`
> kısıtında **iki yerde** yazılı. Enum'a değer eklenip migrasyon yazılmazsa
> uygulama derlenir ama kayıt atarken kısıt ihlaline düşer — ve bunu ancak
> kullanıcı indirmeye çalıştığında görürüz. `KOPYALA` için `V5`.

**Yanıt `204`** — gövde yok.

---

## `content` şeması (v1)

Tam açıklama: [`BRIEF.md` §6](BRIEF.md). Burada bağlayıcı olan kısım:

```json
{
  "schemaVersion": 1,
  "header":  { "title": "…", "period": "…", "teamLabel": "…" },
  "meeting": {
    "date": "03.09.2026", "time": "10:00", "place": "…",
    "title": "…", "moderator": "…", "attendees": "…"
  },
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

`tone` değeri `blue` | `green` | `orange`. Bu bir **renk adı değil, rol adı** —
gerçek renk temadan gelir. İstemci asla renk kodu göndermez.

| Ton | Rol | Takıma göre değişir mi |
|---|---|---|
| `blue` | nötr / varsayılan | **Evet** — takımın kurumsal rengi buranın yerini alır |
| `green` | geliştirme / tamamlandı | Hayır |
| `orange` | bekliyor / dikkat | Hayır |

Yeşil ve turuncu rol rengidir: "bekleyen iş" her takımda aynı şey demek.
Sekiz temanın hepsi üç tonu da taşır, yani her bölüm her tonda çizilebilir.

### Satır alanları

Tek sözlük; hangi alanın kullanılacağını bölümün `columns` dizisi söyler.

**Sprint Kapanış**

| Alan | Başlık | Not |
|---|---|---|
| `sector` | Sektör | Görünür sütunsa tablolar buna göre **gruplanır** ve sütun olarak ayrıca çizilmez |
| `jira` | JIRA | Listedeki ilk sütun kalın ve renkli çizilir |
| `ci` | CI | |
| `process` | Süreç | |
| `stage` | Aşama | |
| `stake` | Paydaşlar | |
| `note` | Kritik Not | |
| `gain` | Kazanç (Saat/Yıl) | Varsayılan **kapalı** |

**Sprint Planlama**

| Alan | Başlık | Not |
|---|---|---|
| `topicType` | Konu Türü | `Hikaye` \| `Görev` \| `Bug` \| `İyileştirme` \| `Diğer` |
| `jira` | Konu Anahtarı | Kapanış'takiyle aynı alan |
| `summary` | Özet | |
| `status` | Durum | Sayaç şeridi buna göre dağılım gösterir (en fazla üç durum) |
| `expected` | Beklenen Konular | |
| `stake` | Paydaşlar | |
| `sprint` | Sprint | Varsayılan **kapalı** |
| `department` | Departman | Varsayılan **kapalı** |

Planlamada tablolar **gruplanmaz** — konular kullanıcının girdiği sırada tek
listede durur.

**Yönetici Özeti**

Diğer iki tipten farkı: bölümler serbest değil, **dördü de sabit**. Yapı tipe
değil bölüme bağlı olduğu için sütun seçimi bu tipte kapalıdır — `url`'i
görüşülen konular tablosuna eklemek anlamsız olurdu.

| `key` | Başlık | Ton | Sütunlar |
|---|---|---|---|
| `discussed` | GÖRÜŞÜLEN KONULAR | `blue` | `team`, `topic`, `detail` |
| `decisions` | ALINAN KARARLAR | `green` | `no`, `decision`, `team` |
| `actions` | BEKLEYEN KONULAR VE AKSİYONLAR | `orange` | `team`, `pending`, `owner`, `due`, `status` |
| `links` | İNCELEME VE ERİŞİM BAĞLANTILARI | `blue` | `linkType`, `title`, `description`, `button`, `url` |

Üç davranış istemciyi ilgilendiriyor:

**`no` içeriğe yazılmaz.** Boş bırakılır, çizerken `K-01`, `K-02` üretilir.
Kullanıcı ortadaki kararı silince kalanlar kendiliğinden yeniden numaralanır —
kaydedilseydi `K-01`, `K-03` diye boşluklu giderdi. Arayüz bu alanı
göstermez.

**`links` tablo değil kart.** Kartlar tek satırda yan yana durur; Outlook
alta kaydırmaz, o yüzden bağlantı sayısı **dörtle sınırlıdır**. Buton yalnızca
`url` doluysa çizilir — tıklanıp hiçbir şey olmayan buton maildeki en can
sıkıcı şeydir.

**Sayaçları sunucu hesaplar**, istemci gönderemez:

| Sayaç | Nasıl |
|---|---|
| YER ALAN EKİP | `discussed` içindeki tekrarsız `team` |
| GÖRÜŞÜLEN KONU | `discussed` satır sayısı |
| ALINAN KARAR | `decisions` satır sayısı |
| BEKLEYEN KONU | `actions` içinde `status != "Tamamlandı"` (boş durum da bekliyor sayılır) |

Arayüz bu tipte kendi sayaç şeridini **göstermemeli**: genel şerit toplam
satırı sayar, mail tamamlananı düşer. İkisi de doğrudur ama farklı şeyi sayar
ve kullanıcı bunu hata sanar.

`status` bu tipte dört sabit değer alır: `Bekliyor`, `Devam Ediyor`,
`Karar Bekliyor`, `Tamamlandı`. **Sprint Planlama'da aynı alan serbest
metindir** — orada ortak liste dayatmak veri kaybettirirdi.

**Toplantı Notları**

Sprint dışı toplantılar için. Yönetici Özeti'yle örtüşüyor ama **ayrı
duruyor**: Yönetici Özeti bir takımın **sprint'ine** bağlı, bu ise
**herhangi bir toplantıya** ve takımlar üstü. Birleştirilseydi her iki
durumda da yarısı boş kalan bir form çıkardı.

| `key` | Başlık | Ton | Sütunlar |
|---|---|---|---|
| `discussed` | 1. KONUŞULAN VE DEĞERLENDİRİLEN KONULAR | `blue` | `team`, `topic`, `detail` |
| `decisions` | 2. TOPLANTIDA ALINAN RESMİ KARARLAR | `green` | `no`, `decision`, `scope`, `team` |
| `actions` | 3. BEKLENEN AKSİYONLAR VE SORUMLULAR | `orange` | `team`, `pending`, `owner`, `due`, `status` |

Bölüm başlıkları **numaralı** ve numarayı **içerik taşır** (şablon üretmez) —
kullanıcı başlığı değiştirebilsin diye. Bu mail bir tutanak gibi okunuyor.

Yönetici Özeti'nden **üç farkı**:

**1. `scope` sütunu** (`KAPSAM / ALAN`). Resmi bir karar kaydında "hangi
alanda karar alındı" ayrı bir bilgi — Mimari, Kapsam, Tasarım…

**2. Toplantı kutusu.** `meeting` üç yeni alan taşır:

| Alan | Ne |
|---|---|
| `meeting.title` | Toplantının adı |
| `meeting.moderator` | Moderatör / not alan |
| `meeting.attendees` | Katılımcı ekipler / paydaşlar |

Üçü de düzenleme adresi taşır. **Hiçbiri dolu değilse kutu çizilmez** — boş
etiketlerle dolu bir kutu mailin en üstünde "burası eksik" derdi, ve yeni
belge tam da bu hâlde doğar. Kullanıcı bunları sol panelden doldurur.

> **Şema sürümü değişmedi.** Gövde `jsonb`; eski kayıtlarda bu alanlar yok
> ve `null` okunuyor. Diğer üç tip onları taşıyabilir ama göstermez.

**3. Bağlantı kartları yok.** Bu mail bir tutanak; erişim bağlantıları
Yönetici Özeti'ne ait.

Sayaçlar: `GÖRÜŞÜLEN KONU`, `ALINAN KARAR`, `AÇIK AKSİYON`, `İLGİLİ EKİP`.

> **`İLGİLİ EKİP` üç bölümü de tarar**, yalnızca görüşülen konuları değil:
> bir ekip hiç konu açmadan karar veya aksiyon almış olabilir, o da
> toplantıya dahildir.

Başlık türetmesinde **takım adı kullanılmaz**: `TOPLANTI NOTLARI`. Tip takımlar üstü — sprint dışı bir toplantının çıktısı bir
takımın adıyla başlamaz. Belge yine bir takıma aittir (yetki için).

> **Neden bazı alanlar varsayılan kapalı:** mail gövdesi 760 piksel sabit
> (Outlook yüzde genişlikli iç içe kutuyu bozar). Altı sütundan fazlası
> okunmaz hale geliyor.

`columns` hangi alanın **hangi sırayla** çizileceğini söyler. Satırda olup
`columns`'ta olmayan alan çizilmez ama **silinmez** — sütunu kapatıp açmak
veri kaybettirmez.

---

## Kapsam dışı — sorulursa cevap "şimdilik hayır"

Excel/CSV içe aktarma · Jira'dan veri çekme · şablon yönetim paneli ·
manuel sayaç butonu.

İçerik şeması dördüne de uygun; yayından sonra konuşulur.
