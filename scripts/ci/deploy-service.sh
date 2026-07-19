#!/usr/bin/env bash
# Despliega un servicio con rollback.
set -Eeuo pipefail

service=${1:?Service required}
jar=${2:?JAR required}
case "$service" in
  productor) image=ms-productor-guias; container=ms-productor-guias; jar_name=gestion-guias.jar ;;
  consumidor) image=ms-consumidor-guias; container=ms-consumidor-guias; jar_name=ms-consumidor-guias.jar ;;
  *) echo "Unknown service: $service" >&2; exit 1 ;;
esac
for name in SSH_USER HOST PORT RABBITMQ_HOST RABBITMQ_PORT RABBITMQ_USERNAME RABBITMQ_PASSWORD AZURE_B2C_JWK_SET_URI AZURE_B2C_ISSUER_URI AZURE_B2C_AUDIENCE; do
  test -n "${!name:-}" || { echo "Missing $name" >&2; exit 1; }
done
test -s "$jar"
sha=${GITHUB_SHA:0:12}
release="/home/$SSH_USER/gestion-guias/releases/$service/$sha"
local_sum=$(sha256sum "$jar" | awk '{print $1}')

ssh -i "$HOME/.ssh/id_ec2" "$SSH_USER@$HOST" "mkdir -p '$release'"
scp -i "$HOME/.ssh/id_ec2" "$jar" "$SSH_USER@$HOST:$release/$jar_name"
remote_sum=$(ssh -i "$HOME/.ssh/id_ec2" "$SSH_USER@$HOST" "sha256sum '$release/$jar_name'" | awk '{print $1}')
test "$local_sum" = "$remote_sum" || { echo "Remote checksum mismatch" >&2; exit 1; }

printf '%s\n' 'FROM eclipse-temurin:17-jre' 'WORKDIR /app' "COPY $jar_name app.jar" 'USER 10001:10001' "EXPOSE $PORT" 'ENTRYPOINT ["java","-jar","app.jar"]' |
  ssh -i "$HOME/.ssh/id_ec2" "$SSH_USER@$HOST" "cat > '$release/Dockerfile'"

env_file=$(mktemp)
trap 'rm -f "$env_file"' EXIT
{
  printf 'SERVER_PORT=%s\n' "$PORT"
  printf 'RABBITMQ_HOST=%s\n' "$RABBITMQ_HOST"
  printf 'RABBITMQ_PORT=%s\n' "$RABBITMQ_PORT"
  printf 'RABBITMQ_USERNAME=%s\n' "$RABBITMQ_USERNAME"
  printf 'RABBITMQ_PASSWORD=%s\n' "$RABBITMQ_PASSWORD"
  printf 'AZURE_B2C_JWK_SET_URI=%s\n' "$AZURE_B2C_JWK_SET_URI"
  printf 'AZURE_B2C_ISSUER_URI=%s\n' "$AZURE_B2C_ISSUER_URI"
  printf 'AZURE_B2C_AUDIENCE=%s\n' "$AZURE_B2C_AUDIENCE"
  if [[ $service == consumidor ]]; then
    : "${AWS_REGION:?Missing AWS_REGION}" "${AWS_S3_BUCKET:?Missing AWS_S3_BUCKET}"
    printf 'AWS_REGION=%s\nAWS_S3_BUCKET=%s\nSTORAGE_PATH=/tmp/storage\n' "$AWS_REGION" "$AWS_S3_BUCKET"
  fi
} > "$env_file"
scp -i "$HOME/.ssh/id_ec2" "$env_file" "$SSH_USER@$HOST:$release/runtime.env"

ssh -i "$HOME/.ssh/id_ec2" "$SSH_USER@$HOST" bash -s -- "$release" "$image" "$container" "$sha" "$PORT" <<'REMOTE'
set -Eeuo pipefail
release=$1 image=$2 container=$3 sha=$4 port=$5
new_image="$image:$sha"
previous_image=$(docker inspect -f '{{.Config.Image}}' "$container" 2>/dev/null || true)
rollback() {
  docker rm -f "$container" >/dev/null 2>&1 || true
  if [[ -n "$previous_image" ]]; then
    docker run -d --name "$container" --network host --restart unless-stopped --env-file "$release/runtime.env" "$previous_image" >/dev/null
  fi
  echo "Rollback executed for $container" >&2
}
trap 'rollback' ERR
docker build -t "$new_image" "$release"
docker rm -f "$container" >/dev/null 2>&1 || true
docker run -d --name "$container" --network host --restart unless-stopped --env-file "$release/runtime.env" "$new_image" >/dev/null
for _ in $(seq 1 30); do
  running=$(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null || true)
  restarts=$(docker inspect -f '{{.RestartCount}}' "$container" 2>/dev/null || echo 99)
  if [[ $running == true && $restarts == 0 ]] && curl -sS -o /dev/null "http://127.0.0.1:$port/api/$( [[ $container == ms-productor-guias ]] && echo guias || echo guias-procesadas )"; then
    rm -f "$release/runtime.env"
    trap - ERR
    echo "$container deployed with image $new_image"
    exit 0
  fi
  sleep 5
done
docker logs --tail 80 "$container" 2>&1 | sed -E 's/(password|secret|token)=[^ ]+/\1=***MASKED***/Ig'
false
REMOTE
