# EC2 배포 설정

## 배포 구성

- AWS 계정: `310971189070`
- AWS 리전: `ap-northeast-2`
- ECR 저장소: `a1-back-prod`
- EC2 태그: `Name=a1-back-prod`
- 컨테이너 이름: `a1-backend`
- Redis 컨테이너 이름: `a1-redis`
- 애플리케이션 포트: `8080`
- 배포 트리거: `main` 브랜치의 `Backend CI` 성공

Docker 이미지는 커밋 SHA와 `latest` 태그로 ECR에 푸시합니다. 실제 EC2 배포에는
재현 가능한 커밋 SHA 태그를 사용합니다.

## GitHub 설정

GitHub Actions Secret에 다음 값을 등록합니다.

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

해당 IAM 사용자 또는 Role에는 최소한 다음 권한이 필요합니다.

- ECR 로그인 및 이미지 Push
- `ssm:SendCommand`
- `ssm:ListCommandInvocations`

AWS Access Key는 코드나 EC2 파일에 작성하지 않고 GitHub Actions Secret으로만 관리합니다.
초기 배포가 안정화되면 장기 Access Key 대신 GitHub OIDC Role로 전환하는 것을 권장합니다.

## EC2 설정

EC2에는 다음 항목이 필요합니다.

- Docker
- AWS CLI
- 실행 중인 SSM Agent
- ECR Pull 권한이 있는 EC2 Instance Role
- `Name=a1-back-prod` 태그

EC2 Instance Role에는 최소한 ECR 이미지 Pull 권한이 필요합니다.

## Parameter Store

운영 설정은 다음 Parameter Store 경로에서 조회합니다.

```text
/config/a1-backend_prod/spring.datasource.url
/config/a1-backend_prod/spring.datasource.username
/config/a1-backend_prod/spring.datasource.password
/config/a1-backend_prod/cloud.aws.s3.bucket
/config/a1-backend_prod/cloud.aws.region.static
```

EC2 Role에는 해당 경로의 `ssm:GetParameter` 권한이 필요합니다. 비밀번호가 고객 관리형
KMS 키로 암호화된 `SecureString`이면 해당 키의 `kms:Decrypt` 권한도 추가합니다.

`deploy.sh`는 배포할 때 파라미터를 복호화해 `/run/a1-back/a1-back.env`에 `600` 수준의
권한으로 생성합니다. 이 파일은 Git에 저장되지 않으며 재부팅하면 삭제됩니다. Redis
접속 정보는 배포 스크립트가 컨테이너 실행 시 직접 주입합니다.

## 배포 흐름

1. `main`에 코드가 Push됩니다.
2. `Backend CI`가 테스트와 Docker 빌드를 검증합니다.
3. CI 성공 후 배포 워크플로가 ECR에 이미지를 Push합니다.
4. GitHub Actions가 SSM으로 `deploy.sh`를 EC2에 전달합니다.
5. EC2가 전용 Docker 네트워크와 영속 볼륨을 사용하는 Redis 컨테이너를 준비합니다.
6. EC2가 이미지를 Pull하고 기존 Spring Boot 컨테이너를 교체합니다.
7. `/actuator/health`가 `UP`인지 확인한 후 배포를 완료합니다.

`compose.prod.yml`은 자동 배포에 사용하지 않으며 장애 대응 시 동일 구성을 수동으로
실행하기 위한 보조 파일입니다. 현재 운영 진입점은 `deploy.sh`입니다.
