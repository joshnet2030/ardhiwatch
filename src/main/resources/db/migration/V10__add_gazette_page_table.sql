CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE gazette_page
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gazette_issue_id                UUID         NOT NULL REFERENCES gazette_issue (id) ON DELETE CASCADE,
    page_number                     INTEGER      NOT NULL,
    pdf_system_file_id              UUID REFERENCES system_file (id),
    ocr_text                        TEXT,
    ocr_text_tsv                    TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', coalesce(ocr_text, ''))) STORED,
    status                          VARCHAR(50)  NOT NULL DEFAULT 'OCR_DONE',
    failure_message                 TEXT,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active                       BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT,
    CONSTRAINT uq_gazette_page_issue_page_number UNIQUE (gazette_issue_id, page_number)
);

CREATE INDEX idx_gazette_page_issue_id ON gazette_page (gazette_issue_id, page_number);

CREATE INDEX idx_gazette_page_ocr_text_tsv ON gazette_page USING GIN (ocr_text_tsv);

CREATE INDEX idx_gazette_page_ocr_text_trgm ON gazette_page USING GIN (ocr_text gin_trgm_ops);
