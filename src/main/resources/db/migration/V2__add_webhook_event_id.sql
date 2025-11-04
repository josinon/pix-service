-- Add event_id column to webhook_inbox for idempotency
ALTER TABLE webhook_inbox ADD COLUMN IF NOT EXISTS event_id TEXT;

-- Create unique index on event_id for idempotency
CREATE UNIQUE INDEX IF NOT EXISTS uq_webhook_event_id ON webhook_inbox(event_id) WHERE event_id IS NOT NULL;
