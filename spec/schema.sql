-- =========================================================
-- Kenya Gazette Land Alert System — Schema v1
-- =========================================================

-- ---------- USERS (single auth table for both admins and clients) ----------
-- A person can be an admin, a client, or both (e.g. the business owner who
-- also monitors their own land).
CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       full_name       TEXT NOT NULL,
                       email           TEXT UNIQUE,
                       phone           TEXT UNIQUE NOT NULL,      -- E.164 format, e.g. +2547...
                       password_hash   TEXT NOT NULL,
                       is_admin        BOOLEAN NOT NULL DEFAULT false,
                       is_client       BOOLEAN NOT NULL DEFAULT false,
                       admin_role      TEXT,                      -- 'admin' | 'superadmin', only if is_admin
                       status          TEXT NOT NULL DEFAULT 'active', -- active | suspended
                       last_login_at   TIMESTAMPTZ,
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                       CONSTRAINT chk_users_has_role CHECK (is_admin OR is_client)
);

-- ---------- CLIENTS ----------
-- Client-specific profile data, one-to-one with a user that has is_client = true.
-- The 1-1 relationship (and keeping users.is_client in sync) is enforced in app logic.
CREATE TABLE clients (
                         id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                         status          TEXT NOT NULL DEFAULT 'active', -- active | suspended (client account specifically)
                         created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- SUBSCRIPTION PLANS ----------
CREATE TABLE subscription_plans (
                                    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    name            TEXT NOT NULL,             -- e.g. "Basic", "Landlord Pro"
                                    price_kes       NUMERIC(10,2) NOT NULL,
                                    billing_period  TEXT NOT NULL,             -- 'monthly' | 'annual'
                                    max_keywords    INT NOT NULL DEFAULT 1,
                                    is_active       BOOLEAN NOT NULL DEFAULT true,
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- SUBSCRIPTIONS ----------
CREATE TABLE subscriptions (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
                               plan_id         UUID NOT NULL REFERENCES subscription_plans(id),
                               status          TEXT NOT NULL DEFAULT 'pending_payment',
    -- pending_payment | active | expired | cancelled
                               start_date      DATE,
                               end_date        DATE,
                               created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_client ON subscriptions(client_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

-- ---------- PAYMENTS ----------
CREATE TABLE payments (
                          id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          subscription_id         UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
                          amount_kes              NUMERIC(10,2) NOT NULL,
                          method                  TEXT NOT NULL,   -- 'mpesa' | 'card'
                          mpesa_receipt_number    TEXT,
                          status                  TEXT NOT NULL DEFAULT 'pending', -- pending | completed | failed
                          paid_at                 TIMESTAMPTZ,
                          created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_subscription ON payments(subscription_id);

-- ---------- KEYWORDS ----------
CREATE TABLE keywords (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
                          keyword_text    TEXT NOT NULL,
                          keyword_type    TEXT NOT NULL,   -- 'name' | 'lr_number' | 'id_number' | 'plot_number' | 'other'
                          match_mode      TEXT NOT NULL DEFAULT 'fuzzy', -- 'exact' | 'fuzzy'
                          is_active       BOOLEAN NOT NULL DEFAULT true,
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_keywords_client ON keywords(client_id);
CREATE INDEX idx_keywords_active ON keywords(is_active) WHERE is_active = true;
-- trigram index for fuzzy matching (requires pg_trgm extension)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_keywords_trgm ON keywords USING gin (keyword_text gin_trgm_ops);

-- ---------- GAZETTE ISSUES ----------
CREATE TABLE gazette_issues (
                                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                issue_number    TEXT NOT NULL,
                                volume          TEXT,
                                publication_date DATE NOT NULL,
                                original_file_path TEXT NOT NULL,   -- full uploaded PDF
                                page_count      INT,
                                status          TEXT NOT NULL DEFAULT 'uploaded',
    -- uploaded | splitting | ocr_processing | ocr_done
    -- | matching | matched | reviewed | notified | failed
                                uploaded_by     UUID REFERENCES users(id),  -- must be a user with is_admin = true (enforced in app layer)
                                uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_gazette_issues_date ON gazette_issues(publication_date);
CREATE INDEX idx_gazette_issues_status ON gazette_issues(status);

-- ---------- GAZETTE PAGES ----------
CREATE TABLE gazette_pages (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               issue_id        UUID NOT NULL REFERENCES gazette_issues(id) ON DELETE CASCADE,
                               page_number     INT NOT NULL,
                               page_pdf_path   TEXT NOT NULL,      -- single-page PDF, unmodified
                               page_image_path TEXT,               -- rasterized image used for OCR
                               ocr_text        TEXT,               -- full extracted text, raw
                               ocr_tsv         TSVECTOR,           -- full-text search index
                               ocr_status      TEXT NOT NULL DEFAULT 'pending', -- pending | done | failed
                               ocr_confidence  NUMERIC(5,2),       -- average OCR confidence for the page
                               created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                               UNIQUE (issue_id, page_number)
);

CREATE INDEX idx_gazette_pages_issue ON gazette_pages(issue_id);
CREATE INDEX idx_gazette_pages_tsv ON gazette_pages USING gin (ocr_tsv);


CREATE INDEX idx_ocr_words_page ON ocr_words(page_id);
-- trigram index to locate word matches fast for highlighting
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_ocr_words_trgm ON ocr_words USING gin (word_text gin_trgm_ops);

-- ---------- MATCHES ----------
CREATE TABLE matches (
                         id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         keyword_id          UUID NOT NULL REFERENCES keywords(id) ON DELETE CASCADE,
                         page_id             UUID NOT NULL REFERENCES gazette_pages(id) ON DELETE CASCADE,
                         matched_text        TEXT NOT NULL,
                         match_score         NUMERIC(5,2),        -- similarity score, 0-100
                         status              TEXT NOT NULL DEFAULT 'pending_review',
    -- pending_review | approved | rejected | auto_approved
                         highlighted_pdf_path TEXT,
                         reviewed_by         UUID REFERENCES users(id),  -- must be a user with is_admin = true (enforced in app layer)
                         reviewed_at         TIMESTAMPTZ,
                         created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_matches_keyword ON matches(keyword_id);
CREATE INDEX idx_matches_page ON matches(page_id);
CREATE INDEX idx_matches_status ON matches(status);

-- ---------- NOTIFICATIONS ----------
CREATE TABLE notifications (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               match_id        UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
                               client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
                               channel         TEXT NOT NULL,     -- 'email' | 'sms'
                               status          TEXT NOT NULL DEFAULT 'pending', -- pending | sent | delivered | failed
                               link_token      TEXT UNIQUE,       -- secure token for SMS "view page" link
                               link_expires_at TIMESTAMPTZ,
                               sent_at         TIMESTAMPTZ,
                               created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_client ON notifications(client_id);
CREATE INDEX idx_notifications_match ON notifications(match_id);

-- ---------- Auto-maintain ocr_tsv ----------
CREATE FUNCTION gazette_pages_tsv_update() RETURNS trigger AS $$
BEGIN
    NEW.ocr_tsv := to_tsvector('english', coalesce(NEW.ocr_text, ''));
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_gazette_pages_tsv
    BEFORE INSERT OR UPDATE OF ocr_text ON gazette_pages
    FOR EACH ROW EXECUTE FUNCTION gazette_pages_tsv_update();