CREATE TABLE users_subscriptions (
  user_id uuid NOT NULL,
  subscription varchar(255) NOT NULL,
  PRIMARY KEY (user_id, subscription)
);

ALTER TABLE users_subscriptions
  ADD CONSTRAINT fk_users_subscriptions_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_users_subscriptions_subscription ON users_subscriptions(subscription);

INSERT INTO users_subscriptions (user_id, subscription)
    SELECT u.id, 'Maintenance' FROM users u
    LEFT JOIN users_subscriptions s ON s.user_id = u.id AND s.subscription = 'Maintenance'
    WHERE s.user_id IS NULL;

INSERT INTO users_subscriptions (user_id, subscription)
    SELECT u.id, 'Release' FROM users u
    LEFT JOIN users_subscriptions s ON s.user_id = u.id AND s.subscription = 'Release'
    WHERE s.user_id IS NULL;