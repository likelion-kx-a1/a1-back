ALTER TABLE library_projects
    ADD COLUMN IF NOT EXISTS source_project_id BIGINT;

ALTER TABLE library_projects
    ADD CONSTRAINT fk_library_projects_source_project
    FOREIGN KEY (source_project_id)
    REFERENCES projects(id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_library_projects_user_source_project_status
    ON library_projects(user_id, source_project_id, status);

INSERT INTO library_projects (
    user_id,
    parent_project_id,
    source_project_id,
    name,
    depth,
    status,
    created_at,
    updated_at
)
SELECT
    p.user_id,
    NULL,
    p.id,
    p.name || ' 보관함',
    0,
    'ACTIVE',
    NOW(),
    NOW()
FROM projects p
WHERE p.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM library_projects lp
      WHERE lp.user_id = p.user_id
        AND lp.source_project_id = p.id
        AND lp.status = 'ACTIVE'
  );
