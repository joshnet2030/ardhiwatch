ALTER TABLE client_keyword_match
    ADD COLUMN IF NOT EXISTS highlighted_pdf_system_file_id UUID REFERENCES system_file (id);
