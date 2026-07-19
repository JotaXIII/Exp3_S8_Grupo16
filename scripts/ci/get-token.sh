#!/usr/bin/env bash
# Obtiene y valida el token OAuth.
set -Eeuo pipefail
for name in AZURE_TOKEN_URL AZURE_CLIENT_ID AZURE_CLIENT_SECRET AZURE_SCOPE; do
  test -n "${!name:-}" || { echo "Missing $name" >&2; exit 1; }
done

response=$(curl --fail-with-body --silent --show-error --request POST "$AZURE_TOKEN_URL" \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "client_id=$AZURE_CLIENT_ID" \
  --data-urlencode "client_secret=$AZURE_CLIENT_SECRET" \
  --data-urlencode "scope=$AZURE_SCOPE" \
  --data-urlencode 'grant_type=client_credentials')
token=$(jq -er '.access_token' <<<"$response")
echo "::add-mask::$token"

payload=$(cut -d. -f2 <<<"$token" | tr '_-' '/+')
case $((${#payload} % 4)) in 2) payload+='==';; 3) payload+='=';; esac
claims=$(printf '%s' "$payload" | base64 -d 2>/dev/null)
jq -e '(.exp // 0) > now and ((.scp // "") | split(" ") | index("GESTOR_GUIAS") != null)' <<<"$claims" >/dev/null
printf 'access_token=%s\n' "$token" >> "$GITHUB_OUTPUT"
echo "OAuth token exists, is current and contains GESTOR_GUIAS."
