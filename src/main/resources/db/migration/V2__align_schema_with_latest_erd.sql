-- Align the V1 schema with the latest ERD while preserving existing identifiers and data.

-- Users
ALTER TABLE users RENAME COLUMN nickname TO name;
ALTER TABLE users RENAME COLUMN status TO approval_status;

ALTER TABLE users
    ADD COLUMN login_id VARCHAR(100),
    ADD COLUMN birth_date DATE,
    ADD COLUMN phone_number VARCHAR(30),
    ADD COLUMN account_status VARCHAR(30) NOT NULL DEFAULT 'INACTIVE',
    ADD COLUMN approved_by BIGINT,
    ADD COLUMN rejected_at TIMESTAMPTZ,
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE users
SET login_id = CASE
        WHEN LENGTH(email) <= 100 THEN LOWER(email)
        ELSE 'user-' || id
    END,
    birth_date = DATE '1970-01-01',
    phone_number = '',
    account_status = CASE
        WHEN approval_status IN ('APPROVED', 'ACTIVE') THEN 'ACTIVE'
        ELSE 'INACTIVE'
    END,
    approval_status = CASE
        WHEN approval_status = 'ACTIVE' THEN 'APPROVED'
        WHEN approval_status IN ('PENDING', 'APPROVED', 'REJECTED') THEN approval_status
        ELSE 'PENDING'
    END;

ALTER TABLE users
    ALTER COLUMN login_id SET NOT NULL,
    ALTER COLUMN birth_date SET NOT NULL,
    ALTER COLUMN phone_number SET NOT NULL,
    ALTER COLUMN approval_status SET DEFAULT 'PENDING';

ALTER TABLE users
    ADD CONSTRAINT uk_users_login_id UNIQUE (login_id),
    ADD CONSTRAINT fk_users_approved_by
        FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL;

-- Authentication
UPDATE auth_sessions
SET refresh_token_hash = 'revoked-migrated-session-' || id,
    status = 'REVOKED',
    revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP)
WHERE refresh_token_hash IS NULL;

ALTER TABLE auth_sessions
    ALTER COLUMN refresh_token_hash SET NOT NULL,
    DROP COLUMN remember_me;

CREATE TABLE email_verifications (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    verification_code_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    expired_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_email_verifications_email_purpose
    ON email_verifications(email, purpose, created_at DESC);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reset_token_hash VARCHAR(255) NOT NULL,
    expired_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_password_reset_tokens_user
    ON password_reset_tokens(user_id, created_at DESC);

-- Libraries become chats. PostgreSQL keeps existing foreign-key references after table renames.
ALTER TABLE libraries RENAME TO chats;
ALTER TABLE chats RENAME COLUMN name TO title;
ALTER INDEX idx_libraries_project_status RENAME TO idx_chats_project_status;

ALTER TABLE chats
    ADD COLUMN user_id BIGINT,
    ADD COLUMN first_message_id BIGINT,
    ADD COLUMN is_generating BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE chats c
SET user_id = COALESCE(
    (SELECT p.user_id FROM projects p WHERE p.id = c.project_id),
    (SELECT MIN(gj.user_id) FROM generation_jobs gj WHERE gj.library_id = c.id),
    (SELECT MIN(u.id) FROM users u)
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM chats WHERE user_id IS NULL) THEN
        RAISE EXCEPTION
            'Cannot migrate libraries without an owner: create at least one user or assign every library to a project';
    END IF;
END $$;

ALTER TABLE chats
    ALTER COLUMN user_id SET NOT NULL,
    DROP COLUMN description,
    ADD CONSTRAINT fk_chats_user
        FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_chats_user_status ON chats(user_id, status, created_at DESC);

-- Messages
ALTER TABLE library_messages RENAME TO chat_messages;
ALTER TABLE chat_messages RENAME COLUMN library_id TO chat_id;
ALTER INDEX idx_library_messages_library_order RENAME TO idx_chat_messages_chat_order;

UPDATE chat_messages SET sender_type = 'SYSTEM' WHERE sender_type IS NULL;
UPDATE chat_messages SET message_type = 'TEXT' WHERE message_type IS NULL;

ALTER TABLE chat_messages
    ALTER COLUMN sender_type SET NOT NULL,
    ALTER COLUMN message_type SET NOT NULL;

ALTER TABLE chats
    ADD CONSTRAINT fk_chats_first_message
        FOREIGN KEY (first_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL;

ALTER TABLE message_files
    ADD COLUMN stored_filename VARCHAR(255),
    ADD COLUMN width INT,
    ADD COLUMN height INT,
    ADD COLUMN duration_seconds INT;

-- AI model registry and generation jobs
CREATE TABLE ai_models (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    model_code VARCHAR(100) NOT NULL UNIQUE,
    model_type VARCHAR(30) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE generation_jobs RENAME COLUMN library_id TO chat_id;
ALTER TABLE generation_jobs
    ADD COLUMN ai_model_id BIGINT REFERENCES ai_models(id) ON DELETE SET NULL;

UPDATE generation_jobs SET job_type = 'TEXT' WHERE job_type IS NULL;
UPDATE generation_jobs SET status = 'PENDING' WHERE status IS NULL;

ALTER TABLE generation_jobs
    ALTER COLUMN job_type SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'PENDING',
    DROP COLUMN model_name;

ALTER INDEX idx_generation_jobs_library_created RENAME TO idx_generation_jobs_chat_created;
CREATE INDEX idx_generation_jobs_user_status
    ON generation_jobs(user_id, status, created_at DESC);

CREATE TABLE generation_queue (
    id BIGSERIAL PRIMARY KEY,
    generation_job_id BIGINT NOT NULL REFERENCES generation_jobs(id) ON DELETE CASCADE,
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 3,
    available_at TIMESTAMPTZ,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_generation_queue_job ON generation_queue(generation_job_id);
CREATE INDEX idx_generation_queue_poll
    ON generation_queue(status, priority DESC, available_at, created_at);

-- Generated media and physical files
ALTER TABLE generated_media RENAME COLUMN library_id TO chat_id;
ALTER TABLE generated_media ADD COLUMN user_id BIGINT;

UPDATE generated_media gm
SET user_id = c.user_id
FROM chats c
WHERE c.id = gm.chat_id;

UPDATE generated_media SET media_type = 'IMAGE' WHERE media_type IS NULL;

ALTER TABLE generated_media
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN media_type SET NOT NULL,
    ADD CONSTRAINT fk_generated_media_user
        FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_generated_media_user_status
    ON generated_media(user_id, status, created_at DESC);
ALTER INDEX idx_generated_media_library_status RENAME TO idx_generated_media_chat_status;

ALTER TABLE storage_files RENAME TO generated_media_files;
ALTER INDEX idx_storage_files_generated_media RENAME TO idx_generated_media_files_media;
ALTER TABLE generated_media_files ADD COLUMN stored_filename VARCHAR(255);

-- User library
CREATE TABLE saved_folders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    parent_folder_id BIGINT REFERENCES saved_folders(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_saved_folders_user_parent
    ON saved_folders(user_id, parent_folder_id, status);

CREATE TABLE saved_media (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    generated_media_id BIGINT NOT NULL REFERENCES generated_media(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    folder_id BIGINT REFERENCES saved_folders(id) ON DELETE SET NULL,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uk_saved_media_active
    ON saved_media(user_id, generated_media_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_saved_media_user_folder
    ON saved_media(user_id, folder_id, created_at DESC);

INSERT INTO saved_media (
    user_id,
    generated_media_id,
    project_id,
    folder_id,
    display_name,
    created_at
)
SELECT
    gm.user_id,
    gm.id,
    c.project_id,
    NULL,
    COALESCE(NULLIF(gm.title, ''), 'Saved media ' || gm.id),
    COALESCE(gm.saved_at, gm.created_at)
FROM generated_media gm
JOIN chats c ON c.id = gm.chat_id
WHERE gm.is_saved = TRUE;

ALTER TABLE generated_media
    DROP COLUMN is_saved,
    DROP COLUMN saved_at;

CREATE TABLE downloads (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    generated_media_id BIGINT NOT NULL REFERENCES generated_media(id),
    generated_media_file_id BIGINT REFERENCES generated_media_files(id) ON DELETE SET NULL,
    download_filename VARCHAR(255) NOT NULL,
    ip_address VARCHAR(50),
    user_agent TEXT,
    downloaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_downloads_user_date ON downloads(user_id, downloaded_at DESC);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    related_type VARCHAR(50),
    related_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_user_read
    ON notifications(user_id, is_read, created_at DESC);
