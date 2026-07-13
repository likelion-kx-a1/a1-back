# S3 · MinIO 스토리지 설정

## 1. 목적

파일 업로드 API를 구현하기 전에 로컬 MinIO와 운영 S3를 같은 코드 경로로 사용할 수 있도록 스토리지 기반을 정리한다.

현재 단계에서는 실제 업로드 API는 만들지 않는다.

이번 단계 범위:

- `MediaStoragePort` 개선
- 업로드 요청/결과 DTO 추가
- S3/MinIO 공통 어댑터 정리
- 로컬 MinIO 버킷 자동 생성
- 환경변수 정리

## 2. 구조

```text
media
└── application
    └── port
        └── out
            ├── MediaStoragePort.java
            ├── StorageUploadCommand.java
            └── StorageUploadResult.java

media
└── infrastructure
    └── storage
        └── s3
            └── S3MediaStorageAdapter.java
```

## 3. MediaStoragePort

서비스 계층은 S3나 MinIO를 직접 알지 않는다.

```java
public interface MediaStoragePort {
  StorageUploadResult upload(StorageUploadCommand command);
}
```

이 포트만 바라보면 로컬에서는 MinIO, 운영에서는 S3를 같은 방식으로 사용할 수 있다.

## 4. StorageUploadCommand

업로드 요청 정보다.

```java
public record StorageUploadCommand(
    byte[] content,
    String originalFilename,
    String contentType,
    String extension,
    String directory
) {}
```

`directory` 예시:

```text
message-files
generated-media
source-uploads
```

## 5. StorageUploadResult

업로드 완료 후 DB에 저장할 수 있는 메타데이터다.

```java
public record StorageUploadResult(
    String bucketName,
    String storagePath,
    String publicUrl,
    String originalFilename,
    String storedFilename,
    String mimeType,
    long fileSize
) {}
```

나중에 아래 엔티티 저장 시 그대로 사용할 수 있다.

```text
MessageFile
GeneratedMediaFile
```

## 6. 로컬 MinIO 실행

프로젝트 루트에서 실행한다.

```bash
docker-compose up -d
```

실행되는 구성:

```text
PostgreSQL  : localhost:5432
Redis       : localhost:6379
MinIO API   : http://localhost:9000
MinIO Console: http://localhost:9001
```

MinIO 콘솔 로그인:

```text
ID: minio
PW: minio-local-password
```

`compose.yml`의 `minio-init` 서비스가 로컬 버킷을 자동 생성한다.

```text
bucket: a1-media
anonymous download: enabled
```

## 7. 로컬 .env 예시

실제 `.env`에 아래 값을 넣으면 로컬 MinIO를 사용한다.

```env
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=minio
AWS_SECRET_ACCESS_KEY=minio-local-password
STORAGE_BUCKET=a1-media
STORAGE_PUBLIC_BASE_URL=http://localhost:9000/a1-media
STORAGE_ENDPOINT=http://localhost:9000
STORAGE_PATH_STYLE_ACCESS=true
```

## 8. 운영 S3 .env 예시

운영에서는 EC2 IAM Role을 사용한다.

운영 환경에서는 AWS Access Key를 `.env`에 넣지 않는 것을 원칙으로 한다.

```env
AWS_REGION=ap-northeast-2
STORAGE_BUCKET=likelion-kx-storage-2026-310971189070-ap-northeast-2-an
STORAGE_PUBLIC_BASE_URL=
STORAGE_ENDPOINT=
STORAGE_PATH_STYLE_ACCESS=false
```

운영에서 `STORAGE_PUBLIC_BASE_URL`을 비워두면 업로드 결과의 `publicUrl`은 아래 형식으로 반환된다.

```text
s3://{bucketName}/{storagePath}
```

CloudFront나 공개 URL 정책을 붙이면 이후 `STORAGE_PUBLIC_BASE_URL`에 해당 주소를 넣으면 된다.

## 9. 다음 단계

다음 브랜치에서 구현할 기능:

```http
POST /api/chats/{chatId}/messages/{messageId}/files
GET /api/chats/{chatId}/messages/{messageId}/files
DELETE /api/chats/{chatId}/messages/{messageId}/files/{fileId}
```

이때 `MediaStoragePort.upload()`를 호출하고, 반환된 `StorageUploadResult`를 `MessageFile`에 저장한다.
