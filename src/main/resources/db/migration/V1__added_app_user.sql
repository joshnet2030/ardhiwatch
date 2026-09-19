CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE app_user
(
    id               UUID    NOT NULL,
    created_by       VARCHAR(255),
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    last_modified_by VARCHAR(255),
    last_modified_at TIMESTAMP WITHOUT TIME ZONE,
    is_active        BOOLEAN NOT NULL,
    email            VARCHAR(255),
    full_name        VARCHAR(255),
    phone            VARCHAR(255),
    password_hash    VARCHAR(255),
    is_admin         BOOLEAN,
    is_clinet        BOOLEAN,
    last_login_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_app_user PRIMARY KEY (id)
);

CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);