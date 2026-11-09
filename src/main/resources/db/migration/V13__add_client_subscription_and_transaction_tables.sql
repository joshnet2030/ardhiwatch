CREATE TABLE client_subscription_plan
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_user_id                  UUID           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    subscription_plan_id            UUID           NOT NULL REFERENCES subscription_plan (id),
    start_date                      DATE           NOT NULL,
    end_date                        DATE           NOT NULL,
    amount                          NUMERIC(12, 2) NOT NULL,
    paid_amount                     NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    balance                         NUMERIC(12, 2) NOT NULL,
    status                          VARCHAR(50)    NOT NULL DEFAULT 'PENDING_PAYMENT',
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active                       BOOLEAN        NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT
);

CREATE INDEX idx_client_sub_plan_client ON client_subscription_plan (client_user_id, is_active, end_date DESC);

CREATE TABLE payment_transaction
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_user_id                  UUID           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    client_subscription_plan_id     UUID REFERENCES client_subscription_plan (id) ON DELETE SET NULL,
    subscription_plan_id            UUID REFERENCES subscription_plan (id) ON DELETE SET NULL,
    amount                          NUMERIC(12, 2) NOT NULL,
    payment_method                  VARCHAR(50)    NOT NULL DEFAULT 'MPESA',
    receipt_number                  VARCHAR(100),
    paid_from                       VARCHAR(100),
    reference                       VARCHAR(100),
    status                          VARCHAR(50)    NOT NULL DEFAULT 'COMPLETED',
    completed_at                    TIMESTAMP WITHOUT TIME ZONE,
    admin_notes                     TEXT,
    created_by_app_session_id       UUID,
    created_at                      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by_app_session_id UUID,
    last_modified_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active                       BOOLEAN        NOT NULL DEFAULT TRUE,
    deactivation_memo               TEXT
);

CREATE INDEX idx_payment_tx_client ON payment_transaction (client_user_id, created_at DESC);
CREATE INDEX idx_payment_tx_status ON payment_transaction (status, created_at DESC);
