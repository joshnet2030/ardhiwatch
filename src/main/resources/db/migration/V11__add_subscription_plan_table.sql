CREATE TABLE subscription_plan
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                            VARCHAR(120)   NOT NULL,
    description                     TEXT,
    price_kes                       NUMERIC(12, 2) NOT NULL,
    billing_period                  VARCHAR(50)    NOT NULL DEFAULT 'Annual',
    keyword_limit                   INTEGER        NOT NULL DEFAULT 1,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active                       BOOLEAN        NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT
);

INSERT INTO subscription_plan (name, description, price_kes, billing_period, keyword_limit, is_active, created_at, last_modified_at)
VALUES ('Basic', 'Ideal for individual homeowners tracking a single title deed or name.', 1500.00, 'Annual', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Landlord Pro', 'Designed for property owners managing multiple plots or family parcels.', 4500.00, 'Annual', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Portfolio', 'Comprehensive coverage for real estate investors, SACCOs, and law firms.', 12000.00, 'Annual', 20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Starter (2025)', 'Legacy introductory plan.', 900.00, 'Annual', 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
