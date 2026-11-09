CREATE TABLE system_file
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_file_name              VARCHAR(255) NOT NULL,
    file_type                       VARCHAR(100),
    size_in_bytes                   BIGINT,
    generated_file_name             VARCHAR(255) NOT NULL,
    file_path                       TEXT         NOT NULL,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE,
    is_active                       BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT
);

CREATE TABLE gazette_issue
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    volume_number                   INTEGER      NOT NULL,
    issue_number                    INTEGER      NOT NULL,
    publication_date                DATE         NOT NULL,
    status                          VARCHAR(50)  NOT NULL DEFAULT 'UPLOADED',
    pdf_system_file_id              UUID REFERENCES system_file (id),
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE,
    is_active                       BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT
);
