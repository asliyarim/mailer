-- mailer_download_logs.user_email -> created_by
--
-- Sutun e-posta bekliyordu ama odyssey-auth'un token'inda e-posta claim'i YOK:
-- sicil, role, fullName, department, teamId(s) var. Oraya sicil yazmak,
-- tabloyu sonradan okuyani yanilturdi ("neden e-posta yerine numara var?").
--
-- created_by adi diger tablolarla da tutarli: mailer_documents ve
-- mailer_document_versions zaten sicili bu adla tutuyor.
--
-- V1 dondurulmus oldugu icin degisiklik yeni bir migrasyonla geliyor.

ALTER TABLE mailer_download_logs RENAME COLUMN user_email TO created_by;

ALTER TABLE mailer_download_logs ALTER COLUMN created_by TYPE VARCHAR(32);

ALTER TABLE mailer_download_logs ALTER COLUMN created_by SET NOT NULL;
