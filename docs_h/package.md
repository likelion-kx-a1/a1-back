역할: 시니어 Next.js 및 소프트웨어 아키텍트
목표: Next.js 14 App Router와 도메인 주도 설계(DDD) 및 계층형(Layered) 아키텍처를 결합한 프론트엔드/백엔드 하이브리드 프로젝트 패키지 구조 설계 및 보일러플레이트 작성.

[아키텍처 설계 원칙]
1. Next.js 14 App Router의 'app/' 디렉토리는 오직 라우팅, 페이지 엔트리, HTTP Request/Response 파싱, 그리고 Server Actions 정의라는 "인프라 및 프레젠테이션 진입점" 역할만 수행한다. 비즈니스 로직은 절대 'app/' 내부에 깊게 두지 않는다.
2. 모든 도메인 로직은 프로젝트 루트의 'src/modules/' 디렉토리 내에 도메인별로 캡슐화하여 격리한다.
3. 각 도메인은 계층형 아키텍처(Layered Architecture) 구조를 가진다:
   - domain: 도메인 엔티티 유형, 핵심 비즈니스 인터페이스, 순수 함수 도메인 비즈니스 룰.
   - application: Use Case 서비스 레이어. API 흐름 통제, 외부 서비스 오케스트레이션, 비동기 폴링 트리거 등 도메인 시나리오 조율.
   - infrastructure: 외부 API 연동(fal.ai, Gemini API client), Supabase Repository 구현체 등 구체적인 인프라스트럭처 디테일.
   - presentation: 해당 도메인 전용으로 재사용되는 클라이언트 UI 컴포넌트들.

[도메인 분류 정의]
- 'shared': 공통 유틸리티, 전역 UI 테마, 공통 통신 레이어.
- 'prompt': 번역, 프롬프트 가이드 생성, 안티그래비티 물리 엔진 프롬프트 변환, 이미지 역프롬프트 엔드포인트 연동.
- 'media': fal.ai 비동기 큐 호출, 이미지 생성, 비디오 생성 상태 기계(State Machine) 및 백엔드 스케줄 관리.
- 'library': 저장된 에셋의 검색, 필터링, 정렬, 스토리지 스트리밍 관리.

[요구사항]
위 아키텍처 요구사항을 충족하는 Next.js 14 프로젝트의 구체적인 디렉토리 구조(Tree)를 시각화하여 제시하고, 각 도메인과 레이어가 어떻게 소통하는지 직관적인 Import 흐름 가이드를 작성하라. 
또한 'src/modules/media/infrastructure/fal-client.ts' 및 'src/modules/media/application/generate-video.usecase.ts'에 대한 기본 인터페이스 및 구조 설계 예시 코드를 작성해라.

실용적 Spring Boot DDD + 계층형 하이브리드 아키텍처 가이드

이 가이드는 a1-back 프로젝트의 성공적인 MVP 완수를 위해 설계된 현실적인 아키텍처 가이드라인입니다. 무겁고 복잡한 헥사고날의 매핑 규칙을 걷어내고, Spring Boot의 강력한 기능과 인터페이스 기반 설계를 융합하여 빠른 개발 속도와 유연성을 동시에 확보합니다.

1. 실용적인 패키지 구조 (Pragmatic Hybrid)

기존에 구상하신 도메인 격리 방식을 유지하되, 내부 계층을 극도로 단순화하여 보일러플레이트 코드를 줄입니다.

src/main/java/com/likelion/a1
├── A1BackApplication.java
│
├── global                     # 전역 공통 설정 및 예외 처리
│   ├── config                 # AWS S3, Spring Security 등 설정
│   ├── exception              # 전역 예외 처리기 (GlobalExceptionHandler)
│   └── response               # API 공통 응답 포맷 (ApiResponse)
│
├── generation                 # 이미지 생성 및 프롬프트 보정 도메인
│   ├── presentation           # Controller, Request/Response DTO
│   ├── application            # Service (비즈니스 로직, 외부 API 오케스트레이션)
│   ├── domain                 # JPA Entities (도메인 모델 겸용), Repository 인터페이스
│   └── infrastructure         # Gemini API Client 구현체, S3 파일 업로더
│
├── media                      # fal.ai 영상 생성 도메인 (비동기 폴링 핵심)
│   ├── presentation
│   ├── application
│   ├── domain                 # VideoTask (비동기 작업 상태 추적 엔티티)
│   └── infrastructure         # fal.ai API Client 구현체
│
└── library                    # 보관함 조회 및 필터링 도메인
    ├── presentation
    ├── application
    └── domain


아키텍처의 핵심 규칙

JPA Entity = Domain Model: 순수 도메인 객체와 JPA 엔티티를 강제로 분리하지 않고 하나로 통합하여 매퍼 클래스 생성을 방지합니다.

Infrastructure의 추상화: 외부 API(Gemini, fal.ai, S3)와 결합하는 서비스는 반드시 application 계층에 인터페이스를 두고, 구현체만 infrastructure에 위치시킵니다. 이 구조가 API 키 없이 개발을 가능하게 만드는 핵심입니다.