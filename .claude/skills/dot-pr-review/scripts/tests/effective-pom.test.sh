#!/usr/bin/env bash
# effective-pom.test.sh — drives scripts/effective-pom.sh in a throwaway git repo whose mvnw is a
# stub, so no Maven, JDK or network is needed. Checks the verdict line, the bundle, the path
# normalization, and that nothing is written to the caller's directory.
# Run it from anywhere: bash scripts/tests/effective-pom.test.sh
set -uo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
script="$here/../effective-pom.sh"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

passed=0
failed=0
ok() { printf 'ok    %s\n' "$1"; passed=$((passed + 1)); }
nok() { printf 'FAIL  %s\n%s\n' "$1" "${2:-}" | sed 's/^/      /;1s/^      //'; failed=$((failed + 1)); }
expect() { # expect <name> <got> <want>
  if [ "$2" = "$3" ]; then ok "$1"; else nok "$1" "want [$3]
got  [$2]"; fi
}

# A repo with a stub mvnw: it writes the effective POM as its own path plus pom.xml, and fails
# when pom.xml says FAIL. It also drops a target/ file into its working directory, as core's
# build-cache extension does.
root="$work/root"
mkdir -p "$root"
(
  cd "$root" || exit 1
  git init -q -b main .
  git config user.email t@example.com && git config user.name t
  cat >mvnw <<'EOF'
#!/usr/bin/env bash
out=""
for a in "$@"; do case "$a" in -Doutput=*) out="${a#-Doutput=}" ;; esac; done
echo "$*" >"$out.args"
mkdir -p target && echo '{}' >target/build-report.json
grep -q FAIL pom.xml && { echo "[ERROR] Non-resolvable parent POM"; exit 1; }
{ echo "<basedir>$PWD</basedir>"; cat pom.xml; } >"$out"
EOF
  chmod +x mvnw
  printf '<project>\n  <argLine>@{argLine} -Xmx1024m</argLine>\n</project>\n' >pom.xml
  git add -A && git commit -qm base
)
base_sha="$(git -C "$root" rev-parse HEAD)"
printf '<project>\n  <argLine>--enable-native-access=ALL-UNNAMED</argLine>\n</project>\n' >"$root/pom.xml"
echo pom.xml >"$work/changed.txt"

caller="$work/caller"
mkdir -p "$caller"
run() { # run <run-dir> [extra args...]
  local rd="$1"; shift
  out="$(cd "$caller" && bash "$script" --root "$root" --head "$root" --base-sha "$base_sha" \
    --run-dir "$rd" --modules . --changed "$work/changed.txt" --out "$rd/bundle.diff" "$@" 2>"$work/err")"
  code=$?
}

# A resolvable change: the verdict counts the changed lines, the bundle shows them, and the two
# checkouts' paths are gone from it.
run "$work/run1"
expect "a resolvable change prints OK with its changed lines" "$code $out" "0 OK lines=2"
expect "the bundle shows the head's argLine" \
  "$(grep -c -F -- '+  <argLine>--enable-native-access=ALL-UNNAMED</argLine>' "$work/run1/bundle.diff")" "1"
expect "no checkout path is left in the bundle" "$(grep -c -e "$root" -e "$work/run1/base" "$work/run1/bundle.diff")" "0"
expect "the base worktree was added" "$(git -C "$root" worktree list | grep -c "$work/run1/base")" "1"
expect "the plugin is named in full, so its version comes from the POM" \
  "$(grep -c -F 'org.apache.maven.plugins:maven-help-plugin:effective-pom' "$work/run1/maven/head.raw.xml.args")" "1"
expect "nothing is written to the caller's directory" "$(find "$caller" -mindepth 1 | wc -l | tr -d ' ')" "0"

# --skip writes the reason and runs nothing.
run "$work/run2" --skip "PR from a fork"
expect "--skip prints NOT_COMPUTED" "$code $out" "0 NOT_COMPUTED PR from a fork"
expect "--skip writes the reason as the bundle" "$(cat "$work/run2/bundle.diff")" "NOT COMPUTED: PR from a fork"
expect "--skip adds no worktree" "$(git -C "$root" worktree list | grep -c "$work/run2/base")" "0"

# A change to .mvn/ or mvnw is never resolved.
printf 'pom.xml\n.mvn/jvm.config\n' >"$work/changed.txt"
run "$work/run3"
expect "a .mvn/ change is not resolved" "$code ${out%%,*}" "0 NOT_COMPUTED the change touches mvnw or .mvn/"
printf 'mvnw\n' >"$work/changed.txt"
run "$work/run3"
expect "a mvnw change is not resolved" "$code ${out%%,*}" "0 NOT_COMPUTED the change touches mvnw or .mvn/"

# A wrapper-only PR has no changed pom.xml, so the orchestrator sends no modules: still skipped.
printf 'mvnw\n.mvn/wrapper/maven-wrapper.properties\n' >"$work/changed.txt"
wrapper() { # wrapper <run-dir> [--modules args...]
  local rd="$1"; shift
  out="$(cd "$caller" && bash "$script" --root "$root" --head "$root" --base-sha "$base_sha" \
    --run-dir "$rd" "$@" --changed "$work/changed.txt" --out "$rd/bundle.diff" 2>"$work/err")"
  code=$?
}
wrapper "$work/run5"
expect "a wrapper-only change without --modules is not resolved" "$code ${out%%,*}" "0 NOT_COMPUTED the change touches mvnw or .mvn/"
wrapper "$work/run5" --modules ""
expect "a wrapper-only change with empty --modules is not resolved" "$code ${out%%,*}" "0 NOT_COMPUTED the change touches mvnw or .mvn/"
expect "a wrapper-only change adds no worktree" "$(git -C "$root" worktree list | grep -c "$work/run5/base")" "0"
echo pom.xml >"$work/changed.txt"

# Maven failing on one side is reported, not fatal.
printf '<project>FAIL</project>\n' >"$root/pom.xml"
run "$work/run4"
expect "a head that does not resolve prints NOT_COMPUTED" "$code ${out%%:*}" "0 NOT_COMPUTED the head does not resolve offline"
expect "the bundle names Maven's error" "$(cat "$work/run4/bundle.diff")" \
  "NOT COMPUTED: the head does not resolve offline: [ERROR] Non-resolvable parent POM"

# Usage errors exit 2.
(cd "$caller" && bash "$script" --root "$root" >/dev/null 2>&1)
expect "no --out is a usage error" "$?" "2"
(cd "$caller" && bash "$script" --out "$work/x.diff" --root "$root" >/dev/null 2>&1)
expect "missing required flags are a usage error" "$?" "2"
(cd "$caller" && bash "$script" --out "$work/x.diff" --root "$root" --head "$root" --base-sha "$base_sha" \
  --run-dir "$work/run6" --changed "$work/changed.txt" >/dev/null 2>&1)
expect "a pom.xml change without --modules is a usage error" "$?" "2"
(cd "$caller" && bash "$script" --out "$work/x.diff" --root "$root" --head "$root" --base-sha "$base_sha" \
  --run-dir "$work/run6" --modules . --changed "$work/missing.txt" >/dev/null 2>&1)
expect "an unreadable --changed is a usage error" "$?" "2"

for d in "$work"/run*/base; do [ -d "$d" ] && git -C "$root" worktree remove --force "$d"; done

printf '\n%d passed, %d failed\n' "$passed" "$failed"
[ "$failed" -eq 0 ]
