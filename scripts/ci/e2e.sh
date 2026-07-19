#!/usr/bin/env bash
# Prueba el flujo completo por API Gateway.
set -Eeuo pipefail
: "${API_GATEWAY_BASE_URL:?Missing API_GATEWAY_BASE_URL}"
: "${ACCESS_TOKEN:?Missing ACCESS_TOKEN}"
: "${RUN_ID:?Missing RUN_ID}"
base=${API_GATEWAY_BASE_URL%/}
work=$(mktemp -d)
id= numero_guia= s3_key= created=false

sanitize() { head -c 500 "$1" | sed -E 's/(token|secret|password|credential)[^,}]*/\1=***MASKED***/Ig'; }
request() {
  local expected=$1 method=$2 path=$3 body=${4:-} output=${5:-$work/body}
  local args=(-sS -o "$output" -w '%{http_code}' -X "$method" "$base$path")
  [[ -n ${AUTHENTICATED:-} ]] && args+=(-H "Authorization: Bearer $ACCESS_TOKEN")
  [[ -n $body ]] && args+=(-H 'Content-Type: application/json' --data "$body")
  local status
  status=$(curl "${args[@]}")
  if [[ $status != "$expected" ]]; then
    echo "HTTP validation failed: $method $path expected=$expected actual=$status" >&2
    sanitize "$output" >&2
    return 1
  fi
}
cleanup() {
  set +e
  if [[ $created == true && -n $numero_guia ]]; then
    AUTHENTICATED=1 request 204 DELETE "/api/guias-procesadas/$numero_guia" "" "$work/cleanup"
    [[ -n $id ]] && AUTHENTICATED=1 request 204 DELETE "/api/guias/$id" "" "$work/cleanup-producer"
    if [[ -n $s3_key && -n ${AWS_ACCESS_KEY_ID:-} ]]; then
      aws s3api head-object --bucket "$AWS_S3_BUCKET" --key "$s3_key" >/dev/null 2>&1 && echo "Cleanup warning: S3 object still exists" >&2
    fi
  fi
  rm -rf "$work"
}
trap cleanup EXIT

unset AUTHENTICATED
request 401 GET /api/guias
request 401 GET /api/guias-procesadas
invalid_status=$(curl -sS -o "$work/invalid" -w '%{http_code}' -H 'Authorization: Bearer invalid.jwt.value' "$base/api/guias")
test "$invalid_status" = 401
invalid_status=$(curl -sS -o "$work/invalid-consumer" -w '%{http_code}' -H 'Authorization: Bearer invalid.jwt.value' "$base/api/guias-procesadas")
test "$invalid_status" = 401

AUTHENTICATED=1
request 200 GET /api/guias
request 200 GET /api/guias-procesadas
request 400 POST /api/guias '{"transportista":"","cliente":"","direccionDestino":""}'
suffix="${RUN_ID}-$(date +%s)-$RANDOM"
transportista="CI-TRANSPORTISTA-$suffix"
cliente="CI-CLIENTE-$suffix"
direccion="CI-DIRECCION-$suffix"
create_body=$(jq -nc --arg t "$transportista" --arg c "$cliente" --arg d "$direccion" '{transportista:$t,cliente:$c,direccionDestino:$d}')
request 202 POST /api/guias "$create_body" "$work/create.json"
id=$(jq -er '.id' "$work/create.json")
numero_guia=$(jq -er '.numeroGuia' "$work/create.json")
created=true
fecha=$(jq -er '.fechaEmision' "$work/create.json")

processed=false
for _ in $(seq 1 30); do
  status=$(curl -sS -o "$work/processed.json" -w '%{http_code}' -H "Authorization: Bearer $ACCESS_TOKEN" "$base/api/guias-procesadas/$numero_guia")
  if [[ $status == 200 ]]; then processed=true; break; fi
  [[ $status == 404 ]] || { echo "Unexpected polling status: $status" >&2; exit 1; }
  sleep 5
done
test "$processed" = true
jq -e --arg n "$numero_guia" --arg t "$transportista" '.numeroGuia==$n and .transportista==$t and (.s3Key|length>0) and (.nombreArchivo|length>0)' "$work/processed.json" >/dev/null
s3_key=$(jq -r '.s3Key' "$work/processed.json")

request 200 GET "/api/guias/$id"
request 200 GET "/api/guias/transportista/$(jq -rn --arg v "$transportista" '$v|@uri')"
request 200 GET "/api/guias/fecha/$fecha"
query="transportista=$(jq -rn --arg v "$transportista" '$v|@uri')&fecha=$fecha"
request 200 GET "/api/guias-procesadas/buscar?$query"

if [[ -n ${AWS_ACCESS_KEY_ID:-} ]]; then
  aws s3api head-object --bucket "$AWS_S3_BUCKET" --key "$s3_key" > "$work/s3-before.json"
fi
request 200 GET "/api/guias-procesadas/$numero_guia/descarga" "" "$work/guide.pdf"
test -s "$work/guide.pdf"
test "$(head -c 4 "$work/guide.pdf")" = '%PDF'

updated="CI-CLIENTE-ACTUALIZADO-$suffix"
update_body=$(jq -nc --arg t "$transportista" --arg c "$updated" --arg d "$direccion" '{transportista:$t,cliente:$c,direccionDestino:$d}')
request 200 PUT "/api/guias-procesadas/$numero_guia" "$update_body" "$work/update.json"
jq -e --arg c "$updated" '.cliente==$c' "$work/update.json" >/dev/null
request 200 GET "/api/guias-procesadas/$numero_guia" "" "$work/updated-get.json"
jq -e --arg c "$updated" --arg k "$s3_key" '.cliente==$c and .s3Key==$k' "$work/updated-get.json" >/dev/null
request 200 GET "/api/guias-procesadas/$numero_guia/descarga" "" "$work/updated.pdf"
test "$(head -c 4 "$work/updated.pdf")" = '%PDF'
if [[ -n ${AWS_ACCESS_KEY_ID:-} ]]; then
  aws s3api head-object --bucket "$AWS_S3_BUCKET" --key "$s3_key" > "$work/s3-after.json"
  test "$(jq -r .LastModified "$work/s3-before.json")" != "$(jq -r .LastModified "$work/s3-after.json")"
fi

request 404 GET /api/guias/999
request 404 GET /api/guias-procesadas/999
request 404 GET /api/guias-procesadas/999/descarga
request 404 PUT /api/guias-procesadas/999 "$update_body"
request 404 DELETE /api/guias-procesadas/999
request 204 DELETE "/api/guias-procesadas/$numero_guia"
request 404 GET "/api/guias-procesadas/$numero_guia"
if [[ -n ${AWS_ACCESS_KEY_ID:-} ]]; then
  if aws s3api head-object --bucket "$AWS_S3_BUCKET" --key "$s3_key" >/dev/null 2>&1; then
    echo "Deleted guide still exists in S3" >&2
    exit 1
  fi
fi
request 204 DELETE "/api/guias/$id"
request 404 GET "/api/guias/$id"
created=false

printf '%s\n' \
  'Security API Gateway: PASS' 'Creation and RabbitMQ processing: PASS' \
  'S3/PDF validation: PASS' 'Update: PASS' 'Consumer, S3 and producer cleanup: PASS' > e2e-summary.txt
cat e2e-summary.txt >> "$GITHUB_STEP_SUMMARY"
