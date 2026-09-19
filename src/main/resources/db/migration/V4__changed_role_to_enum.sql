ALTER TABLE app_user
    ADD role VARCHAR(255);

ALTER TABLE app_user
    DROP COLUMN is_admin;

ALTER TABLE app_user
    DROP COLUMN is_clinet;