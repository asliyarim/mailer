# Odyssey'e kayıt

Aksa Mailer kendi Odyssey kabuğunu taşıyor ([`docker-compose.odyssey.yml`](../docker-compose.odyssey.yml)).
Kabuk imajı `Downloads/Landing Page/` klasöründen derleniyor; yönlendirme
[`local/odyssey-shell.nginx.conf`](../local/odyssey-shell.nginx.conf) ile
bizim repomuzdan geliyor.

Yerel kayıt **tamam** — `localhost:4173` → "Aksa Mailer" kartı çalışıyor.
Bu belge neyin nerede olduğunu ve neyin kaldığını anlatır.

---

## 1. Katalog kartı — `Landing Page/projects.js`

Kabuk deposunda, **eklendi**. `ADRESLER` içine:

```js
"aksa-mailer": {
  // Odyssey nginx'i /mailer/ yolunu bizim frontend'e proxy'ler -
  // ayrı bir alan adı KULLANILMAZ (oturum çerezi first-party kalsın).
  prod: "/mailer/",
  dev: "/mailer/",
},
```

`PROJECTS` dizisine `slug: "mailer"`, `status: "dev"`, `accent: "teal"` taşıyan
bir kayıt. Yayına çıkınca `status` → `"live"`.

> **Tuzak:** kabuk statik dosyaları **imajın içinden** servis ediyor.
> `projects.js`'i diskte değiştirmek çalışan konteyneri etkilemez —
> imaj yeniden derlenmeli:
>
> ```bash
> docker compose -f docker-compose.yml -f docker-compose.odyssey.yml up -d --build odyssey-shell
> ```

## 2. Yönlendirme — `local/odyssey-shell.nginx.conf`

**Bizim repomuzda**, hazır. Üç blok taşıyor:

| Yol | Hedef |
|---|---|
| `/api/auth/` | `odyssey-auth:8081` |
| `/api/mailer/` | `mailer-backend:8082` |
| `/mailer/` | `mailer-frontend:80` |

Adresler `set $degisken` üzerinden veriliyor. Sebebi önemli: değişkenle
yazıldığında nginx adresi **istek anında** çözer. Doğrudan
`proxy_pass http://mailer-backend:8082` yazılsaydı nginx **açılışta** çözmeye
çalışır ve konteyner henüz yokken başlamayı reddederdi.

> **Tuzak:** mount edilen dosya bir **şablon**. nginx onu yalnızca konteyner
> **başlarken** `envsubst`'ten geçirir — `nginx -s reload` şablonu yeniden
> işlemez. Değiştirdikten sonra:
>
> ```bash
> docker compose -f docker-compose.yml -f docker-compose.odyssey.yml restart odyssey-shell
> ```

## 3. Üretim yönlendirmesi — `Landing Page/nginx.conf`

**Henüz yapılmadı, Sprint 3'e kadar gerekmiyor.**

Aynı iki blok (`/mailer/` ve `/api/mailer/`), ama üretim sürümü upstream'lere
Railway'in alan adlarıyla gidiyor. Mevcut `/kapasite/` bloğunun desenini
birebir takip et; tek fark hedef adres ve önek.

Railway tarafında Aksa Mailer, `aksa-proje-merkezi` projesine eklenmeli
(kabuğun ve odyssey-auth'un olduğu yer) ve **kendi Postgres'ini** almalı —
`aksa-board`'un yaptığı gibi.

---

## Doğrulama

```bash
docker compose -f docker-compose.yml -f docker-compose.odyssey.yml up -d --build
```

Sonra `http://localhost:4173` → **Aksa Mailer** kartı → tıkla.

Beklenen: kabuk `/uygulama/mailer` rotasına geçer, başlık
"Aksa Mailer · Odyssey" olur, iframe `/mailer/belgeler`'i açar ve üst şeritte
giriş yapmış kullanıcının adı görünür.

Çerezsiz istek 401 dönmeli:

```bash
curl -i http://localhost:4173/api/mailer/teams
```
