create table IF NOT EXISTS wallet (
  id uuid primary key,
  status varchar(16) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  version int not null default 0
);

create table IF NOT EXISTS pix_key (
  id uuid primary key,
  wallet_id uuid not null references wallet(id),
  type varchar(16) not null,
  value varchar(255) not null,
  status varchar(16) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  revoked_at timestamptz null,
  constraint uq_pix_active unique (type, value, status),
  CONSTRAINT fk_pix_key_wallet FOREIGN KEY (wallet_id) REFERENCES wallet(id)
);

create index IF NOT EXISTS idx_pix_key_wallet on pix_key(wallet_id);

-- Unicidade apenas para chaves ativas
CREATE UNIQUE INDEX IF NOT EXISTS uq_pixkey_active ON pix_key (value)
  WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS transfer (
  id               UUID PRIMARY KEY,
  end_to_end_id    TEXT NOT NULL UNIQUE,
  from_wallet_id   UUID NOT NULL REFERENCES wallet(id),
  to_wallet_id     UUID NOT NULL REFERENCES wallet(id),
  amount           NUMERIC(15,2) NOT NULL CHECK (amount > 0),
  currency         CHAR(3) NOT NULL,
  status           TEXT NOT NULL,   -- PENDING, CONFIRMED, REJECTED
  reason_code      TEXT,            -- opcional (ex.: INSUFFICIENT_FUNDS)
  initiated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  applied_at       TIMESTAMPTZ,     -- quando último estado foi aplicado (para ordenação)
  version          INT NOT NULL DEFAULT 0, -- controle otimista p/ webhook
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_transfer_from ON transfer(from_wallet_id);
CREATE INDEX IF NOT EXISTS ix_transfer_to   ON transfer(to_wallet_id);

-- 4) Lançamentos imutáveis (ledger)
-- Particionar por mês em effective_at (não mostrado aqui o template de partições)
CREATE TABLE IF NOT EXISTS ledger_entry (
  id               uuid PRIMARY KEY,
  wallet_id        UUID NOT NULL REFERENCES wallet(id),
  transfer_id      UUID REFERENCES transfer(id),
  operation_type   TEXT NOT NULL,  -- DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN, REVERSAL, ADJUSTMENT
  amount           NUMERIC(15,2) NOT NULL, -- sinal + (crédito) / - (débito)
  available        BOOLEAN NOT NULL DEFAULT TRUE, -- se afeta saldo disponível
  effective_at     TIMESTAMPTZ NOT NULL,          -- quando passa a valer
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  idempotency_key VARCHAR(64),
  CONSTRAINT ck_nonzero_amount CHECK (amount <> 0)
);

-- Idempotência por carteira+idempotency_key
CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_entry_idempotency_key ON ledger_entry(wallet_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_ledger_wallet_time ON ledger_entry(wallet_id, effective_at);
CREATE INDEX IF NOT EXISTS ix_ledger_wallet ON ledger_entry(wallet_id);

-- 5) Saldo corrente (1 linha por carteira)
CREATE TABLE IF NOT EXISTS wallet_balance (
  wallet_id        UUID PRIMARY KEY REFERENCES wallet(id),
  balance          NUMERIC(15,2) NOT NULL DEFAULT 0,
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6) Snapshots de saldo (para consultas históricas)
CREATE TABLE IF NOT EXISTS balance_snapshot (
  id               BIGSERIAL PRIMARY KEY,
  wallet_id        UUID NOT NULL REFERENCES wallet(id),
  as_of            TIMESTAMPTZ NOT NULL,
  balance          NUMERIC(15,2) NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (wallet_id, as_of)
);

CREATE INDEX IF NOT EXISTS ix_snapshot_wallet_time ON balance_snapshot(wallet_id, as_of);

-- 7) Inbox de Webhook (robustez a duplicados/fora de ordem)
CREATE TABLE IF NOT EXISTS webhook_inbox (
  id               UUID PRIMARY KEY,
  end_to_end_id    TEXT NOT NULL,
  event_type       TEXT NOT NULL, -- CONFIRMED/REJECTED
  event_time       TIMESTAMPTZ NOT NULL, -- carimbo vindo do emissor (se houver) ou received_at
  payload_hash     TEXT NOT NULL,
  received_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  processed        BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE (end_to_end_id, payload_hash)
);

CREATE INDEX IF NOT EXISTS ix_webhook_e2e_time ON webhook_inbox(end_to_end_id, event_time DESC);