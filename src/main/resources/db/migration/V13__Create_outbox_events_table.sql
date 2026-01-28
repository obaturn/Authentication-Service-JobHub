-- Create outbox_events table for reliable event publishing
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    last_error TEXT
);

-- Index for efficient querying of pending events
CREATE INDEX idx_outbox_events_status_created_at ON outbox_events(status, created_at);

-- Index for event type queries
CREATE INDEX idx_outbox_events_event_type ON outbox_events(event_type);