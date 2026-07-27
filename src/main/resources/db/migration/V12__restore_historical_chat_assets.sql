UPDATE chat_messages message
SET generated_asset_id = generated_asset.id,
    generation_job_id = COALESCE(
        message.generation_job_id,
        generated_asset.generation_job_id
    ),
    updated_at = GREATEST(message.updated_at, generated_asset.updated_at)
FROM generated_assets generated_asset
WHERE generated_asset.response_message_id = message.id
  AND (
      message.generated_asset_id IS NULL
      OR message.generation_job_id IS NULL
  );

INSERT INTO chat_message_files (
    message_id,
    file_type,
    bucket_name,
    storage_path,
    public_url,
    original_filename,
    stored_filename,
    mime_type,
    file_size,
    width,
    height,
    duration_seconds,
    created_at
)
SELECT
    generated_asset.response_message_id,
    asset_file.file_type,
    asset_file.bucket_name,
    asset_file.storage_path,
    asset_file.public_url,
    asset_file.original_filename,
    asset_file.stored_filename,
    asset_file.mime_type,
    asset_file.file_size,
    asset_file.width,
    asset_file.height,
    asset_file.duration_seconds,
    asset_file.created_at
FROM generated_assets generated_asset
JOIN asset_files asset_file
  ON asset_file.generated_asset_id = generated_asset.id
WHERE generated_asset.response_message_id IS NOT NULL
  AND generated_asset.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM chat_message_files message_file
      WHERE message_file.message_id = generated_asset.response_message_id
        AND message_file.storage_path = asset_file.storage_path
  );
