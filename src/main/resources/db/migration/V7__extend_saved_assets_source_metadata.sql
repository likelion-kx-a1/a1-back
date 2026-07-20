ALTER TABLE saved_assets
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30) NOT NULL DEFAULT 'GENERATED_ASSET',
    ADD COLUMN IF NOT EXISTS source_chat_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_message_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_message_file_id BIGINT;

UPDATE saved_assets saved_asset
SET source_chat_id = generated_asset.chat_id,
    source_message_id = generated_asset.response_message_id
FROM generated_assets generated_asset
WHERE saved_asset.source_generated_asset_id = generated_asset.id
  AND saved_asset.source_chat_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_saved_assets_source_chat_id
    ON saved_assets (source_chat_id);

CREATE INDEX IF NOT EXISTS idx_saved_assets_source_message_file_id
    ON saved_assets (source_message_file_id);
