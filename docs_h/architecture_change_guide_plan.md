# 아키텍처 변경 방안 가이드 문서 작성 계획 (Architecture Refactoring Guide Plan)

현재 `a1-back` 프로젝트의 백엔드 디렉토리 구조를 분석하고, 이를 DDD(도메인 주도 설계) 및 계층형(Layered) 하이브리드 아키텍처로 변환하여 외부 API, EC2, S3 버킷 저장소 등 인프라를 효율적으로 결합하는 구체적인 변경 방안을 담은 가이드라인 마크다운 파일을 작성합니다.

## Proposed Changes

### Documentation Component

#### [NEW] [architecture_change_guide.md](file:///c:/kx/a1-back/docs_h/architecture_change_guide.md)

작성할 문서의 주요 구성 항목은 다음과 같습니다:

1. **현재 `src` 패키지 구조 분석**:
   - `generation`, `media`, `library`, `user`, `global` 등 각 모듈의 역할과 사용 용도 분석.
   - 헥사고날(Ports & Adapters)과 레이어드 혼합 형태의 현재 코드 패턴 평가.
2. **인프라 및 연결 설계 분석**:
   - 외부 API 연동(Gemini API, fal.ai 등)에 대한 추상화 계층 설계.
   - S3 미디어 스토리지 설계 및 EC2 연동 로직 분석.
3. **계층형 DDD 변경 방안 (Pragmatic Hybrid)**:
   - 복잡한 헥사고날 매핑 대신 Entity와 Domain Model을 일치시키는 등 실용적 아키텍처 제언.
   - `application` 계층에 인터페이스(Interface) 선언, `infrastructure`에 구현체(Implementation)를 배치하여 제어의 역전(IoC) 활용 방안 제시.
   - 변경 후의 권장 패키지 트리 시각화.

## Verification Plan

### Manual Verification
- `docs_h/architecture_change_guide.md` 마크다운 문서 생성을 완료한 후, 목차 및 마크다운 문법 오류가 없는지 파악하고 가독성 검토.
- 사용자에게 변경안 가이드를 검토받고 피드백을 반영함.
