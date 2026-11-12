#!/usr/bin/env bash
# Run with a JDK on PATH: bash docker/java-base/remove-compromised-cas.test.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT
KEYSTORE="$WORKDIR/cacerts"

# Arbitrary aliases ensure removal depends on the subject, not a vendor's naming.
ROOTS=('emSign ECC Root CA - G3' 'emSign Root CA - G1' 'emSign Root CA - G2' 'AAA Certificate Services' 'Unrelated CA')
for i in "${!ROOTS[@]}"; do
    keytool -genkeypair -alias "test-$i" -dname "CN=${ROOTS[$i]},O=Test,C=US" \
        -keyalg EC -groupname secp256r1 -validity 1 -ext bc=ca:true \
        -keystore "$KEYSTORE" -storepass changeit -keypass changeit >/dev/null 2>&1
done

bash "$SCRIPT_DIR/remove-compromised-cas.sh" "$KEYSTORE"
for i in 0 1 2 3; do
    if keytool -list -keystore "$KEYSTORE" -storepass changeit -alias "test-$i" >/dev/null 2>&1; then
        echo "FAIL: compromised root remains: ${ROOTS[$i]}" >&2
        exit 1
    fi
done
keytool -list -keystore "$KEYSTORE" -storepass changeit -alias test-4 >/dev/null

# Re-running against an already mitigated keystore must preserve it.
cp "$KEYSTORE" "$WORKDIR/mitigated"
bash "$SCRIPT_DIR/remove-compromised-cas.sh" "$KEYSTORE"
cmp "$KEYSTORE" "$WORKDIR/mitigated"

if bash "$SCRIPT_DIR/remove-compromised-cas.sh" "$WORKDIR/missing" >/dev/null 2>&1; then
    echo 'FAIL: missing keystore was silently accepted' >&2
    exit 1
fi
printf 'PASS: four roots removed, unrelated root retained, repeatable, missing keystore rejected\n'
