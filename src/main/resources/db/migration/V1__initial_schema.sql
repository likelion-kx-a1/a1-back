CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    profile_image_url TEXT,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    login_count INT NOT NULL DEFAULT 0,
    approved_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    last_logout_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));

CREATE TABLE auth_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(255),
    ip_address VARCHAR(50),
    user_agent TEXT,
    remember_me BOOLEAN NOT NULL DEFAULT FALSE,
    expired_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auth_sessions_user_status ON auth_sessions(user_id, status);

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_projects_user_status ON projects(user_id, status);

CREATE TABLE libraries (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_libraries_project_status ON libraries(project_id, status);

CREATE TABLE generation_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    library_id BIGINT NOT NULL REFERENCES libraries(id),
    request_message_id BIGINT,
    model_name VARCHAR(100),
    job_type VARCHAR(50),
    prompt TEXT,
    request_payload JSONB,
    response_payload JSONB,
    status VARCHAR(30),
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_generation_jobs_library_created ON generation_jobs(library_id, created_at DESC);

CREATE TABLE library_messages (
    id BIGSERIAL PRIMARY KEY,
    library_id BIGINT NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    sender_type VARCHAR(20),
    message_type VARCHAR(20),
    content_text TEXT,
    parent_message_id BIGINT REFERENCES library_messages(id) ON DELETE SET NULL,
    generation_job_id BIGINT REFERENCES generation_jobs(id) ON DELETE SET NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_library_messages_library_order ON library_messages(library_id, sort_order, created_at);

ALTER TABLE generation_jobs
    ADD CONSTRAINT fk_generation_jobs_request_message
    FOREIGN KEY (request_message_id) REFERENCES library_messages(id) ON DELETE SET NULL;

CREATE TABLE message_files (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES library_messages(id) ON DELETE CASCADE,
    file_type VARCHAR(30),
    bucket_name VARCHAR(100) NOT NULL,
    storage_path TEXT NOT NULL,
    public_url TEXT,
    original_filename VARCHAR(255),
    mime_type VARCHAR(100),
    file_size BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_message_files_message ON message_files(message_id);

CREATE TABLE generated_media (
    id BIGSERIAL PRIMARY KEY,
    library_id BIGINT NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
    generation_job_id BIGINT REFERENCES generation_jobs(id) ON DELETE SET NULL,
    response_message_id BIGINT REFERENCES library_messages(id) ON DELETE SET NULL,
    parent_media_id BIGINT REFERENCES generated_media(id) ON DELETE SET NULL,
    media_type VARCHAR(20),
    title VARCHAR(255),
    prompt TEXT,
    is_saved BOOLEAN NOT NULL DEFAULT FALSE,
    saved_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_generated_media_library_status ON generated_media(library_id, status, created_at DESC);

CREATE TABLE storage_files (
    id BIGSERIAL PRIMARY KEY,
    generated_media_id BIGINT NOT NULL REFERENCES generated_media(id) ON DELETE CASCADE,
    file_type VARCHAR(30),
    bucket_name VARCHAR(100) NOT NULL,
    storage_path TEXT NOT NULL,
    public_url TEXT,
    original_filename VARCHAR(255),
    mime_type VARCHAR(100),
    file_size BIGINT,
    width INT,
    height INT,
    duration_seconds INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_storage_files_generated_media ON storage_files(generated_media_id);
