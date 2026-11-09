CREATE TABLE contact_message
(
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name                    VARCHAR(150)              NOT NULL,
    email                        VARCHAR(255)              NOT NULL,
    phone                        VARCHAR(50),
    subject                      VARCHAR(200)              NOT NULL,
    message                      TEXT                      NOT NULL,
    is_read                      BOOLEAN          DEFAULT FALSE,
    created_by_app_session_id    UUID,
    created_at                   TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at             TIMESTAMP,
    is_active                    BOOLEAN          DEFAULT TRUE,
    deactivation_memo            TEXT
);
