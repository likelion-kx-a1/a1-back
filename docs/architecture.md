# Architecture

이 프로젝트는 DDD 전술 패턴을 단계적으로 적용하는 도메인 중심 모듈형 모놀리스다.

## Bounded Context

- `user`: 계정, 세션, 사용자 설정
- `project`: 사용자 작업 프로젝트
- `generation`: 생성 작업
- `library`: 라이브러리, 메시지, 첨부 파일
- `media`: 생성 미디어와 저장 파일

## Context 내부 구조

```text
context/
├── domain/
│   ├── model/          # Entity, Aggregate, Value Object, Enum
│   └── repository/     # Domain Repository Port
├── application/
│   ├── service/        # Use Case와 트랜잭션 경계
│   └── port/
│       ├── in/         # 외부에서 호출하는 Use Case Port
│       └── out/        # 저장소·외부 서비스 Port
├── presentation/
│   ├── controller/     # HTTP Adapter
│   └── dto/            # API Request/Response
└── infrastructure/
    ├── persistence/    # JPA Adapter
    ├── external/       # 외부 API Adapter
    └── storage/        # S3 등 저장소 Adapter
```

구현되지 않은 계층은 `package-info.java`만 두며, 기능을 추가할 때 실제 클래스로 대체한다.

## 의존성 규칙

```text
presentation → application → domain
infrastructure → application/domain
domain → 다른 계층 의존 금지
```

- Controller에서 JPA Repository를 직접 호출하지 않는다.
- Application Service가 Infrastructure 구현 클래스에 직접 의존하지 않는다.
- Repository 인터페이스는 Domain에 두고 JPA 구현은 Infrastructure에 둔다.
- 외부 시스템은 Application의 Output Port를 통해 호출한다.
- Context 사이에서 다른 Context의 Entity를 직접 참조하지 않고 식별자 또는 Event를 사용한다.
- API에서 Entity를 직접 반환하지 않는다.

현재 엔티티는 JPA 매핑과 Domain Model을 함께 사용한다. 업무 규칙이 복잡해질 경우 순수 Domain Model과
JPA Persistence Model 분리를 ADR로 검토한다.
