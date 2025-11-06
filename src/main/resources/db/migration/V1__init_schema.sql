-- 1) Wallet table
CREATE TABLE IF NOT EXISTS wallet (
  id         UUID PRIMARY KEY,
  status     VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version    INT NOT NULL DEFAULT 0
);

-- 2) PIX Key table
CREATE TABLE IF NOT EXISTS pix_key (
  id         UUID PRIMARY KEY,
  wallet_id  UUID NOT NULL REFERENCES wallet(id),
  type       VARCHAR(16) NOT NULL,
  value      VARCHAR(255) NOT NULL,
  status     VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_pix_key_wallet FOREIGN KEY (wallet_id) REFERENCES wallet(id),
  CONSTRAINT uq_pix_active UNIQUE (type, value, status)
);

CREATE INDEX IF NOT EXISTS idx_pix_key_wallet ON pix_key(wallet_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pixkey_active ON pix_key(value)
  WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS transfer (
  id               UUID PRIMARY KEY,
  end_to_end_id    TEXT NOT NULL UNIQUE,
  idempotency_key  VARCHAR(64),
  from_wallet_id   TEXT NOT NULL,
  to_wallet_id     TEXT NOT NULL,
  amount           NUMERIC(15,2) NOT NULL CHECK (amount > 0),
  currency         CHAR(3) NOT NULL DEFAULT 'BRL',
  status           TEXT NOT NULL,
  initiated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  version          INT NOT NULL DEFAULT 0,
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_transfer_from ON transfer(from_wallet_id);
CREATE INDEX IF NOT EXISTS ix_transfer_to ON transfer(to_wallet_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_transfer_idempotency_key ON transfer(idempotency_key) 
  WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS ledger_entry (
  id               UUID PRIMARY KEY,
  wallet_id        UUID NOT NULL REFERENCES wallet(id),
  operation_type   TEXT NOT NULL,
  amount           NUMERIC(15,2) NOT NULL CHECK (amount <> 0),
  effective_at     TIMESTAMPTZ NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  idempotency_key  VARCHAR(64)
);

COMMENT ON COLUMN ledger_entry.operation_type IS 
'Operation types: DEPOSIT (add funds), WITHDRAW (remove funds), PIX_OUT (legacy), PIX_IN (legacy), ADJUSTMENT (manual), RESERVED (block funds for PENDING transfer), UNRESERVED (release blocked funds)';

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_entry_idempotency_key ON ledger_entry(wallet_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_ledger_balance ON ledger_entry(wallet_id, operation_type, amount);

CREATE INDEX IF NOT EXISTS ix_ledger_historical ON ledger_entry(wallet_id, created_at, operation_type, amount);

CREATE INDEX IF NOT EXISTS ix_ledger_wallet_time ON ledger_entry(wallet_id, effective_at);

CREATE TABLE IF NOT EXISTS webhook_inbox (
  id             UUID PRIMARY KEY,
  end_to_end_id  TEXT NOT NULL,
  event_id       TEXT NOT NULL UNIQUE,
  event_type     TEXT NOT NULL,
  event_time     TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_webhook_e2e_time ON webhook_inbox(end_to_end_id, event_time DESC);