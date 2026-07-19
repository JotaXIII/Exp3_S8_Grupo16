#!/usr/bin/env bash
# Prepara SSH con claves confiables.
set -Eeuo pipefail

: "${SSH_KEY:?Missing SSH_KEY}"
: "${KNOWN_HOSTS:?Missing trusted known_hosts entry}"
install -m 700 -d "$HOME/.ssh"
printf '%s\n' "$SSH_KEY" > "$HOME/.ssh/id_ec2"
printf '%s\n' "$KNOWN_HOSTS" > "$HOME/.ssh/known_hosts"
chmod 600 "$HOME/.ssh/id_ec2" "$HOME/.ssh/known_hosts"
