CREATE TABLE imail
(
    id                              UUID    NOT NULL,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE,
    is_active                       BOOLEAN NOT NULL,
    deactivation_memo               TEXT,
    from_email                      VARCHAR(255),
    to_email                        VARCHAR(255),
    subject                         VARCHAR(255),
    body                            TEXT,
    was_sent_successfully           BOOLEAN NOT NULL,
    last_sending_attempt_at         TIMESTAMP WITHOUT TIME ZONE,
    sending_attempt_count           INTEGER NOT NULL,
    failure_message                 TEXT,
    CONSTRAINT pk_imail PRIMARY KEY (id)
);