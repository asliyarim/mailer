-- Dorduncu mail tipi: Toplanti Ciktilari.
--
-- Yonetici Ozeti'yle ortusuyor ama ayri duruyor: Yonetici Ozeti bir TAKIMIN
-- SPRINT'ine bagli, bu ise HERHANGI BIR TOPLANTIYA ve takimlar ustu.
-- Birlestirilseydi her iki durumda da yarisi bos kalan bir form cikardi.
--
-- CHECK kisiti TemplateType enum'uyla AYNI degerleri tasimak zorunda.
-- Enum'a deger eklenip bu migrasyon yazilmazsa uygulama derlenir ama belge
-- olusturulurken kisit ihlaline duser - ve bunu ancak kullanici o tipi
-- secmeye calistiginda goruruz.
--
-- Icerikteki toplanti kutusu uc yeni alan tasiyor (title, moderator,
-- attendees). Onlar icin MIGRASYON GEREKMIYOR: govde jsonb, sema surumu
-- degismiyor, eski kayitlarda alanlar yok ve null okunuyor.

ALTER TABLE mailer_documents
    DROP CONSTRAINT IF EXISTS mailer_documents_template_type_check;

ALTER TABLE mailer_documents
    ADD CONSTRAINT mailer_documents_template_type_check
        CHECK (template_type IN ('KAPANIS', 'PLANLAMA', 'YONETICI_OZETI', 'TOPLANTI_CIKTILARI'));
