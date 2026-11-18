#!/usr/bin/env bash
# Only a missing tag/platform permits publication; registry failures fail closed.
set -euo pipefail

readonly MISSING=3
readonly HTTP_OK=200
readonly HTTP_NOT_FOUND=404
tag="${1:?Java-base tag required}"
if [[ ! "$tag" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
    echo "::error::Invalid Java-base tag" >&2
    exit 1
fi

response=$(mktemp)
trap 'rm -f "$response"' EXIT
if ! status=$(curl --silent --show-error --location --retry 3 --retry-delay 5 \
    --connect-timeout 15 --max-time 60 --output "$response" --write-out '%{http_code}' \
    "https://hub.docker.com/v2/repositories/dotcms/java-base/tags/$tag"); then
    echo '::error::Java-base lookup transport failed; refusing to assume absence.' >&2
    exit 1
fi
case "$status" in
    "$HTTP_NOT_FOUND")
        echo "Java-base $tag is missing."
        exit "$MISSING"
        ;;
    "$HTTP_OK")
        # Reject malformed responses instead of mistaking them for an incomplete image.
        if ! jq -e '.images | type == "array" and all(.[];
            type == "object" and (.os | type == "string") and (.architecture | type == "string"))' \
            "$response" >/dev/null; then
            echo '::error::Malformed Java-base registry response; refusing to assume absence.' >&2
            exit 1
        fi
        if jq -e 'any(.images[]; .os == "linux" and .architecture == "amd64") and
                  any(.images[]; .os == "linux" and .architecture == "arm64")' "$response" >/dev/null; then
            echo "Java-base $tag is available for linux/amd64 and linux/arm64."
            exit 0
        fi
        echo "Java-base $tag is missing a required Linux architecture."
        exit "$MISSING"
        ;;
    *)
        echo "::error::Java-base lookup failed (HTTP $status); refusing to assume absence." >&2
        exit 1
        ;;
esac
