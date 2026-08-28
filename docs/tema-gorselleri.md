# Tema görselleri

Her tema beş görsel taşır ve hepsi `cid:` ile mailin içine gömülür.
`data:` URI Outlook'ta çalışmaz; uzak URL "görselleri indir" uyarısına takılır
ve kullanıcı tıklayana kadar mail bomboş görünür.

Dosyalar: `src/main/resources/themes/<tema>/`

| Dosya | Ölçü | Nerede |
|---|---|---|
| `hero.png` | 315 × 235 | Üst şerit, takım maskotu |
| `intro.png` | 120 × 120 | Giriş kutusu illüstrasyonu |
| `notes.png` | 225 × 151 | Alt not kutusu illüstrasyonu |
| `logo.png` | 285 × 42 | Footer — Aksa \| Kazancı Holding |
| `mascot.png` | 108 × 99 | Footer maskotu |

**Dosyadaki ölçü, mailde çizilen ölçüyle aynı olmalı.** Büyük dosyayı HTML'de
küçültmek boyutu boşa şişirir; Outlook ayrıca ölçüsü verilmemiş görseli doğal
boyutunda çizip düzeni bozar.

---

## Mail boyutu

```
ham toplam       85 KB
üretilen .eml   124 KB     ← ölçüldü
sınır           300 KB
```

`EmlBuilderTest` bu sınırı test ediyor. Kırılırsa önce tema klasörüne bakın —
büyük bir PNG eklenmiş olması en olası sebep.

### Nasıl bu boyuta indi

Görseller v2 prototipinden çıkarıldığında **265 KB** ham, **380 KB** mail
ediyordu — sınırın 80 KB üzerinde. İki adımda çözüldü:

1. **Gösterildikleri ölçüye indirildiler.** `hero.png` dosyada 381 × 286 iken
   mailde 315 × 235 çiziliyordu; aradaki fark boşa taşınan veriydi.
2. **8-bit palete çevrildiler.** İllüstrasyonlarda renk sayısı az, bu yüzden
   PNG-8 gözle fark edilmeden ciddi kazanç veriyor.

| Dosya | Önce | Sonra | Kazanç |
|---|---|---|---|
| `hero.png` | 144 KB | 39 KB | %73 |
| `notes.png` | 57 KB | 20 KB | %65 |
| `intro.png` | 27 KB | 10 KB | %63 |
| `logo.png` | 22 KB | 9 KB | %58 |
| `mascot.png` | 15 KB | 7 KB | %53 |

Kayıp ölçüldü: ortalama piksel farkı **255'te 1** (%0.4), gözle fark
edilebilecek sapma taşıyan piksel oranı **%1'in altında**.

### Yeni görsel eklerken

`sharp` ile (yerel geliştirme aracı, projeye bağımlılık değil):

```js
await sharp(kaynak)
  .resize(genislik, yukseklik, { fit: 'fill', kernel: 'lanczos3' })
  .png({ palette: true, quality: 90, effort: 10, compressionLevel: 9 })
  .toFile(hedef);
```

> **Windows'ta `convert` komutunu kullanmayın.** `C:\Windows\System32\convert.exe`
> ImageMagick değil, dosya sistemi dönüştürücüsüdür.
>
> PowerShell'in `System.Drawing`'i de iş görmez: ölçüyü küçültür ama 32-bit
> RGBA yazdığı için dosyalar **büyür** (denendi, hero 144 KB → 156 KB).

---

## Yeni tema eklemek

1. `src/main/resources/themes/<anahtar>/` altına beş görseli koy.
2. `render/theme/` içine bir sınıf yaz — `RpaTheme` örnek.
3. `ThemeRegistry`'ye kaydet.
4. `mail_teams.theme_key` bu anahtarı göstersin.

Başka hiçbir yere dokunulmaz. Renderer temayı parametre olarak alır; içinde
takıma özel hiçbir varsayım yoktur — `MailHtmlRendererTest` bunu ikinci
temayla test ediyor.

## İş Zekâsı teması

Renkleri tanımlı ama **görselleri henüz yer tutucu** — RPA'nınkiler
kullanılıyor. Kendi maskotu ve illüstrasyonları gelince yalnızca
`IsZekasiTheme` içindeki beş yol değişecek.
