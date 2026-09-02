#!/usr/bin/env bash
# Dort mail tipini de DOLU icerikle olusturur - tanitim ve elle deneme icin.
#
#   ./scripts/ornek-veri.sh
#
# Belgeler Urun Gelistirme Takimi (id 3) adina, Gozde Son'un sicili (29547)
# ile yazilir; yani "Son Taslaklar" listesinde onun kaydi gibi gorunur.
#
# Token'i .env'deki APP_JWT_SECRET ile KENDIMIZ imzaliyoruz - duman testiyle
# ayni yontem. Giris yapmis bir kullaniciya ihtiyac yok.
#
# Biraktigi belgeler "[ÖRNEK]" ile BASLAMAZ: bunlar gercekci gorunmeli.
# Silmek istersen sondaki not'a bak.
#
# SADECE YEREL. Bu betik uretimde calistirilmaz.

set -euo pipefail

TABAN="${TABAN:-http://localhost:8082}"
KOK="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TAKIM=3
SICIL=29547

SECRET="$(grep '^APP_JWT_SECRET=' "$KOK/.env" | cut -d= -f2-)"
[[ -z "$SECRET" ]] && { echo "HATA: APP_JWT_SECRET bos." >&2; exit 1; }

TOKEN="$(node "$KOK/scripts/mint-jwt.js" "$SECRET" "$SICIL" PO "$TAKIM")"
CSRF="ornek-veri"
CEREZ="access_token=$TOKEN; XSRF-TOKEN=$CSRF"

G="$(mktemp -d)"; trap 'rm -rf "$G"' EXIT

alan() {
  node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{
    const o=JSON.parse(s); console.log(process.argv[1].split(".").reduce((a,k)=>a?.[k],o));})' "$1"
}

# olustur <tip> <baslik> <konu> <icerik-dosyasi>
olustur() {
  local tip="$1" baslik="$2" konu="$3" icerikDosya="$4"

  printf '%s' "{\"teamId\":$TAKIM,\"templateType\":\"$tip\",\"title\":\"$baslik\"}" > "$G/yeni.json"
  local belge
  belge="$(curl -s -X POST -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
    -H 'Content-Type: application/json; charset=UTF-8' \
    --data-binary "@$G/yeni.json" "$TABAN/api/mailer/documents")"
  printf '%s' "$belge" > "$G/belge.json"

  local id
  id="$(alan id < "$G/belge.json")"
  if [[ -z "$id" || "$id" == "undefined" ]]; then
    echo "  HATA olusturulamadi: $(head -c 200 "$G/belge.json")" >&2
    return 1
  fi

  # Sunucunun urettigi VARSAYILAN icerigi al, bolum satirlarini doldur.
  # Bolum yapisini yeniden yazmiyoruz: anahtarlar, sutunlar ve tonlar
  # sunucudan gelsin - ikinci bir tanim olusturmayalim.
  node -e '
const fs=require("fs");
const belge=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));
const dolgu=JSON.parse(fs.readFileSync(process.argv[2],"utf8"));
const c=belge.content;

if (dolgu.header)  Object.assign(c.header, dolgu.header);
if (dolgu.meeting) Object.assign(c.meeting, dolgu.meeting);
if (dolgu.intro)   c.intro = dolgu.intro;
if (dolgu.notes)   c.notes = dolgu.notes;

// Satirlari BOLUM ANAHTARINA gore yerlestir - sira degisirse bozulmasin.
for (const [anahtar, satirlar] of Object.entries(dolgu.rows || {})) {
  const bolum = c.sections.find(b => b.key === anahtar);
  if (!bolum) { console.error("  UYARI: bolum yok -> " + anahtar); continue; }
  bolum.rows = satirlar;
}

fs.writeFileSync(process.argv[3], JSON.stringify({
  title: belge.title, subject: process.argv[4], expectedVersion: 1, content: c,
}));' "$G/belge.json" "$icerikDosya" "$G/kaydet.json" "$konu"

  local kayit
  kayit="$(curl -s -X PUT -b "$CEREZ" -H "X-CSRF-Token: $CSRF" \
    -H 'Content-Type: application/json; charset=UTF-8' \
    --data-binary "@$G/kaydet.json" "$TABAN/api/mailer/documents/$id")"

  local surum
  surum="$(printf '%s' "$kayit" | alan currentVersion)"
  if [[ "$surum" == "2" ]]; then
    printf '  \033[32m✓\033[0m %-20s id=%-4s %s\n' "$tip" "$id" "$baslik"
  else
    echo "  HATA kaydedilemedi ($tip): $(printf '%s' "$kayit" | head -c 200)" >&2
    return 1
  fi
}

echo "Ürün Geliştirme Takımı (id $TAKIM) · Gözde Son (sicil $SICIL)"
echo

# ---------------------------------------------------------------- PLANLAMA
cat > "$G/planlama.json" <<'JSON'
{
  "header": { "period": "01.09.2026 – 14.09.2026 · Sprint 42" },
  "meeting": { "date": "01.09.2026", "time": "10:00", "place": "Microsoft Teams" },
  "rows": {
    "topics": [
      { "topicType": "Hikaye", "jira": "UG-1042", "summary": "Müşteri Portalı – yeni talep akışı",
        "status": "Analiz", "expected": "İhtiyaç listesinin iş birimi tarafından onaylanması",
        "stake": "Müşteri Hizmetleri, Dijital Uygulamalar" },
      { "topicType": "Hikaye", "jira": "UG-1038", "summary": "Dijital başvuru – deneyim iyileştirmesi",
        "status": "Tasarım", "expected": "Ekran tasarımlarının değerlendirilmesi",
        "stake": "Dijital Uygulamalar, Müşteri Deneyimi" },
      { "topicType": "Görev", "jira": "UG-1029", "summary": "Geri bildirim modülü altyapı hazırlığı",
        "status": "Geliştirme", "expected": "", "stake": "Müşteri Deneyimi" },
      { "topicType": "Bug", "jira": "UG-1051", "summary": "Başvuru formunda tarih alanı hatası",
        "status": "Geliştirme", "expected": "", "stake": "Müşteri Hizmetleri" },
      { "topicType": "İyileştirme", "jira": "UG-1055", "summary": "Portal arama performansı",
        "status": "Analiz", "expected": "Mevcut sorgu sürelerinin ölçülmesi",
        "stake": "Veri Yönetimi" }
    ]
  },
  "notes": [
    { "tone": "blue", "text": "Bekleyen konuların hedef tarihlere kadar tamamlanması, çalışmaların sprint planına uygun ilerleyebilmesi açısından önem taşımaktadır." },
    { "tone": "green", "text": "Sprint kapsamında bir değişiklik gerekmesi hâlinde en geç 05.09.2026 tarihine kadar bilgilendirme yapılmasını rica ederiz." }
  ]
}
JSON
olustur PLANLAMA "Eylül 1. Sprint Planlama" \
  "Ürün Geliştirme | Sprint Planlama Bilgilendirmesi | 01.09.2026 – 14.09.2026" \
  "$G/planlama.json"

# ----------------------------------------------------------------- KAPANIS
cat > "$G/kapanis.json" <<'JSON'
{
  "header": { "period": "01.09.2026 – 14.09.2026 Sprint Kapanışı" },
  "meeting": { "date": "16.09.2026", "time": "10:00", "place": "Microsoft Teams" },
  "rows": {
    "analysis": [
      { "jira": "UG-1042", "ci": "CI4821", "process": "Müşteri Portalı – yeni talep akışı",
        "stage": "Analiz Tamamlandı", "stake": "Müşteri Hizmetleri, Dijital Uygulamalar",
        "note": "Onay adımları sadeleştirildi, iş birimi mutabakatı alındı." },
      { "jira": "UG-1055", "ci": "CI4833", "process": "Portal arama performansı",
        "stage": "Analiz Tamamlandı", "stake": "Veri Yönetimi",
        "note": "Sorgu süreleri ölçüldü, iyileştirme sonraki sprinte alındı." }
    ],
    "development": [
      { "jira": "UG-1038", "ci": "CI4809", "process": "Dijital başvuru – deneyim iyileştirmesi",
        "stage": "Canlıya Alındı", "stake": "Dijital Uygulamalar, Müşteri Deneyimi",
        "note": "Mobil uyumluluk testleri tamamlandı." },
      { "jira": "UG-1029", "ci": "CI4815", "process": "Geri bildirim modülü altyapı hazırlığı",
        "stage": "Canlıya Alındı", "stake": "Müşteri Deneyimi", "note": "" },
      { "jira": "UG-1051", "ci": "CI4840", "process": "Başvuru formunda tarih alanı hatası",
        "stage": "Canlıya Alındı", "stake": "Müşteri Hizmetleri",
        "note": "Hata giderildi, regresyon testleri geçti." }
    ]
  },
  "notes": [
    { "tone": "blue", "text": "Sprint kapsamında gerçekleştirilen çalışmaların çıktıları ve mevcut durumları toplantıda paydaşlarla birlikte değerlendirilecektir." },
    { "tone": "green", "text": "İlave paydaşların bulunması hâlinde toplantı davetinin ilgili kişilerle paylaşılmasını rica ederiz." }
  ]
}
JSON
olustur KAPANIS "Eylül 1. Sprint Kapanışı" \
  "Ürün Geliştirme | Sprint Kapanış Toplantısı | 01.09.2026 – 14.09.2026" \
  "$G/kapanis.json"

# ---------------------------------------------------------- YONETICI OZETI
cat > "$G/yonetici.json" <<'JSON'
{
  "header": { "period": "16.09.2026 · Sprint Değerlendirme" },
  "meeting": { "date": "16.09.2026", "time": "14:00", "place": "Microsoft Teams" },
  "rows": {
    "discussed": [
      { "team": "Ürün Geliştirme", "topic": "Müşteri Portalı",
        "detail": "Yeni talep akışının analizi tamamlandı; onay adımları üç kademeden ikiye indirildi." },
      { "team": "Dijital Uygulamalar", "topic": "Dijital Başvuru",
        "detail": "Deneyim iyileştirmesi canlıya alındı, mobil kullanım oranı takip ediliyor." },
      { "team": "Veri Yönetimi", "topic": "Portal Arama Performansı",
        "detail": "Mevcut sorgu süreleri ölçüldü; iyileştirme bir sonraki sprinte planlandı." }
    ],
    "decisions": [
      { "no": "", "decision": "Yeni talep akışının iş birimi onayıyla geliştirmeye alınması", "team": "Ürün Geliştirme" },
      { "no": "", "decision": "Portal arama iyileştirmesinin Sprint 43 kapsamına alınması", "team": "Veri Yönetimi" }
    ],
    "actions": [
      { "team": "Ürün Geliştirme", "pending": "Onaylanan akışın geliştirme tahmininin çıkarılması",
        "owner": "Gözde Son", "due": "19.09.2026", "status": "Devam Ediyor" },
      { "team": "Veri Yönetimi", "pending": "Arama sorguları için indeks önerisinin paylaşılması",
        "owner": "Veri Yönetimi Ekibi", "due": "22.09.2026", "status": "Bekliyor" },
      { "team": "Dijital Uygulamalar", "pending": "Mobil kullanım raporunun iletilmesi",
        "owner": "Dijital Uygulamalar", "due": "18.09.2026", "status": "Tamamlandı" }
    ],
    "links": [
      { "linkType": "Uygulama", "title": "Müşteri Portalı", "description": "Yeni talep akışını ve güncel ekranları inceleyin.",
        "button": "Uygulamayı Aç", "url": "https://portal.aksa.com.tr" },
      { "linkType": "Dashboard", "title": "Sprint Panosu", "description": "Ekip göstergelerini ve sprint çıktılarını görüntüleyin.",
        "button": "Panoyu Aç", "url": "https://jira.aksa.com.tr/dashboard" },
      { "linkType": "Doküman", "title": "Analiz Dokümanı", "description": "Talep akışının kapsam ve analiz detayları.",
        "button": "Dokümanı Gör", "url": "https://wiki.aksa.com.tr/UG-1042" }
    ]
  },
  "notes": [
    { "tone": "green", "text": "Kritik konuların ilgili yöneticilerle ayrıca değerlendirilmesi planlanmıştır." },
    { "tone": "blue", "text": "Açık aksiyonların güncel durumu bir sonraki değerlendirme toplantısında kontrol edilecektir." }
  ]
}
JSON
olustur YONETICI_OZETI "Sprint 42 Yönetici Özeti" \
  "Ürün Geliştirme | Sprint Değerlendirme Toplantı Özeti | 16.09.2026" \
  "$G/yonetici.json"

# ------------------------------------------------------- TOPLANTI CIKTILARI
cat > "$G/toplanti.json" <<'JSON'
{
  "meeting": {
    "date": "18.09.2026", "time": "14:00 – 15:30",
    "place": "Microsoft Teams & Toplantı Salonu A",
    "title": "Ürün Yol Haritası ve Öncelik Değerlendirme Toplantısı",
    "moderator": "Gözde Son (Ürün Geliştirme)",
    "attendees": "Ürün Geliştirme, Dijital Uygulamalar, İş Analizi, Veri Yönetimi"
  },
  "rows": {
    "discussed": [
      { "team": "Ürün Geliştirme", "topic": "Çeyrek Yol Haritası",
        "detail": "Dördüncü çeyrek için öncelikli üç başlık belirlendi; kapasite ile karşılaştırıldı." },
      { "team": "İş Analizi", "topic": "Talep Toplama Süreci",
        "detail": "İş birimlerinden gelen taleplerin tek kanaldan toplanması için akış önerisi sunuldu." },
      { "team": "Veri Yönetimi", "topic": "Raporlama Altyapısı",
        "detail": "Mevcut raporların güncellenme sıklığı ve veri kaynakları gözden geçirildi." }
    ],
    "decisions": [
      { "no": "", "decision": "Dördüncü çeyrekte önceliğin müşteri portalı deneyimine verilmesi",
        "scope": "Yol Haritası", "team": "Ürün Geliştirme" },
      { "no": "", "decision": "Taleplerin tek kanaldan toplanması için form akışının kurulması",
        "scope": "Süreç", "team": "İş Analizi" },
      { "no": "", "decision": "Raporlama altyapısı yenilemesinin bir sonraki çeyreğe ötelenmesi",
        "scope": "Kapsam", "team": "Veri Yönetimi" }
    ],
    "actions": [
      { "team": "Ürün Geliştirme", "pending": "Öncelik listesinin yönetim sunumuna hazırlanması",
        "owner": "Gözde Son", "due": "23.09.2026", "status": "Devam Ediyor" },
      { "team": "İş Analizi", "pending": "Talep formu taslağının paylaşılması",
        "owner": "İş Analizi Ekibi", "due": "25.09.2026", "status": "Bekliyor" },
      { "team": "Veri Yönetimi", "pending": "Mevcut rapor envanterinin çıkarılması",
        "owner": "Veri Yönetimi Ekibi", "due": "30.09.2026", "status": "Karar Bekliyor" }
    ]
  },
  "notes": [
    { "tone": "green", "text": "Bir sonraki değerlendirme toplantısı 02.10.2026 Cuma günü saat 14:00 olarak planlanmıştır." },
    { "tone": "blue", "text": "Açık aksiyonların ilerleme durumu haftalık ekip toplantısında takip edilecektir." }
  ]
}
JSON
olustur TOPLANTI_CIKTILARI "Ürün Yol Haritası Toplantı Çıktıları" \
  "Ürün Yol Haritası ve Öncelik Değerlendirme Toplantısı | 18.09.2026" \
  "$G/toplanti.json"

echo
echo "Dört belge de hazır. Silmek istersen:"
echo "  DELETE FROM mailer_documents WHERE team_id = $TAKIM AND created_by = '$SICIL';"
echo "  (once mailer_document_versions ve mailer_download_logs satirlari)"
