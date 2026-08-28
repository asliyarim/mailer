# Yerel kurulum ve test

Aksa Mailer **kendi yığınını** taşır: veritabanı, kimlik servisi ve Odyssey
kabuğu dahil. Capacity Planner'a (Scrum Sprint projesi) **ihtiyaç yok** — o
repo klonlanmasa da çalışır.

## Ön koşullar

- **Docker Desktop** — çalışıyor olmalı
- **Node 22+** — frontend ve test betiği için
- **JDK gerekmez.** Backend Docker içinde derleniyor (`eclipse-temurin:17`).
- Bu iki klasöre erişim (imajlar onlardan derleniyor):
  - `C:\Users\40538\Downloads\odyssey-auth`
  - `C:\Users\40538\Downloads\Landing Page`

  Repo ile **kardeş** konumda olmalılar — compose onlara `../Downloads/…`
  diye bakıyor.

## 1. `.env` hazırla

```bash
cp .env.example .env
```

Doldurulması gereken **tek** değer `APP_JWT_SECRET`. En az 32 karakter,
rastgele bir dize yeterli. Aynı değeri hem `odyssey-auth` hem `mailer-backend`
kullanır: token'ı odyssey-auth üretir, backend aynı sırla doğrular.
Farklı olursa her istek 401 döner ve sebebi logda görünmez.

Geri kalanı varsayılanlarıyla çalışır.

## 2. Kaldır

```bash
docker compose -f docker-compose.yml -f docker-compose.odyssey.yml up -d --build
```

İlk açılış birkaç dakika sürer (üç imaj derleniyor). Sonrası hızlı.

Elle veri hazırlama adımı **yok**:

| Ne | Kim yapıyor |
|---|---|
| `aksa_mailer` veritabanı | Postgres, `POSTGRES_DB` ile |
| `odyssey_auth` veritabanı | `local/init-db/01-odyssey-auth.sql` |
| Tablolar | Flyway, her iki serviste |
| Kullanıcılar | odyssey-auth'un `UserSeeder`'ı — tablo boşken çalışır |
| Takımlar | `V1__init.sql` içindeki tohum |

## 3. Gir

```
http://localhost:4173  →  "Aksa Mailer" kartı  →  Uygulamayı Aç
```

Giriş bilgileri `UserSeeder`'da tanımlı (`odyssey-auth` deposu). Kullanıcı
tablosu bir kez yazıldıktan sonra bir daha ezilmez.

### Diğer adresler

| Adres | Ne |
|---|---|
| `localhost:4173` | Odyssey kabuğu — **normal giriş yolu** |
| `localhost:5174` | Arayüz, kabuk olmadan doğrudan |
| `localhost:8082/swagger-ui/index.html` | Backend uçları, "Try it out" ile denenebilir |
| `localhost:8082` | API kökü — **tarayıcıda 401 döner**, gösterilecek sayfası yok |

Doğrudan erişimde (`5174`, `8082`) de önce kabukta oturum açmış olman gerekir;
çerezler port ayrımı yapmaz.

> Katalogdaki diğer kartlar duruyor. Railway'de yayında olanlar (Aksa Board,
> İzin Takvimi, RetroInsight) açılır; **PO Sprint Sunumu Hazırlayıcı** ise
> `/kapasite/` yoluna bakar ve o servis bu yığında yok — 404 verir. Beklenen
> davranış: Capacity Planner ayrı bir proje, kendi deposundan kaldırılır.

## Capacity Planner'ı da çalıştıracaksan

İki yığın **aynı anda** çalışamaz: ikisi de 4173 (kabuk) ve 8081 (auth)
portlarını ister. Ya birini durdur, ya `.env`'den portları değiştir:

```
ODYSSEY_SHELL_PORT=4174
ODYSSEY_AUTH_PORT=8083
```

Portlar dışında çakışma yok — konteyner adları, ağ ve volume ayrı.

---

## Test

### Birim testleri — veritabanı gerekmez

```bash
./mvnw test
```

15 test, yarım saniye. İş mantığı sahte portlarla çalışıyor: Spring context
yok, veritabanı yok. Hexagonal mimarinin asıl getirisi bu.

`JAVA_HOME` ayarlı değilse IntelliJ'in gömülü JDK'sı iş görür:

```bash
JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2026.1.4/jbr" ./mvnw test
```

### Duman testi — çalışan backend'e karşı

```bash
bash scripts/smoke-test.sh
```

19 kontrol: kimlik doğrulama, CSRF, hata kodları, belge yaşam döngüsü,
sürüm çakışması, geri alma, Türkçe karakterler.

Token'ı `.env`'deki sırla kendisi imzalıyor
([`scripts/mint-jwt.js`](../scripts/mint-jwt.js)), o yüzden giriş yapmış bir
kullanıcıya ihtiyacı yok. Bıraktığı belgeler `[DUMAN TESTİ]` ile başlar.

### Frontend geliştirme

```bash
cd frontend
npm install
npm run dev
```

`localhost:5173` açılır; `/api/*` çağrıları backend'e ve odyssey-auth'a
proxy'lenir (bkz. `vite.config.js`). Oturum için önce kabukta giriş yapılmalı.

> **Portları `.env`'den değiştirdiyseniz dev sunucusuna da söyleyin.**
>
> `vite.config.js` varsayılan olarak `.env.example`'daki portları kullanır
> (auth 8081, backend 8082). `.env`'de `ODYSSEY_AUTH_PORT` veya
> `BACKEND_PORT` değiştirdiyseniz — örneğin Capacity Planner yığınıyla
> çakışmamak için — dev sunucusu hâlâ eski porta gider ve **oturum sürekli
> düşer**. Belirti: `npm run dev`'de giriş yapılamıyor ama `localhost:5174`
> üzerinden her şey çalışıyor.
>
> ```bash
> VITE_AUTH_BASE_URL=http://localhost:8083 npm run dev
> ```

> **Yerel `npm run build` alacaksan PowerShell kullan.** Git Bash,
> `VITE_BASE_PATH=/mailer/` değerini Windows yoluna çevirir ve sayfa boş
> açılır. Ayrıntı: [`frontend/README.md`](../frontend/README.md).

---

## Sorun giderme

| Belirti | Sebep |
|---|---|
| Her istek 401 | `APP_JWT_SECRET` boş, ya da iki serviste farklı |
| `port is already allocated` | Capacity Planner yığını ayakta — birini durdur veya portları değiştir |
| `path ../Downloads/... not found` | Kaynak klasörler repo ile kardeş konumda değil |
| Arayüzde "Oturumunuz bulunamadı" | Kabukta giriş yapılmamış — `localhost:4173` |
| Kabukta giriş çalışmıyor | `docker logs aksa-mailer-odyssey-auth` — tohumlama satırlarına bak |
| Sayfa boş beyaz | Git Bash'ten build alınmış (yukarıdaki uyarı) |
| nginx değişikliği etkisiz | Mount edilen dosya bir şablon; `reload` yetmez, `restart` gerekir |

Loglar:

```bash
docker compose -f docker-compose.yml -f docker-compose.odyssey.yml logs -f mailer-backend
```

## Sıfırdan başlamak

Veritabanını komple silip yeniden kurmak için:

```bash
docker compose -f docker-compose.yml -f docker-compose.odyssey.yml down -v
```

`-v` volume'u da siler; sonraki açılışta tablolar, kullanıcılar ve takımlar
yeniden tohumlanır.
