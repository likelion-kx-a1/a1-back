# EC2 배포 준비

## AWS 리소스

1. ECR 저장소 `a1-back`
2. S3 버킷과 EC2 애플리케이션 Role
3. RDS PostgreSQL과 ElastiCache Redis
4. Docker, Compose plugin, SSM Agent가 설치된 EC2
5. GitHub OIDC 배포 Role

EC2 Role에는 해당 S3 버킷의 `s3:PutObject`, `s3:GetObject` 권한을 부여합니다.
정적 AWS Access Key를 EC2 환경변수에 저장하지 않습니다.

## GitHub 설정

Actions secret:

- `AWS_DEPLOY_ROLE_ARN`
- `EC2_INSTANCE_ID`

EC2의 `/opt/a1-back/.env`에 DB, Redis, OpenAI, fal.ai 설정을 저장하고 권한을 `600`으로 제한합니다.
`compose.prod.yml`과 `infra/nginx/default.conf`도 `/opt/a1-back`에 배치합니다.

운영에서는 `STORAGE_ENDPOINT`를 비워 AWS 기본 S3 엔드포인트를 사용하고 EC2 Instance Profile로
인증합니다. S3 객체를 직접 공개하지 않는 경우 `STORAGE_PUBLIC_BASE_URL`에는 CloudFront 주소를
지정하거나, 이후 다운로드 API에 presigned URL 발급을 구현합니다.
