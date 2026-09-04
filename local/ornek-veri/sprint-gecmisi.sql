-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  YEREL ORNEK VERI - "Gecen sprintten devam et" ozelligini denemek     ║
-- ╚══════════════════════════════════════════════════════════════════════╝
--
-- NE YAPAR: dort takima ucer sprint gecmisi yazar. Boylece giris
-- sayfasindaki serit bos kalmaz ve "Kopyala" dugmesi denenebilir.
--
-- NEREDE CALISIR: YALNIZCA YEREL. Baslıklar "[ORNEK]" ile isaretli, canliya
-- gitmesi dusunulmemistir. Migration DEGILDIR - src/main/resources/db/
-- migration altinda durmamasinin sebebi bu; Flyway bunu hicbir zaman
-- calistirmaz.
--
-- CALISTIRMA:
--   docker exec -i aksa-mailer-postgres \
--     psql -U aksa_mailer -d aksa_mailer < local/ornek-veri/sprint-gecmisi.sql
--
-- TEKRAR CALISTIRILABILIR: her calismada once kendi yazdiklarini siler.
-- Elle olusturulmus belgelere DOKUNMAZ - yalnizca "[ORNEK]" onekli
-- basliklari hedefler.
--
-- ICERIK NEREDEN GELIYOR: sifirdan JSON yazmak yerine gercek bir belgenin
-- content'i kopyalaniyor. Uydurma bir govde yazsaydik sema degistiginde
-- burasi sessizce eskir ve "ornek veri bozuk" diye vakit kaybettirirdi.

BEGIN;

-- ── 1. Onceki ornek veriyi temizle ────────────────────────────────────
DELETE FROM mailer_document_versions
 WHERE document_id IN (SELECT id FROM mailer_documents WHERE title LIKE '[ÖRNEK]%');

DELETE FROM mailer_documents WHERE title LIKE '[ÖRNEK]%';

-- ── 2. Sprint gecmisini yaz ───────────────────────────────────────────
--
-- Doner: (takim, sicil) x (0,1,2) = 12 belge.
-- s.i = 0 en yeni sprint, 2 en eski. Tarihler bugunden GERIYE dogru
-- ilerliyor ki "son mailler" listesi gercekci sirada ciksin.
WITH kaynak AS (
    -- Gercek bir Kapanis belgesinin govdesi. Yoksa betik hicbir sey
    -- yazmaz (INSERT ... SELECT bos kumeyle sessizce biter) - bu da
    -- dogru davranis: uydurma icerikle devam etmektense hic yazmamak.
    SELECT content
      FROM mailer_documents
     WHERE template_type = 'KAPANIS'
       AND content -> 'header' ->> 'period' IS NOT NULL
       AND title NOT LIKE '[ÖRNEK]%'
     ORDER BY id DESC
     LIMIT 1
),
takimlar (team_id, sicil) AS (
    VALUES (1, '37547'),   -- Pelinsu Çevikel · RPA
           (2, '30816'),   -- Ece Sena Salan · İş Zekâsı
           (3, '29547'),   -- Gözde Son · Ürün Geliştirme
           (5, '35834')    -- Büşra Can · Dijital Uygulamalar
),
donemler AS (
    SELECT s.i,
           -- En yeni sprint 18.08.2026'da baslamis; her adim 14 gun geriye.
           (DATE '2026-08-18' - (s.i * 14))      AS bas,
           (DATE '2026-08-18' - (s.i * 14) + 13) AS bit,
           41 - s.i                              AS sprint_no
      FROM generate_series(0, 2) AS s(i)
)
INSERT INTO mailer_documents
    (team_id, template_type, title, content, status, current_version,
     created_by, updated_by, created_at, updated_at)
SELECT
    t.team_id,
    'KAPANIS',
    format('[ÖRNEK] Sprint %s Kapanışı', d.sprint_no),
    -- Iki alan degisiyor: donem (tarih araligi + sprint no) ve takim
    -- etiketi. Etiket duzeltilmezse butun takimlarda kaynak belgenin
    -- takim adi gorunurdu.
    jsonb_set(
        jsonb_set(
            k.content,
            '{header,period}',
            to_jsonb(to_char(d.bas, 'DD.MM.YYYY') || ' – ' ||
                     to_char(d.bit, 'DD.MM.YYYY') || ' · Sprint ' || d.sprint_no)
        ),
        '{header,teamLabel}',
        to_jsonb(mt.name)
    ),
    'DRAFT',
    1,
    t.sicil,
    t.sicil,
    -- Kayit zamani sprintin bitisinin ertesi gunu: liste "en yeni once"
    -- siraladigi icin sirali gorunsun.
    (d.bit + 1)::timestamptz,
    (d.bit + 1)::timestamptz
FROM takimlar t
CROSS JOIN donemler d
CROSS JOIN kaynak k
JOIN mail_teams mt ON mt.id = t.team_id;

-- ── 3. Her belgeye 1. surumu yaz ──────────────────────────────────────
-- Uygulama her belgeyi bir versiyon satiriyla dogurur (MailerDocumentService
-- .olustur). Ornek veri de ayni sekilde dogsun ki "Surum gecmisi" ekrani
-- bos gelip kafa karistirmasin.
INSERT INTO mailer_document_versions (document_id, version, content, created_by, created_at)
SELECT id, 1, content, created_by, created_at
  FROM mailer_documents
 WHERE title LIKE '[ÖRNEK]%';

COMMIT;

-- ── Ozet ──────────────────────────────────────────────────────────────
SELECT mt.name AS takim,
       d.title,
       d.content -> 'header' ->> 'period' AS donem,
       d.created_by AS sicil
  FROM mailer_documents d
  JOIN mail_teams mt ON mt.id = d.team_id
 WHERE d.title LIKE '[ÖRNEK]%'
 ORDER BY d.team_id, d.updated_at DESC;
