# AI Media Studio Backend Requirements

## 1. 프로젝트 목적

방송 제작자가 하나의 서비스에서 AI 이미지와 영상을 생성하고, 생성 결과를 S3에 보관하며,
라이브러리에서 검색·재사용할 수 있는 백엔드 API를 제공한다.

## 2. 기술 기준

- Java 21
- Spring Boot 4
- Gradle Wrapper
- PostgreSQL 16
- Redis 7
- Flyway
- Spring Data JPA
- Spring Security
- 이미지 생성 AI 공급자: 담당자 선정 후 결정
- 영상 생성 AI 공급자: 담당자 선정 후 결정
- AWS S3, ECR, EC2, SSM
- Docker, Docker Compose, Nginx
- GitHub Actions

## 3. 아키텍처 원칙

프로젝트는 도메인 중심 모듈형 모놀리스로 구현한다.

```text
presentation → application → domain
                         ↑
                infrastructure
```

- 최상위 패키지는 `user`, `generation`, `media` 등 업무 도메인으로 구분한다.
- Controller는 HTTP 변환과 입력 검증만 담당한다.
- Application Service는 유스케이스와 트랜잭션 경계를 담당한다.
- Domain 객체가 상태 변경과 핵심 규칙을 담당한다.
- 외부 API, JPA, S3는 Port를 통해 Application/Domain과 분리한다.
- Entity를 API Request/Response로 직접 사용하지 않는다.
- 외부 API 호출 중 DB 트랜잭션을 유지하지 않는다.
- 공개 API에는 내부 BIGINT PK 대신 UUID `publicId`를 사용한다.

## 4. 사용자 및 인증 요구사항

### 필수

- 이메일과 비밀번호를 이용한 회원가입
- BCrypt 비밀번호 해싱
- 로그인과 로그아웃
- JWT Access Token 발급
- Refresh Token rotation
- Refresh Token 해시 저장
- 여러 기기 세션 관리
- 세션 강제 만료
- 사용자 역할 `USER`, `ADMIN`
- 계정 상태 `ACTIVE`, `SUSPENDED`, `DELETED`

### OAuth 확장

- Google 로그인
- GitHub 로그인
- `authProvider + providerId` 조합 중복 방지

## 5. AI 생성 요구사항

### 이미지 생성

- 이미지 생성 공급자와 기본 모델은 담당자 선정 후 결정한다.
- 프롬프트와 이미지 옵션을 검증한다.
- 이미지 생성 결과를 S3에 저장한다.
- 생성 작업과 결과 미디어를 분리해서 저장한다.
- 실패 원인과 외부 응답을 추적할 수 있어야 한다.

### 영상 생성

- 영상 생성 공급자와 기본 모델은 담당자 선정 후 결정한다.
- 요청 제출 후 `externalRequestId`를 저장한다.
- HTTP 요청을 장시간 유지하지 않고 비동기로 처리한다.
- 상태를 polling 또는 webhook으로 갱신한다.
- 완료된 영상을 외부 공급자 저장소에서 S3로 이전한다.
- 다중 서버 환경에서 polling 중복 실행을 방지한다.

### 작업 상태

```text
PENDING → QUEUED → PROCESSING → COMPLETED
                              └→ FAILED
```

추가 종료 상태:

```text
CANCELED
EXPIRED
```

- 허용되지 않은 상태 전이는 Domain에서 차단한다.
- 생성 요청과 응답 흐름은 `library_messages` 및 `generation_jobs`에 연결해 추적한다.
- 재시도 횟수와 다음 처리 시간을 기록한다.
- Consumer와 polling 작업은 멱등성을 가져야 한다.

## 6. 미디어 요구사항

- 생성 완료 시 `generated_media`를 생성한다.
- 실제 S3 파일 정보는 `storage_files`에 저장한다.
- 원본, 썸네일, 미리보기, 업로드 원본을 구분한다.
- 이미지 크기와 영상 길이 등 메타데이터를 저장한다.
- S3 객체는 기본적으로 비공개로 관리한다.
- 다운로드 시 presigned URL 또는 CloudFront URL을 사용한다.
- 삭제는 우선 소프트 삭제로 처리한다.
- 재생성, 변형, 업스케일 결과는 `media_versions`로 관리한다.

## 7. 라이브러리 요구사항

- 사용자별 미디어 목록 조회
- 이미지·영상 유형 필터
- 생성일 정렬
- 폴더 생성과 중첩 폴더
- 사용자별 태그
- 즐겨찾기 추가와 해제
- 제목·프롬프트·태그 검색
- Cursor 또는 Page 기반 페이지네이션
- 다른 사용자의 비공개 미디어 접근 차단

## 8. 역프롬프트 요구사항

- 사용자가 업로드하거나 생성한 이미지를 분석한다.
- 추출 프롬프트와 스타일, 조명, 카메라, 구도, 피사체, 색감 정보를 저장한다.
- 추출 결과로 이미지 재생성 요청을 만들 수 있어야 한다.
- 입력 파일과 원본 미디어의 소유권을 검증한다.

## 9. 운영 요구사항

- 모델 목록과 활성 여부를 `ai_models`에서 관리한다.
- 모델별 사용량과 예상 비용을 `api_usage_logs`에 기록한다.
- 주요 사용자 활동 로그는 운영 로깅 정책 확정 후 별도 저장소에 기록한다.
- 로그에 비밀번호, Token, API Key, 개인정보를 기록하지 않는다.
- 모든 요청 로그에 Request ID를 포함한다.
- Actuator health endpoint를 제공한다.
- 외부 API timeout과 재시도 정책을 명시한다.

## 10. 데이터베이스 규칙

- PostgreSQL을 사용한다.
- DB 변경은 Flyway로만 수행한다.
- 운영에서 `ddl-auto=update`를 사용하지 않는다.
- JPA는 `ddl-auto=validate`로 설정한다.
- 적용된 migration 파일을 수정하지 않는다.
- 모든 FK, unique, nullable, 길이와 인덱스를 명시한다.
- enum은 문자열로 저장한다.
- 모든 연관관계는 기본 LAZY를 사용한다.
- N:M 관계는 연결 엔티티 또는 연결 테이블로 분리한다.
- 시간은 `TIMESTAMPTZ`로 저장한다.
- JSON 원본은 PostgreSQL `JSONB`로 저장한다.

전체 초기 스키마:

```text
src/main/resources/db/migration/V1__initial_schema.sql
```

## 11. API 규칙

기본 경로:

```text
/api/v1
```

성공 응답:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {},
  "timestamp": "2026-07-01T12:00:00+09:00"
}
```

오류 응답:

```json
{
  "success": false,
  "error": {
    "code": "GENERATION-001",
    "message": "생성 작업을 찾을 수 없습니다.",
    "details": []
  },
  "timestamp": "2026-07-01T12:00:00+09:00"
}
```

- Request와 Response DTO를 분리한다.
- Bean Validation을 적용한다.
- 오류 코드는 도메인별 접두사를 사용한다.
- Entity와 내부 PK를 응답으로 직접 노출하지 않는다.
- 목록 API의 페이지네이션 형식을 통일한다.

## 12. 환경변수

로컬 파일은 `.env.example`을 복사해 사용하며 `.env`는 커밋하지 않는다.

필수 설정:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
REDIS_HOST
REDIS_PORT
AWS_REGION
STORAGE_BUCKET
STORAGE_PUBLIC_BASE_URL
```

운영 AWS 인증은 EC2 Instance Role과 GitHub OIDC를 사용한다. 정적 AWS Access Key를 저장하지 않는다.

## 13. 테스트 요구사항

- Domain 상태 전이 단위 테스트
- Application Service 유스케이스 테스트
- Controller 입력 검증과 권한 테스트
- PostgreSQL Testcontainers 통합 테스트
- Flyway 전체 migration 테스트
- 외부 AI 공급자 API Mock 테스트
- S3 업로드 Mock 또는 LocalStack 테스트
- 영상 polling 멱등성 테스트
- 전체 Spring Context 테스트

PR은 다음 명령이 성공해야 병합할 수 있다.

```bash
./gradlew clean test
docker build -t a1-back:test .
```

## 14. Git 협업 규칙

### 브랜치

```text
main
develop
feature/{issue-number}-{short-name}
fix/{issue-number}-{short-name}
```

- `main`에는 직접 push하지 않는다.
- 기능 브랜치에서 작업하고 Pull Request를 생성한다.
- PR은 최소 1명의 리뷰 후 병합한다.
- 하나의 PR에는 하나의 기능 또는 변경 목적만 포함한다.

### 커밋 메시지

```text
feat: 기능 추가
fix: 버그 수정
refactor: 동작 변경 없는 구조 개선
test: 테스트 추가 또는 수정
docs: 문서 변경
chore: 빌드 및 설정 변경
```

### PR 필수 내용

- 변경 목적
- 주요 구현 내용
- 테스트 결과
- DB migration 유무
- 환경변수 추가·변경 여부
- API 계약 변경 여부
- 배포 시 주의사항

## 15. 현재 구현 상태

### 완료

- 전체 PostgreSQL 초기 스키마
- 사용자 등록
- 생성 작업 상태 모델
- AI 모델·생성 작업·미디어 기본 엔티티와 DTO
- S3 저장 Port/Adapter 기반
- 공통 응답과 예외 처리
- Docker, Nginx, GitHub Actions 배포 골격

### 미완료

- JWT 로그인과 세션 관리
- AI 공급자 선정 및 이미지·영상 생성 연동 구현
- OAuth 로그인
- 생성 완료 시 `generated_media`, `storage_files` 등록
- 생성 작업과 라이브러리 메시지 연결
- 미디어 라이브러리 API
- 폴더, 태그, 즐겨찾기
- 역프롬프트
- 실제 PostgreSQL migration 통합 테스트
- 실제 AI 공급자/S3 통합 검증
- EC2/RDS/Redis/S3/ECR 실제 리소스 구성

## 16. MVP 완료 조건

아래 항목이 모두 충족되면 1차 MVP로 본다.

- 회원가입, 로그인, Token 재발급이 동작한다.
- 인증 사용자만 생성 API를 사용할 수 있다.
- 이미지 생성 결과가 S3와 미디어 라이브러리에 저장된다.
- 영상 생성 요청이 비동기로 완료되고 S3에 저장된다.
- 사용자가 자신의 생성 결과 목록과 상세 정보를 조회할 수 있다.
- 실패 작업의 원인과 상태 이력을 확인할 수 있다.
- API 사용량과 예상 비용을 기록한다.
- PostgreSQL migration 및 주요 API 통합 테스트가 통과한다.
- Docker 이미지가 빌드되고 EC2에 배포할 수 있다.
