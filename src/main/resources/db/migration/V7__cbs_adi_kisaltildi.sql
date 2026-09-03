-- CBS takiminin adi kisaltiliyor (yonetici istegi):
--   "Konum Tabanli Urun Gelistirme Ekibi (CBS)"  ->  "CBS"
--
-- Ad UC yerde gorunuyor ve ucu de bu satirdan besleniyor: takim secici,
-- yeni belgenin header.teamLabel varsayilani ve documents/recent'in
-- dondurdugu teamName. Ayrica baslik TURETMESI de buradan geliyor -
-- yeni belgeler "CBS SPRINT BILGILENDIRME" basligiyla dogacak.
--
-- MEVCUT belgelere DOKUNULMUYOR. Onlarin header.teamLabel'i eski adi
-- tasiyor ama o alan KULLANICI ICERIGI: belge kaydedildiginde o anki
-- metin yazildi, kullanici degistirmis de olabilir. Toplu guncelleme
-- kullanicinin elle yazdigi etiketleri de ezerdi.

UPDATE mail_teams
   SET name = 'CBS'
 WHERE id = 7
   AND name = 'Konum Tabanlı Ürün Geliştirme Ekibi (CBS)';
