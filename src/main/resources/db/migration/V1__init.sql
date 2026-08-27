-- Aksa Mailer ilk semasi.
--
-- Ayrim (docs/BRIEF.md, Kural 4): ICERIK burada yasar ve kullanici doldurur;
-- TEMA kodda yasar (render/theme/*.java) ve gelistirici ekler. Bu sinir
-- bulanirsa sablon yonetim paneli yazmak gerekir, o kapsam disi.
--
-- Bu dosya DONDURULMUS sayilir: uzerine yazilmaz, degisiklik yeni bir
-- V2__*.sql ile gelir. Flyway calismis bir migrasyonun checksum'i degisirse
-- uygulama acilista hata verir.

-- Mail uretebilen takimlar. Ince ve okuma agirlikli tablo.
CREATE TABLE mail_teams (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(64)  NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    -- Kodda kayitli bir temayi isaret eder: 'rpa', 'is-zekasi'.
    -- Yabanci anahtar YOK - karsiligi veritabaninda degil, ThemeRegistry'de.
    theme_key  VARCHAR(64)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Bir mail belgesi (taslak veya gonderilmis).
CREATE TABLE mailer_documents (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    team_id          BIGINT       NOT NULL REFERENCES mail_teams (id),
    template_type    VARCHAR(32)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    subject          VARCHAR(500),
    -- Sema docs/BRIEF.md §6. Satirlar dizi degil NESNE tutar (indeks kaymasin),
    -- sayaclar SAKLANMAZ (satir sayisindan hesaplanir).
    content          JSONB        NOT NULL,
    status           VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    current_version  INTEGER      NOT NULL DEFAULT 1,
    created_by       VARCHAR(32)  NOT NULL,
    updated_by       VARCHAR(32)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT mailer_documents_template_type_check
        CHECK (template_type IN ('KAPANIS', 'PLANLAMA', 'YONETICI_OZETI')),
    CONSTRAINT mailer_documents_status_check
        CHECK (status IN ('DRAFT', 'FINAL'))
);

-- Her kayit yeni bir versiyon satiri yazar; geri alma bu tablodan okur.
CREATE TABLE mailer_document_versions (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id  BIGINT      NOT NULL REFERENCES mailer_documents (id) ON DELETE CASCADE,
    version      INTEGER     NOT NULL,
    content      JSONB       NOT NULL,
    created_by   VARCHAR(32) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT mailer_document_versions_unique UNIQUE (document_id, version)
);

-- Kim, hangi maili, hangi formatta indirdi. Kullanim olcumu icin.
CREATE TABLE mailer_download_logs (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id  BIGINT       NOT NULL REFERENCES mailer_documents (id) ON DELETE CASCADE,
    team_id      BIGINT       NOT NULL REFERENCES mail_teams (id),
    format       VARCHAR(16)  NOT NULL,
    user_email   VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT mailer_download_logs_format_check CHECK (format IN ('EML', 'PDF'))
);

CREATE INDEX idx_mailer_documents_team ON mailer_documents (team_id);
CREATE INDEX idx_mailer_document_versions_document ON mailer_document_versions (document_id);
CREATE INDEX idx_mailer_download_logs_document ON mailer_download_logs (document_id);

-- Gelistirme verisi. Tema anahtarlarinin kodda karsiligi olmali
-- (render/theme/RpaTheme.java, IsZekasiTheme.java).
INSERT INTO mail_teams (code, name, theme_key, active) VALUES
    ('RPA',       'RPA Takımı',        'rpa',       TRUE),
    ('IS_ZEKASI', 'İş Zekâsı Takımı',  'is-zekasi', TRUE);
