CREATE
EXTENSION IF NOT EXISTS unaccent;

UPDATE users
SET username = trim(both '._-' from lower(unaccent(username)))
WHERE username !~ '^[a-zA-Z0-9]+([._-][a-zA-Z0-9]+)*$';

UPDATE notes n
SET full_name = u.username || '/' || n.name FROM users u
WHERE n.user_id = u.id;