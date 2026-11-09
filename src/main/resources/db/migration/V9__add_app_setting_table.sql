CREATE TABLE app_setting
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- General
    app_name                        VARCHAR(150) NOT NULL DEFAULT 'Dokezo',
    support_email                   VARCHAR(255) NOT NULL DEFAULT 'support@dokezo.co.ke',
    support_phone                   VARCHAR(100) NOT NULL DEFAULT '+254 700 000 000',
    support_hours                   VARCHAR(255) NOT NULL DEFAULT 'Monday to Friday, 8:00 to 17:00 EAT',
    -- Matching and Alerts
    fuzzy_threshold                 INTEGER      NOT NULL DEFAULT 80,
    link_expiry_days                INTEGER      NOT NULL DEFAULT 30,
    manual_review                   BOOLEAN      NOT NULL DEFAULT TRUE,
    auto_approve_exact              BOOLEAN      NOT NULL DEFAULT FALSE,
    low_confidence                  INTEGER      NOT NULL DEFAULT 70,
    -- Security
    session_hours                   INTEGER      NOT NULL DEFAULT 8,
    max_attempts                    INTEGER      NOT NULL DEFAULT 5,
    lock_minutes                    INTEGER      NOT NULL DEFAULT 15,
    min_password                    INTEGER      NOT NULL DEFAULT 8,
    reset_hours                     INTEGER      NOT NULL DEFAULT 1,
    verify_hours                    INTEGER      NOT NULL DEFAULT 48,
    -- M-Pesa
    mpesa_env                       VARCHAR(50)  NOT NULL DEFAULT 'production',
    mpesa_shortcode                 VARCHAR(50)  NOT NULL DEFAULT '174379',
    mpesa_callback                  VARCHAR(500) NOT NULL DEFAULT 'https://dokezo.co.ke/webhooks/mpesa',
    mpesa_key                       TEXT,
    mpesa_secret                    TEXT,
    mpesa_passkey                   TEXT,
    -- SMS
    sms_provider                    VARCHAR(50)  NOT NULL DEFAULT 'africastalking',
    sms_username                    VARCHAR(150) NOT NULL DEFAULT 'dokezo',
    sms_sender                      VARCHAR(50)  NOT NULL DEFAULT 'DOKEZO',
    sms_key                         TEXT,
    -- Email (SMTP)
    smtp_host                       VARCHAR(255) NOT NULL DEFAULT 'smtp.example.com',
    smtp_port                       INTEGER      NOT NULL DEFAULT 587,
    smtp_security                   VARCHAR(50)  NOT NULL DEFAULT 'starttls',
    smtp_user                       VARCHAR(255) NOT NULL DEFAULT 'system@dokezo.co.ke',
    smtp_password                   TEXT,
    from_address                    VARCHAR(255) NOT NULL DEFAULT 'system@dokezo.co.ke',
    from_name                       VARCHAR(150) NOT NULL DEFAULT 'Dokezo',
    -- Data Retention
    retain_logs                     INTEGER      NOT NULL DEFAULT 12,
    retain_contact                  INTEGER      NOT NULL DEFAULT 24,
    retain_closed                   INTEGER      NOT NULL DEFAULT 12,
    -- Auditable
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active                       BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT
);

-- Enforce that at most one AppSetting record can be active at any time
CREATE UNIQUE INDEX uq_app_setting_single_active
    ON app_setting (is_active)
    WHERE is_active = TRUE;

-- Seed initial active settings record
INSERT INTO app_setting (id, is_active)
VALUES (gen_random_uuid(), TRUE);
