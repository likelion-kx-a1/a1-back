#!/usr/bin/env bash

set -Eeuo pipefail

AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-310971189070}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
ECR_REPOSITORY="${ECR_REPOSITORY:-a1-back-prod}"
IMAGE_TAG="${1:-latest}"
CONTAINER_NAME="${CONTAINER_NAME:-a1-backend}"
REDIS_CONTAINER_NAME="${REDIS_CONTAINER_NAME:-a1-redis}"
DOCKER_NETWORK="${DOCKER_NETWORK:-a1-network}"
REDIS_VOLUME="${REDIS_VOLUME:-a1-redis-data}"
ENV_FILE="${ENV_FILE:-/run/a1-back/a1-back.env}"
DB_URL_PARAMETER="${DB_URL_PARAMETER:-/config/a1-backend_prod/spring.datasource.url}"
DB_USERNAME_PARAMETER="${DB_USERNAME_PARAMETER:-/config/a1-backend_prod/spring.datasource.username}"
DB_PASSWORD_PARAMETER="${DB_PASSWORD_PARAMETER:-/config/a1-backend_prod/spring.datasource.password}"
STORAGE_BUCKET_PARAMETER="${STORAGE_BUCKET_PARAMETER:-/config/a1-backend_prod/cloud.aws.s3.bucket}"
APP_AWS_REGION_PARAMETER="${APP_AWS_REGION_PARAMETER:-/config/a1-backend_prod/cloud.aws.region.static}"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1"
}

get_parameter() {
  aws ssm get-parameter \
    --name "$1" \
    --with-decryption \
    --region "${AWS_REGION}" \
    --query 'Parameter.Value' \
    --output text
}

log "Parameter Store에서 운영 설정 조회"
DB_URL="$(get_parameter "${DB_URL_PARAMETER}")"
DB_USERNAME="$(get_parameter "${DB_USERNAME_PARAMETER}")"
DB_PASSWORD="$(get_parameter "${DB_PASSWORD_PARAMETER}")"
STORAGE_BUCKET="$(get_parameter "${STORAGE_BUCKET_PARAMETER}")"
APP_AWS_REGION="$(get_parameter "${APP_AWS_REGION_PARAMETER}")"

install -d -m 700 "$(dirname "${ENV_FILE}")"
umask 077
{
  printf 'DB_URL=%s\n' "${DB_URL}"
  printf 'DB_USERNAME=%s\n' "${DB_USERNAME}"
  printf 'DB_PASSWORD=%s\n' "${DB_PASSWORD}"
  printf 'AWS_REGION=%s\n' "${APP_AWS_REGION}"
  printf 'STORAGE_BUCKET=%s\n' "${STORAGE_BUCKET}"
  printf 'STORAGE_PUBLIC_BASE_URL=\n'
  printf 'STORAGE_ENDPOINT=\n'
  printf 'STORAGE_PATH_STYLE_ACCESS=false\n'
} >"${ENV_FILE}"

log "Amazon ECR 로그인"
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

log "이미지 다운로드: ${IMAGE_URI}"
docker pull "${IMAGE_URI}"

log "Docker 네트워크 및 Redis 준비"
docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1 \
  || docker network create "${DOCKER_NETWORK}"

if ! docker ps -aq --filter "name=^/${REDIS_CONTAINER_NAME}$" | grep -q .; then
  docker run -d \
    --name "${REDIS_CONTAINER_NAME}" \
    --restart unless-stopped \
    --network "${DOCKER_NETWORK}" \
    --volume "${REDIS_VOLUME}:/data" \
    redis:7-alpine \
    redis-server --appendonly yes
elif [[ "$(docker inspect -f '{{.State.Running}}' "${REDIS_CONTAINER_NAME}")" != "true" ]]; then
  docker start "${REDIS_CONTAINER_NAME}"
fi

if docker ps -aq --filter "name=^/${CONTAINER_NAME}$" | grep -q .; then
  log "기존 컨테이너 제거: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}"
fi

log "새 컨테이너 실행"
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --network "${DOCKER_NETWORK}" \
  --env-file "${ENV_FILE}" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e REDIS_HOST="${REDIS_CONTAINER_NAME}" \
  -e REDIS_PORT=6379 \
  -p 8080:8080 \
  "${IMAGE_URI}"

log "애플리케이션 상태 확인"
for attempt in {1..30}; do
  if docker exec "${CONTAINER_NAME}" \
    wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    log "배포 완료"
    docker image prune -f
    exit 0
  fi

  if [[ "$(docker inspect -f '{{.State.Running}}' "${CONTAINER_NAME}")" != "true" ]]; then
    log "컨테이너가 종료되었습니다."
    docker logs "${CONTAINER_NAME}"
    exit 1
  fi

  log "기동 대기 중 (${attempt}/30)"
  sleep 5
done

log "헬스 체크 시간 초과"
docker logs "${CONTAINER_NAME}"
exit 1
