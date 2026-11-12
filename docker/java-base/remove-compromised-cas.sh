#!/usr/bin/env bash
# OpenJDK's eMudhra mitigation also removes the cross-signing Comodo root.
# https://openjdk.org/emudhra (CVE-2026-91403 and CVE-2026-114820)
set -euo pipefail

KEYSTORE="${1:?Usage: remove-compromised-cas.sh <cacerts>}"
# Match certificate subjects, not vendor-specific aliases; already absent is safe.
ALIASES="$(keytool -J-Duser.language=en -J-Duser.country=US \
    -list -v -keystore "$KEYSTORE" -storepass changeit | awk '
    /^Alias name: / { alias = substr($0, 13) }
    /^Owner: CN=(emSign ECC Root CA - G3|emSign Root CA - G1|emSign Root CA - G2|AAA Certificate Services),/ { print alias }
')"
while IFS= read -r alias; do
    [ -n "$alias" ] || continue
    keytool -delete -keystore "$KEYSTORE" -storepass changeit -alias "$alias"
done <<< "$ALIASES"
