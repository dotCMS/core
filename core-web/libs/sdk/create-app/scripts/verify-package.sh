#!/usr/bin/env bash
#
# AC-013 — the bundled compose file must be in the PUBLISHED PACKAGE.
#
# Why this exists as a separate check, over the build output:
#
#   Asserting the two manifests is necessary but not sufficient. `packaging.spec.ts` reads
#   package.json and project.json from the SOURCE TREE, and verify-cold-start.sh --static
#   resolves the asset relative to src/. Both pass identically whether or not the file ever
#   ships. A wrong `output:` in the esbuild assets entry satisfies every one of them and still
#   publishes a package with no compose file — and then every local-Docker run fails at its
#   first step, which is exactly what AC-013 exists to prevent.
#
#   So this asserts the artifact: the file is in dist at the path the CLI resolves at runtime,
#   and npm would actually put it in the tarball.
#
# Usage:  ./verify-package.sh [dist-dir]      (defaults to the nx output path)
# Exit 0 = the asset ships.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST="${1:-$SCRIPT_DIR/../../../../dist/libs/sdk/create-app}"
ASSET_REL="assets/docker-compose.yml"

PASS=0
FAIL=0
ok()  { printf '  \033[32mPASS\033[0m  %s\n' "$1"; PASS=$((PASS + 1)); }
bad() { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAIL=$((FAIL + 1)); }

printf '\n\033[1mverify-package.sh\033[0m — %s\n\n' "$DIST"

if [ ! -d "$DIST" ]; then
    bad "no build output at $DIST — run: pnpm nx build sdk-create-app"
    printf '\n\033[1mSummary\033[0m\n  %d passed, %d failed\n\n' "$PASS" "$FAIL"
    exit 1
fi

# 1. The file is where the CLI will look for it. resolveComposeSource() walks up from the
#    bundle entry, so the asset must sit beside index.js exactly as it does in the source tree.
if [ -f "$DIST/$ASSET_REL" ]; then
    ok "$ASSET_REL is in the build output"
else
    bad "$ASSET_REL is MISSING from the build output — check project.json's esbuild \`assets\` (input/glob/output)"
fi

# 2. npm would actually pack it. This is the half that `files` in package.json controls, and it
#    is a separate failure from the esbuild copy above: either alone ships a broken package.
PACKED="$(cd "$DIST" && npm pack --dry-run --json 2>/dev/null | grep -o "\"path\": *\"[^\"]*\"" | sed 's/.*: *"//; s/"$//')"

if [ -z "$PACKED" ]; then
    bad "npm pack --dry-run produced no file list — cannot confirm the tarball contents"
elif printf '%s\n' "$PACKED" | grep -qx "$ASSET_REL"; then
    ok "npm pack includes $ASSET_REL in the tarball"
else
    bad "npm pack does NOT include $ASSET_REL — check \`files\` in package.json"
    printf '        tarball contains: %s\n' "$(printf '%s ' $PACKED)"
fi

printf '\n\033[1mSummary\033[0m\n  %d passed, %d failed\n\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
