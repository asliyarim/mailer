-- Odyssey takimlarinin tamami.
--
-- KIMLIKLER ELLE VERILIYOR ve odyssey-auth'un user_team_ids tablosundakiyle
-- AYNI olmak zorunda. Sebebi: token icinde teamIds claim'i o kimlikleri
-- tasiyor; yetki kontrolu "token'daki teamIds, mail_teams.id'yi icersin"
-- diye calisiyor. Kimlikler kaysaydi yanlis takimin mailleri gorunurdu.
--
-- 1 ve 2 V1'de tohumlanmisti ve zaten dogru kimliklerde:
--   1 RPA Ekibi        2 Is Zekasi Ekibi
-- Asagidakiler eksik olanlar. Adlar odyssey_auth.users.department'tan.
--
-- 7 ve 8 numarali takimlar odyssey-auth'ta VAR (Busra Can ve Zuleyha Kades
-- Tanriverdi'nin yetkisi bulunuyor) ama hicbir kullanicinin ana takimi
-- olmadigi icin ADLARI BILINMIYOR. Ogrenilince bir V4 migrasyonuyla eklenecek;
-- o zamana kadar bu takimlara yetkisi olan kullanici mail uretemez.

INSERT INTO mail_teams (id, code, name, theme_key, active)
OVERRIDING SYSTEM VALUE
VALUES
    (3, 'URUN_GELISTIRME', 'Ürün Geliştirme Takımı',        'urun-gelistirme',     TRUE),
    (4, 'YAPAY_ZEKA',      'Yapay Zeka Takımı',             'yapay-zeka',          TRUE),
    (5, 'DIJITAL',         'Dijital Uygulamalar Takımı',    'dijital-uygulamalar', TRUE),
    (6, 'DOKUMAN',         'Doküman ve Süreç Yönetimi Takımı', 'dokuman',          TRUE);

-- IDENTITY sayaci elle verilen kimliklerden haberdar degil; bir sonraki
-- otomatik kimlik 7'den baslasin diye ileri sariyoruz. Yapilmazsa sonraki
-- INSERT id=1 deneyip birincil anahtar cakismasi verirdi.
SELECT setval(pg_get_serial_sequence('mail_teams', 'id'),
              (SELECT max(id) FROM mail_teams));
