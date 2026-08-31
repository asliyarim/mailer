#!/usr/bin/env bash
# Uc mail tipini de TAM yasam dongusunden gecirir:
#   ./scripts/uctan-uca.sh
#
#   olustur -> varsayilan icerik -> onizle -> satir ekle -> kaydet
#   -> surum gecmisi -> .eml -> pano -> indirme kaydi
#
# Duman testi bunlarin cogunu KAPANIS icin yapiyor; buradaki soru "ucu de
# ayni olgunlukta mi". Ozellikle .eml uretimi yalnizca Kapanis'ta sinanmisti -
# bu betik yazilinca fark edildi.
#
# Biraktigi belgeler "[UCTAN UCA]" ile baslar.
#
# SADECE YEREL. Bu betik uretimde calistirilmaz.
set -euo pipefail

KOK="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TABAN="${TABAN:-http://localhost:8082}"
SECRET="$(grep '^APP_JWT_SECRET=' "$KOK/.env" | cut -d= -f2-)"
TOKEN="$(node "$KOK/scripts/mint-jwt.js" "$SECRET")"
CSRF="uctan-uca"
CEREZ="access_token=$TOKEN; XSRF-TOKEN=$CSRF"
G="$(mktemp -d)"; trap 'rm -rf "$G"' EXIT

gecti=0; kaldi=0
kontrol() {
  if [[ "$2" == "$3" ]]; then printf '  \033[32m✓\033[0m %s (%s)\n' "$1" "$3"; gecti=$((gecti+1))
  else printf '  \033[31m✗\033[0m %s — beklenen %s, gelen %s\n' "$1" "$2" "$3"; kaldi=$((kaldi+1)); fi
}
kod() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
alan() { node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{
  const o=JSON.parse(s);console.log(process.argv[1].split(".").reduce((a,k)=>a?.[k],o));})' "$1"; }

for TIP in KAPANIS PLANLAMA YONETICI_OZETI; do
  echo
  echo "═══ $TIP ═══"

  # 1. Belge dogar
  printf '%s' "{\"teamId\":1,\"templateType\":\"$TIP\",\"title\":\"[UCTAN UCA] $TIP\"}" > "$G/yeni.json"
  BELGE="$(curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
    -H 'Content-Type: application/json; charset=UTF-8' \
    --data-binary "@$G/yeni.json" "$TABAN/api/mailer/documents")"
  printf '%s' "$BELGE" > "$G/belge.json"
  ID="$(alan id < "$G/belge.json")"
  case "$TIP" in
    YONETICI_OZETI) BEKLENEN_BASLIK="RPA SPRINT YÖNETİCİ ÖZETİ" ;;
    *)              BEKLENEN_BASLIK="RPA SPRINT BİLGİLENDİRME" ;;
  esac
  kontrol "belge dogar" 1 "$(alan currentVersion < "$G/belge.json")"
  kontrol "basligi takimdan turetilmis" "$BEKLENEN_BASLIK" \
    "$(alan content.header.title < "$G/belge.json")"

  # 2. Dokunulmamis taslak onizlenebilir + bolum capalari var
  node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const b=JSON.parse(s);
    process.stdout.write(JSON.stringify({teamId:b.teamId,templateType:b.templateType,content:b.content}));})' \
    < "$G/belge.json" > "$G/onizle.json"
  curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
    -H 'Content-Type: application/json; charset=UTF-8' \
    --data-binary "@$G/onizle.json" "$TABAN/api/mailer/render/preview" > "$G/bos.html"
  kontrol "bos taslak onizlenir" "var" \
    "$(grep -q '<!doctype html' "$G/bos.html" && echo var || echo yok)"
  BEKLENEN_BOLUM="$(alan content.sections.length < "$G/belge.json")"
  kontrol "bos belgede her bolumun capasi" "$BEKLENEN_BOLUM" \
    "$(grep -o 'data-bolum="[^"]*"' "$G/bos.html" | sort -u | wc -l | tr -d ' ')"

  # 3. Her bolume BIR satir ekle (arayuzun "+ satir ekle" yaptigi sey)
  node -e '
let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{
  const b=JSON.parse(s);
  b.content.sections = b.content.sections.map(bol => ({...bol,
    rows: [Object.fromEntries(bol.columns.map(c => [c, c==="status" ? "Bekliyor" : "değer-"+c]))]}));
  process.stdout.write(JSON.stringify({title:b.title, subject:"Konu", expectedVersion:1, content:b.content}));
})' < "$G/belge.json" > "$G/kaydet.json"

  KAYIT="$(curl -s -X PUT -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
    -H 'Content-Type: application/json; charset=UTF-8' \
    --data-binary "@$G/kaydet.json" "$TABAN/api/mailer/documents/$ID")"
  printf '%s' "$KAYIT" > "$G/kayit.json"
  kontrol "satirlar kaydedilir, surum artar" 2 "$(alan currentVersion < "$G/kayit.json")"
  kontrol "surum gecmisi iki satir" 2 \
    "$(curl -s -b "$CEREZ" "$TABAN/api/mailer/documents/$ID/versions" | alan length)"

  # 4. Dolu belge onizlenir, satir capalari cikar
  node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const b=JSON.parse(s);
    process.stdout.write(JSON.stringify({teamId:b.teamId,templateType:b.templateType,content:b.content}));})' \
    < "$G/kayit.json" > "$G/dolu-onizle.json"
  curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
    -H 'Content-Type: application/json; charset=UTF-8' \
    --data-binary "@$G/dolu-onizle.json" "$TABAN/api/mailer/render/preview" > "$G/dolu.html"
  kontrol "satir capalari cikar" "$BEKLENEN_BOLUM" \
    "$(grep -o 'data-alan="sections\.[a-z]*\.rows\.0"' "$G/dolu.html" | sort -u | wc -l | tr -d ' ')"

  # 5. .eml — ASIL BOSLUK BUYDU, yalnizca Kapanis sinanmisti
  curl -s -b "$CEREZ" "$TABAN/api/mailer/documents/$ID/export.eml" > "$G/mail.eml"
  kontrol "eml uretilir" "var" \
    "$(grep -q 'X-Unsent: 1' "$G/mail.eml" && echo var || echo yok)"
  kontrol "eml bes gorsel gomer" 5 "$(grep -c '^Content-ID: <' "$G/mail.eml")"
  EML_HTML="$(node -e '
const fs=require("fs");const eml=fs.readFileSync(process.argv[1],"utf8");
const g=eml.split("Content-Transfer-Encoding: base64\r\n\r\n")[1].split("\r\n--")[0].replace(/\r\n/g,"");
process.stdout.write(Buffer.from(g,"base64").toString("utf8"));' "$G/mail.eml")"
  kontrol "eml govdesinde editor niteligi YOK" "temiz" \
    "$(echo "$EML_HTML" | grep -q 'data-' && echo KIRLI || echo temiz)"
  kontrol "eml cid: kullanir" "var" \
    "$(echo "$EML_HTML" | grep -q 'src="cid:' && echo var || echo yok)"
  kontrol "eml govdesinde data: URI yok" "temiz" \
    "$(echo "$EML_HTML" | grep -q 'data:image/' && echo KIRLI || echo temiz)"
  kontrol "eml Turkce bozulmaz" "var" \
    "$(echo "$EML_HTML" | grep -q 'RPA Takımı' && echo var || echo yok)"

  # 6. Pano
  curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
    -H 'Content-Type: application/json; charset=UTF-8' \
    --data-binary "@$G/dolu-onizle.json" "$TABAN/api/mailer/render/clipboard" > "$G/pano.html"
  kontrol "pano temiz ve gorselli" "var" \
    "$(! grep -q 'data-' "$G/pano.html" && grep -q 'data:image/' "$G/pano.html" && echo var || echo yok)"

  # 7. Uc indirme bicimi de kaydedilir
  for BICIM in EML PDF KOPYALA; do
    kontrol "indirme kaydi: $BICIM" 204 \
      "$(kod -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" -H 'Content-Type: application/json' \
          -d "{\"format\":\"$BICIM\"}" "$TABAN/api/mailer/documents/$ID/downloads")"
  done
done

echo
echo "═══ Giris sayfasi ═══"
kontrol "son belgeler uc tipi de gorur" 3 \
  "$(curl -s -b "$CEREZ" "$TABAN/api/mailer/documents/recent?limit=50" \
     | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{
       const l=JSON.parse(s).filter(d=>d.title.startsWith("[UCTAN UCA]"));
       console.log(new Set(l.map(d=>d.templateType)).size);})')"

echo
printf 'gecti: %d   kaldi: %d\n' "$gecti" "$kaldi"
[[ "$kaldi" -eq 0 ]]
