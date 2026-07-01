# A1 생성형 AI 통합 엔진 Backend

Java 21과 Spring Boot 기반 도메인 중심 모듈형 모놀리스입니다.

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

Windows에서는 `gradlew.bat bootRun`을 사용합니다.

## 초기 API

- `POST /api/v1/users`: 로컬 사용자 등록
- `POST /api/v1/generations`: 이미지·영상·역프롬프트 작업 생성
- `GET /api/v1/generations/{id}`: 생성 작업 상태 조회
- `GET /actuator/health`: 상태 확인

이미지는 OpenAI `gpt-image-2`, 영상은 fal.ai Seedance 2.0 큐를 사용합니다.
생성된 이미지는 S3에 저장하며 영상은 비동기 요청 ID를 저장합니다.

필수 운영 설정과 EC2 배포 절차는 `docs/deployment.md`를 참고합니다.

각 도메인은 `presentation → application → domain` 방향으로 의존합니다.
외부 AI, 데이터베이스 및 스토리지는 `infrastructure` 어댑터가 담당합니다.

생성 요청의 `type`은 `IMAGE_GENERATION`, `VIDEO_GENERATION`, `REVERSE_PROMPT`,
`IMAGE_VARIATION`, `PROMPT_REGENERATION` 중 하나이며 등록된 사용자의 `userId`가 필요합니다.

전체 PostgreSQL 스키마는 `src/main/resources/db/migration/V1__initial_schema.sql`에서 관리합니다.
