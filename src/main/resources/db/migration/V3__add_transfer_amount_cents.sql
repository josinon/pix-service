-- Add amount_cents column to transfer table
ALTER TABLE transfer ADD COLUMN IF NOT EXISTS amount_cents BIGINT;

-- Migrate existing data (amount to amount_cents)
UPDATE transfer SET amount_cents = (amount * 100)::BIGINT WHERE amount_cents IS NULL;

-- Make amount_cents NOT NULL after migration
ALTER TABLE transfer ALTER COLUMN amount_cents SET NOT NULL;

-- Optionally drop the old amount column after ensuring migration is successful
-- ALTER TABLE transfer DROP COLUMN IF EXISTS amount;
