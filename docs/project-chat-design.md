# Project · Chat · Message CRUD 설계 명세서

## 1. 설계 목적

AI Media Studio의 작업 흐름은 프로젝트 저장소와 채팅방을 함께 사용한다.

- 프로젝트는 사용자의 작업 단위이자 저장소다.
- 채팅방은 프롬프트 대화, 파일 첨부, AI 생성 결과가 쌓이는 작업 공간이다.
- 채팅방은 프로젝트 안에 생성할 수도 있고, 프로젝트 없이 독립적으로 생성할 수도 있다.

## 2. 도메인 관계

```text
User
├── Project
│   └── Chat
│       └── ChatMessage
└── Chat
    └── ChatMessage
```

`Chat.projectId`는 nullable이다.

- `projectId != null`: 프로젝트에 소속된 채팅방
- `projectId == null`: 프로젝트 밖 독립 채팅방

## 3. 구현 범위

이번 단계에서는 파일 업로드, S3/MinIO 저장, AI 생성 연동은 제외한다.

구현 범위:

1. 프로젝트 CRUD
2. 채팅방 CRUD
3. 채팅 메시지 CRUD

## 4. API 명세

### 4.1 프로젝트 API

#### 프로젝트 생성

```http
POST /api/projects
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "name": "방송 썸네일 프로젝트",
  "description": "뉴스 썸네일 제작용 프로젝트"
}
```

#### 내 프로젝트 목록 조회

```http
GET /api/projects
Authorization: Bearer {accessToken}
```

#### 프로젝트 상세 조회

```http
GET /api/projects/{projectId}
Authorization: Bearer {accessToken}
```

#### 프로젝트 수정

```http
PATCH /api/projects/{projectId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "name": "수정된 프로젝트명",
  "description": "수정된 설명"
}
```

#### 프로젝트 삭제

```http
DELETE /api/projects/{projectId}
Authorization: Bearer {accessToken}
```

프로젝트는 물리 삭제하지 않고 `status = DELETED`, `deletedAt`으로 소프트 삭제한다.

## 5. 채팅방 API

### 독립 채팅방 생성

```http
POST /api/chats
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "title": "독립 AI 채팅"
}
```

### 프로젝트 안 채팅방 생성

```http
POST /api/projects/{projectId}/chats
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "title": "프로젝트용 AI 채팅"
}
```

### 전체 채팅방 목록 조회

```http
GET /api/chats
Authorization: Bearer {accessToken}
```

### 독립 채팅방 목록 조회

```http
GET /api/chats/standalone
Authorization: Bearer {accessToken}
```

### 프로젝트 채팅방 목록 조회

```http
GET /api/projects/{projectId}/chats
Authorization: Bearer {accessToken}
```

### 채팅방 상세 조회

```http
GET /api/chats/{chatId}
Authorization: Bearer {accessToken}
```

### 채팅방 제목 수정

```http
PATCH /api/chats/{chatId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "title": "수정된 채팅방 제목"
}
```

### 채팅방 삭제

```http
DELETE /api/chats/{chatId}
Authorization: Bearer {accessToken}
```

채팅방은 물리 삭제하지 않고 `status = DELETED`, `deletedAt`으로 소프트 삭제한다.

## 6. 채팅 메시지 API

### 메시지 생성

```http
POST /api/chats/{chatId}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "senderType": "USER",
  "messageType": "TEXT",
  "contentText": "고급스러운 뉴스 썸네일 이미지를 만들어줘",
  "parentMessageId": null
}
```

`senderType`을 생략하면 `USER`, `messageType`을 생략하면 `TEXT`로 처리한다.

### 메시지 목록 조회

```http
GET /api/chats/{chatId}/messages
Authorization: Bearer {accessToken}
```

### 메시지 수정

```http
PATCH /api/chats/{chatId}/messages/{messageId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "contentText": "수정된 메시지"
}
```

### 메시지 삭제

```http
DELETE /api/chats/{chatId}/messages/{messageId}
Authorization: Bearer {accessToken}
```

메시지는 물리 삭제하지 않고 `status = DELETED`로 처리한다.

## 7. 권한 규칙

- 모든 API는 JWT 인증이 필요하다.
- 사용자는 본인이 생성한 프로젝트만 조회/수정/삭제할 수 있다.
- 사용자는 본인이 생성한 채팅방만 조회/수정/삭제할 수 있다.
- 프로젝트 안 채팅방을 생성할 때는 해당 프로젝트의 소유자여야 한다.
- 메시지 생성/수정/삭제는 해당 채팅방 소유자만 가능하다.

## 8. 다음 단계

다음 개발 단계에서는 아래 기능을 추가한다.

1. 메시지 파일 업로드 API
2. MinIO/S3 저장 연동
3. 생성 미디어 저장 API
4. 프로젝트 저장소 내 미디어 목록 조회
