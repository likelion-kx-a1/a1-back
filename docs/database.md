# Database

PostgreSQL 16과 Flyway를 사용하며, JPA는 운영에서 `ddl-auto=validate`로 동작한다.

## 테이블

- `users`: 사용자 계정과 승인 상태
- `auth_sessions`: 로그인 세션과 Refresh Token
- `projects`: 사용자 프로젝트
- `libraries`: 프로젝트별 또는 독립 라이브러리
- `generation_jobs`: 생성 요청과 결과 상태
- `library_messages`: 라이브러리 대화 메시지
- `message_files`: 메시지 첨부 파일
- `generated_media`: 생성된 미디어와 파생 관계
- `storage_files`: 미디어의 실제 S3 파일

생성 작업과 메시지는 서로를 참조할 수 있으므로 Flyway에서 `library_messages` 생성 후
`generation_jobs.request_message_id` FK를 추가한다.

초기 스키마는 `src/main/resources/db/migration/V1__initial_schema.sql`에서 관리한다.
V1이 실제 환경에 적용된 이후에는 수정하지 않고 V2 이상의 새 migration을 추가해야 한다.
