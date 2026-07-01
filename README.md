# A1 생성형 AI 통합 엔진 Backend

Java 21과 Spring Boot 기반 도메인 중심 모듈형 모놀리스입니다.

## 협업 문서

- [요구사항](docs/requirements.md)
- [데이터베이스](docs/database.md)
- [배포](docs/deployment.md)

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

Windows에서는 `gradlew.bat bootRun`을 사용합니다.

## 초기 API

- `POST /api/v1/users`: 로컬 사용자 등록
- `GET /actuator/health`: 상태 확인

AI 공급자와 생성 처리 구현은 담당자 확정 후 추가합니다. 현재는 생성 작업, AI 모델,
미디어 관련 기본 엔티티와 DTO만 제공합니다.

필수 운영 설정과 EC2 배포 절차는 `docs/deployment.md`를 참고합니다.

각 도메인은 `presentation → application → domain` 방향으로 의존합니다.
외부 AI, 데이터베이스 및 스토리지는 `infrastructure` 어댑터가 담당합니다.

전체 PostgreSQL 스키마는 `src/main/resources/db/migration/V1__initial_schema.sql`에서 관리합니다.
