UPDATE users u
SET notes_count = sub.visible_count FROM (
    SELECT u2.id, COUNT(n.id) FILTER (WHERE n.hidden = FALSE) AS visible_count
    FROM users u2
    JOIN notes n ON n.user_id = u2.id
    GROUP BY u2.id
    HAVING COUNT(n.id) FILTER (WHERE n.hidden = TRUE) > 0
) sub
WHERE u.id = sub.id;