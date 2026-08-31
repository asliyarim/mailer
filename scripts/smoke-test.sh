#!/usr/bin/env bash
# Calisan backend'e karsi ucdan uca duman testi.
#
#   ./scripts/smoke-test.sh
#
# Token'i odyssey-auth'tan almiyoruz: .env'deki APP_JWT_SECRET ile KENDIMIZ
# imzaliyoruz. Ayni sir kullanildigi icin backend acisindan gercek bir
# oturumdan farksiz - ve testin calisan bir kullanici hesabina ihtiyaci olmuyor.
#
# SADECE YEREL. Bu betik uretimde calistirilmaz.

set -euo pipefail

TABAN="${TABAN:-http://localhost:8082}"
KOK="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ ! -f "$KOK/.env" ]]; then
  echo "HATA: .env yok. .env.example'i kopyala ve doldur." >&2
  exit 1
fi

SECRET="$(grep '^APP_JWT_SECRET=' "$KOK/.env" | cut -d= -f2-)"
# Kabuk portu CORS kontrolunde lazim; .env tek dogru kaynak.
ODYSSEY_SHELL_PORT="$(grep '^ODYSSEY_SHELL_PORT=' "$KOK/.env" | cut -d= -f2- || true)"
if [[ -z "$SECRET" ]]; then
  echo "HATA: APP_JWT_SECRET bos." >&2
  exit 1
fi

TOKEN="$(node "$KOK/scripts/mint-jwt.js" "$SECRET")"
CSRF="duman-testi-csrf"
CEREZ="access_token=$TOKEN; XSRF-TOKEN=$CSRF"

GECICI="$(mktemp -d)"
trap 'rm -rf "$GECICI"' EXIT

gecti=0
kaldi=0

kontrol() {
  local ad="$1" beklenen="$2" gercek="$3"
  if [[ "$beklenen" == "$gercek" ]]; then
    printf '  \033[32mGECTI\033[0m  %s (%s)\n' "$ad" "$gercek"
    gecti=$((gecti + 1))
  else
    printf '  \033[31mKALDI\033[0m  %s — beklenen %s, gelen %s\n' "$ad" "$beklenen" "$gercek"
    kaldi=$((kaldi + 1))
  fi
}

kod() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

# JSON govdeleri curl'e DOSYADAN verilir. Git Bash, -d ile gecen UTF-8 metni
# bozabiliyor ("Invalid UTF-8 middle byte 0x75") - dosya yolunda bu olmuyor.
alan() {
  node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{
    const o=JSON.parse(s); console.log(process.argv[1].split(".").reduce((a,k)=>a[k],o));
  })' "$1"
}

echo "== Guvenlik =="
kontrol "cerezsiz istek reddedilir" 401 "$(kod "$TABAN/api/mailer/teams")"
kontrol "gecersiz token reddedilir" 401 "$(kod -b 'access_token=sahte' "$TABAN/api/mailer/teams")"
kontrol "CSRF basligi olmadan POST reddedilir" 403 \
  "$(kod -X POST -b "access_token=$TOKEN" -H 'Content-Type: application/json' \
      -d '{"teamId":1,"templateType":"KAPANIS","title":"x"}' "$TABAN/api/mailer/documents")"
kontrol "gecerli token kabul edilir" 200 "$(kod -b "$CEREZ" "$TABAN/api/mailer/teams")"

# CORS: tarayici AYNI ORIGIN'de bile POST/PUT/DELETE isteklerine Origin
# basligi ekler (GET'e eklemez). Kabuk bu basligi oldugu gibi proxy'ledigi
# icin kabugun adresi izin listesinde olmak zorunda.
#
# Bu kontrol curl'un varsayilan davranisiyla YAKALANMIYORDU: curl Origin
# gondermez, dolayisiyla CORS denetimine hic girmez. Yasanan hata tam da
# buydu - okuma calisiyordu, yazma "403 Invalid CORS request" ile dusuyordu
# ve duman testi yesil yaniyordu. O yuzden basligi ELLE gonderiyoruz.
KABUK_ORIGIN="http://localhost:${ODYSSEY_SHELL_PORT:-4173}"
kontrol "kabuk origin'inden yazma kabul edilir" 201 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H "Origin: $KABUK_ORIGIN" \
      -H 'Content-Type: application/json' \
      -d '{"teamId":1,"templateType":"KAPANIS","title":"[DUMAN TESTI] CORS"}' \
      "$TABAN/api/mailer/documents")"
kontrol "yabanci origin'den yazma reddedilir" 403 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H 'Origin: http://kotu-site.example' \
      -H 'Content-Type: application/json' \
      -d '{"teamId":1,"templateType":"KAPANIS","title":"sizinti"}' \
      "$TABAN/api/mailer/documents")"

# Regresyon: Spring'in kendi urettigi hatalar /error'a forward edilir ve
# guvenlik zinciri ikinci kez calisir. ERROR dispatch serbest birakilmazsa
# bu 400'ler istemciye 401 olarak ulasir ve frontend "oturum dustu" sanir
# (bkz. SecurityConfig).
echo "== Hata kodlari dogru donuyor mu (ERROR dispatch regresyonu) =="
kontrol "zorunlu parametre eksikse 400" 400 \
  "$(kod -b "$CEREZ" "$TABAN/api/mailer/documents")"
kontrol "bozuk JSON govdesi 400" 400 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H 'Content-Type: application/json' \
      -d '{bozuk' "$TABAN/api/mailer/documents")"
kontrol "olmayan yol 404" 404 "$(kod -b "$CEREZ" "$TABAN/api/mailer/boyle-bir-yol-yok")"

# Baska bir takimin PO'su, bizim takimin verisine ulasamamali. Bu bolum bir
# GUVENLIK sinirini koruyor - kirilirsa veri sizintisi demektir.
echo "== Takim yetkisi =="
BASKA_TOKEN="$(node "$KOK/scripts/mint-jwt.js" "$SECRET" 99999 PO 2)"
BASKA="access_token=$BASKA_TOKEN"

kontrol "yetkisiz takimin belgeleri listelenemez" 403 \
  "$(kod -b "$BASKA" "$TABAN/api/mailer/documents?teamId=1")"
kontrol "yetkisiz takimda belge olusturulamaz" 403 \
  "$(kod -X POST -b "$BASKA; XSRF-TOKEN=$CSRF" -H "X-CSRF-Token: $CSRF" \
      -H 'Content-Type: application/json' \
      -d '{"teamId":1,"templateType":"KAPANIS","title":"sizinti"}' \
      "$TABAN/api/mailer/documents")"
kontrol "yetkisiz takim adina onizleme uretilemez" 403 \
  "$(kod -X POST -b "$BASKA; XSRF-TOKEN=$CSRF" -H "X-CSRF-Token: $CSRF" \
      -H 'Content-Type: application/json' \
      -d '{"teamId":1,"templateType":"KAPANIS","content":{"schemaVersion":1,"header":{"title":"x","period":"","teamLabel":"y"},"meeting":{"date":"","time":"","place":""},"intro":[],"sections":[],"notes":[],"footer":{"line1":"","line2":""}}}' \
      "$TABAN/api/mailer/render/preview")"
kontrol "kendi takimini gorebiliyor" 200 "$(kod -b "$BASKA" "$TABAN/api/mailer/documents?teamId=2")"

echo "== Belge yasam dongusu =="
printf '%s' '{"teamId":1,"templateType":"KAPANIS","title":"[DUMAN TESTİ] Ağustos 2026 Sprint Kapanışı"}' \
  > "$GECICI/olustur.json"

OLUSAN="$(curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
  -H 'Content-Type: application/json; charset=UTF-8' \
  --data-binary "@$GECICI/olustur.json" "$TABAN/api/mailer/documents")"

ID="$(echo "$OLUSAN" | alan id)"
kontrol "taslak surum 1'de dogar" 1 "$(echo "$OLUSAN" | alan currentVersion)"
kontrol "varsayilan bolumler geldi" 2 "$(echo "$OLUSAN" | alan content.sections.length)"
kontrol "baslik Turkce karakterleri korudu" "[DUMAN TESTİ] Ağustos 2026 Sprint Kapanışı" \
  "$(echo "$OLUSAN" | alan title)"

# Yeni belge KENDI kuralini gecmeli. header bos dogsaydi belge, dogdugu anda
# PUT ve onizlemenin zorunlu tuttugu alanlari ihlal ederdi; kullanici tek harf
# yazmadan onizlemede 400 gorurdu - yasandi.
kontrol "yeni belgenin basligi takim adindan turetildi" "RPA SPRINT BİLGİLENDİRME" \
  "$(echo "$OLUSAN" | alan content.header.title)"
kontrol "yeni belgenin takim etiketi dolu" "RPA Takımı" \
  "$(echo "$OLUSAN" | alan content.header.teamLabel)"

# Asil kanit: taslak, hic dokunulmadan onizlenebiliyor mu.
#
# DIKKAT: node'a govdeyi <(...) sureç ikamesiyle VERME. Git Bash onu
# /proc/N/fd/M yoluna cevirir, Windows'taki node o yolu acamaz ve dosya BOS
# kalir - istek de "request body is missing" ile 400 doner. Stdin guvenli.
printf '%s' "$OLUSAN" > "$GECICI/olusan.json"
printf '%s' "$OLUSAN" | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{
  const b=JSON.parse(s);
  process.stdout.write(JSON.stringify({teamId:b.teamId,templateType:b.templateType,content:b.content}));
})' > "$GECICI/taze-onizleme.json"
kontrol "dokunulmamis taslak onizlenebilir" 200 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
      -H 'Content-Type: application/json; charset=UTF-8' \
      --data-binary "@$GECICI/taze-onizleme.json" "$TABAN/api/mailer/render/preview")"

# GET /documents/default - arayuz acilista bu icerigi cizer, ortada belge YOK.
# Ucun POST'un urettiginin AYNISINI donmesi sart: ayrisirlarsa kullanici
# ekranda bir sey gorup baska bir sey kaydeder.
curl -s -b "$CEREZ" "$TABAN/api/mailer/documents/default?teamId=1&templateType=KAPANIS" \
  > "$GECICI/varsayilan.json"
kontrol "varsayilan icerik ucu calisiyor" "RPA SPRINT BİLGİLENDİRME" \
  "$(alan header.title < "$GECICI/varsayilan.json")"
kontrol "varsayilan icerik POST'un urettiginin aynisi" "ayni" \
  "$(node -e '
const fs = require("fs");
const a = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
const b = JSON.parse(fs.readFileSync(process.argv[2], "utf8")).content;
console.log(JSON.stringify(a) === JSON.stringify(b) ? "ayni" : "AYRISMIS");
' "$GECICI/varsayilan.json" "$GECICI/olusan.json")"

kontrol "yetkisiz takimin varsayilani sorulamaz" 403 \
  "$(kod -b "$BASKA" "$TABAN/api/mailer/documents/default?teamId=1&templateType=KAPANIS")"

# Olmayan takim icin PO 403 alir, 404 DEGIL - ve bu dogru sira: yetki kapisi
# takim varligindan once kapanmasaydi, yetkisiz biri 404/403 farkindan hangi
# takimlarin var oldugunu cikarabilirdi. 404'u yalnizca ADMIN gorur.
YONETICI="$(node "$KOK/scripts/mint-jwt.js" "$SECRET" 10000 ADMIN)"
kontrol "olmayan takimin varsayilani PO'ya sizdirilmaz" 403 \
  "$(kod -b "$CEREZ" "$TABAN/api/mailer/documents/default?teamId=99999&templateType=KAPANIS")"
kontrol "olmayan takimin varsayilani yok (yonetici)" 404 \
  "$(kod -b "access_token=$YONETICI" "$TABAN/api/mailer/documents/default?teamId=99999&templateType=KAPANIS")"
kontrol "gecersiz mail tipi 400" 400 \
  "$(kod -b "$CEREZ" "$TABAN/api/mailer/documents/default?teamId=1&templateType=OLMAYAN")"

# Asil iddia: bu uc HICBIR SEY YAZMIYOR. Yazsaydi uygulama her acildiginda bir
# "Yeni mail" taslagi dusup Belgelerim listesi cope donerdi.
ONCE="$(curl -s -b "$CEREZ" "$TABAN/api/mailer/documents?teamId=1" | alan length)"
curl -s -o /dev/null -b "$CEREZ" "$TABAN/api/mailer/documents/default?teamId=1&templateType=KAPANIS"
curl -s -o /dev/null -b "$CEREZ" "$TABAN/api/mailer/documents/default?teamId=1&templateType=PLANLAMA"
kontrol "varsayilan icerik sormak belge yaratmaz" "$ONCE" \
  "$(curl -s -b "$CEREZ" "$TABAN/api/mailer/documents?teamId=1" | alan length)"

# Ayni sey olusturmada da gecerli; YONETICI yukarida uretildi.
kontrol "olmayan takimla belge acilmaz (yonetici)" 404 \
  "$(kod -X POST -b "access_token=$YONETICI; XSRF-TOKEN=$CSRF" -H "X-CSRF-Token: $CSRF" \
      -H 'Content-Type: application/json' \
      -d '{"teamId":99999,"templateType":"KAPANIS","title":"hayalet"}' \
      "$TABAN/api/mailer/documents")"

cat > "$GECICI/kaydet.json" <<'JSON'
{"title":"[DUMAN TESTİ] Ağustos 2026 Sprint Kapanışı","subject":"Sprint Bilgilendirme","expectedVersion":1,
 "content":{"schemaVersion":1,
  "header":{"title":"DİJİTAL UYGULAMALAR SPRINT BİLGİLENDİRME","period":"Ağustos 2026 Sprint Kapanışı","teamLabel":"RPA Takımı"},
  "meeting":{"date":"03.09.2026","time":"10:00","place":"Toplantı Salonu"},
  "intro":["ilk paragraf","orta paragraf","kapanış paragrafı"],
  "sections":[{"key":"analysis","title":"ANALİZ ÇALIŞMALARI","tone":"blue","columns":["sector","process"],
    "rows":[{"sector":"Elektrik","process":"Şebeke Operasyonları Aylık Hakediş Faturaları · İĞÜÇÖşğıçö"}]}],
  "notes":[{"tone":"green","text":"Not metni"}],
  "footer":{"line1":"Teşekkür ederiz.","line2":"Başarılar dileriz!"}}}
JSON

KAYIT="$(curl -s -X PUT -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
  -H 'Content-Type: application/json; charset=UTF-8' \
  --data-binary "@$GECICI/kaydet.json" "$TABAN/api/mailer/documents/$ID")"
kontrol "kaydetmek surumu artirir" 2 "$(echo "$KAYIT" | alan currentVersion)"

kontrol "Turkce karakter veritabanindan bozulmadan doner" \
  "Şebeke Operasyonları Aylık Hakediş Faturaları · İĞÜÇÖşğıçö" \
  "$(curl -s -b "$CEREZ" "$TABAN/api/mailer/documents/$ID" | alan content.sections.0.rows.0.process)"

kontrol "eski surumle kaydetmek 409 doner" 409 \
  "$(kod -X PUT -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
      -H 'Content-Type: application/json; charset=UTF-8' \
      --data-binary "@$GECICI/kaydet.json" "$TABAN/api/mailer/documents/$ID")"

printf '%s' '{"title":"x","expectedVersion":2,"content":{"schemaVersion":1,"header":{"title":"","period":"","teamLabel":""},"meeting":{"date":"","time":"","place":""},"intro":[],"sections":[],"notes":[],"footer":{"line1":"","line2":""}}}' \
  > "$GECICI/gecersiz.json"
kontrol "gecersiz icerik 400 doner" 400 \
  "$(kod -X PUT -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
      -H 'Content-Type: application/json; charset=UTF-8' \
      --data-binary "@$GECICI/gecersiz.json" "$TABAN/api/mailer/documents/$ID")"

kontrol "versiyon gecmisi iki satir" 2 \
  "$(curl -s -b "$CEREZ" "$TABAN/api/mailer/documents/$ID/versions" | alan length)"

GERI="$(curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
  "$TABAN/api/mailer/documents/$ID/versions/1/rollback")"
kontrol "geri alma yeni surum yazar" 3 "$(echo "$GERI" | alan currentVersion)"
# Surum 1 = belgenin DOGDUGU icerik. Geri alinca o baslik geri gelmeli.
# Eskiden header bos dogdugu icin burada bos dize bekleniyordu.
kontrol "geri alinan icerik eski surumun icerigi" "RPA SPRINT BİLGİLENDİRME" \
  "$(echo "$GERI" | alan content.header.title)"

kontrol "olmayan belge 404" 404 "$(kod -b "$CEREZ" "$TABAN/api/mailer/documents/999999")"
kontrol "olmayan surume geri alma 404" 404 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
      "$TABAN/api/mailer/documents/$ID/versions/99/rollback")"

echo "== Mail uretimi =="
cat > "$GECICI/onizleme.json" <<'JSON'
{"teamId":1,"templateType":"KAPANIS","content":{"schemaVersion":1,
 "header":{"title":"DİJİTAL UYGULAMALAR SPRINT BİLGİLENDİRME","period":"Ağustos 2026 Sprint Kapanışı","teamLabel":"BT – RPA Takımı"},
 "meeting":{"date":"03.09.2026","time":"10:00","place":"Toplantı Salonu"},
 "intro":["Giriş paragrafı."],
 "sections":[{"key":"analysis","title":"ANALİZ ÇALIŞMALARI","tone":"blue","columns":["sector","jira","process"],
   "rows":[{"sector":"Elektrik","jira":"RPA-2066","process":"Şebeke Operasyonları Aylık Hakediş Faturaları · İĞÜÇÖşğıçö"}]}],
 "notes":[{"tone":"green","text":"Not"}],
 "footer":{"line1":"Teşekkür ederiz.","line2":"Başarılar dileriz!"}}}
JSON

cat > "$GECICI/planlama.json" <<'JSON'
{"teamId":1,"templateType":"PLANLAMA","content":{"schemaVersion":1,
 "header":{"title":"DİJİTAL UYGULAMALAR SPRINT PLANLAMA","period":"Eylül 2026","teamLabel":"BT – RPA Takımı"},
 "meeting":{"date":"06.05.2026","time":"09:30","place":"Toplantı Salonu"},
 "intro":["Önümüzdeki sprintte ele alınacak konular."],
 "sections":[{"key":"topics","title":"SPRINT KONULARI","tone":"blue",
   "columns":["topicType","jira","summary","status","expected","stake"],
   "rows":[{"topicType":"Hikaye","jira":"RPA-1128","summary":"Banka Mutabakat Süreci","status":"UAT","expected":"UAT toplantısı planlanacak.","stake":"Tuba Kaya İşler"}]}],
 "notes":[{"tone":"blue","text":"Not"}],
 "footer":{"line1":"Teşekkür ederiz.","line2":"İyi çalışmalar!"}}}
JSON

ONIZLEME="$(curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
  -H 'Content-Type: application/json; charset=UTF-8' \
  --data-binary "@$GECICI/onizleme.json" "$TABAN/api/mailer/render/preview")"

kontrol "onizleme HTML donuyor" "var" \
  "$(echo "$ONIZLEME" | grep -qF '<!doctype html>' && echo var || echo yok)"
# Gorsel adresleri iki yolda FARKLI olmak zorunda ve bu fark tek yonlu:
#   onizleme -> data:  (tarayici cid: adresini cozemez, iframe sandbox="")
#   .eml     -> cid:   (Outlook data: URI'yi cozemez)
# Ikisi karisirsa bir taraf kirik gorselle calisir. Asagidaki dort kontrol
# bunu her iki yonden de kilitliyor.
kontrol "onizlemede cozulmemis cid: kalmaz" "temiz" \
  "$(echo "$ONIZLEME" | grep -qF 'src="cid:' && echo kirli || echo temiz)"
kontrol "onizlemede gorseller gomulu geliyor" "var" \
  "$(echo "$ONIZLEME" | grep -qF 'src="data:image/png;base64,' && echo var || echo yok)"
kontrol "onizlemede Turkce bozulmuyor" "var" \
  "$(echo "$ONIZLEME" | grep -qF 'Hakediş Faturaları · İĞÜÇÖşğıçö' && echo var || echo yok)"
# Mimari Kural 2: Outlook'un desteklemedigi CSS uretilmemeli.
kontrol "onizlemede yasak CSS yok" "temiz" \
  "$(echo "$ONIZLEME" | grep -qE 'display:flex|border-radius|linear-gradient|background-image|max-width' \
     && echo kirli || echo temiz)"
kontrol "gecersiz icerikle onizleme 400" 400 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H 'Content-Type: application/json' \
      -d '{"teamId":1,"templateType":"KAPANIS","content":{"schemaVersion":1,"header":{"title":"","period":"","teamLabel":""},"meeting":{"date":"","time":"","place":""},"intro":[],"sections":[],"notes":[],"footer":{"line1":"","line2":""}}}' \
      "$TABAN/api/mailer/render/preview")"

# Geri alma, belgeyi surum 1'in BOS varsayilan icerigine dondurdu. Bos baslikli
# belge dogrulamadan gecmez ve export 400 doner - dogru davranis. Disari
# aktarmadan once gecerli icerigi yeniden kaydediyoruz.
SURUM="$(curl -s -b "$CEREZ" "$TABAN/api/mailer/documents/$ID" | alan currentVersion)"
node -e '
const fs=require("fs");
const o=JSON.parse(fs.readFileSync(process.argv[1]+"/onizleme.json","utf8"));
fs.writeFileSync(process.argv[1]+"/eml-kaydet.json", JSON.stringify({
  title:"[DUMAN TESTİ] Ağustos 2026 Sprint Kapanışı",
  subject:"RPA Sprint Kapanış Bilgilendirme – Ağustos 2026",
  expectedVersion:Number(process.argv[2]), content:o.content}));
' "$GECICI" "$SURUM"
kontrol "disari aktarim oncesi kayit" 200 \
  "$(kod -X PUT -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
      -H 'Content-Type: application/json; charset=UTF-8' \
      --data-binary "@$GECICI/eml-kaydet.json" "$TABAN/api/mailer/documents/$ID")"

kontrol "olmayan belge disari aktarilamaz" 404 \
  "$(kod -b "$CEREZ" "$TABAN/api/mailer/documents/999999/export.eml")"

# --- Yonetici Ozeti ---------------------------------------------------------
# Uc tablo + kart bloku. Sayaclari SUNUCU hesapliyor; icerikteki satirlardan
# turetilen dort sayiyi de dogruluyoruz - istemci gonderemedigi icin burada
# kirilirsa ekrandaki sayi ile maildeki sayi ayrisir demektir.
cat > "$GECICI/yonetici.json" <<'JSON'
{"teamId":1,"templateType":"YONETICI_OZETI","content":{"schemaVersion":1,
 "header":{"title":"DİJİTAL UYGULAMALAR SPRİNT DEĞERLENDİRME TOPLANTI ÖZETİ","period":"16.09.2026 · TOPLANTI SONU","teamLabel":"Dijital Uygulamalar Takımı"},
 "meeting":{"date":"16.09.2026","time":"10:00","place":"Microsoft Teams"},
 "intro":["Toplantıda görüşülen konular aşağıda bilgilerinize sunulmuştur."],
 "sections":[
  {"key":"discussed","title":"GÖRÜŞÜLEN KONULAR","tone":"blue","columns":["team","topic","detail"],
   "rows":[{"team":"RPA","topic":"Fatura İtiraz Süreci","detail":"MBS verilerinin veri tabanından alınabilmesi değerlendirildi."},
           {"team":"Ürün Geliştirme","topic":"Müşteri Portalı","detail":"Yeni talep akışının mevcut durumu paylaşıldı."},
           {"team":"CBS","topic":"Harita Servisleri","detail":"Servis entegrasyonunun güncel durumu değerlendirildi."}]},
  {"key":"decisions","title":"ALINAN KARARLAR","tone":"green","columns":["no","decision","team"],
   "rows":[{"no":"","decision":"Fatura İtiraz sürecinin mevcut kapsamla devam etmesi","team":"RPA"},
           {"no":"","decision":"Yeni portal ekranlarının iş birimi değerlendirmesine sunulması","team":"Ürün Geliştirme"}]},
  {"key":"actions","title":"BEKLEYEN KONULAR VE AKSİYONLAR","tone":"orange","columns":["team","pending","owner","due","status"],
   "rows":[{"team":"RPA","pending":"MBS alanlarının incelenmesi","owner":"Ali Osman Bey","due":"05.09.2026","status":"Bekliyor"},
           {"team":"CBS","pending":"Servis bilgilerinin paylaşılması","owner":"CBS Ekibi","due":"07.09.2026","status":"Tamamlandı"}]},
  {"key":"links","title":"İNCELEME VE ERİŞİM BAĞLANTILARI","tone":"blue","columns":["linkType","title","description","button","url"],
   "rows":[{"linkType":"Uygulama","title":"Müşteri Portalı","description":"Güncel ekranları inceleyin.","button":"Uygulamayı Aç","url":"https://example.local/portal"},
           {"linkType":"Dashboard","title":"Sprint Dashboardu","description":"Göstergeleri görüntüleyin.","button":"Dashboardu Aç","url":"https://example.local/dashboard"},
           {"linkType":"Doküman","title":"Süreç Dokümanı","description":"Analiz detaylarını inceleyin.","button":"","url":""}]}],
 "notes":[{"tone":"green","text":"Kritik konular ilgili yöneticilerle ayrıca değerlendirilecektir."}],
 "footer":{"line1":"Bilgilerinize sunar,","line2":"iyi çalışmalar dileriz!"}}}
JSON

YONETICI="$(curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
  -H 'Content-Type: application/json; charset=UTF-8' \
  --data-binary "@$GECICI/yonetici.json" "$TABAN/api/mailer/render/preview")"
printf '%s' "$YONETICI" > "$GECICI/yonetici.html"

kontrol "yonetici ozeti uretiliyor" "var" \
  "$(grep -q 'GÖRÜŞÜLEN KONULAR' "$GECICI/yonetici.html" && echo var || echo yok)"
kontrol "uc tablo da basildi" "var" \
  "$(grep -q 'ALINAN KARARLAR' "$GECICI/yonetici.html" \
     && grep -q 'BEKLEYEN KONULAR VE AKSİYONLAR' "$GECICI/yonetici.html" && echo var || echo yok)"

# Sayaclar: 3 tekrarsiz ekip, 3 gorusulen konu, 2 karar, 1 bekleyen
# (ikinci aksiyon "Tamamlandı" oldugu icin sayilmaz).
sayac() {
  node -e '
const fs=require("fs");
const html=fs.readFileSync(process.argv[1],"utf8");
const m=html.match(new RegExp(">(\\d+)</b><br><span[^>]*>"+process.argv[2]+"<"));
console.log(m?m[1]:"BULUNAMADI");
' "$GECICI/yonetici.html" "$1"
}
kontrol "sayac: yer alan ekip" 3 "$(sayac 'YER ALAN EKİP')"
kontrol "sayac: gorusulen konu" 3 "$(sayac 'GÖRÜŞÜLEN KONU')"
kontrol "sayac: alinan karar" 2 "$(sayac 'ALINAN KARAR')"
kontrol "sayac: bekleyen konu (tamamlanan sayilmaz)" 1 "$(sayac 'BEKLEYEN KONU')"

# Karar numaralari cizerken uretiliyor: icerikte "no" bos geldi.
kontrol "karar numaralari otomatik" "var" \
  "$(grep -q 'K-01' "$GECICI/yonetici.html" && grep -q 'K-02' "$GECICI/yonetici.html" && echo var || echo yok)"

# Turuncu ton: yeni eklendi, tema cozuyor - icerik renk kodu gondermedi.
kontrol "turuncu ton temadan cozuldu" "var" \
  "$(grep -qi '#db6c12' "$GECICI/yonetici.html" && echo var || echo yok)"

# URL'siz kart buton CIZMEMELI - tiklanip hicbir sey olmayan buton en kotusu.
kontrol "baglanti butonlari yalnizca URL varsa" 2 \
  "$(grep -o 'example.local' "$GECICI/yonetici.html" | wc -l | tr -d ' ')"

kontrol "yonetici ozetinde yasak CSS yok" "temiz" \
  "$(grep -Eqi 'display:[ ]*(flex|grid)|position:[ ]*(absolute|fixed)|border-radius|linear-gradient' \
      "$GECICI/yonetici.html" && echo KIRLI || echo temiz)"

# Planlama artik uretiliyor - regresyon kontrolu.
PLAN="$(curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
  -H 'Content-Type: application/json; charset=UTF-8' \
  --data-binary "@$GECICI/planlama.json" "$TABAN/api/mailer/render/preview")"
kontrol "planlama maili uretiliyor" "var" \
  "$(echo "$PLAN" | grep -qF 'TOPLAM KONU' && echo var || echo yok)"
kontrol "planlama tek tablo - gruplama yok" "var" \
  "$(echo "$PLAN" | grep -qF 'KONU TÜRÜ' && echo var || echo yok)"

curl -s -D "$GECICI/eml-basliklar.txt" -o "$GECICI/mail.eml" -b "$CEREZ" \
  "$TABAN/api/mailer/documents/$ID/export.eml"
kontrol "eml message/rfc822 donuyor" "var" \
  "$(grep -qi 'Content-Type: message/rfc822' "$GECICI/eml-basliklar.txt" && echo var || echo yok)"

# Istemci dosya adini BU BASLIKTAN okuyor, title'dan degil (docs/api.md §9).
# Baslik kaybolursa indirme "Agustos_2026....eml" yerine tarayicinin
# uydurdugu bir adla iner; Turkce karakterli ad kimi Windows kurulumlarinda
# bozuk cikar - dosyaAdi()'ndaki ASCII indirgemesi tam bunun icin.
kontrol "eml dosya adi baslikta geliyor" "var" \
  "$(grep -qi 'Content-Disposition:.*filename=.*\.eml' "$GECICI/eml-basliklar.txt" && echo var || echo yok)"
# Ad ASCII'ye indirgenmis olmali - Turkce karakter kalmamali.
kontrol "eml dosya adi ASCII" "temiz" \
  "$(grep -i 'Content-Disposition:' "$GECICI/eml-basliklar.txt" \
     | grep -qE '[ğüşıöçĞÜŞİÖÇ]' && echo kirli || echo temiz)"
kontrol "eml X-Unsent tasiyor" "var" \
  "$(grep -qF 'X-Unsent: 1' "$GECICI/mail.eml" && echo var || echo yok)"
kontrol "eml bes gorseli gomuyor" 5 \
  "$(grep -c '^Content-ID: <' "$GECICI/mail.eml")"
# .eml govdesi base64 kodlu - cid: duz metin olarak GECMEZ, once cozmek
# gerekiyor. Cozulmus HTML'de cid: olmali, data: OLMAMALI: Outlook data:
# URI'yi cozemez, onizlemenin gomme yolu maile sizmamali.
EML_HTML="$(node -e '
const fs=require("fs");
const eml=fs.readFileSync(process.argv[1],"utf8");
const govde=eml.split("Content-Transfer-Encoding: base64\r\n\r\n")[1].split("\r\n--")[0].replace(/\r\n/g,"");
process.stdout.write(Buffer.from(govde,"base64").toString("utf8"));
' "$GECICI/mail.eml")"
kontrol "eml govdesi cid: kullaniyor" "var" \
  "$(echo "$EML_HTML" | grep -qF 'src="cid:hero"' && echo var || echo yok)"
kontrol "eml govdesinde data: URI yok" "temiz" \
  "$(echo "$EML_HTML" | grep -qF 'data:image/' && echo kirli || echo temiz)"

# Onizleme ile mail arasindaki IKINCI fark: duzenleme adresleri. Onizlemede
# OLMALI (arayuz onlarla calisiyor), .eml'de OLMAMALI (Outlook'a giden mailde
# yalnizca editor icin var olan nitelik tasinmasin). Iki yonu de tutuyoruz -
# korkulacak sey farkin sayisi degil, sessizce artmasi.
kontrol "eml govdesinde duzenleme adresi yok" "temiz" \
  "$(echo "$EML_HTML" | grep -qF 'data-alan' && echo KIRLI || echo temiz)"
kontrol "onizlemede duzenleme adresi var" "var" \
  "$(grep -qF 'data-alan="header.title"' "$GECICI/yonetici.html" && echo var || echo yok)"
kontrol "onizlemede satir adresleri var" "var" \
  "$(grep -qF 'data-alan="sections.discussed.rows.0.topic"' "$GECICI/yonetici.html" \
     && echo var || echo yok)"
# Bolum indeksle degil ANAHTARLA adresleniyor: kullanici bolum ekleyip
# silince indeks kayar, anahtar kaymaz.
kontrol "bolum adresi anahtar tasiyor" "temiz" \
  "$(grep -qE 'data-alan="sections\.[0-9]+\.' "$GECICI/yonetici.html" && echo KIRLI || echo temiz)"

# Karar numarasi cizerken uretiliyor, icerikte karsiligi BOS. Adres tasisaydi
# kullanici K-01'e tiklayip yazar, sonraki cizimde sunucu yazdigini ezerdi.
kontrol "uretilen sutun adres tasimaz" "temiz" \
  "$(grep -qF 'data-alan="sections.decisions.rows.0.no"' "$GECICI/yonetici.html" \
     && echo KIRLI || echo temiz)"
kontrol "ayni satirin diger sutunlari duzenlenebilir" "var" \
  "$(grep -qF 'data-alan="sections.decisions.rows.0.decision"' "$GECICI/yonetici.html" \
     && echo var || echo yok)"

# Satirin kendisi de adresli: arayuz ekleme/silme/siralama dugmelerini
# bunun uzerine konumlandiriyor, hucrelerden cikarim yapmiyor.
kontrol "satir capasi basiliyor" "var" \
  "$(grep -qF '<tr data-alan="sections.discussed.rows.0">' "$GECICI/yonetici.html" \
     && echo var || echo yok)"
# Sabit secenekli sutun listeyi kendisi bildiriyor - arayuz ikinci kez yazmasin.
kontrol "durum secenekleri sunucudan geliyor" "var" \
  "$(grep -qF 'data-secenekler="Bekliyor|Devam Ediyor|Karar Bekliyor|Tamamlandı"' \
      "$GECICI/yonetici.html" && echo var || echo yok)"

# --- Outlook icin kopyala ---------------------------------------------------
# Panoya giden HTML: gorseller gomulu (cid: pano uzerinden calismaz), ama
# duzenleme nitelikleri YOK - kullanici bunu Outlook taslagina yapistiracak.
curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
  -H 'Content-Type: application/json; charset=UTF-8' \
  --data-binary "@$GECICI/yonetici.json" "$TABAN/api/mailer/render/clipboard" \
  > "$GECICI/pano.html"

kontrol "pano HTML'i uretiliyor" "var" \
  "$(grep -q 'GÖRÜŞÜLEN KONULAR' "$GECICI/pano.html" && echo var || echo yok)"
kontrol "pano HTML'inde duzenleme niteligi yok" "temiz" \
  "$(grep -q 'data-' "$GECICI/pano.html" && echo KIRLI || echo temiz)"
kontrol "pano HTML'inde gorseller gomulu" "var" \
  "$(grep -qF 'data:image/' "$GECICI/pano.html" && echo var || echo yok)"
kontrol "pano HTML'inde cozulmemis cid: kalmaz" "temiz" \
  "$(grep -qF 'src="cid:' "$GECICI/pano.html" && echo KIRLI || echo temiz)"
kontrol "yetkisiz takim adina pano uretilemez" 403 \
  "$(kod -X POST -b "$BASKA; XSRF-TOKEN=$CSRF" -H "X-CSRF-Token: $CSRF" \
      -H 'Content-Type: application/json; charset=UTF-8' \
      --data-binary "@$GECICI/yonetici.json" "$TABAN/api/mailer/render/clipboard")"

# --- Giris sayfasi: son belgeler --------------------------------------------
# Kullanicinin BUTUN takimlarinin belgeleri tek istekte - bir PO'nun birden
# cok takimi olabiliyor, giris sayfasi N istek atmasin.
kontrol "son belgeler ucu calisiyor" 200 \
  "$(kod -b "$CEREZ" "$TABAN/api/mailer/documents/recent")"
kontrol "son belgeler takim bilgisi tasiyor" "var" \
  "$(curl -s -b "$CEREZ" "$TABAN/api/mailer/documents/recent" \
     | grep -q '"templateType"' && echo var || echo yok)"
# Limit sunucuda sinirli: istemci butun tabloyu cekemez.
kontrol "son belgeler limiti asilamaz" 200 \
  "$(kod -b "$CEREZ" "$TABAN/api/mailer/documents/recent?limit=100000")"

kontrol "indirme logu kaydediliyor" 204 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H 'Content-Type: application/json' \
      -d '{"format":"EML"}' "$TABAN/api/mailer/documents/$ID/downloads")"
# PDF de gecerli bir bicim: "PDF / Yazdir" akisi bunu gonderiyor. EML ile
# birlikte ikisi de kilitli olmali - biri sessizce reddedilirse olcum yarim
# kalir ve kimse fark etmez.
kontrol "PDF indirme kaydi da kabul edilir" 204 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H 'Content-Type: application/json' \
      -d '{"format":"PDF"}' "$TABAN/api/mailer/documents/$ID/downloads")"
kontrol "gecersiz indirme bicimi reddedilir" 400 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H 'Content-Type: application/json' \
      -d '{"format":"DOCX"}' "$TABAN/api/mailer/documents/$ID/downloads")"

echo
echo "gecti: $gecti   kaldi: $kaldi   (belge id: $ID)"
[[ "$kaldi" -eq 0 ]]
