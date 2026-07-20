CREATE TABLE library_projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_project_id BIGINT REFERENCES library_projects(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    depth INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_library_projects_user_status
    ON library_projects(user_id, status, created_at DESC);
CREATE INDEX idx_library_projects_parent
    ON library_projects(user_id, parent_project_id, status);

ALTER TABLE storage_folders
    ADD COLUMN library_project_id BIGINT REFERENCES library_projects(id) ON DELETE CASCADE,
    ADD COLUMN folder_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOM',
    ADD COLUMN asset_type VARCHAR(20);
CREATE INDEX idx_storage_folders_library_project
    ON storage_folders(library_project_id, asset_type, parent_folder_id, status);

ALTER TABLE saved_assets
    DROP CONSTRAINT IF EXISTS saved_media_generated_media_id_fkey,
    DROP CONSTRAINT IF EXISTS saved_assets_generated_asset_id_fkey;
DROP INDEX IF EXISTS uk_saved_assets_active;
ALTER TABLE saved_assets
    RENAME COLUMN generated_asset_id TO source_generated_asset_id;
ALTER TABLE saved_assets
    ALTER COLUMN source_generated_asset_id DROP NOT NULL,
    ADD COLUMN library_project_id BIGINT REFERENCES library_projects(id) ON DELETE SET NULL,
    ADD COLUMN asset_type VARCHAR(20);
CREATE INDEX idx_saved_assets_library_project
    ON saved_assets(user_id, library_project_id, folder_id, asset_type, status, created_at DESC);

CREATE TABLE saved_asset_files (
    id BIGSERIAL PRIMARY KEY,
    saved_asset_id BIGINT NOT NULL REFERENCES saved_assets(id) ON DELETE CASCADE,
    source_asset_file_id BIGINT,
    file_type VARCHAR(30),
    bucket_name VARCHAR(100) NOT NULL,
    storage_path TEXT NOT NULL,
    public_url TEXT,
    original_filename VARCHAR(255),
    stored_filename VARCHAR(255),
    mime_type VARCHAR(100),
    file_size BIGINT,
    width INT,
    height INT,
    duration_seconds INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_saved_asset_files_saved_asset
    ON saved_asset_files(saved_asset_id);
