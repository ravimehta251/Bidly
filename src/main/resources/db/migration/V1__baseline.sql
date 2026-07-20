-- ============================================================
-- BidFlare — V1 baseline migration
-- Creates the initial schema: users, auctions, bids
-- ============================================================

-- ── Users ────────────────────────────────────────────────────
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'BUYER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Auctions ─────────────────────────────────────────────────
CREATE TABLE auctions (
    id                 BIGSERIAL        PRIMARY KEY,
    title              VARCHAR(255)     NOT NULL,
    description        TEXT,
    seller_id          BIGINT           NOT NULL REFERENCES users(id),
    starting_price     NUMERIC(19,2)    NOT NULL,
    min_increment      NUMERIC(19,2)    NOT NULL DEFAULT 1.00,
    current_price      NUMERIC(19,2)    NOT NULL,
    current_winner_id  BIGINT           REFERENCES users(id),
    start_time         TIMESTAMPTZ      NOT NULL,
    end_time           TIMESTAMPTZ      NOT NULL,
    status             VARCHAR(20)      NOT NULL DEFAULT 'SCHEDULED',
    version            BIGINT           NOT NULL DEFAULT 0,   -- optimistic lock
    created_at         TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_end_time ON auctions(end_time);

-- ── Bids ─────────────────────────────────────────────────────
CREATE TABLE bids (
    id               BIGSERIAL     PRIMARY KEY,
    auction_id       BIGINT        NOT NULL REFERENCES auctions(id),
    bidder_id        BIGINT        NOT NULL REFERENCES users(id),
    amount           NUMERIC(19,2) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    idempotency_key  VARCHAR(64)   UNIQUE
);

CREATE INDEX idx_bids_auction_amount ON bids(auction_id, amount DESC);
