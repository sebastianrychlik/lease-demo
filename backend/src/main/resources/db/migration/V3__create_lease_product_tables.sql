-- M5.1.2 — Backend-driven Lease Product configuration.
--
-- The Lease Product is the sole authority for which options Angular may
-- render (currencies, terms, initial-payment/buyout ranges, lease types +
-- APR) AND for validating a submitted lease-quote request. Angular never
-- owns any of this business configuration.
--
-- `market` models a BUSINESS MARKET (e.g. PL, DE) and is intentionally
-- decoupled from UI language (see LeaseMarketResolver / M5.1.3).
--
-- Flyway runs this migration independently against both the PL and DE
-- databases before scripts/spock-configure.sh adds these tables to Spock
-- replication — so the seed rows below already exist, identically, in both
-- databases prior to replication being configured.
CREATE TABLE lease_product (
    id                                 UUID PRIMARY KEY,
    code                               VARCHAR(50)  NOT NULL,
    market                             VARCHAR(10)  NOT NULL,
    name                               VARCHAR(255) NOT NULL,
    enabled                            BOOLEAN      NOT NULL,

    initial_payment_min_percent        NUMERIC(5, 2) NOT NULL,
    initial_payment_max_percent        NUMERIC(5, 2) NOT NULL,
    initial_payment_default_percent    NUMERIC(5, 2) NOT NULL,
    initial_payment_step_percent       NUMERIC(5, 2) NOT NULL,

    buyout_min_percent                 NUMERIC(5, 2) NOT NULL,
    buyout_max_percent                 NUMERIC(5, 2) NOT NULL,
    buyout_default_percent             NUMERIC(5, 2) NOT NULL,
    buyout_step_percent                NUMERIC(5, 2) NOT NULL,

    default_currency                   VARCHAR(3)   NOT NULL,
    default_term_months                INTEGER      NOT NULL,
    default_lease_type                 VARCHAR(20)  NOT NULL,

    valid_from                         DATE,
    valid_to                           DATE,

    created_at                         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_lease_product_code UNIQUE (code)
);

-- Available-products lookup filters by market + enabled — index the pair.
CREATE INDEX idx_lease_product_market_enabled ON lease_product (market, enabled);

CREATE TABLE lease_product_currency (
    product_id UUID       NOT NULL,
    currency   VARCHAR(3) NOT NULL,

    PRIMARY KEY (product_id, currency),
    CONSTRAINT fk_lease_product_currency_product
        FOREIGN KEY (product_id) REFERENCES lease_product (id) ON DELETE CASCADE
);

CREATE TABLE lease_product_term (
    product_id  UUID    NOT NULL,
    term_months INTEGER NOT NULL,

    PRIMARY KEY (product_id, term_months),
    CONSTRAINT fk_lease_product_term_product
        FOREIGN KEY (product_id) REFERENCES lease_product (id) ON DELETE CASCADE
);

CREATE TABLE lease_product_lease_type (
    product_id          UUID          NOT NULL,
    lease_type          VARCHAR(20)   NOT NULL,
    annual_rate_percent NUMERIC(5, 2) NOT NULL,

    PRIMARY KEY (product_id, lease_type),
    CONSTRAINT fk_lease_product_lease_type_product
        FOREIGN KEY (product_id) REFERENCES lease_product (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Initial demo data — small, deterministic seed set.
-- ---------------------------------------------------------------------------

INSERT INTO lease_product (
    id, code, market, name, enabled,
    initial_payment_min_percent, initial_payment_max_percent,
    initial_payment_default_percent, initial_payment_step_percent,
    buyout_min_percent, buyout_max_percent,
    buyout_default_percent, buyout_step_percent,
    default_currency, default_term_months, default_lease_type
) VALUES (
    '11111111-1111-1111-1111-111111111111', 'STANDARD_CAR_PL', 'PL', 'Standard Car Leasing', TRUE,
    0, 45, 20, 1,
    1, 40, 15, 1,
    'EUR', 36, 'OPERATING'
);

INSERT INTO lease_product_currency (product_id, currency) VALUES
    ('11111111-1111-1111-1111-111111111111', 'PLN'),
    ('11111111-1111-1111-1111-111111111111', 'EUR');

INSERT INTO lease_product_term (product_id, term_months) VALUES
    ('11111111-1111-1111-1111-111111111111', 24),
    ('11111111-1111-1111-1111-111111111111', 36),
    ('11111111-1111-1111-1111-111111111111', 48),
    ('11111111-1111-1111-1111-111111111111', 60);

INSERT INTO lease_product_lease_type (product_id, lease_type, annual_rate_percent) VALUES
    ('11111111-1111-1111-1111-111111111111', 'OPERATING', 7.20),
    ('11111111-1111-1111-1111-111111111111', 'FINANCIAL', 6.90);

INSERT INTO lease_product (
    id, code, market, name, enabled,
    initial_payment_min_percent, initial_payment_max_percent,
    initial_payment_default_percent, initial_payment_step_percent,
    buyout_min_percent, buyout_max_percent,
    buyout_default_percent, buyout_step_percent,
    default_currency, default_term_months, default_lease_type
) VALUES (
    '22222222-2222-2222-2222-222222222222', 'STANDARD_CAR_DE', 'DE', 'Standard Car Leasing (DE)', TRUE,
    0, 45, 20, 1,
    1, 40, 15, 1,
    'EUR', 36, 'OPERATING'
);

INSERT INTO lease_product_currency (product_id, currency) VALUES
    ('22222222-2222-2222-2222-222222222222', 'EUR');

INSERT INTO lease_product_term (product_id, term_months) VALUES
    ('22222222-2222-2222-2222-222222222222', 24),
    ('22222222-2222-2222-2222-222222222222', 36),
    ('22222222-2222-2222-2222-222222222222', 48),
    ('22222222-2222-2222-2222-222222222222', 60);

INSERT INTO lease_product_lease_type (product_id, lease_type, annual_rate_percent) VALUES
    ('22222222-2222-2222-2222-222222222222', 'OPERATING', 7.20),
    ('22222222-2222-2222-2222-222222222222', 'FINANCIAL', 6.90);
