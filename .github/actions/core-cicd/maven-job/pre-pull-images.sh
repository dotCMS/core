#!/usr/bin/env bash
#
# Pre-pull the docker-maven-plugin test images (#38189).
#
# environments/environment.properties names canonical Docker Hub images. The plugin pulls
# each one once per job and a single toomanyrequests/unknown-blob error fails the job (and,
# via fail-fast, cancels ~20 siblings). So pull them here first, with retries:
#
#   1. through mirror.gcr.io, then retag as the canonical reference. The plugin's default
#      IfNotPresent pull policy then finds it. (A CI spike on #38189 showed the pull works with
#      the Docker Hub login active, and that a retagged image stops the plugin pulling again.)
#   2. otherwise from Docker Hub with the job's (docker/login-action) credentials.
#
# Best effort by design: this script ALWAYS exits 0. If every pull fails the plugin pulls
# as it did before this step existed.
#
# Usage: PRE_PULL_IMAGES="postgres search" pre-pull-images.sh
# Env:   ENV_FILE (default: environments/environment.properties)

set -uo pipefail

KEYS="${PRE_PULL_IMAGES:-}"
ENV_FILE="${ENV_FILE:-environments/environment.properties}"
MIRROR="mirror.gcr.io"
MIRROR_TRIES=3
HUB_TRIES=2

for key in $KEYS; do
  ref="$(grep -m1 "^docker\.image\.${key}=" "$ENV_FILE" 2>/dev/null | cut -d= -f2-)"
  source="none"

  if [[ -z "$ref" ]]; then
    echo "::warning::pre-pull ${key}: no docker.image.${key} in ${ENV_FILE}"
  elif docker image inspect "$ref" >/dev/null 2>&1; then
    source="present"
  else
    for attempt in $(seq 1 "$MIRROR_TRIES"); do
      if timeout 300 docker pull "${MIRROR}/${ref}" >/dev/null 2>&1 \
         && docker tag "${MIRROR}/${ref}" "$ref"; then
        source="mirror"
        break
      fi
      [[ "$attempt" -lt "$MIRROR_TRIES" ]] && sleep $((10 * attempt))
    done

    if [[ "$source" == "none" ]]; then
      for attempt in $(seq 1 "$HUB_TRIES"); do
        if timeout 300 docker pull "$ref" >/dev/null 2>&1; then
          source="hub"
          break
        fi
        [[ "$attempt" -lt "$HUB_TRIES" ]] && sleep $((10 * attempt))
      done
    fi
  fi

  echo "::notice::pre-pull ${key} source=${source}"
  [[ -n "${GITHUB_STEP_SUMMARY:-}" ]] && echo "pre-pull ${key} source=${source}" >> "$GITHUB_STEP_SUMMARY"
done

exit 0
