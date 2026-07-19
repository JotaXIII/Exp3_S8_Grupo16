#!/usr/bin/env bash
# Verifica el estado de RabbitMQ.
set -Eeuo pipefail
: "${SSH_USER:?Missing SSH_USER}" "${HOST:?Missing RabbitMQ host}"
: "${RABBITMQ_CONTAINER:?Missing RabbitMQ container}"
require_consumer=${REQUIRE_CONSUMER:-false}
[[ $require_consumer == true || $require_consumer == false ]]

ssh -i "$HOME/.ssh/id_ec2" "$SSH_USER@$HOST" 'bash -se' -- \
  "$RABBITMQ_CONTAINER" "$require_consumer" <<'REMOTE'
set -Eeuo pipefail
container=$1 require_consumer=$2
test "$(docker inspect -f '{{.State.Running}}' "$container")" = true
restart=$(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$container")
case "$restart" in unless-stopped|always) ;; *) echo "Unsafe restart policy: $restart" >&2; exit 1;; esac
test -n "$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/var/lib/rabbitmq"}}{{.Name}}{{end}}{{end}}' "$container")"
docker exec "$container" rabbitmq-diagnostics -q ping
queues=$(docker exec "$container" rabbitmqctl -q list_queues name messages_ready messages_unacknowledged consumers arguments)
grep -q '^guias.queue' <<<"$queues"
grep -q '^guias.dlq' <<<"$queues"
if [[ $require_consumer == true ]]; then
  awk '$1=="guias.queue" && $2==0 && $3==0 && $4>=1 {ok=1} END {exit !ok}' <<<"$queues"
fi
exchanges=$(docker exec "$container" rabbitmqctl -q list_exchanges name type)
grep -q '^guias.exchange' <<<"$exchanges"
grep -q '^guias.dlx' <<<"$exchanges"
bindings=$(docker exec "$container" rabbitmqctl -q list_bindings source_name destination_name routing_key)
grep -q 'guias.exchange.*guias.queue.*guias.routing-key' <<<"$bindings"
grep -q 'guias.dlx.*guias.dlq.*guias.dlq.routing-key' <<<"$bindings"
docker exec "$container" rabbitmq-diagnostics -q listeners | grep -q '5672'
echo "RabbitMQ node, persistent volume, AMQP, queues, exchanges and bindings validated."
REMOTE
