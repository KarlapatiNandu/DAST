-- =========================================================
-- DATABASE SCHEMA
-- =========================================================
-- This file runs automatically on startup (spring.sql.init.mode=always).
-- It creates the tables our vulnerable endpoints query against.

-- Users table for the login SQL injection demo
CREATE TABLE IF NOT EXISTS users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email    VARCHAR(255),
    role     VARCHAR(50) DEFAULT 'user'
);
