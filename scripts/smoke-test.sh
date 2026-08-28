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
kontrol "geri alinan icerik eski surumun icerigi" "" \
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
kontrol "onizlemede cid: gorseller var" "var" \
  "$(echo "$ONIZLEME" | grep -qF 'src="cid:hero"' && echo var || echo yok)"
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

# Yonetici Ozeti Sprint 2'de gelecek. O zamana kadar sessizce yanlis sablon
# uretmek yerine acikca hata vermeli.
kontrol "hazir olmayan mail tipi onizlenemez" 400 \
  "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H 'Content-Type: application/json' \
      -d '{"teamId":1,"templateType":"YONETICI_OZETI","content":{"schemaVersion":1,"header":{"title":"x","period":"","teamLabel":"y"},"meeting":{"date":"","time":"","place":""},"intro":[],"sections":[],"notes":[],"footer":{"line1":"","line2":""}}}' \
      "$TABAN/api/mailer/render/preview")"

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
kontrol "eml X-Unsent tasiyor" "var" \
  "$(grep -qF 'X-Unsent: 1' "$GECICI/mail.eml" && echo var || echo yok)"
kontrol "eml bes gorseli gomuyor" 5 \
  "$(grep -c '^Content-ID: <' "$GECICI/mail.eml")"

echo
echo "gecti: $gecti   kaldi: $kaldi   (belge id: $ID)"
[[ "$kaldi" -eq 0 ]]
