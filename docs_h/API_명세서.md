# a1-back Generation API 명세서

`com.likelion.a1.generation.presentation` 패키지의 실제 소스코드(`GenerationController`, `AiClientTestDtos`, `GenerationJobDtos`, `ApiResponse`, `ErrorCode`, `GlobalExceptionHandler`)를 기준으로 작성된 명세서입니다. 코드가 변경되면 이 문서도 함께 갱신해야 합니다.

## 공통 사항

### Base URL

```
/api/v1/generation
```

### 공통 응답 래퍼 (`ApiResponse<T>`)

모든 성공 응답은 아래 포맷으로 감싸져 내려갑니다.

```jsonc
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    /* 각 API별 data 필드 참고 */
  },
  "timestamp": "2026-07-13T10:00:00+09:00",
}
```

### 공통 에러 응답 포맷 (`ErrorResponse`)

```jsonc
{
  "success": false,
  "error": {
    "code": "GENERATION-001",
    "message": "생성 작업을 찾을 수 없습니다.",
    "details": [], // 필드 검증 실패 시 "필드명: 메시지" 형태의 문자열 배열
  },
  "timestamp": "2026-07-13T10:00:00+09:00",
}
```

### 인증에 대한 주의사항

`userId`, `chatId`는 아직 JWT 인증 및 채팅방 생성 API가 붙지 않아 **요청 바디로 직접 전달**받는 임시 방식입니다. 추후 인증이 붙으면 `userId`는 세션/토큰에서, `chatId`는 실제 채팅 생성 플로우에서 얻어오는 방식으로 변경될 예정이니 프론트엔드에서는 이 두 필드가 임시 스펙임을 인지하고 연동해야 합니다.

### `GenerationJob` 공통 응답 필드 (`GenerationJobDtos.Response`)

아래 4개 API가 공통으로 반환하는 `data` 객체의 필드 정의입니다.

| 필드               | 타입                     | 설명                                                                                                                  |
| ------------------ | ------------------------ | --------------------------------------------------------------------------------------------------------------------- |
| `id`               | number                   | 생성 작업(GenerationJob) PK                                                                                           |
| `userId`           | number                   | 요청한 사용자 ID                                                                                                      |
| `chatId`           | number                   | 연결된 채팅방 ID                                                                                                      |
| `aiModelId`        | number \| null           | 사용된 AI 모델 ID (현재 미사용, 항상 null)                                                                            |
| `requestMessageId` | number \| null           | 연결된 채팅 메시지 ID (현재 미사용, 항상 null)                                                                        |
| `jobType`          | string                   | 작업 종류. `PROMPT_REGENERATION`, `REVERSE_PROMPT`, `IMAGE_GENERATION`, `VIDEO_GENERATION`, `IMAGE_VARIATION` 중 하나 |
| `prompt`           | string \| null           | 사용자 지시문(instruction) 또는 fal.ai 입력의 `prompt` 값                                                             |
| `responsePayload`  | object \| null           | AI 응답 원본/가공 데이터. API별로 스키마가 다름(각 API 항목 참고)                                                     |
| `status`           | string                   | `PENDING`, `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELED`, `EXPIRED` 중 하나                               |
| `errorMessage`     | string \| null           | 실패 시 원인 메시지                                                                                                   |
| `startedAt`        | string(ISO-8601)         | 작업 생성/시작 시각                                                                                                   |
| `completedAt`      | string(ISO-8601) \| null | 완료 또는 실패 확정 시각                                                                                              |
| `createdAt`        | string(ISO-8601)         | 레코드 생성 시각                                                                                                      |

---

## 1. 프롬프트 보정 (Claude 기반)

### API 개요

사용자가 업로드한 참고 이미지와 한국어 지시문을 Claude API에 전달해, 안티그래비티 연출이 반영된 영문 프롬프트로 보정한다. 동기(synchronous) 방식이며 호출 즉시 결과가 확정된다. `jobType`은 `PROMPT_REGENERATION`으로 저장된다.

### HTTP Method & URL

```
POST /api/v1/generation/prompts
```

### 요청 헤더

```
Content-Type: application/json
```

### 요청 바디 예시

```jsonc
{
  "userId": 1, // number, 필수 - 요청 사용자 ID (임시 스펙, 인증 붙기 전까지 직접 전달)
  "chatId": 10, // number, 필수 - 채팅방 ID (임시 스펙)
  "imageBase64": "iVBORw0KGgoAAAANS...", // string, 필수 - 참고 이미지 원본 바이트의 Base64 인코딩 문자열
  "mimeType": "image/png", // string, 필수 - 이미지 MIME 타입 (예: image/png, image/jpeg)
  "instruction": "우주에서 떠다니는 컵을 그려줘", // string, 필수 - 사용자의 한국어 원본 지시문
}
```

### 성공 응답 예시 (200 OK)

```jsonc
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 101,
    "userId": 1,
    "chatId": 10,
    "aiModelId": null,
    "requestMessageId": null,
    "jobType": "PROMPT_REGENERATION",
    "prompt": "우주에서 떠다니는 컵을 그려줘",
    "responsePayload": {
      "text": "A floating cup in zero gravity, anti-gravity physics, cinematic lighting, ultra-detailed, 8k",
      "raw": {
        /* Claude Messages API 원본 응답(JSON) 전체 */
      },
    },
    "status": "COMPLETED",
    "errorMessage": null,
    "startedAt": "2026-07-13T10:00:00+09:00",
    "completedAt": "2026-07-13T10:00:03+09:00",
    "createdAt": "2026-07-13T10:00:00+09:00",
  },
  "timestamp": "2026-07-13T10:00:03+09:00",
}
```

- `responsePayload.text` : Claude가 보정/번역한 최종 영문 프롬프트 (프론트가 다음 단계인 이미지 생성 요청의 `input.prompt`로 그대로 사용하면 됨)
- `responsePayload.raw` : Claude API 원본 응답 (디버깅용, 프론트에서 파싱 불필요)

### 실패 및 예외 응답

| 상황                                                   | HTTP Status | code           | message                        | 비고                                                                                                                |
| ------------------------------------------------------ | ----------- | -------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| 요청 바디 필드 검증 실패 (`@NotNull`/`@NotBlank` 위반) | 400         | `COMMON-001`   | 입력값이 올바르지 않습니다.    | `details`에 `"필드명: 메시지"` 형태로 위반 필드 목록 포함                                                           |
| 존재하지 않는 `userId`로 요청 (FK 위반)                | 404         | `USER-002`     | 존재하지 않는 사용자입니다.    | `GenerationJob` 최초 저장 시 `user_id` FK 위반 → `DataIntegrityViolationException` → 전역 핸들러가 변환             |
| 존재하지 않는 `chatId`로 요청 (FK 위반)                | 404         | `CHAT-001`     | 존재하지 않는 채팅방입니다.    | 위와 동일하되 `chat_id` FK 위반인 경우                                                                              |
| Claude API 호출 실패 (외부 API가 비-2xx 응답)          | 502         | `PROVIDER-002` | AI 공급자 호출에 실패했습니다. | `details`에 `"{HTTP상태}: {응답바디}"` 포함. 이 경우 GenerationJob은 `FAILED` 상태로 DB에 저장된 뒤 예외가 재발생함 |

---

## 2. 이미지 역프롬프트 (GPT Vision 기반)

### API 개요

업로드된 이미지를 GPT Vision(Chat Completions) API로 분석해, 해당 이미지를 재현할 수 있는 역프롬프트(reverse prompt) 텍스트를 추출한다. 동기 방식이며 `jobType`은 `REVERSE_PROMPT`로 저장된다.

### HTTP Method & URL

```
POST /api/v1/generation/reverse-prompts
```

### 요청 헤더

```
Content-Type: application/json
```

### 요청 바디 예시

```jsonc
{
  "userId": 1, // number, 필수 - 요청 사용자 ID (임시 스펙)
  "chatId": 10, // number, 필수 - 채팅방 ID (임시 스펙)
  "imageBase64": "iVBORw0KGgoAAAANS...", // string, 필수 - 분석 대상 이미지의 Base64 인코딩 문자열
  "mimeType": "image/jpeg", // string, 필수 - 이미지 MIME 타입
  "instruction": "이 이미지의 스타일과 구도를 설명해줘", // string, 필수 - 분석 지시문(질문)
}
```

### 성공 응답 예시 (200 OK)

```jsonc
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 102,
    "userId": 1,
    "chatId": 10,
    "aiModelId": null,
    "requestMessageId": null,
    "jobType": "REVERSE_PROMPT",
    "prompt": "이 이미지의 스타일과 구도를 설명해줘",
    "responsePayload": {
      "text": "A minimalist product photo of a ceramic cup, soft studio lighting, shallow depth of field...",
      "raw": {
        /* OpenAI Chat Completions API 원본 응답(JSON) 전체 */
      },
    },
    "status": "COMPLETED",
    "errorMessage": null,
    "startedAt": "2026-07-13T10:05:00+09:00",
    "completedAt": "2026-07-13T10:05:02+09:00",
    "createdAt": "2026-07-13T10:05:00+09:00",
  },
  "timestamp": "2026-07-13T10:05:02+09:00",
}
```

- `responsePayload.text` : GPT Vision이 생성한 역프롬프트/이미지 설명 텍스트
- `responsePayload.raw` : OpenAI API 원본 응답 (디버깅용)

### 실패 및 예외 응답

| 상황                                              | HTTP Status | code           | message                        | 비고                                           |
| ------------------------------------------------- | ----------- | -------------- | ------------------------------ | ---------------------------------------------- |
| 요청 바디 필드 검증 실패                          | 400         | `COMMON-001`   | 입력값이 올바르지 않습니다.    | `details`에 위반 필드 목록 포함                |
| 존재하지 않는 `userId`로 요청 (FK 위반)           | 404         | `USER-002`     | 존재하지 않는 사용자입니다.    | `GenerationJob` 최초 저장 시 `user_id` FK 위반 → 전역 핸들러가 변환 |
| 존재하지 않는 `chatId`로 요청 (FK 위반)           | 404         | `CHAT-001`     | 존재하지 않는 채팅방입니다.    | 위와 동일하되 `chat_id` FK 위반인 경우          |
| GPT Vision API 호출 실패 (외부 API가 비-2xx 응답) | 502         | `PROVIDER-002` | AI 공급자 호출에 실패했습니다. | GenerationJob은 `FAILED`로 저장 후 예외 재발생 |

---

## 3. fal.ai 비동기 작업 제출 (이미지/영상 생성)

### API 개요

fal.ai 큐 API에 이미지 생성(예: `openai/gpt-image-2`) 또는 영상 생성(예: `fal-ai/kling-video/o3/pro/text-to-video`) 작업을 비동기로 제출한다. 제출 즉시 `request_id`만 확보하고 `QUEUED` 상태로 응답하며, 실제 완료 여부는 아래 4번 API(`GET .../status`)로 폴링하거나 백엔드의 5초 주기 백그라운드 스케줄러가 자동으로 갱신한다.

### HTTP Method & URL

```
POST /api/v1/generation/fal-jobs
```

### 요청 헤더

```
Content-Type: application/json
```

### 요청 바디 예시

```jsonc
{
  "userId": 1, // number, 필수 - 요청 사용자 ID (임시 스펙)
  "chatId": 10, // number, 필수 - 채팅방 ID (임시 스펙)
  "jobType": "VIDEO_GENERATION", // string, 필수 - GenerationType Enum 값. IMAGE_GENERATION | VIDEO_GENERATION | REVERSE_PROMPT | IMAGE_VARIATION | PROMPT_REGENERATION 중 하나 (유효하지 않은 문자열이면 400)
  "modelCode": "fal-ai/kling-video/o3/pro/text-to-video", // string, 필수 - fal.ai 카탈로그 모델 슬러그
  "input": {
    // object, 필수(비어있으면 안 됨) - fal.ai 모델별 입력 페이로드를 그대로 전달
    "prompt": "A floating cup in zero gravity, anti-gravity physics, cinematic lighting",
    "duration": 5,
    "aspect_ratio": "16:9",
  },
}
```

> `input.prompt`는 있으면 `GenerationJob.prompt` 컬럼에 그대로 저장되고, 나머지 키는 fal.ai에 그대로 전달된다.

### 성공 응답 예시 (200 OK)

> 실제로는 즉시 값이 확정되지 않는 비동기 작업이지만, 현재 컨트롤러 구현은 200 OK로 응답한다 (202 Accepted 아님. 이는 프론트가 알고 있어야 하는 실제 구현 스펙).

```jsonc
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 103,
    "userId": 1,
    "chatId": 10,
    "aiModelId": null,
    "requestMessageId": null,
    "jobType": "VIDEO_GENERATION",
    "prompt": "A floating cup in zero gravity, anti-gravity physics, cinematic lighting",
    "responsePayload": {
      "requestId": "req_abcd1234", // fal.ai가 발급한 request_id. 4번 API 폴링에는 필요 없음(jobId로 조회)
      "statusUrl": "https://queue.fal.run/.../requests/req_abcd1234/status",
      "responseUrl": "https://queue.fal.run/.../requests/req_abcd1234",
      "raw": {
        /* fal.ai submit 원본 응답(JSON) 전체 */
      },
    },
    "status": "QUEUED",
    "errorMessage": null,
    "startedAt": "2026-07-13T10:10:00+09:00",
    "completedAt": null,
    "createdAt": "2026-07-13T10:10:00+09:00",
  },
  "timestamp": "2026-07-13T10:10:00+09:00",
}
```

- 프론트는 응답의 `data.id`(GenerationJob PK)를 저장해두었다가, 4번 API(`GET /api/v1/generation/fal-jobs/{jobId}/status`)의 `{jobId}` 경로 변수로 사용해 폴링한다.

### 실패 및 예외 응답

| 상황                                                                      | HTTP Status | code           | message                        | 비고                                                          |
| ------------------------------------------------------------------------- | ----------- | -------------- | ------------------------------ | ------------------------------------------------------------- |
| 요청 바디 필드 검증 실패 (`jobType`/`modelCode` 공백, `input` 빈 객체 등) | 400         | `COMMON-001`   | 입력값이 올바르지 않습니다.    | `details`에 위반 필드 목록 포함                               |
| `jobType`이 `GenerationType` Enum에 존재하지 않는 문자열                  | 400         | `COMMON-001`   | 입력값이 올바르지 않습니다.    | 컨트롤러가 `GenerationType.valueOf()` 실패 시 명시적으로 던짐 |
| 존재하지 않는 `userId`로 요청 (FK 위반)                                   | 404         | `USER-002`     | 존재하지 않는 사용자입니다.    | `GenerationJob` 최초 저장 시 `user_id` FK 위반 → 전역 핸들러가 변환 |
| 존재하지 않는 `chatId`로 요청 (FK 위반)                                   | 404         | `CHAT-001`     | 존재하지 않는 채팅방입니다.    | 위와 동일하되 `chat_id` FK 위반인 경우                        |
| fal.ai 제출 API 호출 실패 (외부 API가 비-2xx 응답)                        | 502         | `PROVIDER-002` | AI 공급자 호출에 실패했습니다. | GenerationJob은 `FAILED`로 저장 후 예외 재발생                |

---

## 4. fal.ai 작업 상태 폴링

### API 개요

3번 API로 제출한 fal.ai 비동기 작업의 최신 상태를 조회한다. 호출 시점에 fal.ai 상태 API를 직접 재호출하여 최신 상태로 갱신 후 반환한다. `status`가 `COMPLETED`가 될 때까지 프론트에서 일정 주기로 재호출(폴링)해야 한다.

> 참고: 백엔드에는 이와 별개로 5초 주기 `@Scheduled` 백그라운드 스케줄러가 동작하여, `QUEUED`/`PROCESSING` 상태인 `IMAGE_GENERATION`/`VIDEO_GENERATION` 작업을 자동으로 폴링하고, `COMPLETED` 확인 시 fal.ai의 임시 미디어 URL을 AWS S3로 업로드한 뒤 `responsePayload.s3Url` 필드에 영구 URL을 채워 넣는다. 따라서 프론트에서 이 API를 폴링하지 않고 기다리기만 해도 일정 시간 뒤 상태가 자동으로 `COMPLETED`로 바뀔 수 있다. `responsePayload.s3Url`이 채워진 경우 그 값을 최종 미디어 URL로 사용하면 된다.

### HTTP Method & URL

```
GET /api/v1/generation/fal-jobs/{jobId}/status
```

- `{jobId}` : path variable, number, 필수 - 3번 API 응답의 `data.id` 값

### 요청 헤더

```
(본문 없음 — Content-Type 불필요)
```

### 요청 바디 예시

없음 (GET 요청, path variable로만 조회)

### 성공 응답 예시 (200 OK)

**진행 중(PROCESSING)인 경우:**

```jsonc
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 103,
    "userId": 1,
    "chatId": 10,
    "aiModelId": null,
    "requestMessageId": null,
    "jobType": "VIDEO_GENERATION",
    "prompt": "A floating cup in zero gravity, anti-gravity physics, cinematic lighting",
    "responsePayload": {
      "request_id": "req_abcd1234",
      "status": "IN_PROGRESS",
      /* fal.ai status API 원본 응답 필드가 그대로 담김 */
    },
    "status": "PROCESSING",
    "errorMessage": null,
    "startedAt": "2026-07-13T10:10:00+09:00",
    "completedAt": null,
    "createdAt": "2026-07-13T10:10:00+09:00",
  },
  "timestamp": "2026-07-13T10:10:05+09:00",
}
```

**완료(COMPLETED)된 경우 (백그라운드 스케줄러가 S3 업로드까지 마친 뒤):**

```jsonc
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 103,
    "status": "COMPLETED",
    "responsePayload": {
      "request_id": "req_abcd1234",
      "status": "COMPLETED",
      "video": { "url": "https://v3.fal.media/files/.../output.mp4" },
      "s3Url": "https://cdn.example.com/generated/2026-07-13/9c1e....mp4",
      /* 나머지 fal.ai 원본 필드 포함 */
    },
    "errorMessage": null,
    "completedAt": "2026-07-13T10:10:15+09:00",
    /* 이하 공통 필드 동일 */
  },
  "timestamp": "2026-07-13T10:10:20+09:00",
}
```

- `status` 매핑 규칙: fal.ai `IN_QUEUE` → `QUEUED`, `IN_PROGRESS` → `PROCESSING`, `COMPLETED` → `COMPLETED`, 그 외 문자열 → `FAILED`
- 프론트는 최종적으로 `responsePayload.s3Url`(있는 경우) 또는 `responsePayload.video.url` / `responsePayload.images[0].url`(fal.ai 원본, 임시 URL일 수 있음)을 파싱해 미디어를 표시한다.

### 실패 및 예외 응답

| 상황                                                    | HTTP Status | code             | message                        | 비고 |
| ------------------------------------------------------- | ----------- | ---------------- | ------------------------------ | ---- |
| `jobId`에 해당하는 GenerationJob이 DB에 없음            | 404         | `GENERATION-001` | 생성 작업을 찾을 수 없습니다.  |      |
| fal.ai 상태 조회 API 호출 실패 (외부 API가 비-2xx 응답) | 502         | `PROVIDER-002`   | AI 공급자 호출에 실패했습니다. |      |

---

## 부록 A. `ErrorCode` 전체 목록

| code                                          | HTTP Status | message                                    | 현재 소스에서 발생 조건                                                                                                                                                                                           |
| --------------------------------------------- | ----------- | ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `COMMON-001` (`INVALID_INPUT`)                | 400         | 입력값이 올바르지 않습니다.                | `@Valid` 검증 실패(`MethodArgumentNotValidException`), 또는 3번 API의 `jobType` 파싱 실패                                                                                                                         |
| `USER-001` (`USER_EMAIL_DUPLICATE`)           | 409         | 이미 사용 중인 이메일입니다.               | `generation` 도메인과 무관 (user 도메인 전용)                                                                                                                                                                     |
| `USER-002` (`USER_NOT_FOUND`)                 | 404         | 존재하지 않는 사용자입니다.                | 1~3번 API에서 `GenerationJob` 최초 저장 시 `user_id` FK 위반(`DataIntegrityViolationException`) → 전역 핸들러가 제약조건 이름(`user_id` 포함 여부)으로 판별해 변환                                              |
| `CHAT-001` (`CHAT_NOT_FOUND`)                 | 404         | 존재하지 않는 채팅방입니다.                | 위와 동일하되 제약조건 이름에 `chat_id`/`library_id`가 포함된 경우                                                                                                                                                |
| `GENERATION-001` (`GENERATION_NOT_FOUND`)     | 404         | 생성 작업을 찾을 수 없습니다.              | 4번 API에서 `jobId`로 조회 실패 시                                                                                                                                                                                |
| `GENERATION-002` (`INVALID_GENERATION_STATE`) | 409         | 현재 상태에서는 요청을 처리할 수 없습니다. | 정의만 되어 있고 현재 `generation` 컨트롤러/서비스에서는 아직 발생시키는 코드 없음(예약된 에러코드)                                                                                                               |
| `PROVIDER-001` (`AI_PROVIDER_UNAVAILABLE`)    | 503         | AI 공급자가 설정되지 않았습니다.           | 정의만 되어 있고 현재 발생시키는 코드 없음(예약된 에러코드)                                                                                                                                                       |
| `PROVIDER-002` (`AI_PROVIDER_REQUEST_FAILED`) | 502         | AI 공급자 호출에 실패했습니다.             | Claude/OpenAI/fal.ai 호출이 비-2xx를 반환하면 `RestClientResponseException` → 전역 핸들러가 변환                                                                                                                  |
| `MEDIA-001` (`MEDIA_UPLOAD_FAILED`)           | 502         | 미디어 업로드에 실패했습니다.              | 4개 API 응답으로는 직접 노출되지 않음. 백그라운드 스케줄러가 fal.ai 임시 미디어를 S3로 업로드하는 중 실패하면 내부적으로 발생(해당 GenerationJob은 폴링 응답의 `status: FAILED`, `errorMessage`로 간접 확인 가능) |

## 부록 B. `GenerationType` / `GenerationStatus` Enum 값

**GenerationType** (`jobType` 필드에 사용):
`IMAGE_GENERATION`, `VIDEO_GENERATION`, `REVERSE_PROMPT`, `IMAGE_VARIATION`, `PROMPT_REGENERATION`

**GenerationStatus** (`status` 필드에 사용):
`PENDING`, `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELED`, `EXPIRED`

## 변경 이력

- **2026-07-12** — 최초 작성 (v2.0, `GenerationJob` 단일 테이블 + JSONB 페이로드 기반).
- **2026-07-14** — `USER-002`/`CHAT-001` FK 위반 매핑이 `GlobalExceptionHandler`에 실제로 추가되어, 존재하지 않는 `userId`/`chatId`로 호출 시 더 이상 500이 아니라 문서에 명시된 404 `ErrorResponse` 포맷으로 응답함을 회귀 테스트로 확인 (자세한 검증 내역은 `테스트_및_시뮬레이션_가이드.md` 참고).
