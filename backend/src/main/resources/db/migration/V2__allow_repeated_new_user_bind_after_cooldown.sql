DROP INDEX IF EXISTS ux_tickets_open_new_user_bind;

CREATE INDEX IF NOT EXISTS idx_tickets_user_type_created_id
    ON tickets (user_id, ticket_type, created_at DESC, id DESC);
