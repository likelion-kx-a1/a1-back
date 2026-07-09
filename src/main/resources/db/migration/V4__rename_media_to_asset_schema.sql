-- Align media naming with the latest ERD.
-- V2 used "generated_media/saved_media" naming; the latest ERD uses "asset" naming.

-- Chats
ALTER TABLE chats
    ADD COLUMN generation_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    ADD COLUMN image_category VARCHAR(30);

-- Chat messages
ALTER TABLE chat_messages
    ADD COLUMN generated_asset_id BIGINT;

-- Chat message files
ALTER TABLE message_files RENAME TO chat_message_files;
ALTER INDEX IF EXISTS idx_message_files_message RENAME TO idx_chat_message_files_message;

-- Generation jobs
ALTER TABLE generation_jobs RENAME COLUMN job_type TO generation_type;
ALTER TABLE generation_jobs
    ADD COLUMN image_category VARCHAR(30);

-- Generated assets
ALTER TABLE generated_media RENAME COLUMN parent_media_id TO parent_asset_id;
ALTER TABLE generated_media RENAME COLUMN media_type TO asset_type;
ALTER TABLE generated_media
    ADD COLUMN image_category VARCHAR(30);
ALTER TABLE generated_media RENAME TO generated_assets;
ALTER INDEX IF EXISTS idx_generated_media_user_status RENAME TO idx_generated_assets_user_status;
ALTER INDEX IF EXISTS idx_generated_media_chat_status RENAME TO idx_generated_assets_chat_status;

-- Asset files
ALTER TABLE generated_media_files RENAME COLUMN generated_media_id TO generated_asset_id;
ALTER TABLE generated_media_files RENAME TO asset_files;
ALTER INDEX IF EXISTS idx_generated_media_files_media RENAME TO idx_asset_files_asset;

-- User storage folders
ALTER TABLE saved_folders DROP COLUMN project_id;
ALTER TABLE saved_folders RENAME TO storage_folders;
ALTER INDEX IF EXISTS idx_saved_folders_user_parent RENAME TO idx_storage_folders_user_parent;

-- Saved assets
ALTER TABLE saved_media RENAME COLUMN generated_media_id TO generated_asset_id;
ALTER TABLE saved_media DROP COLUMN project_id;
ALTER TABLE saved_media
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE saved_media RENAME TO saved_assets;
ALTER INDEX IF EXISTS idx_saved_media_user_folder RENAME TO idx_saved_assets_user_folder;
ALTER INDEX IF EXISTS uk_saved_media_active RENAME TO uk_saved_assets_active;

-- Downloads
ALTER TABLE downloads RENAME COLUMN generated_media_id TO generated_asset_id;
ALTER TABLE downloads RENAME COLUMN generated_media_file_id TO asset_file_id;

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_messages_generated_asset
        FOREIGN KEY (generated_asset_id) REFERENCES generated_assets(id) ON DELETE SET NULL;
