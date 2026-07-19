#!/usr/bin/env bash
# Valida la estructura y seguridad del código.
set -Eeuo pipefail

producer=gestion-guias-main/ms-productor-guias
consumer=gestion-guias-main/ms-consumidor-guias
for required in "$producer/mvnw" "$producer/pom.xml" "$producer/Dockerfile" \
  "$consumer/mvnw" "$consumer/pom.xml" "$consumer/Dockerfile"; do
  test -f "$required" || { echo "Missing required file: $required" >&2; exit 1; }
done

for route in '@PostMapping' '@GetMapping' 'transportista/{transportista}' 'fecha/{fecha}'; do
  grep -Fq "$route" "$producer/src/main/java/com/transportista/gestionguias/controller/GuiaDespachoController.java"
done
for route in '@GetMapping' '@PutMapping' '@DeleteMapping' '/{numeroGuia}/descarga' '/buscar'; do
  grep -Fq "$route" "$consumer/src/main/java/com/transportista/gestionguias/controller/GuiaProcesadaController.java"
done

active=("$producer/src/main" "$consumer/src/main")
grep -Rqs 'SCOPE_GESTOR_GUIAS' "${active[@]}"
if grep -REn 'setAuthoritiesClaimName\("roles"\)|ROLE_GESTOR_GUIAS|ROLE_DESCARGA_GUIAS|hasRole\(|hasAnyRole\(' "${active[@]}"; then
  echo "Legacy role authorization remains in active code" >&2
  exit 1
fi

# Busca credenciales versionadas.
if git grep -IlE '(AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}|BEGIN (RSA |OPENSSH )?PRIVATE KEY)' -- \
  ':!*.example' ':!**/target/**' | grep -q .; then
  echo "Potential credential material exists in tracked files" >&2
  exit 1
fi

echo "Repository structure, endpoints and active scope configuration validated."
