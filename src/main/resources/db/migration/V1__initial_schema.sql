CREATE TABLE users (
 id BIGSERIAL PRIMARY KEY, public_id UUID NOT NULL UNIQUE, email VARCHAR(255) NOT NULL UNIQUE,
 password_hash VARCHAR(255), nickname VARCHAR(100) NOT NULL, profile_image_url TEXT,
 auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL', provider_id VARCHAR(255),
 role VARCHAR(30) NOT NULL DEFAULT 'USER', status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
 login_count INT NOT NULL DEFAULT 0, last_login_at TIMESTAMPTZ, last_logout_at TIMESTAMPTZ,
 remember_me_enabled BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(auth_provider, provider_id)
);
CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));

CREATE TABLE auth_sessions (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 session_id VARCHAR(255) NOT NULL UNIQUE, refresh_token_hash VARCHAR(255) NOT NULL,
 device_name VARCHAR(255), device_type VARCHAR(50), browser VARCHAR(100), operating_system VARCHAR(100),
 ip_address INET, user_agent TEXT, remember_me BOOLEAN NOT NULL DEFAULT FALSE,
 access_token_expired_at TIMESTAMPTZ, refresh_token_expired_at TIMESTAMPTZ NOT NULL,
 last_accessed_at TIMESTAMPTZ, revoked_at TIMESTAMPTZ,
 status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auth_sessions_user_status ON auth_sessions(user_id, status);

CREATE TABLE user_cookie_preferences (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
 necessary_cookie BOOLEAN NOT NULL DEFAULT TRUE, analytics_cookie BOOLEAN NOT NULL DEFAULT FALSE,
 marketing_cookie BOOLEAN NOT NULL DEFAULT FALSE, preference_cookie BOOLEAN NOT NULL DEFAULT TRUE,
 consent_version VARCHAR(30) NOT NULL DEFAULT 'v1.0', consented_at TIMESTAMPTZ,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_cache_settings (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
 cache_generated_media BOOLEAN NOT NULL DEFAULT TRUE, cache_prompt_history BOOLEAN NOT NULL DEFAULT TRUE,
 cache_library BOOLEAN NOT NULL DEFAULT TRUE, cache_search_result BOOLEAN NOT NULL DEFAULT TRUE,
 cache_duration_seconds INT NOT NULL DEFAULT 3600 CHECK (cache_duration_seconds >= 0),
 last_cache_cleared_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_models (
 id BIGSERIAL PRIMARY KEY, provider VARCHAR(50) NOT NULL, model_name VARCHAR(100) NOT NULL UNIQUE,
 display_name VARCHAR(100) NOT NULL, media_type VARCHAR(20) NOT NULL, task_type VARCHAR(50) NOT NULL,
 is_active BOOLEAN NOT NULL DEFAULT TRUE, is_default BOOLEAN NOT NULL DEFAULT FALSE,
 cost_per_request NUMERIC(12,6), cost_per_second NUMERIC(12,6), cost_per_token NUMERIC(12,6),
 max_duration_seconds INT, max_prompt_length INT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_ai_models_default_task
 ON ai_models(provider, task_type) WHERE is_default = TRUE;

CREATE TABLE generation_jobs (
 id BIGSERIAL PRIMARY KEY, public_id UUID NOT NULL UNIQUE,
 user_id BIGINT NOT NULL REFERENCES users(id), ai_model_id BIGINT REFERENCES ai_models(id),
 job_type VARCHAR(50) NOT NULL, media_type VARCHAR(20) NOT NULL,
 provider VARCHAR(50) NOT NULL, model_name VARCHAR(100) NOT NULL,
 prompt TEXT NOT NULL, negative_prompt TEXT, request_payload JSONB, response_payload JSONB,
 status VARCHAR(30) NOT NULL DEFAULT 'PENDING', external_request_id VARCHAR(255),
 external_status VARCHAR(100), progress INT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
 retry_count INT NOT NULL DEFAULT 0, max_retry_count INT NOT NULL DEFAULT 3,
 next_poll_at TIMESTAMPTZ, last_polled_at TIMESTAMPTZ, poll_count INT NOT NULL DEFAULT 0,
 error_code VARCHAR(100), error_message TEXT, started_at TIMESTAMPTZ,
 completed_at TIMESTAMPTZ, failed_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_generation_jobs_user_created ON generation_jobs(user_id, created_at DESC);
CREATE INDEX idx_generation_jobs_polling ON generation_jobs(status, next_poll_at)
 WHERE status IN ('QUEUED', 'PROCESSING');

CREATE TABLE job_events (
 id BIGSERIAL PRIMARY KEY, job_id BIGINT NOT NULL REFERENCES generation_jobs(id) ON DELETE CASCADE,
 previous_status VARCHAR(30), current_status VARCHAR(30) NOT NULL,
 message TEXT, metadata JSONB, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_job_events_job_created ON job_events(job_id, created_at);

CREATE TABLE media_folders (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 parent_folder_id BIGINT REFERENCES media_folders(id) ON DELETE CASCADE, name VARCHAR(100) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id, parent_folder_id, name)
);

CREATE TABLE media_assets (
 id BIGSERIAL PRIMARY KEY, public_id UUID NOT NULL UNIQUE,
 user_id BIGINT NOT NULL REFERENCES users(id), job_id BIGINT UNIQUE REFERENCES generation_jobs(id),
 folder_id BIGINT REFERENCES media_folders(id) ON DELETE SET NULL,
 media_type VARCHAR(20) NOT NULL, title VARCHAR(255), description TEXT,
 original_prompt TEXT NOT NULL, final_prompt TEXT NOT NULL, reverse_prompt TEXT,
 provider VARCHAR(50) NOT NULL, model_name VARCHAR(100) NOT NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE',
 width INT, height INT, duration_seconds INT, aspect_ratio VARCHAR(20), metadata JSONB,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_media_assets_library ON media_assets(user_id, status, created_at DESC);

CREATE TABLE storage_files (
 id BIGSERIAL PRIMARY KEY, media_asset_id BIGINT REFERENCES media_assets(id) ON DELETE CASCADE,
 file_type VARCHAR(30) NOT NULL DEFAULT 'ORIGINAL', storage_provider VARCHAR(50) NOT NULL DEFAULT 'S3',
 bucket_name VARCHAR(100), storage_path TEXT NOT NULL, public_url TEXT,
 original_filename VARCHAR(255), mime_type VARCHAR(100) NOT NULL, file_size BIGINT,
 checksum VARCHAR(255), width INT, height INT, duration_seconds INT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_storage_files_asset_type ON storage_files(media_asset_id, file_type);

CREATE TABLE media_versions (
 id BIGSERIAL PRIMARY KEY, media_asset_id BIGINT NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
 job_id BIGINT REFERENCES generation_jobs(id), version_number INT NOT NULL, prompt TEXT,
 model_name VARCHAR(100), provider VARCHAR(50), storage_file_id BIGINT REFERENCES storage_files(id),
 change_type VARCHAR(50) NOT NULL, change_note TEXT,
 created_by BIGINT NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(media_asset_id, version_number)
);

CREATE TABLE reverse_prompts (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id),
 media_asset_id BIGINT REFERENCES media_assets(id), job_id BIGINT REFERENCES generation_jobs(id),
 input_file_id BIGINT REFERENCES storage_files(id), extracted_prompt TEXT NOT NULL,
 style_keywords TEXT, lighting_keywords TEXT, camera_keywords TEXT, composition_keywords TEXT,
 subject_keywords TEXT, color_keywords TEXT, confidence_score NUMERIC(5,2),
 provider VARCHAR(50) NOT NULL, model_name VARCHAR(100) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tags (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 name VARCHAR(50) NOT NULL, color VARCHAR(20),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(user_id, name)
);
CREATE TABLE media_tags (
 media_asset_id BIGINT NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
 tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(media_asset_id, tag_id)
);

CREATE TABLE prompt_templates (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 title VARCHAR(100) NOT NULL, description TEXT, template_text TEXT NOT NULL,
 media_type VARCHAR(20) NOT NULL, category VARCHAR(50), is_public BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE favorites (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 media_asset_id BIGINT NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(user_id, media_asset_id)
);

CREATE TABLE api_usage_logs (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id),
 job_id BIGINT REFERENCES generation_jobs(id), ai_model_id BIGINT REFERENCES ai_models(id),
 provider VARCHAR(50) NOT NULL, model_name VARCHAR(100) NOT NULL,
 request_count INT NOT NULL DEFAULT 1, input_tokens INT, output_tokens INT,
 generated_image_count INT, generated_video_seconds INT, estimated_cost NUMERIC(12,6),
 currency CHAR(3) NOT NULL DEFAULT 'USD', pricing_snapshot JSONB,
 status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_usage_logs_user_created ON api_usage_logs(user_id, created_at DESC);

CREATE TABLE audit_logs (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
 action VARCHAR(100) NOT NULL, target_type VARCHAR(50), target_id BIGINT,
 ip_address INET, user_agent TEXT, metadata JSONB,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_logs_user_created ON audit_logs(user_id, created_at DESC);

INSERT INTO ai_models(provider, model_name, display_name, media_type, task_type, is_default)
VALUES ('OPENAI', 'gpt-image-2', 'GPT Image 2', 'IMAGE', 'IMAGE_GENERATION', TRUE),
       ('FAL_AI', 'bytedance/seedance-2.0/text-to-video', 'Seedance 2.0', 'VIDEO', 'VIDEO_GENERATION', TRUE);
