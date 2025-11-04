-- Add idempotency_key column to transfer table for ensuring idempotent creation
ALTER TABLE transfer ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64);

-- Backfill: no data needed (new transfers only). If duplicates somehow exist this will fail when setting NOT NULL/UNIQUE later.

-- Add unique index to enforce single transfer per idempotency key
CREATE UNIQUE INDEX IF NOT EXISTS uq_transfer_idempotency_key ON transfer(idempotency_key) WHERE idempotency_key IS NOT NULL;
