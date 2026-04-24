#!/usr/bin/env bash
##
# One-time setup for the PR-preview VM.
#
# Assumes: Ubuntu 22.04+ with a public IP, a domain with a wildcard A record
# (*.previews.example.com) pointing at this VM, and ports 80/443 open.
#
# After this script runs, the VM is ready to receive `docker compose up` calls
# for any compose file that tags the dotcms service with caddy labels.
##
set -euo pipefail

: "${PREVIEW_DOMAIN:?Set PREVIEW_DOMAIN (e.g. previews.example.com) before running}"

# 1. Docker engine
if ! command -v docker >/dev/null; then
    curl -fsSL https://get.docker.com | sh
    usermod -aG docker "${SUDO_USER:-$USER}" || true
fi

# 2. External network shared between Caddy and every PR stack
docker network inspect caddy >/dev/null 2>&1 || docker network create caddy

# 3. Caddy reverse proxy with automatic HTTPS (Let's Encrypt).
#    caddy-docker-proxy watches the docker socket for containers with caddy=*
#    labels and generates a live Caddyfile — zero config changes per PR.
docker rm -f caddy 2>/dev/null || true
docker run -d \
    --name caddy \
    --restart unless-stopped \
    --network caddy \
    -p 80:80 -p 443:443 \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v caddy-data:/data \
    -e CADDY_INGRESS_NETWORKS=caddy \
    lucaslorentz/caddy-docker-proxy:ci-alpine

echo "Host is ready. Point *.${PREVIEW_DOMAIN} at this VM and you can start deploying PR stacks."
