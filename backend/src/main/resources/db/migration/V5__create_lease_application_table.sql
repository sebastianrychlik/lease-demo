-- M5.3 — Lease Application persistence + credit scoring.
--
-- LeaseApplication is a durable BUSINESS SNAPSHOT (see LeaseApplication
-- entity javadoc): every quote/insurance/product value below is captured at
-- submission time and must never be recomputed from a later-modified
-- LeaseProduct. This table is the durable event SOURCE for the M5.5 Kafka
-- milestone — no outbox/producer is introduced here.
--
-- Flyway runs this migration independently against both the PL and DE
-- databases before scripts/spock-configure.sh adds this table to Spock
-- replication (see infrastructure/scripts/spock-configure.sh).
CREATE TABLE lease_application (
    id                          UUID PRIMARY KEY,
    customer_id                 UUID NOT NULL,

    product_code                VARCHAR(50)   NOT NULL,
    product_name_snapshot       VARCHAR(255)  NOT NULL,

    status                      VARCHAR(20)   NOT NULL,
    credit_score                INTEGER       NOT NULL,

    monthly_net_income          NUMERIC(14, 2) NOT NULL,
    monthly_obligations         NUMERIC(14, 2) NOT NULL,

    vehicle_price_original      NUMERIC(14, 2) NOT NULL,
    vehicle_price_currency      VARCHAR(3)     NOT NULL,

    settlement_currency         VARCHAR(3)     NOT NULL,

    exchange_rate               NUMERIC(14, 6) NOT NULL,
    exchange_rate_date          DATE,
    vehicle_price_settlement    NUMERIC(14, 2) NOT NULL,

    term_months                 INTEGER        NOT NULL,

    initial_payment_percent     NUMERIC(5, 2)  NOT NULL,
    initial_payment             NUMERIC(14, 2) NOT NULL,

    buyout_percent              NUMERIC(5, 2)  NOT NULL,
    buyout                      NUMERIC(14, 2) NOT NULL,

    lease_type                  VARCHAR(20)    NOT NULL,
    annual_rate_percent         NUMERIC(5, 2)  NOT NULL,

    financed_amount             NUMERIC(14, 2) NOT NULL,
    monthly_payment             NUMERIC(14, 2) NOT NULL,
    total_lease_cost            NUMERIC(14, 2) NOT NULL,
    estimated_vat               NUMERIC(14, 2) NOT NULL,

    insurance_monthly_premium   NUMERIC(14, 2) NOT NULL,
    insurance_configuration     JSONB          NOT NULL,

    submitted_at                TIMESTAMPTZ    NOT NULL,
    decided_at                  TIMESTAMPTZ,

    CONSTRAINT fk_lease_application_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id)
);

-- M5.6 Admin Dashboard will list applications ordered by submission time and
-- filter by customer — index both access patterns now.
CREATE INDEX idx_lease_application_customer_id ON lease_application (customer_id);
CREATE INDEX idx_lease_application_submitted_at ON lease_application (submitted_at DESC);
