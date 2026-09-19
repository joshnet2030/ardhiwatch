ALTER TABLE app_user
    ADD password_reset_token VARCHAR(255);

ALTER TABLE app_user
    ADD password_reset_token_sent_at TIMESTAMP WITHOUT TIME ZONE;