# EC2 배포 구성

## 운영 구성

- AWS 계정: `310971189070`
- 리전: `ap-northeast-2`
- ECR: `a1-back-prod`
- EC2 대상 태그: `Name=a1-back-prod`
- Backend 컨테이너: `a1-backend`
- Redis 컨테이너: `a1-redis`
- 운영 API: `https://api.likelionxkx-a1.link`

EC2 호스트의 Nginx가 `80/443` 요청을 받고
`127.0.0.1:8080`의 Backend 컨테이너로 전달합니다. Backend의 `8080` 포트는
외부에 직접 공개하지 않습니다.

## GitHub Actions

Repository Actions Secrets:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

장기적으로는 고정 Access Key 대신 GitHub OIDC Role로 전환합니다. 현재 배포 IAM
주체에는 ECR Push, SSM SendCommand, SSM 명령 조회 권한이 필요합니다.

`main`의 Backend CI가 성공하면 배포 워크플로가 다음 작업을 실행합니다.

1. 커밋 SHA와 `latest` 태그로 Docker 이미지를 빌드합니다.
2. 이미지를 ECR `a1-back-prod`에 Push합니다.
3. `Name=a1-back-prod` 태그의 EC2에 SSM 명령을 보냅니다.
4. 저장소의 `deploy.sh`를 EC2에 전달해 실행합니다.
5. `/actuator/health`가 `UP`인지 확인합니다.

## Parameter Store

`deploy.sh`는 EC2 Instance Role로 다음 파라미터를 조회합니다.

```text
/config/a1-back/SPRING_DATASOURCE_URL
/config/a1-back/SPRING_DATASOURCE_USERNAME
/config/a1-back/SPRING_DATASOURCE_PASSWORD
/config/a1-back/AWS_S3_BUCKET
/config/a1-back/AWS_REGION
```

EC2 Role에 필요한 Parameter Store 리소스:

```json
{
  "Effect": "Allow",
  "Action": [
    "ssm:GetParameter",
    "ssm:GetParameters",
    "ssm:GetParametersByPath"
  ],
  "Resource": "arn:aws:ssm:ap-northeast-2:310971189070:parameter/config/a1-back/*"
}
```

고객 관리 KMS 키로 `SecureString`을 암호화했다면 `kms:Decrypt` 권한도 필요합니다.

조회된 값은 EC2의 `/run/a1-back/a1-back.env`에 제한된 권한으로 생성되며 Git에
저장되지 않습니다.

## EC2 Docker

`deploy.sh`가 관리하는 리소스:

```text
Docker network: a1-network
Backend:        a1-backend
Redis:          a1-redis
Redis volume:   a1-redis-data
```

Backend는 `127.0.0.1:8080`에만 바인딩하고 Redis는 호스트 포트를 공개하지 않습니다.
두 컨테이너 모두 `restart unless-stopped` 정책을 사용합니다.

`compose.prod.yml`은 장애 대응을 위한 수동 실행용 보조 파일이며 자동 배포의 진입점은
`deploy.sh`입니다.

## Nginx와 인증서

Nginx와 Certbot은 EC2 호스트에서 관리합니다.

```text
https://api.likelionxkx-a1.link
  → Nginx :443
  → 127.0.0.1:8080
  → Spring Boot
```

인증서 자동 갱신은 EC2에서 다음 명령으로 검증합니다.

```bash
sudo certbot renew --dry-run
```
