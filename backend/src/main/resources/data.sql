-- =========================================================
-- SEED DATA
-- =========================================================
-- Populates the users table with sample accounts.
-- Passwords are stored in plain text — intentional vulnerability
-- (CWE-256: Plaintext Storage of a Password).

-- Clear existing data (in case of restart)
DELETE FROM users;

-- Insert sample users
INSERT INTO users (username, password, email, role) VALUES
    ('admin',   'password123',  'admin@dast-lab.com',   'admin'),
    ('alice',   'alice2024',    'alice@example.com',    'user'),
    ('bob',     'bob_secret',   'bob@example.com',      'user'),
    ('charlie', 'ch4rli3!',     'charlie@example.com',  'moderator');
