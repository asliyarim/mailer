-- Postgres, /docker-entrypoint-initdb.d altindaki script'leri YALNIZCA volume
-- bosken (ilk acilista) calistirir. Sonraki acilislarda hic bakmaz.
--
-- POSTGRES_DB ile yalnizca bir veritabani yaratilabiliyor (aksa_mailer);
-- odyssey-auth'un kendi veritabanina da ihtiyaci var. Ikisi ayni sunucuda
-- ama AYRI veritabani - birbirlerinin tablolarini gormezler.
--
-- Tablolari Flyway kuracak, kullanicilari odyssey-auth'un UserSeeder'i
-- yazacak (tablo bosken calisir). Yani elle veri tasimaya gerek yok.

CREATE DATABASE odyssey_auth;
