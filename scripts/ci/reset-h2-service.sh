#!/usr/bin/env bash
set -Eeuo pipefail

for name in SSH_USER HOST CONTAINER PORT; do
  test -n "${!name:-}" || { echo "Missing $name" >&2; exit 1; }
done

ssh -i "$HOME/.ssh/id_ec2" "$SSH_USER@$HOST" bash -s -- "$CONTAINER" "$PORT" <<'REMOTE'
set -Eeuo pipefail
container=$1
port=$2
docker restart "$container" >/dev/null
for _ in $(seq 1 30); do
  running=$(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null || true)
  status=$(curl -sS -o /dev/null -w '%{http_code}' "http://127.0.0.1:$port/api/$( [[ $container == ms-productor-guias ]] && echo guias || echo guias-procesadas )" || true)
  if [[ $running == true && ( $status == 200 || $status == 401 ) ]]; then
    echo "$container restarted with an empty H2 database"
    exit 0
  fi
  sleep 5
done
docker logs --tail 80 "$container" 2>&1 | sed -E 's/(password|secret|token)=[^ ]+/\1=***MASKED***/Ig'
exit 1
REMOTE
