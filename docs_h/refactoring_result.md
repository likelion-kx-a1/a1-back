# 백엔드 아키텍처 리팩토링 변경 결과 보고서

이 문서는 `a1-back` 백엔드 프로젝트의 아키텍처 변경(계층형 DDD 하이브리드 아키텍처 도입) 작업 내역, 변경 이유, 수정한 패키지 구조 및 사용법을 상세히 기술합니다.

---

## 1. 아키텍처 변경 배경 및 이유 (Why)

기존 프로젝트는 **헥사고날 아키텍처(Ports & Adapters)** 패턴을 충실히 규격화하여 아래와 같이 하나의 Repository 처리를 위해 여러 레이어의 파일들을 생성해 왔습니다.

* 기존 흐름: `Service` -> `domain.repository.UserRepository (Port 인터페이스)` -> `infrastructure.persistence.UserRepositoryAdapter (Adapter 구현체)` -> `infrastructure.persistence.SpringDataUserRepository (Spring Data JPA 인터페이스)`

### 헥사고날 구조의 문제점
1. **과도한 보일러플레이트 코드**: 단순 CRUD 성격이 강한 소형 도메인 기능 개발 시에도 최소 4개 이상의 인터페이스 및 클래스를 작성하고 매번 복사/매핑 코드를 정의해야 하므로 개발 속도가 크게 저하되었습니다.
2. **불필요한 추상화막 형성**: 거의 모든 JPA 엔티티가 1:1 관계의 어댑터만을 가지고 있어, 계층을 통과하여 단순 호출을 위임할 분 실제 추상화로서의 효용이 낮았습니다.

### 변경 방향
이에 따라 `docs_h/architecture_change_guide.md` 가이드에 명시된 **Pragmatic Hybrid Layered DDD** 구조를 정합적으로 이식하였습니다. 
도메인의 핵심 비즈니스를 주도하는 엔티티(Entity)는 도메인 영역에 위치시키고, 실제 데이터베이스 제어의 상세 기술인 Spring Data JPA Repository(Spring 인터컨테이너 빈)는 **Infrastructure**의 영속성 패키지에 집약시킨 후, **Application Service**가 이를 직접 주입받아 사용하도록 구성하여 중간 어댑터 보일러플레이트 레이어를 제거했습니다.

---

## 2. 변경된 패키지 구조 및 파일 목록 (Before & After)

### Before (헥사고날 보일러플레이트 레이어)
```text
com/likelion/a1/
├── user
│   ├── application/service/UserCommandService.java   # UserRepository(Port) 의존
│   ├── domain/repository/UserRepository.java         # [Port 인터페이스]
│   └── infrastructure/persistence/
│       ├── SpringDataUserRepository.java             # [Spring Data JPA 인터페이스]
│       └── UserRepositoryAdapter.java                 # [Adapter 구현체]
└── generation
    ├── domain/repository/GenerationJobRepository.java # [Port 인터페이스]
    └── infrastructure/persistence/
        ├── SpringDataGenerationJobRepository.java     # [Spring Data JPA 인터페이스]
        └── GenerationJobRepositoryAdapter.java         # [Adapter 구현체]
```

### After (실용적 계층형 DDD 하이브리드 아키텍처)
보일러플레이트인 Port 인터페이스 및 Adapter 구현체를 전면 제거하고 단일 Spring Data JPA Repository 인터페이스로 심플하게 통합했습니다.

```text
com/likelion/a1/
├── user
│   ├── application/service/UserCommandService.java   # UserRepository를 직접 의존
│   └── infrastructure/persistence/
│       └── UserRepository.java                       # [통합 Spring Data JPA 리포지토리 인터페이스]
└── generation
    └── infrastructure/persistence/
        └── GenerationJobRepository.java              # [통합 Spring Data JPA 리포지토리 인터페이스]
```

### 상세 파일 처리 결과
* **삭제된 파일 (총 6개)**:
  * `com/likelion/a1/user/domain/repository/UserRepository.java`
  * `com/likelion/a1/user/infrastructure/persistence/UserRepositoryAdapter.java`
  * `com/likelion/a1/user/infrastructure/persistence/SpringDataUserRepository.java`
  * `com/likelion/a1/generation/domain/repository/GenerationJobRepository.java`
  * `com/likelion/a1/generation/infrastructure/persistence/GenerationJobRepositoryAdapter.java`
  * `com/likelion/a1/generation/infrastructure/persistence/SpringDataGenerationJobRepository.java`
* **신규/생성된 파일 (총 2개)**:
  * `com/likelion/a1/user/infrastructure/persistence/UserRepository.java` (Spring Data JPA 통합 리포지토리)
  * `com/likelion/a1/generation/infrastructure/persistence/GenerationJobRepository.java` (Spring Data JPA 통합 리포지토리)
* **수정된 파일 (총 1개)**:
  * `com/likelion/a1/user/application/service/UserCommandService.java`

---

## 3. 새로운 아키텍처 사용 방법 및 개발 가이드라인 (How-To)

앞으로 새로운 도메인 개발 또는 CRUD 영속성 계층을 설계할 때 다음과 같은 절차를 따릅니다.

### 1) JPA Entity(Domain Model) 및 Repository 추가
새로운 도메인(예: `product`) 엔티티와 영속성 처리를 구성할 경우:
* JPA Entity는 `domain/model` 패키지에 생성합니다.
* Spring Data JPA Repository는 `infrastructure/persistence` 패키지에 바로 구현하며, `JpaRepository`를 직접 상속받습니다.

```java
// src/main/java/com/likelion/a1/user/infrastructure/persistence/UserRepository.java
package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  // Spring Data JPA 명명 규칙을 활용한 사용자 검색 로직 바로 추가
  boolean existsByEmailIgnoreCase(String email);
}
```

### 2) 서비스 계층(Application Service)에서 Repository 사용
* 생성한 JPA Repository를 사용해야 하는 `Application Service`에서는 생성자 주입 형태로 리포지토리를 바로 주입받아 사용합니다.
* 더는 중간 Adapter나 Port 인터페이스를 별도로 생성하여 우회하지 않습니다.

```java
// src/main/java/com/likelion/a1/user/application/service/UserCommandService.java
package com.likelion.a1.user.application.service;

import com.likelion.a1.user.infrastructure.persistence.UserRepository; // 직접 import
...

@Service
public class UserCommandService {
  private final UserRepository repository; // 바로 사용 가능

  public UserCommandService(UserRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public User register(String email, String password, String nickname) {
    if (repository.existsByEmailIgnoreCase(email)) { // 메서드 직접 호출
      throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
    }
    return repository.save(User.local(email, password, nickname));
  }
}
```

### 3) ⚠️ [주의 및 예외 규칙] 복잡한 외부 인프라 연동
* 이 리팩토링은 데이터베이스 CRUD 연동 등 **프라이머리 데이터 레이어**에 대응하는 단순화 조치입니다.
* 반면, AWS S3 스토리지 연동, 외부 AI API(Gemini, fal.ai) client 연동 등 **세부 구현 기술(SDK, HTTP Client 등)에 종속성이 크고 목업 테스팅 격리가 필수적인 외부 아웃바운드 인프라**는 그대로 `application/port/out` 및 `infrastructure/client` 포트/어댑터 형식을 유지하여 책임을 명확하게 분할합니다.

---

## 4. 변경 내용 검증 결과
* **프로젝트 컴파일 및 자동 테스트**: `./gradlew test`를 수행하여 Spring Boot 컨텍스트 정상 로드 및 SQL 바인딩 런타임 검증을 정상 완료하였습니다.
