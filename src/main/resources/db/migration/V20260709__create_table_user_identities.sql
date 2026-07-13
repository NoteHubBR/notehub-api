CREATE TABLE user_identities (
    id UUID PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    host VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (host, provider_id)
);

CREATE INDEX idx_user_identities_user_id ON user_identities(user_id);

INSERT INTO user_identities (id, user_id, host, provider_id, provider_email, linked_at)
SELECT gen_random_uuid(), id, host, provider_id, email, created_at
FROM users
WHERE host <> 'NoteHub' AND provider_id IS NOT NULL;

DROP INDEX IF EXISTS idx_users_provider_id;
ALTER TABLE users DROP COLUMN provider_id;
ALTER TABLE users DROP COLUMN host;