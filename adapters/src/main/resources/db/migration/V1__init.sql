-- Initial schema (skeletons for core entities)
CREATE TABLE IF NOT EXISTS orders (
  id VARCHAR(64) PRIMARY KEY,
  merchant_id VARCHAR(64) NOT NULL,
  amount_minor BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(32) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_intents (
  id VARCHAR(64) PRIMARY KEY,
  order_id VARCHAR(64) NOT NULL REFERENCES orders(id),
  type VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL,
  gateway_ref VARCHAR(128),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS charges (
  id VARCHAR(64) PRIMARY KEY,
  intent_id VARCHAR(64) NOT NULL REFERENCES payment_intents(id),
  amount_minor BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  settled_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refunds (
  id VARCHAR(64) PRIMARY KEY,
  charge_id VARCHAR(64) NOT NULL REFERENCES charges(id),
  amount_minor BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS idempotency_records (
  scope_key VARCHAR(200) PRIMARY KEY,
  request_hash VARCHAR(128) NOT NULL,
  response_snapshot TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS webhook_events (
  id VARCHAR(64) PRIMARY KEY,
  vendor_event_id VARCHAR(128) NOT NULL,
  signature_hash VARCHAR(128) NOT NULL,
  type VARCHAR(64) NOT NULL,
  received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_webhook_dedupe ON webhook_events(vendor_event_id, signature_hash);
