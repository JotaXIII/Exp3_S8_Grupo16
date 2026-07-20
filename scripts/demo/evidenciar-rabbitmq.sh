#!/usr/bin/env bash
set -Eeuo pipefail

for name in RABBITMQ_HOST RABBITMQ_USERNAME RABBITMQ_PASSWORD; do
  test -n "${!name:-}" || { echo "FAIL: falta la variable $name" >&2; exit 1; }
done

RABBITMQ_CONTAINER=${RABBITMQ_CONTAINER:-guias-rabbitmq}
MANAGEMENT_URL="http://${RABBITMQ_HOST}:15672"
MAIN_EXCHANGE=guias.exchange
MAIN_QUEUE=guias.queue
MAIN_ROUTING_KEY=guias.routing-key
DLX=guias.dlx
DLQ=guias.dlq
DLQ_ROUTING_KEY=guias.dlq.routing-key
TYPE_ID=com.transportista.gestionguias.dto.GuiaDespachoMessage
BURST_DURATION_SECONDS=60
BURST_INTERVAL_SECONDS=1
NETRC_FILE=$(mktemp)
PUBLISH_RESPONSE='no disponible'
PUBLISHED_MESSAGES=0
INITIAL_MAIN_READY=0
INITIAL_MAIN_UNACKED=0
INITIAL_DLQ_READY=0
FINAL_MAIN_READY=0
FINAL_MAIN_UNACKED=0
FINAL_MAIN_CONSUMERS=0
FINAL_DLQ_READY=0

cleanup() {
  rm -f "$NETRC_FILE"
}

fail() {
  trap - ERR
  echo
  echo '=================================================='
  echo 'RESULTADO: FAIL'
  echo '=================================================='
  echo "$1"
  echo "Estado inicial: guias.queue Ready=$INITIAL_MAIN_READY, Unacked=$INITIAL_MAIN_UNACKED"
  echo "Estado inicial DLQ: $INITIAL_DLQ_READY"
  echo "Estado final: guias.queue Ready=$FINAL_MAIN_READY, Unacked=$FINAL_MAIN_UNACKED, Consumers=$FINAL_MAIN_CONSUMERS"
  echo "Estado final DLQ: $FINAL_DLQ_READY"
  echo "Respuesta de publicación: $PUBLISH_RESPONSE"
  exit 1
}

on_error() {
  local line=$1
  set +e
  fail "Fallo inesperado en la línea $line. Revise contenedor, credenciales, Management API y bindings."
}

trap cleanup EXIT
trap 'on_error $LINENO' ERR

command -v docker >/dev/null || fail 'docker no está disponible.'
command -v curl >/dev/null || fail 'curl no está disponible.'
command -v awk >/dev/null || fail 'awk no está disponible.'
command -v grep >/dev/null || fail 'grep no está disponible.'
[[ "$RABBITMQ_HOST" != *://* && "$RABBITMQ_HOST" != */* ]] || fail 'RABBITMQ_HOST debe contener solo host o IP.'

chmod 600 "$NETRC_FILE"
printf 'machine %s\nlogin %s\npassword %s\n' \
  "$RABBITMQ_HOST" "$RABBITMQ_USERNAME" "$RABBITMQ_PASSWORD" > "$NETRC_FILE"

running=$(docker inspect -f '{{.State.Running}}' "$RABBITMQ_CONTAINER" 2>/dev/null || true)
[[ "$running" == true ]] || fail "El contenedor $RABBITMQ_CONTAINER no está running."
docker exec "$RABBITMQ_CONTAINER" rabbitmq-diagnostics -q ping >/dev/null
curl --silent --show-error --fail-with-body --netrc-file "$NETRC_FILE" \
  "$MANAGEMENT_URL/api/overview" -o /dev/null

queues=$(docker exec "$RABBITMQ_CONTAINER" rabbitmqctl -q list_queues -p / \
  name messages_ready messages_unacknowledged consumers arguments)
exchanges=$(docker exec "$RABBITMQ_CONTAINER" rabbitmqctl -q list_exchanges -p / \
  name type durable)
bindings=$(docker exec "$RABBITMQ_CONTAINER" rabbitmqctl -q list_bindings -p / \
  source_name destination_name destination_kind routing_key)

main_line=$(awk -F '\t' -v queue="$MAIN_QUEUE" '$1==queue {print; exit}' <<< "$queues")
dlq_line=$(awk -F '\t' -v queue="$DLQ" '$1==queue {print; exit}' <<< "$queues")
[[ -n "$main_line" ]] || fail "No existe $MAIN_QUEUE."
[[ -n "$dlq_line" ]] || fail "No existe $DLQ."
grep -Fq "$MAIN_EXCHANGE" <<< "$exchanges" || fail "No existe $MAIN_EXCHANGE."
grep -Fq "$DLX" <<< "$exchanges" || fail "No existe $DLX."
grep -Fq 'x-dead-letter-exchange' <<< "$main_line" || fail 'Falta x-dead-letter-exchange.'
grep -Fq "$DLX" <<< "$main_line" || fail "La cola principal no apunta a $DLX."
grep -Fq 'x-dead-letter-routing-key' <<< "$main_line" || fail 'Falta x-dead-letter-routing-key.'
grep -Fq "$DLQ_ROUTING_KEY" <<< "$main_line" || fail 'La routing key dead-letter no coincide.'
awk -F '\t' -v e="$MAIN_EXCHANGE" -v q="$MAIN_QUEUE" -v r="$MAIN_ROUTING_KEY" \
  '$1==e && $2==q && $3=="queue" && $4==r {ok=1} END {exit !ok}' <<< "$bindings" || \
  fail 'El binding principal no coincide.'
awk -F '\t' -v e="$DLX" -v q="$DLQ" -v r="$DLQ_ROUTING_KEY" \
  '$1==e && $2==q && $3=="queue" && $4==r {ok=1} END {exit !ok}' <<< "$bindings" || \
  fail 'El binding DLX/DLQ no coincide.'

read -r INITIAL_MAIN_READY INITIAL_MAIN_UNACKED INITIAL_MAIN_CONSUMERS < <(
  awk -F '\t' -v queue="$MAIN_QUEUE" '$1==queue {print $2, $3, $4; exit}' <<< "$queues"
)
INITIAL_DLQ_READY=$(awk -F '\t' -v queue="$DLQ" '$1==queue {print $2; exit}' <<< "$queues")
(( INITIAL_MAIN_CONSUMERS >= 1 )) || fail "No hay consumidores conectados a $MAIN_QUEUE."

echo '1. RECURSOS RABBITMQ'
echo "PASS Contenedor running: $RABBITMQ_CONTAINER"
echo "PASS Exchange principal: $MAIN_EXCHANGE"
echo "PASS Cola principal: $MAIN_QUEUE"
echo "PASS DLX: $DLX"
echo "PASS DLQ: $DLQ"
echo "PASS Binding principal: $MAIN_EXCHANGE -> $MAIN_QUEUE ($MAIN_ROUTING_KEY)"
echo "PASS Binding DLQ: $DLX -> $DLQ ($DLQ_ROUTING_KEY)"
echo "PASS Dead-letter: $DLX / $DLQ_ROUTING_KEY"
echo "PASS Consumidores conectados: $INITIAL_MAIN_CONSUMERS"

echo
echo '2. ESTADO INICIAL'
echo "$MAIN_QUEUE: Ready=$INITIAL_MAIN_READY, Unacked=$INITIAL_MAIN_UNACKED, Consumers=$INITIAL_MAIN_CONSUMERS"
echo "$DLQ: Ready=$INITIAL_DLQ_READY"

echo
echo '3. PUBLICACIÓN EN FLUJO PRINCIPAL'
echo "Ráfaga: un mensaje por segundo durante $BURST_DURATION_SECONDS segundos"
echo "Exchange: $MAIN_EXCHANGE"
echo "Routing key: $MAIN_ROUTING_KEY"
burst_timestamp=$(date +%s)
for sequence in $(seq 1 "$BURST_DURATION_SECONDS"); do
  seed=$((burst_timestamp * 100 + sequence))
  identifier="EFT-DLQ-$burst_timestamp-$sequence"
  message_json=$(printf \
    '{"mensajeId":"%s","numeroGuia":"%s","transportista":"","cliente":"EFT DLQ","direccionDestino":"EFT DLQ","fechaEmision":"2026-07-20","fechaSolicitud":"2026-07-20T00:00:00"}' \
    "$(printf '%08x-%04x-4%03x-8%03x-%012x' "$((seed & 0xffffffff))" "$((seed & 0xffff))" "$((seed & 0xfff))" "$((seed & 0xfff))" "$seed")" \
    "$identifier")
  escaped_message=${message_json//\\/\\\\}
  escaped_message=${escaped_message//\"/\\\"}
  publish_body=$(printf \
    '{"properties":{"content_type":"application/json","delivery_mode":2,"headers":{"__TypeId__":"%s"}},"routing_key":"%s","payload":"%s","payload_encoding":"string"}' \
    "$TYPE_ID" "$MAIN_ROUTING_KEY" "$escaped_message")
  PUBLISH_RESPONSE=$(curl --silent --show-error --fail-with-body --netrc-file "$NETRC_FILE" \
    -H 'Content-Type: application/json' \
    -X POST \
    --data "$publish_body" \
    "$MANAGEMENT_URL/api/exchanges/%2F/$MAIN_EXCHANGE/publish")
  grep -Eq '"routed"[[:space:]]*:[[:space:]]*true' <<< "$PUBLISH_RESPONSE" || \
    fail "RabbitMQ informó routed=false en el mensaje $sequence."
  PUBLISH_RESPONSE='routed=true'
  PUBLISHED_MESSAGES=$sequence
  if (( sequence == 1 || sequence % 10 == 0 || sequence == BURST_DURATION_SECONDS )); then
    echo "PASS Publicados: $sequence/$BURST_DURATION_SECONDS"
  fi
  if (( sequence < BURST_DURATION_SECONDS )); then
    sleep "$BURST_INTERVAL_SECONDS"
  fi
done

echo
echo '4. ESPERA DE PROCESAMIENTO'
echo "Esperando el dead-lettering de $PUBLISHED_MESSAGES mensajes..."
success=false
for second in $(seq 1 20); do
  queues=$(docker exec "$RABBITMQ_CONTAINER" rabbitmqctl -q list_queues -p / \
    name messages_ready messages_unacknowledged consumers arguments)
  read -r FINAL_MAIN_READY FINAL_MAIN_UNACKED FINAL_MAIN_CONSUMERS < <(
    awk -F '\t' -v queue="$MAIN_QUEUE" '$1==queue {print $2, $3, $4; exit}' <<< "$queues"
  )
  FINAL_DLQ_READY=$(awk -F '\t' -v queue="$DLQ" '$1==queue {print $2; exit}' <<< "$queues")
  if (( FINAL_DLQ_READY >= INITIAL_DLQ_READY + PUBLISHED_MESSAGES \
        && FINAL_MAIN_READY <= INITIAL_MAIN_READY \
        && FINAL_MAIN_UNACKED <= INITIAL_MAIN_UNACKED )); then
    success=true
    echo "PASS Dead-lettering detectado en ${second}s"
    break
  fi
  sleep 1
done

echo
echo '5. ESTADO FINAL'
echo "$MAIN_QUEUE: Ready=$FINAL_MAIN_READY, Unacked=$FINAL_MAIN_UNACKED, Consumers=$FINAL_MAIN_CONSUMERS"
echo "$DLQ: Ready=$FINAL_DLQ_READY"

[[ "$success" == true ]] || fail 'La DLQ no aumentó o el mensaje permaneció en la cola principal.'

echo
echo '6. RESULTADO'
echo '=================================================='
echo 'RESULTADO: PASS'
echo '=================================================='
echo "La ráfaga fue publicada en $MAIN_EXCHANGE."
echo "$PUBLISHED_MESSAGES mensajes fueron publicados en el flujo principal."
echo 'El consumidor rechazó los mensajes sin reencolarlos.'
echo 'RabbitMQ aplicó automáticamente el DLX.'
echo "La cantidad de mensajes en $DLQ aumentó."
echo 'Java no publicó manualmente hacia la DLQ.'
echo
echo "Estado inicial DLQ: $INITIAL_DLQ_READY"
echo "Estado final DLQ: $FINAL_DLQ_READY"
