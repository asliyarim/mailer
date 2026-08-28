-- Kalan iki takim.
--
-- V3 yazilirken bu ikisinin ADI bilinmiyordu: odyssey-auth'ta yetki kaydi
-- vardi (Busra Can 5,7 - Zuleyha Kades Tanriverdi 5,7,8) ama hicbir
-- kullanicinin ANA takimi olmadigi icin department alanindan cikarilamiyordu.
-- Adlar Capacity Planner'in teams tablosundan geldi.
--
-- Kimlikler yine elle veriliyor ve odyssey-auth'takiyle AYNI - token'daki
-- teamIds dogrudan mail_teams.id'ye karsilik gelsin diye (bkz. V3__takimlar.sql).

INSERT INTO mail_teams (id, code, name, theme_key, active)
OVERRIDING SYSTEM VALUE
VALUES
    (7, 'CBS',   'Konum Tabanlı Ürün Geliştirme Ekibi (CBS)', 'cbs',   TRUE),
    (8, 'MOBIL', 'Mobil Uygulamalar Ekibi',                   'mobil', TRUE);

SELECT setval(pg_get_serial_sequence('mail_teams', 'id'),
              (SELECT max(id) FROM mail_teams));
