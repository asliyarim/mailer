#!/bin/sh
# Odyssey katalogUNA "Aksa Mailler" kartini EKLER.
#
# nginx resmi imaji, acilista /docker-entrypoint.d/*.sh dosyalarini calistirir.
# Bu betik oraya mount edilir (bkz. docker-compose.odyssey.yml) ve kabugun
# projects.js dosyasinin SONUNA birkac satir ekler.
#
# Neden boyle, neden projects.js'i kopyalamadik:
# projects.js butun Odyssey uygulamalarinin katalogu. Kendi kopyamizi tasisak
# kabuga yeni bir uygulama eklendiginde bizim kopya bayatlar ve yerelde eksik
# katalog gosteririz. Ekleme yapmak, catallamaktan iyi.
#
# Neden dosyanin SONUNA: ADRESLER, PROJECTS ve adres() yukarida tanimli.
# const bir nesne/dizi olsalar da icerikleri degistirilebilir.
#
# Bu betik olmadan /mailer/ yolu yine calisir (yonlendirme nginx tarafinda),
# ama kart katalogda GORUNMEZ - uygulamaya ancak adresi elle yazarak
# girilebilir.

set -e

KATALOG=/usr/share/nginx/html/projects.js

if [ ! -f "$KATALOG" ]; then
  echo "40-aksa-mailer-karti: $KATALOG yok, atlaniyor" >&2
  exit 0
fi

# Kart zaten varsa (kabuk deposuna elle eklenmisse) tekrar ekleme.
if grep -q '"aksa-mailer"' "$KATALOG"; then
  echo "40-aksa-mailer-karti: kart zaten var, atlandi"
  exit 0
fi

cat >> "$KATALOG" <<'JS'

/* --- Aksa Mailler (aksa-mailer deposundan eklendi) --- */
ADRESLER["aksa-mailer"] = {
  // Odyssey nginx'i /mailer/ yolunu uygulamanin frontend'ine proxy'ler -
  // ayri bir alan adi KULLANILMAZ, oturum cerezi first-party kalsin diye.
  prod: "/mailer/",
  dev: "/mailer/",
};
PROJECTS.push({
  id: "aksa-mailer",
  slug: "mailer",
  name: "Aksa Mailler",
  tagline: "Sprint bilgilendirme maili üretici",
  description:
    "Sprint kapanış, planlama ve yönetici özeti maillerini hazırlar; Outlook'ta bozulmadan açılan .eml dosyası üretir.",
  status: "live",
  url: adres("aksa-mailer"),
  accent: "teal",
  icon: "spark",
  preview: `<svg viewBox="0 0 260 78" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="16" y="14" width="86" height="8" rx="4" fill="currentColor" opacity=".55"/>
    <rect x="16" y="30" width="60" height="5" rx="2.5" fill="currentColor" opacity=".3"/>
    <rect x="16" y="41" width="72" height="5" rx="2.5" fill="currentColor" opacity=".3"/>
    <rect x="16" y="52" width="48" height="5" rx="2.5" fill="currentColor" opacity=".3"/>
    <rect x="122" y="14" width="122" height="50" rx="4" stroke="currentColor" opacity=".45"/>
    <rect x="132" y="24" width="60" height="6" rx="3" fill="currentColor" opacity=".4"/>
    <rect x="132" y="36" width="102" height="4" rx="2" fill="currentColor" opacity=".22"/>
    <rect x="132" y="45" width="88" height="4" rx="2" fill="currentColor" opacity=".22"/>
  </svg>`,
});
JS

echo "40-aksa-mailer-karti: katalog kartı eklendi"
