CREATE TABLE feed (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    event VARCHAR(50) NOT NULL,
    recipient_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    related_user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    note_id UUID REFERENCES notes (id) ON DELETE CASCADE,
    flame_id UUID REFERENCES flames (id) ON DELETE CASCADE,
    comment_id UUID REFERENCES comments (id) ON DELETE CASCADE
);

CREATE INDEX idx_feed_recipient_created ON feed (recipient_id, created_at DESC);