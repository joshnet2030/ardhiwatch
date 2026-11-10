CREATE TABLE client_keyword_match
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_user_id                  UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    client_keyword_id               UUID         NOT NULL REFERENCES client_keyword (id) ON DELETE CASCADE,
    gazette_issue_id                UUID         NOT NULL REFERENCES gazette_issue (id) ON DELETE CASCADE,
    gazette_page_id                 UUID         NOT NULL REFERENCES gazette_page (id) ON DELETE CASCADE,
    keyword_text                    VARCHAR(255) NOT NULL,
    matched_text                    TEXT         NOT NULL,
    snippet_text                    TEXT,
    match_precision                 VARCHAR(50)  NOT NULL DEFAULT 'EXACT',
    similarity_score                INTEGER      NOT NULL DEFAULT 100,
    status                          VARCHAR(50)  NOT NULL DEFAULT 'PENDING_REVIEW',
    rejection_reason                VARCHAR(100),
    review_notes                    TEXT,
    reviewed_at                     TIMESTAMP WITHOUT TIME ZONE,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active                       BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT,
    CONSTRAINT uq_client_keyword_page_match UNIQUE (client_keyword_id, gazette_page_id)
);

CREATE INDEX idx_client_keyword_match_client ON client_keyword_match (client_user_id, created_at DESC);
CREATE INDEX idx_client_keyword_match_status ON client_keyword_match (status, created_at DESC);
CREATE INDEX idx_client_keyword_match_issue ON client_keyword_match (gazette_issue_id, created_at DESC);

CREATE TABLE client_notification
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_user_id                  UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    client_keyword_match_id         UUID REFERENCES client_keyword_match (id) ON DELETE CASCADE,
    channel                         VARCHAR(50)  NOT NULL,
    recipient                       VARCHAR(255) NOT NULL,
    subject                         VARCHAR(255),
    message_body                    TEXT,
    details                         VARCHAR(500),
    status                          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    sent_at                         TIMESTAMP WITHOUT TIME ZONE,
    delivered_at                    TIMESTAMP WITHOUT TIME ZONE,
    failure_message                 TEXT,
    attempt_count                   INTEGER      NOT NULL DEFAULT 0,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active                       BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT
);

CREATE INDEX idx_client_notification_client ON client_notification (client_user_id, created_at DESC);
CREATE INDEX idx_client_notification_match ON client_notification (client_keyword_match_id, created_at DESC);
CREATE INDEX idx_client_notification_status ON client_notification (status, created_at ASC);
