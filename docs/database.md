# Database

PostgreSQL 16을 기준으로 하며 운영에서는 Hibernate `ddl-auto=validate`와 Flyway를 사용합니다.

## 도메인별 테이블

- user: `users`, `auth_sessions`, `user_cookie_preferences`, `user_cache_settings`
- generation: `ai_models`, `generation_jobs`, `job_events`
- media: `media_assets`, `storage_files`, `media_versions`
- prompt/library: `reverse_prompts`, `media_folders`, `tags`, `media_tags`, `prompt_templates`, `favorites`
- operation: `api_usage_logs`, `audit_logs`

내부 관계에는 `BIGINT` PK를 사용하고 외부 API에 노출되는 사용자·작업·미디어 식별자는
`public_id UUID`를 사용합니다. JSON 원본은 `JSONB`, IP 주소는 `INET`, 시간은 `TIMESTAMPTZ`로 저장합니다.

이미 적용된 Flyway 파일은 수정하지 않습니다. 현재 V1은 초기 개발 단계의 기준 스키마이며 배포 이후의
변경은 `V2__...sql`처럼 새 버전으로 추가해야 합니다.
