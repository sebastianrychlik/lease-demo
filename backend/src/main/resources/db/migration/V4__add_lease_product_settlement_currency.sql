-- M5.1.3.2 — Lease Settlement Currency Foundation.
--
-- Introduces settlement_currency directly on lease_product: the currency in
-- which the product's lease amounts are calculated/settled. This is
-- distinct from lease_product_currency (the accepted VEHICLE PRICE
-- currencies) and from UI language (unrelated concern, see Transloco).
--
-- IMPORTANT — Spock replication safety:
-- lease_product is already in the Spock "default" replication set. This
-- migration both alters the schema of a replicated table AND backfills a
-- new NOT NULL column for existing rows. Flyway schema changes are NOT
-- replicated by Spock, so scripts/database-migrate.sh must run this
-- migration against BOTH the PL and DE databases while replication
-- subscriptions are temporarily disabled, and only re-enable them once
-- both databases have this column. See scripts/database-migrate.sh for the
-- orchestration.
--
-- Nullable-then-backfill-then-NOT NULL pattern, in one migration, since
-- both databases receive this migration independently and identically
-- (Flyway remains the schema authority — Spock does not replicate DDL).
ALTER TABLE lease_product
    ADD COLUMN settlement_currency VARCHAR(3);

-- Deterministic product codes seeded in V3 — backfill their settlement
-- currency per the M5.1.3.2 product configuration:
--   STANDARD_CAR_PL -> PLN
--   STANDARD_CAR_DE -> EUR
UPDATE lease_product SET settlement_currency = 'PLN' WHERE code = 'STANDARD_CAR_PL';
UPDATE lease_product SET settlement_currency = 'EUR' WHERE code = 'STANDARD_CAR_DE';

-- Any other/future row must also have a settlement currency before the
-- column can become mandatory. Default remaining rows (if any) to their
-- existing default_currency so the NOT NULL constraint below can never
-- fail on legacy data.
UPDATE lease_product SET settlement_currency = default_currency WHERE settlement_currency IS NULL;

ALTER TABLE lease_product
    ALTER COLUMN settlement_currency SET NOT NULL;
