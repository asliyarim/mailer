# Aksa Mailer — Teknik Brief

> **Bu belge nedir:** Aksa Mailer projesine yeni katılan geliştirici için bağlam belgesi.
> 
> Repo henüz yeni; burada anlatılan yapının çoğu **kurulacak**, hazır değil.
>
> **Ekip:** Aslı  · Efe · Emren
> **Tarih:** 27.08.2026

---

## 1. Bağlam — Odyssey nedir

Odyssey, Aksa'nın iç uygulamalarını tek çatı altında toplayan **kabuk (shell)** uygulaması.
Kendisi bir uygulama değil; bir katalog sayfası + oturum kapısı + iframe çerçevesi.

Şu an Odyssey'de kayıtlı uygulamalar:

| Uygulama | Ne yapar |
|---|---|
| Proje Sunum Editörü | Excel portföyünden yönetim sunumu üretir |
| PO Sprint Sunumu Hazırlayıcı | Sprint sunumu + ekip kapasite panosu (kod adı: Capacity Planner) |
| Aksa Board | Pano uygulaması |
| İzin Takvimi | İzin takibi |
| RetroInsight | Retrospektif aracı |
| **Aksa Mailer** | **Bizim yazacağımız — henüz yok** |

### Kabuğun dosya yapısı

Odyssey ayrı bir klasörde duruyor (`Landing Page/`), vanilla JS — framework yok:

```
Landing Page/
├─ index.html
├─ projects.js     ← uygulama kataloğu. Yeni uygulama buraya eklenir.
├─ shell.js        ← rotalama + iframe yönetimi
├─ auth.js         ← oturum istemcisi
├─ nginx.conf      ← prod proxy yapılandırması
└─ dev-server.js   ← yerel proxy
```

### Rotalama

Kabuk `/uygulama/<slug>` rotasını kullanır ve `history.pushState` ile geçiş yapar
(tarayıcının geri tuşu merkeze döner). Seçilen uygulama **iframe içinde** açılır.
Aksa Mailer'ın slug'ı `mailer` olacak → `/uygulama/mailer`.

### Kritik tasarım kararı: aynı origin

Uygulamalar **ayrı alan adı değil**, kabuğun kendi origin'i üzerinden yol önekiyle
servis ediliyor (`/kapasite/`, `/mailer/`). Sebebi şu: oturum çerezi Odyssey'in
origin'ine yazılıyor. Uygulama ayrı bir `*.up.railway.app` alt alan adında olsaydı,
tarayıcı onu iframe içinde üçüncü taraf sayıp çerezi göndermez ve oturum düşerdi.

**Bunu unutma:** frontend derlenirken API adresi **göreli** olmalı
(`VITE_API_BASE_URL=""`), mutlak URL değil.

### İstek akışı

```
Tarayıcı
   │
   └─→ odyssey-shell (nginx :80)
         ├─ /api/auth/*     → odyssey-auth:8081        (oturum)
         ├─ /api/mailer/*   → mailer-backend:8082      ← BİZ
         ├─ /api/*          → capacity-planner:8080    (mevcut)
         ├─ /mailer/*       → mailer-frontend:80       ← BİZ
         ├─ /kapasite/*     → capacity-frontend:80     (mevcut)
         └─ /*              → kabuğun kendi statik sayfası
```

nginx **en uzun öneki** seçer. Yani `/api/mailer/` bloğu, sırası neresi olursa olsun
mevcut `/api/` bloğunu yener — Capacity Planner'ı bozma riskimiz yok.

### Kimlik doğrulama

`odyssey-auth` ayrı bir servis. Giriş yapınca JWT'yi **çereze** yazar:

| Çerez | İşi |
|---|---|
| `access_token` | Kısa ömürlü erişim token'ı |
| `refresh_token` | Süresi dolunca yenileme |
| `XSRF-TOKEN` | CSRF koruması; istek `X-CSRF-Token` başlığıyla eşleşmeli |

Uç noktalar: `/api/auth/login`, `/api/auth/me`, `/api/auth/refresh`, `/api/auth/logout`.

Her uygulama token'ı **kendisi doğrular** — auth servisine sormaz. Aynı
`APP_JWT_SECRET` ile imzayı kontrol eden bir servlet filtresi var. Token'ın içinde
`sicil` (personel no) ve `role` (`ADMIN` | `PO`) var.

Aksa Mailer bu filtreyi aynen kuracak. Yani `/api/mailer/**` altındaki her istek
çerezle gelir; çerezsiz istek **401** döner.

---

## 2. Aksa Mailer ne yapacak

Bugün her takım, sprint sonunda yöneticilerine elle HTML mail hazırlıyor.
Aksa Mailer bunu araca çeviriyor.

**Ekran iki panelli:**
- **Sol:** form. Numaralı kartlar — mail tipi, takım ayarları, satır kartları, taslaklar.
- **Sağ:** canlı mail önizlemesi (iframe).

**Üç mail tipi:**
1. **Sprint Kapanış** — analiz + geliştirme çalışmaları, sektöre göre gruplu tablolar
2. **Sprint Planlama** — konu anahtarı, özet, durum, beklenen konular
3. **Yönetici Özeti** — sayaçlar + sektör özeti + dikkat gerektiren konular (tasarımı yeni)

**İki buton:**
- `Outlook Maili İndir` → `.eml` dosyası, Outlook çift tıkla açar
- `PDF / Yazdır`

### Kapsam DIŞI (sorulursa cevap: şimdilik hayır)
- Excel / CSV içe aktarma
- Jira'dan veri çekme
- Şablon yönetim paneli (temalar **kodda** tanımlı, yeni takım = bir PR)
- Manuel sayaç butonu (sayaçlar satır sayısından otomatik)

---

## 3. Teknoloji yığını

Capacity Planner ile aynı — kasten. Ekip zaten biliyor, öğrenme maliyeti sıfır.

| Katman | Teknoloji |
|---|---|
| Backend | Java 17, Spring Boot 4.1, hexagonal mimari |
| Güvenlik | Spring Security + JJWT (çerezden JWT) |
| Veritabanı | PostgreSQL, şema yönetimi Flyway |
| ORM | Spring Data JPA, JSON alanlar için `@JdbcTypeCode(SqlTypes.JSON)` → jsonb |
| Frontend | React 19, Vite 8, react-router 7 — **TypeScript yok, düz JSX** |
| Paketleme | Docker, docker-compose |

---

## 4. Mimari kurallar — pazarlığa kapalı

Bu dört kural projenin omurgası. Değiştirmek istersen önce Aslı'ya sor.

### Kural 1 — Mail HTML'i tek bir yerde, sunucuda üretilir

Önizleme, o HTML'in iframe'de gösterilmiş hâlidir. **Asla ikinci bir HTML üretimi yazma.**

```
İçerik JSON (kullanıcı doldurur)  ─┐
                                   ├─→ MailHtmlRenderer ─→ HTML ─┬─→ iframe önizleme
Tema (kodda tanımlı)              ─┘                             └─→ EmlBuilder → .eml
```

**Neden:** ilk prototipte mail iki kez yazılmıştı — ekranda flexbox'lı bir sürüm,
indirmede tablo tabanlı başka bir sürüm. Önizlemede düzeltilen hata mailde
düzelmiyordu. Bu tekrar olmayacak.

### Kural 2 — O tek yer sadece Outlook'un anladığı CSS'i üretir

Kural 1 tek başına yetmiyor. İkinci prototip tek renderer kullanıyordu ama
`linear-gradient`, `background-image`, `border-radius` üretiyordu — Chrome'daki
önizlemede çalışıyor, Outlook masaüstünde hiçbiri çalışmıyor. Önizleme yine yalan
söylüyordu.

Outlook masaüstü HTML'i **Word'ün motoruyla** çizer. İzin verilenler:

| Serbest | Yasak |
|---|---|
| `<table>` ile düzen (`role="presentation"`) | flexbox, grid, `position` |
| Satır içi `style` nitelikleri | `<style>` bloğu, harici CSS |
| Sabit piksel genişlik (gövde 760px) | `border-radius`, gölge, `linear-gradient` |
| `<col width>` ile sütun oranları | `background-image` |
| Düz arkaplan rengi, `border`, `padding` | `max-width`, yüzde genişlikli iç içe kutular |
| `cid:` gömülü görsel + açık `width`/`height` | `data:` URI görsel |
| Arial / Segoe UI | Web fontu |

Ayrıca: mail HTML'inde `<head>` ve `<meta charset="UTF-8">` **mutlaka olsun**
(Türkçe karakterler bozulmasın), ve çok satırlı metinlerin satır sonlarını koru.

### Kural 3 — Görseller `cid:` ile mailin içine gömülür

Her tema **beş görsel** taşıyor: hero maskotu (315×235), giriş illüstrasyonu (120×120),
alt not illüstrasyonu (225×151), footer logosu (285px), footer maskotu (108×99).

`data:` URI Outlook'ta çalışmaz. Uzak URL, Outlook'un "görselleri indir" uyarısına
takılır — kullanıcı tıklayana kadar mail bomboş görünür. **Tek güvenilir yol
`multipart/related` + `cid:`**, bu da sunucu tarafı üretim demek.

Görseller repoda `src/main/resources/themes/<tema>/` altında dosya olarak durur.
Toplam mail boyutu **300 KB altında** kalmalı.

### Kural 4 — İçerik veritabanında, tema kodda

- **İçerik** = kullanıcının doldurduğu her şey → `mailer_documents.content` (jsonb)
- **Tema** = renk, logo, maskot, blok sırası → Java sınıfı, repoda

Bu sınır bulanırsa şablon yönetim paneli yazmak zorunda kalırız. Kapsam dışı.

---

## 5. Proje yapısı

```
aksa-mailer/
├─ pom.xml · Dockerfile · docker-compose.yml
├─ docs/api.md            ← API sözleşmesi. DONDURULMUŞ, izinsiz değiştirilmez.
├─ src/main/java/com/aksa/mailer/
│  ├─ MailerApplication.java
│  ├─ auth/                odyssey-auth JWT çerezini doğrular
│  │  └─ security/{JwtCookieAuthFilter, JwtTokenProvider, SecurityConfig}.java
│  ├─ common/{config, web, domain}
│  ├─ team/                mail_teams — okuma ağırlıklı, ince modül
│  ├─ document/            taslak, kaydet, versiyon
│  │  ├─ api/          MailerDocumentController + dto/
│  │  ├─ domain/       MailerDocument, MailContent, MailSection
│  │  ├─ port/in/ · port/out/
│  │  ├─ usecase/      MailerDocumentService
│  │  └─ adapter/out/persistence/
│  └─ render/              PROJENİN KALBİ — Aslı'nın sorumluluğunda
│     ├─ api/          RenderController
│     ├─ domain/       MailTheme, ThemeRegistry
│     ├─ theme/        RpaTheme.java, IsZekasiTheme.java
│     ├─ template/     KapanisTemplate, PlanlamaTemplate, YoneticiOzetiTemplate
│     └─ usecase/      MailHtmlRenderer, EmlBuilder, CidImageResolver
├─ src/main/resources/
│  ├─ db/migration/V1__init.sql
│  └─ themes/rpa/      hero.png · intro.png · notes.png · logo.png · mascot.png
├─ src/test/
│  ├─ java/…/render/MailHtmlRendererTest.java   ← anlık görüntü testi
│  └─ resources/render/rpa-kapanis-beklenen.html
└─ frontend/
   └─ src/
      ├─ App.jsx        /  ·  /belgeler  ·  /editor/new  ·  /editor/:id
      ├─ lib/           apiClient.js · mailContent.js (boş şema, doğrulama)
      ├─ components/editor/    EditorPage · MetaForm · RowCard · SectionList
      │                        NotesForm · PreviewPane · TopActions
      └─ components/shared/    TopBar · DocumentListPage · Button · Modal
```

### Hexagonal mimari — kısa açıklama

Capacity Planner'daki desen. Her modül (örn. `document/`) şu katmanlardan oluşur:

- `domain/` — saf iş nesneleri, hiçbir framework bağımlılığı yok
- `port/in/` — dışarıdan çağrılabilecek işlemlerin arayüzü (use case)
- `port/out/` — modülün dışarıya ihtiyaç duyduğu şeylerin arayüzü (repository)
- `usecase/` — `port/in`'in gerçeklemesi, iş mantığı burada
- `adapter/out/persistence/` — `port/out`'un JPA gerçeklemesi
- `api/` — HTTP controller + DTO'lar

Kural: `domain` ve `usecase` hiçbir zaman JPA veya HTTP sınıfı import etmez.
Yeni bir şey yazarken `document/` klasörünü örnek al.

---

## 6. Veri modeli

```sql
mail_teams
  id, code, name, theme_key, active
  -- theme_key kodda kayıtlı bir temayı işaret eder: 'rpa', 'is-zekasi'

mailer_documents
  id, team_id, template_type, title, subject, content jsonb,
  status, current_version, created_by, updated_by, created_at, updated_at
  -- template_type: KAPANIS | PLANLAMA | YONETICI_OZETI

mailer_document_versions
  id, document_id, version, content jsonb, created_by, created_at

mailer_download_logs
  id, document_id, team_id, format, user_email, created_at
```

### `content` jsonb şeması (v1)

```json
{
  "schemaVersion": 1,
  "header":  { "title": "DİJİTAL UYGULAMALAR SPRINT BİLGİLENDİRME",
               "period": "Ağustos 2026 Sprint Kapanışı",
               "teamLabel": "RPA Takımı" },
  "meeting": { "date": "03.09.2026", "time": "10:00", "place": "Toplantı Salonu" },
  "intro":   ["ilk paragraf", "orta paragraf", "kapanış paragrafı"],
  "sections": [
    { "key": "analysis", "title": "ANALİZ ÇALIŞMALARI", "tone": "blue",
      "columns": ["sector","jira","ci","process","stage","stake","note"],
      "rows": [{ "sector": "Elektrik", "jira": "RPA-2066", "ci": "CI9353",
                 "process": "Şebeke Operasyonları Aylık Hakediş Faturaları",
                 "stage": "Analiz", "stake": "Tuba Kaya İşler",
                 "note": "Analiz tamamlanarak geliştirme kapsamı netleştirilecek" }] }
  ],
  "notes":  [{ "tone": "blue", "text": "…" }, { "tone": "green", "text": "…" }],
  "footer": { "line1": "Teşekkür ederiz.", "line2": "Başarılar dileriz!" }
}
```

İki tasarım notu:

1. **Satırlar dizi değil, nesne.** Prototipte `data[0]`, `data[1]` diye indeksle
   geziliyordu; bir sütun eklendiğinde her şey kayıyordu. Anahtar kullan.
2. **Sayaçlar saklanmaz**, satır sayısından hesaplanır.
3. **`sections` bir dizi**, sabit "analiz + geliştirme" ikilisi değil. Başka bir
   takımın üç bölümü olabilir; şema bunu kod değişikliği olmadan taşır.

---

## 7. API yüzeyi

`docs/api.md` içinde tam hâli var ve **dondurulmuş**. Değiştirmek için PR ve Aslı'nın
onayı gerekir. Buna güvenerek çalış — "bu veriyi nereden alacağım" diye sormana gerek yok.

| Endpoint | Ne yapar |
|---|---|
| `GET /api/mailer/teams` | Kullanıcının erişebildiği takımlar + tema anahtarları |
| `GET /api/mailer/documents?teamId=` | Takımın mail listesi (özet) |
| `POST /api/mailer/documents` | Yeni taslak — tip ve temanın varsayılan içeriğiyle doğar |
| `GET /api/mailer/documents/{id}` | İçerik + tip + tema anahtarı |
| `PUT /api/mailer/documents/{id}` | Kaydet — her kayıt yeni versiyon satırı yazar |
| `GET /api/mailer/documents/{id}/versions` | Versiyon geçmişi |
| `POST /api/mailer/documents/{id}/versions/{v}/rollback` | Geri al |
| `POST /api/mailer/render/preview` | Gövdedeki JSON'u `text/html` döner |
| `GET /api/mailer/documents/{id}/export.eml` | `message/rfc822` — Outlook Maili İndir |
| `POST /api/mailer/documents/{id}/downloads` | İndirme logu |

**Neden önizleme `POST /render/preview`?** Kullanıcı yazarken her tuşta kaydetmek
istemiyoruz, ama yazdığını da görmesi gerekiyor. İstemci geçerli JSON'u gönderir,
dönen HTML'i iframe'e basar (300 ms geciktirilmiş).

---

## 8. Odyssey'e kayıt — üç dosya

Bunu Aslı yapacak, ama ne olduğunu bilmen faydalı:

1. **`Landing Page/projects.js`** — `ADRESLER`'e `"aksa-mailer": { prod: "/mailer/", dev: "/mailer/" }`,
   `PROJECTS`'e kart (`slug: "mailer"`, `status: "dev"`, inline SVG önizleme).
2. **`Landing Page/nginx.conf`** + **`local/odyssey-shell.nginx.conf`** —
   `location /mailer/` ve `location /api/mailer/` blokları.
3. **`docker-compose.odyssey.yml`** — `mailer-backend` + `mailer-frontend` servisleri.
   `APP_JWT_SECRET` odyssey-auth ile **aynı** olmalı; `VITE_API_BASE_URL: ""` (göreli).

Frontend derleme değişkenleri: `VITE_BASE_PATH=/mailer/`, `VITE_API_BASE_URL=""`.

---

## 9. Yerel çalıştırma

Odyssey kabuğu ve auth servisi ayrı klasörlerde (`C:\Users\40538\Downloads\`).
Uygulama tek başına açılmaz — kabukla birlikte ayağa kalkması gerekir:

```bash
docker compose -f docker-compose.yml -f docker-compose.odyssey.yml up -d
```

Giriş adresi: `http://localhost:4173` → Aksa Mailer kartına tıkla.

---

## 10. Çalışma kuralları

- **Branch:** `main`'e doğrudan push yok. Kendi dalında çalış, PR aç.
- **PR:** Aslı onaylamadan birleşmez. Küçük PR'lar aç — 500 satırlık PR incelenemez.
- **Dokunma:** `render/` paketi, `V*__*.sql` migrasyonları, `auth/`, `docs/api.md`.
  Bunlarda değişiklik gerekiyorsa önce Aslı'ya söyle.
- **Kod incelemesinde standart soru:** "İkinci bir HTML üretimi eklendi mi?"
- **Türkçe karakter:** her yerde UTF-8. Test verine `şğıİçöü` koy.

---

## 11. Bu belgeyi Claude'a verdikten sonra

İyi soru örnekleri:

- "Bu yapıda `document/` modülünün `port/out` arayüzünü ve JPA adapter'ını yaz."
- "React'te sol paneldeki satır kartı bileşenini yaz — şu alanlar olacak: … .
  Durum yukarıda tutulacak, bileşen kontrollü olsun."
- "Şu JSON şemasından boş bir `content` nesnesi üreten yardımcı fonksiyonu yaz."
- "Şu HTML tablosu Outlook masaüstünde bozulur mu? Yukarıdaki yasak listesine göre kontrol et."

Dikkat: Claude'a "mail HTML'ini frontend'de üret" dedirtme — Kural 1'e aykırı.

