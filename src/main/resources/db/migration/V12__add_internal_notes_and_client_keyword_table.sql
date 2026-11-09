ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS internal_notes TEXT;

CREATE TABLE client_keyword
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_user_id                  UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    keyword                         VARCHAR(255) NOT NULL,
    keyword_type                    VARCHAR(50)  NOT NULL DEFAULT 'TITLE',
    match_precision                 VARCHAR(30)  NOT NULL DEFAULT 'EXACT',
    notes                           VARCHAR(255),
    is_paused                       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active                       BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT
);

CREATE INDEX idx_client_keyword_client_user_id ON client_keyword (client_user_id, is_active);
