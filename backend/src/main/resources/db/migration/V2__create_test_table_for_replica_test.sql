CREATE TABLE replication_demo (
    id UUID PRIMARY KEY,
    source VARCHAR(10) NOT NULL,
    message VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
