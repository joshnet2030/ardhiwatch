ALTER TABLE app_user
    ADD email_verification_token VARCHAR(255);

ALTER TABLE app_user
    ADD email_verification_token_expires_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE app_user
    ADD email_verified_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE app_user
    ADD is_email_verified BOOLEAN;

ALTER TABLE app_user
    ALTER COLUMN is_email_verified SET NOT NULL;