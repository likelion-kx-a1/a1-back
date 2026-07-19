# API 연동 현황 & 현재 빌드 오류 정리

기준: `feature/api` 브랜치 HEAD(`ed77464`, `dev` 병합 완료) + 작업 트리에 스테이징된 AI 연동 계층 커밋 전 변경분. 실제 소스코드(`src/main/java`)와 `./gradlew compileJava` 실행 결과를 직접 확인하고 작성했다. 자세한 근거는 `docs_h/기술문서.md`, `docs_h/트러블슈팅_가이드.md` 참고.

> **2026-07-19 갱신**: 2장에 정리했던 컴파일 실패 5가지 원인을 모두 수정 완료했다. 이 문서 작성 시점에는 빌드가 깨져 있었으나, 지금은 `./gradlew compileJava` / `compileTestJava`가 정상적으로 통과한다.

---

## 1. 현재 연동된 API (AI 생성 계층)

패키지: `com.likelion.a1.generation`. `GenerationJob`(`generation_jobs` 테이블)에 요청/응답을 기록하면서 외부 AI API를 호출하는 구조다.

### 1.1 엔드포인트 (`GenerationController`, base `/api/v1/generation`)

컨트롤러 주석에 "Postman 통신 테스트용 엔드포인트"라고 명시되어 있다. **JWT 인증을 쓰지 않고 `userId`/`chatId`를 요청 바디에 직접 받는 임시 스펙**이며, `SecurityConfig`의 `permitAll` 목록에도 없어 실제로는 `anyRequest().authenticated()`에 걸린다(즉 호출하려면 유효한 JWT는 있어야 하지만, 그 안의 `userId`와 요청 바디의 `userId`가 다를 수 있어 신뢰 검증이 없다).

| Method | Path | 설명 | 내부 호출 |
|---|---|---|---|
| POST | `/prompts` | 이미지 + 지시문을 Claude에 보내 프롬프트 보정 텍스트를 받음 | `GenerationAiService.generatePrompt` → `PromptGenerationPort` |
| POST | `/reverse-prompts` | 이미지 + 지시문을 GPT Vision에 보내 역프롬프트(이미지 분석) 텍스트를 받음 | `GenerationAiService.analyzeImage` → `ImageAnalysisPort` |
| POST | `/fal-jobs` | fal.ai 큐에 이미지/영상 생성 작업 제출 | `GenerationAiService.submitFalJob` → `FalGenerationPort.submit` |
| GET | `/fal-jobs/{jobId}/status` | 제출한 fal.ai 작업 상태 폴링(수동) | `GenerationAiService.pollFalJob` → `FalGenerationPort.poll` |

요청 DTO(`AiClientTestDtos`)는 모두 `userId`, `chatId`를 `@NotNull`로 요구하고, `/prompts`·`/reverse-prompts`는 이미지를 `imageBase64`(Base64 문자열)로 받는다. `/fal-jobs`는 `jobType`(= `GenerationType` enum 이름 문자열), `modelCode`(fal.ai 카탈로그 모델 슬러그), `input`(Map)을 받는다.

### 1.2 외부 API 연동 방식

- **Claude** (`ClaudePromptGenerationAdapter`): `RestClient`로 `POST /v1/messages` 호출. 헤더 `x-api-key`(키), `anthropic-version`(`app.ai.claude.version`), 모델은 `app.ai.claude.model`. 이미지+텍스트를 한 메시지에 담아 보내고, 응답의 `content[0].text`를 추출해 반환한다.
- **GPT Vision** (`GptVisionImageAnalysisAdapter`): `POST /v1/chat/completions`. 이미지를 `data:{mimeType};base64,...` 형태의 data URL로 변환해 `image_url` 콘텐츠 파트로 전송. 헤더 `Authorization: Bearer {키}`, 모델은 `app.ai.openai.model`.
- **fal.ai** (`FalGenerationAdapter`): 헤더 `Authorization: Key {키}`. 제출은 `POST /{modelCode}`, 상태 조회는 `GET /{modelCode}/requests/{requestId}/status`. `modelCode`가 `fal-ai/kling-video/o3/pro/text-to-video`처럼 슬래시를 포함하므로, URI 템플릿 변수로 넘기지 않고 문자열을 직접 이어붙여 `%2F` 인코딩 문제를 피한다.
- **`AiClientConfig`**: 위 세 개의 `RestClient` 빈(`falRestClient`, `claudeRestClient`, `openAiRestClient`)을 base-url + 인증 헤더와 함께 구성한다.
- 세 어댑터 모두 `@Profile("!local")`이며, `local` 프로필에서는 `MockPromptGenerationAdapter` / `MockImageAnalysisAdapter` / `MockFalGenerationAdapter`가 대신 등록되어 실제 API 호출 없이 즉시 가짜 응답을 반환한다(비용 $0).

### 1.3 비동기 처리 — fal.ai 폴링 스케줄러

`GenerationVideoPollingScheduler`가 5초 간격(`@Scheduled(fixedDelay = 5000)`)으로 상태가 `QUEUED`/`PROCESSING`인 `IMAGE_GENERATION`/`VIDEO_GENERATION` 타입 `GenerationJob`을 자동으로 재폴링한다. `COMPLETED`로 확인되면 fal.ai 응답에서 미디어 URL을 찾아 `MediaStoragePort.storeFromUrl(...)`로 S3(또는 로컬 Mock)에 업로드하고 작업을 완료 처리한다. 즉 `/fal-jobs/{jobId}/status` GET 엔드포인트는 수동 확인용이고, 실제 완료 감지와 미디어 저장은 이 스케줄러가 백그라운드로 수행한다.

### 1.4 아직 연결되지 않은 부분

- 위 어떤 흐름도 `GenerationResultService`(채팅 메시지·`GeneratedAsset` 저장 담당)를 호출하지 않는다. 즉 AI 호출 결과가 `GenerationJob` 테이블에는 쌓이지만, **채팅 메시지로는 아직 자동으로 이어지지 않는다.**
- 관련 설정: `app.ai.fal.base-url`/`api-key`, `app.ai.claude.base-url`/`api-key`/`model`/`version`, `app.ai.openai.base-url`/`api-key`/`model` (`application.yml`, 기본 키 값은 빈 문자열). `.env.example`에는 `OPENAI_API_KEY`/`FAL_API_KEY`는 있지만 `ANTHROPIC_API_KEY` 항목이 빠져 있다.

---

## 2. 현재 서버 빌드 시 발생했던 오류 — ✅ 해결됨 (2026-07-19)

`./gradlew compileJava`가 실패하던 문제를 원인별로 모두 수정했고, 지금은 `./gradlew compileJava` / `compileTestJava` 둘 다 정상적으로 통과한다. AI API 키나 DB 설정 문제가 아니라 **자바 문법 자체가 깨진 상태**였던 것이라, 코드/설정이 아니라 소스 파일을 직접 고쳐야 했다.

### 최초 증상

```
ErrorCode.java:16: error: enum constant not expected here
S3MediaStorageAdapter.java:55: error: illegal start of expression
S3MediaStorageAdapter.java:79: error: ';' expected
S3MediaStorageAdapter.java:84: error: ';' expected / not a statement
S3MediaStorageAdapter.java:131: error: illegal start of expression
9 errors
```

### 원인과 수정 내역 — `dev` → `feature/api` 병합(`ed77464`)이 파일들을 충돌 마커 없이 뒤섞어 놓음

`dev`와 `feature/api`가 각각 독립적으로 수정했던 파일을 병합하면서, 두 버전의 코드가 정리되지 않고 그대로 이어붙은 상태로 커밋되어 있었다. 파싱 에러부터 순서대로 고치면서 뒤에 가려져 있던 에러가 하나씩 드러났다.

1. **`global/exception/ErrorCode.java`** — `AI_PROVIDER_UNAVAILABLE`/`USER_NOT_FOUND` enum 상수가 두 번씩 선언되어 있었다(`feature/api`가 쓰던 `GENERATION-*`/`PROVIDER-*` 코드 스타일과 `dev`가 쓰던 enum-이름-그대로 코드 스타일이 둘 다 남아 있던 것). → 중복 선언 제거, `USER_NOT_FOUND`는 기존 `USER-002` 코드로 통일.
2. **`media/infrastructure/storage/s3/S3MediaStorageAdapter.java`** — `feature/api` 버전(`upload(StorageUploadCommand)`)과 `dev` 버전(`store`/`storeFromUrl`)의 메서드 몸통이 서로의 안쪽으로 잘려 들어가 있었다. 호출부를 확인해보니 `FileUploadService`는 `upload(...)`만, `GenerationVideoPollingScheduler`(1.3절)는 `storeFromUrl(...)`만 쓰고 `store(byte[], ...)`는 어디서도 호출되지 않는 죽은 메서드였다. → `MediaStoragePort`에서 `store(...)` 제거, `S3MediaStorageAdapter`를 `upload`/`storeFromUrl` 두 메서드로 깔끔하게 재작성.
3. **`MockMediaStorageAdapter`(로컬용)** — `store`/`storeFromUrl`만 구현하고 `upload`는 구현하지 않아 `MediaStoragePort`를 완전히 구현하지 못하고 있었다. → `upload(StorageUploadCommand)` 구현 추가.
4. **`generation/presentation/dto/GenerationJobDtos.java`** — `Response.from(GenerationJob job)`이 `Response` 레코드의 필드 14개(특히 `imageCategory`) 대비 생성자 인자를 13개만 채워 넘기고 있었다. → `job.getImageCategory()`를 제자리에 추가.
5. **`generation/domain/model/GenerationJob.java`(위 넷을 고친 뒤 새로 드러남)** — DB 마이그레이션 `V4`가 `job_type` 컬럼을 `generation_type`으로 리네임했는데, `create()` 팩토리 메서드는 여전히 존재하지 않는 `jobType` 필드에 대입하고 있었고(`job.jobType = jobType;`), `GenerationVideoPollingScheduler`와 `GenerationJobDtos.Response.from()`도 옛 게터 이름(`getJobType()`)을 참조하고 있었다. → 필드 대입을 `job.generationType = jobType;`로, 호출부 3곳을 `getGenerationType()`으로 통일(다른 도메인의 `Chat`/`ChatMessage`와 이름 규칙 일치).

### 검증

위 다섯 가지를 모두 고친 뒤 `./gradlew compileJava`, `./gradlew compileTestJava` 둘 다 에러 없이 통과하는 것을 확인했다. (단, DB/외부 API가 필요한 실제 런타임 동작까지 이 세션에서 검증한 것은 아니다.)

자세한 재현 로그와 수정 근거는 `docs_h/트러블슈팅_가이드.md` 7~9장, `docs_h/기술문서.md` 0장·7.1절·9.8절을 참고.

---

## 3. 이번 커밋 대상 파일 15개 역할 정리

`git status`에 `A`(신규 추가)로 표시된 15개 파일. AI 연동 계층(포트 6개, 어댑터 3개, 서비스 2개, 컨트롤러/DTO 2개, 설정 1개) + 스냅샷 문서 1개로 구성된다.

### 3.1 포트 (`generation/application/port/out`) — 외부 API 의존성을 분리하는 인터페이스/값 객체

| 파일 | 역할 |
|---|---|
| `PromptGenerationPort.java` | Claude 프롬프트 보정 기능의 인터페이스. `generateFromImage(imageBytes, mimeType, instruction)` 한 메서드만 정의해 `GenerationAiService`가 실제 구현체(Claude/Mock)를 몰라도 되게 한다. |
| `ImageAnalysisPort.java` | GPT Vision 역프롬프트(이미지 분석) 기능의 인터페이스. `analyze(imageBytes, mimeType, instruction)`. |
| `FalGenerationPort.java` | fal.ai 큐 작업 제출(`submit`)과 상태 폴링(`poll`)을 정의하는 인터페이스. |
| `AiTextGenerationResult.java` | Claude/GPT Vision 호출 결과를 담는 값 객체(record) — 추출된 텍스트(`text`)와 원본 응답(`rawResponse`). |
| `FalGenerationSubmission.java` | fal.ai 제출 응답 값 객체 — `externalRequestId`, `statusUrl`, `responseUrl`, 원본 응답. |
| `FalGenerationStatus.java` | fal.ai 폴링 응답 값 객체 — 상태 문자열(`status`)과 원본 응답. |

### 3.2 어댑터 (`generation/infrastructure/client/**`) — 포트를 실제 외부 API 호출로 구현

| 파일 | 역할 |
|---|---|
| `ClaudePromptGenerationAdapter.java` | `PromptGenerationPort` 구현체. `RestClient`로 Anthropic `POST /v1/messages`를 호출해 프롬프트 보정 텍스트를 받는다. `@Profile("!local")`. |
| `GptVisionImageAnalysisAdapter.java` | `ImageAnalysisPort` 구현체. OpenAI `POST /v1/chat/completions`를 호출해 이미지 분석 텍스트를 받는다. `@Profile("!local")`. |
| `FalGenerationAdapter.java` | `FalGenerationPort` 구현체. fal.ai 큐 API에 작업 제출/상태 폴링을 수행한다. `@Profile("!local")`. |

### 3.3 애플리케이션 서비스 (`generation/application/service`)

| 파일 | 역할 |
|---|---|
| `GenerationAiService.java` | 세 포트를 조합해 4개 유스케이스(`generatePrompt`/`analyzeImage`/`submitFalJob`/`pollFalJob`)를 제공. 각 유스케이스는 "`GenerationJob` 저장 → 외부 호출(트랜잭션 밖) → 결과 저장"을 순서대로 처리하고 실패 시 `job.fail(...)` 후 예외를 다시 던진다. |
| `GenerationVideoPollingScheduler.java` | 5초 간격으로 `QUEUED`/`PROCESSING` 상태인 fal.ai 기반 이미지/영상 작업을 자동 재폴링하는 스케줄러. `COMPLETED` 확인 시 임시 미디어 URL을 `MediaStoragePort`로 S3(또는 Mock)에 업로드하고 작업을 완료 처리한다. |

### 3.4 프레젠테이션 계층

| 파일 | 역할 |
|---|---|
| `GenerationController.java` | `/api/v1/generation/**` 4개 엔드포인트(`/prompts`, `/reverse-prompts`, `/fal-jobs`, `/fal-jobs/{jobId}/status`)를 노출하는 REST 컨트롤러. 주석에 명시된 대로 Postman 테스트용이며 `userId`/`chatId`를 요청 바디로 직접 받는다. |
| `AiClientTestDtos.java` | 위 컨트롤러가 쓰는 요청 DTO 3종(`PromptGenerationRequest`, `ImageAnalysisRequest`, `FalJobRequest`) — 검증 애너테이션(`@NotNull`/`@NotBlank`/`@NotEmpty`) 포함. |

### 3.5 설정

| 파일 | 역할 |
|---|---|
| `global/config/AiClientConfig.java` | fal/Claude/OpenAI 각각의 `RestClient` 빈을 base-url + 인증 헤더(`Key`/`x-api-key`/`Bearer`)와 함께 등록하는 설정 클래스. 위 세 어댑터가 `@Qualifier`로 주입받아 쓴다. |

### 3.6 기타

| 파일 | 역할 |
|---|---|
| `gemini_sync_status.txt` | 기획/아키텍처 설계를 담당하는 Gemini AI가 백엔드 현재 상태를 코드 기준으로 파악하도록 만든 스냅샷 리포트(패키지 트리 등). 소스코드가 아니라 문서성 파일이며, 이번 커밋의 다른 14개 파일과 달리 애플리케이션 동작에는 영향이 없다. **커밋에 포함할지 여부는 별도 판단이 필요** — 팀 문서 공유 목적이 아니라면 저장소에 남길 필요가 없을 수 있다. |

> 참고: 이 15개 파일 자체에는 문법 오류가 없었다. 2장에서 설명한 컴파일 실패는 이 15개 파일이 아니라 **병합으로 이미 커밋되어 있던** `ErrorCode.java`/`S3MediaStorageAdapter.java`(및 `GenerationJob.java`)에서 발생했다. 이 15개 파일 중 `GenerationVideoPollingScheduler`가 의존하는 `MediaStoragePort.storeFromUrl`이 그 깨진 파일들과 맞물려 있었지만, 2장의 문제를 모두 고쳐 지금은 이 15개 파일을 포함한 전체 빌드가 통과한다.
