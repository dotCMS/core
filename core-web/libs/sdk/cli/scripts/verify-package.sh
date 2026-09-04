#!/usr/bin/env bash
#
# Packaging invariants for the published `dotcms` CLI.
#
# Every check here corresponds to a failure that the unit tests CANNOT see, because they run
# against the source tree and this runs against the artifact npm would upload. Each one was a
# real defect during development, not a hypothetical:
#
#   * The shebang lives only in the `production` esbuild configuration. A default build produces
#     a file that is not executable as a bin, and nothing else notices.
#   * `ora` and `chalk` were declared dependencies AND esbuild externals that nothing imported —
#     every `npx dotcms` user installing two packages for nothing.
#   * `@dotcms/http` is an internal, unpublished library. If esbuild ever stops inlining it, the
#     published package gains an unresolvable dependency and fails on first run.
#
# Usage:  ./verify-package.sh [dist-dir]      (defaults to the nx output path)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST="${1:-$SCRIPT_DIR/../../../../dist/libs/sdk/cli}"
SRC_PKG="$SCRIPT_DIR/../package.json"

PASS=0
FAIL=0
ok()  { printf '  \033[32mPASS\033[0m  %s\n' "$1"; PASS=$((PASS + 1)); }
bad() { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAIL=$((FAIL + 1)); }

printf '\n\033[1mverify-package.sh\033[0m — %s\n\n' "$DIST"

if [ ! -d "$DIST" ] || [ ! -f "$DIST/index.js" ]; then
    bad "no build output at $DIST — run: pnpm nx build sdk-cli --configuration=production"
    printf '\n\033[1mSummary\033[0m\n  %d passed, %d failed\n\n' "$PASS" "$FAIL"
    exit 1
fi

# 1. The bin is executable. `npx dotcms` runs this file directly; without the shebang the shell
#    has no interpreter for it.
if [ "$(head -c 2 "$DIST/index.js")" = '#!' ]; then
    ok "index.js carries a shebang"
else
    bad "index.js has NO shebang — it was built without --configuration=production"
fi

# 2. package.json's bin points at a file that exists.
BIN_PATH="$(cd "$DIST" && node -p "Object.values(require('./package.json').bin)[0]" 2>/dev/null)"
if [ -n "$BIN_PATH" ] && [ -f "$DIST/${BIN_PATH#./}" ]; then
    ok "bin -> $BIN_PATH exists in the build output"
else
    bad "bin -> ${BIN_PATH:-<unset>} is missing from the build output"
fi

# 3. The internal library is INLINED, never left as an import. It is private and unpublished, so
#    a surviving import would be unresolvable for every user.
if grep -q '@dotcms/http' "$DIST/index.js"; then
    bad "@dotcms/http survives as an import — it is unpublished, so the package would not run"
else
    ok "@dotcms/http is inlined, not imported"
fi

# 4. Every declared runtime dependency is actually used. A dependency nobody imports is install
#    weight for every `npx` user and a sign the code drifted from the manifest.
DEPS="$(node -p "Object.keys(require('$SRC_PKG').dependencies || {}).join(' ')")"
UNUSED=""
for dep in $DEPS; do
    grep -q "\"$dep\"\|'$dep'" "$DIST/index.js" || UNUSED="$UNUSED $dep"
done
if [ -z "$UNUSED" ]; then
    ok "every declared dependency is imported by the bundle"
else
    bad "declared but never imported:$UNUSED — drop them from package.json, or use them"
fi

# 5. npm would actually pack the entry point and the README.
PACKED="$(cd "$DIST" && npm pack --dry-run --json 2>/dev/null | grep -o "\"path\": *\"[^\"]*\"" | sed 's/.*: *"//; s/"$//')"
if [ -z "$PACKED" ]; then
    bad "npm pack --dry-run produced no file list — cannot confirm the tarball contents"
else
    for want in index.js README.md; do
        if printf '%s\n' "$PACKED" | grep -qx "$want"; then
            ok "npm pack includes $want"
        else
            bad "npm pack does NOT include $want — check \`files\` in package.json"
        fi
    done
fi

# 6. The published name is the unscoped one the release guard now resolves by reading .name.
#    Asserted so a rename cannot silently reintroduce the @dotcms/<dir> assumption the SDK
#    deploy action used to make.
NAME="$(node -p "require('$SRC_PKG').name")"
if [ "$NAME" = "dotcms" ]; then
    ok "package name is 'dotcms' (unscoped, as the release pipeline expects)"
else
    bad "package name is '$NAME' — update .github/actions/core-cicd/deployment/deploy-javascript-sdk"
fi

printf '\n\033[1mSummary\033[0m\n  %d passed, %d failed\n\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
