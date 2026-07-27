INSERT INTO notifications (
    user_id,
    notification_type,
    title,
    content,
    related_type,
    related_id,
    is_read,
    read_at,
    created_at
)
SELECT
    generation_job.user_id,
    CASE
        WHEN generation_job.generation_type LIKE '%VIDEO%'
            AND generation_job.status = 'COMPLETED'
            THEN 'VIDEO_GENERATION_COMPLETED'
        WHEN generation_job.generation_type LIKE '%VIDEO%'
            THEN 'VIDEO_GENERATION_FAILED'
        WHEN generation_job.status = 'COMPLETED'
            THEN 'IMAGE_GENERATION_COMPLETED'
        ELSE 'IMAGE_GENERATION_FAILED'
    END,
    CASE
        WHEN generation_job.generation_type LIKE '%VIDEO%'
            AND generation_job.status = 'COMPLETED'
            THEN '영상 생성이 완료되었습니다.'
        WHEN generation_job.generation_type LIKE '%VIDEO%'
            THEN '영상 생성에 실패했습니다.'
        WHEN generation_job.status = 'COMPLETED'
            THEN '이미지 생성이 완료되었습니다.'
        ELSE '이미지 생성에 실패했습니다.'
    END,
    CASE
        WHEN generation_job.status = 'COMPLETED'
            THEN '과거 생성 작업에서 복원된 알림입니다.'
        ELSE '과거 생성 작업의 실패 기록에서 복원된 알림입니다.'
    END,
    'GENERATION_JOB',
    generation_job.id,
    TRUE,
    COALESCE(generation_job.completed_at, generation_job.updated_at),
    COALESCE(
        generation_job.completed_at,
        generation_job.updated_at,
        generation_job.created_at
    )
FROM generation_jobs generation_job
WHERE generation_job.status IN ('COMPLETED', 'FAILED')
  AND NOT EXISTS (
      SELECT 1
      FROM notifications notification
      WHERE notification.user_id = generation_job.user_id
        AND notification.related_type = 'GENERATION_JOB'
        AND notification.related_id = generation_job.id
  );
