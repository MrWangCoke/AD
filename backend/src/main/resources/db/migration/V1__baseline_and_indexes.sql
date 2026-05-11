CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(32) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    name VARCHAR(64) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    avatar_url VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_no VARCHAR(30) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    student_id VARCHAR(50) NOT NULL,
    ticket_type INTEGER NOT NULL,
    status INTEGER DEFAULT 0,
    broadband_account VARCHAR(50),
    new_password VARCHAR(255),
    phone VARCHAR(20),
    result_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(32),
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(120),
    ADD COLUMN IF NOT EXISTS name VARCHAR(64),
    ADD COLUMN IF NOT EXISTS student_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS ticket_no VARCHAR(30),
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS student_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ticket_type INTEGER,
    ADD COLUMN IF NOT EXISTS status INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS broadband_account VARCHAR(50),
    ADD COLUMN IF NOT EXISTS new_password VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS result_message TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE tickets SET status = 0 WHERE status IS NULL;
UPDATE tickets SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE tickets SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

ALTER TABLE tickets ALTER COLUMN status SET DEFAULT 0;
ALTER TABLE tickets ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tickets ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_phone'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_phone UNIQUE (phone);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_tickets_ticket_no'
    ) THEN
        ALTER TABLE tickets ADD CONSTRAINT uk_tickets_ticket_no UNIQUE (ticket_no);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_phone_student_id
    ON users (phone, student_id);

CREATE INDEX IF NOT EXISTS idx_tickets_status_created_id
    ON tickets (status, created_at, id);

CREATE INDEX IF NOT EXISTS idx_tickets_user_created_id
    ON tickets (user_id, created_at, id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tickets_open_new_user_bind
    ON tickets (user_id, student_id, phone)
    WHERE ticket_type = 1 AND status IN (0, 1, 2);

CREATE OR REPLACE FUNCTION set_updated_at_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tickets_updated_at ON tickets;
CREATE TRIGGER trg_tickets_updated_at
    BEFORE UPDATE ON tickets
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at_timestamp();
