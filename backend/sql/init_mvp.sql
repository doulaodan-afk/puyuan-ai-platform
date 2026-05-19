-- One-file initializer for MVP database
CREATE DATABASE IF NOT EXISTS puyuan_ai_mvp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE puyuan_ai_mvp;

-- # MEMORY: keep schema and seed split for easier rollback; init script orchestrates both to support one-command onboarding.
SOURCE ./schema_mvp.sql;
SOURCE ./seed_mvp.sql;
