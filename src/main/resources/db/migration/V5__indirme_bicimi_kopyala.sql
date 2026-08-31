-- "Outlook İçin Kopyala" da bir dagitim yolu: kullanici maili panoya alip
-- kendi Outlook taslagina yapistiriyor. Olcumde EML ve PDF ile ayni yerde
-- durmali, yoksa "kac mail uretildi" sorusunun cevabi eksik cikar.
--
-- CHECK kisiti DownloadFormat enum'uyla AYNI degerleri tasimak zorunda.
-- Enum'a deger eklenip bu migrasyon yazilmazsa uygulama calisir ama kayit
-- atarken kisit ihlaline duser - ve bunu ancak kullanici indirmeye
-- calistiginda goruruz.

ALTER TABLE mailer_download_logs
    DROP CONSTRAINT IF EXISTS mailer_download_logs_format_check;

ALTER TABLE mailer_download_logs
    ADD CONSTRAINT mailer_download_logs_format_check
        CHECK (format IN ('EML', 'PDF', 'KOPYALA'));
