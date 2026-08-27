# Aksa Mailer — Frontend

React 19 · Vite 8 · react-router 7 · **TypeScript yok, düz JSX**.

Mimari bağlam: [`../docs/BRIEF.md`](../docs/BRIEF.md). Bu dosya sadece
"hangi dosya nereye" sorusunu cevaplar.

---

## Yeni dosyayı nereye açacağım

| Yazacağın şey | Yeri |
|---|---|
| Backend'e giden yeni bir istek | `src/lib/apiClient.js` içine **fonksiyon ekle** — yeni dosya açma |
| `content` şemasıyla ilgili yardımcı (boş nesne, doğrulama, sayaç) | `src/lib/mailContent.js` |
| Editörde bir form parçası (kart, alan grubu) | `src/components/editor/` |
| Birden fazla ekranda kullanılan parça (düğme, modal, üst şerit) | `src/components/shared/` |
| Yeni bir rota / ekran | `src/App.jsx` içine `<Route>` + ekran bileşeni `components/shared/` |
| Genel stil, düzen sınıfı | `src/index.css` |

**Yeni klasör açma.** Yukarıdaki dört klasör (`lib`, `components/editor`,
`components/shared`, kök) yeterli. Bir şey hiçbirine uymuyorsa Aslı'ya sor.

---

## Üç kural

### 1. Burada mail HTML'i üretilmez

Önizlemede gördüğün HTML sunucudan hazır gelir
(`POST /api/mailer/render/preview`) ve `iframe srcdoc`'una olduğu gibi basılır.
İndirilen `.eml` de aynı HTML'i taşır.

Bu dosyalarda **string birleştirme ile HTML kurma, şablon doldurma, "sadece
önizleme için" küçük bir düzeltme yapma.** Mailin görünümü yanlışsa düzeltilecek
yer backend'deki tema/şablon sınıfıdır, burası değil.

Kod incelemesinin standart sorusu: *"İkinci bir HTML üretimi eklendi mi?"*

### 2. Durum yukarıda, bileşen kontrollü

Editörün tek durum sahibi `components/editor/EditorPage.jsx`. Alt bileşenler
kendi içinde içerik tutmaz; `değer` + `onChange` alır. Örnek desen:
[`RowCard.jsx`](src/components/editor/RowCard.jsx) — yeni kartlar bundan
türetilir.

### 3. API adresi göreli

`VITE_API_BASE_URL` üretimde bilerek **boş string**. Mutlak URL yazarsan
tarayıcı isteği cross-site sayar ve oturum çerezi gönderilmez — kullanıcı
sürekli giriş ekranına düşer. `apiClient.js` bunu doğru yapıyor; başka yerde
`fetch` çağırma.

---

## Çalıştırma

```bash
npm install
npm run dev
```

`npm run dev` tek başına yetmez: `/api/auth/*` çağrıları odyssey-auth'a (8081),
`/api/mailer/*` çağrıları backend'e (8082) proxy'lenir — ikisi de ayakta olmalı.
Gerçek ortama en yakın çalıştırma kabukla birlikte:

```bash
docker compose -f docker-compose.yml -f docker-compose.odyssey.yml up -d
```

Sonra `http://localhost:4173` → Aksa Mailer kartı.

---

## Derleme değişkenleri

| Değişken | Değer | Ne işe yarar |
|---|---|---|
| `VITE_BASE_PATH` | `/mailer/` | Vite'in varlık adresi öneki; router `basename`'i de bunu okur |
| `VITE_API_BASE_URL` | `""` | Göreli API adresi — çerezin first-party kalması için |
| `VITE_ODYSSEY_URL` | `/` | Oturum yoksa kullanıcının gönderileceği giriş adresi |

### Yerel build'i Git Bash'ten alma

Git Bash (MSYS), `/` ile başlayan argümanları dosya yolu sanıp Windows yoluna
çevirir. `VITE_BASE_PATH=/mailer/` bir URL öneki, yol değil — ama MSYS bunu
ayırt edemez ve Vite'e şunu verir:

```
C:/Users/<kullanıcı>/AppData/Local/Programs/Git/mailer/
```

Sonuç: `index.html` içindeki varlık adresleri `/Users/.../Git/mailer/assets/...`
olur, sunucuda böyle bir yol yoktur, sayfa **boş beyaz** açılır. Hata mesajı
çıkmaz, sadece 404'lar.

PowerShell'de ve Docker'ın içinde bu çeviri yok. Yerel build alacaksan
PowerShell kullan:

```powershell
$env:VITE_BASE_PATH = "/mailer/"; npm run build
```

Git Bash'te ısrar edersen `MSYS_NO_PATHCONV=1` öneki çeviriyi kapatır.
