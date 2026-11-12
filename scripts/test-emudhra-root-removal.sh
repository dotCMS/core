#!/usr/bin/env bash
# Exercise the runtime Dockerfile's actual removal block with temporary keystores.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT
export KEYSTORE="$WORKDIR/cacerts"

# Redirect only the keystore, leaving the Dockerfile's shell logic unchanged.
awk '
    /^RUN set -eu;/ { sub(/^RUN /, ""); copying = 1 }
    copying && /^$/ { exit }
    copying { gsub("/java/bin/keytool -cacerts", "keytool -keystore \"$KEYSTORE\""); print }
' "$ROOT/dotCMS/src/main/docker/original/Dockerfile" > "$WORKDIR/remove.sh"
test -s "$WORKDIR/remove.sh"

ALIAS='cn=emsign_ecc_root_ca_-_g3,o=emudhra_technologies_limited,ou=emsign_pki,c=in'
for alias in "$ALIAS" unrelated; do
    keytool -genkeypair -alias "$alias" -dname 'CN=Test CA' \
        -keyalg EC -groupname secp256r1 -validity 1 \
        -keystore "$KEYSTORE" -storepass changeit -keypass changeit >/dev/null 2>&1
done
sh "$WORKDIR/remove.sh"
if keytool -list -keystore "$KEYSTORE" -storepass changeit -alias "$ALIAS" >/dev/null 2>&1; then
    echo 'FAIL: distrusted root remains' >&2
    exit 1
fi
keytool -list -keystore "$KEYSTORE" -storepass changeit -alias unrelated >/dev/null

# A patched base image starts here: the root is already absent.
cp "$KEYSTORE" "$WORKDIR/mitigated"
sh "$WORKDIR/remove.sh"
cmp "$KEYSTORE" "$WORKDIR/mitigated"

printf 'invalid keystore' > "$KEYSTORE"
if sh "$WORKDIR/remove.sh" >/dev/null 2>&1; then
    echo 'FAIL: invalid keystore was silently accepted' >&2
    exit 1
fi
rm "$KEYSTORE"
if sh "$WORKDIR/remove.sh" >/dev/null 2>&1; then
    echo 'FAIL: missing keystore was silently accepted' >&2
    exit 1
fi
printf 'PASS: root removed, unrelated entry preserved, absent root accepted, invalid/missing keystores rejected\n'
