CREATE TABLE app_session
(
    id                              UUID    NOT NULL,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE,
    is_active                       BOOLEAN NOT NULL,
    deactivation_memo               TEXT,
    user_agent                      VARCHAR(255),
    ip_address                      VARCHAR(255),
    app_user_id                     UUID,
    CONSTRAINT pk_app_session PRIMARY KEY (id)
);

ALTER TABLE app_user
    ADD created_by_app_session_id UUID;

ALTER TABLE app_user
    ADD deactivation_memo TEXT;

ALTER TABLE app_user
    ADD last_modified_by_app_session_id UUID;

ALTER TABLE app_session
    ADD CONSTRAINT FK_APP_SESSION_ON_APP_USER FOREIGN KEY (app_user_id) REFERENCES app_user (id);

ALTER TABLE app_user
    DROP COLUMN created_by;

ALTER TABLE app_user
    DROP COLUMN last_modified_by;