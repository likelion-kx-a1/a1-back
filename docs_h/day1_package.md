# 현재 패키지 아키텍처

현재 프로젝트(`a1-back`)의 백엔드 패키지 트리 구조입니다. 도메인 주도 설계(DDD) 및 헥사고날 아키텍처(Ports and Adapters)의 패키지 분리 방식을 따르고 있습니다.

```text
src/main/java/com/likelion/a1
├── A1BackApplication.java
│   
├── generation
│   ├── application
│   │   └── service
│   ├── domain
│   │   ├── model
│   │   └── repository
│   ├── infrastructure
│   │   └── persistence
│   └── presentation
│       ├── controller
│       └── dto
│
├── global
│   ├── config
│   ├── exception
│   └── response
│
├── library
│   ├── application
│   │   └── service
│   ├── domain
│   │   └── model
│   ├── infrastructure
│   │   └── persistence
│   └── presentation
│       ├── controller
│       └── dto
│
├── media
│   ├── application
│   │   ├── port/out
│   │   └── service
│   ├── domain
│   │   └── model
│   ├── infrastructure
│   │   ├── persistence
│   │   └── storage/s3
│   └── presentation
│       ├── controller
│       └── dto
│
├── project
│   ├── application
│   │   └── service
│   ├── domain
│   │   └── model
│   ├── infrastructure
│   │   └── persistence
│   └── presentation
│       ├── controller
│       └── dto
│
└── user
    ├── application
    │   └── service
    ├── domain
    │   ├── model
    │   └── repository
    ├── infrastructure
    │   └── persistence
    └── presentation
        ├── controller
        └── dto
```

## 디렉토리 설명

- **`global`**: 프로젝트 전반에 걸쳐 사용되는 공통 설정(`config`), 예외 처리(`exception`), 공통 응답(`response`) 구조가 포함되어 있습니다.
- **`도메인 패키지 (generation, library, media, project, user)`**: 각 도메인 별로 분리되어 있습니다.
  - **`presentation`**: 외부 요청(HTTP Web Request 등)을 받아들이고 반환하는 계층입니다. (Controller, DTO)
  - **`application`**: 비즈니스 유스케이스를 구현하는 계층입니다. (Service, Port Interfaces)
  - **`domain`**: 핵심 비즈니스 로직과 도메인 모델, 레포지토리 인터페이스 등을 포함합니다.
  - **`infrastructure`**: 외부 시스템(DB, S3 저장소 등)과의 연동 및 구현체(Adapter)를 담당합니다.
