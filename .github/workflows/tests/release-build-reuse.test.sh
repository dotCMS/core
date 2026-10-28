#!/usr/bin/env bash
#
# Contract tests for release-artifact reuse and Docker cache isolation.
# Tracking issue: #37607
#   "CI: speed up the release GitHub Action with Maven artifact reuse and
#    Docker cache isolation"
#
# This began life as the TDD "Red" suite for Phase 1 of #37607 and is now the
# regression suite for it: every section is green on main and should stay that
# way. A non-zero exit means something regressed. Section 12 covers the SAML
# release integration (#38104). The sections are independent, so a failure names
# the behaviour that broke rather than just a count.
#
# Two kinds of assertion:
#
#   * Wiring assertions — grep/awk over the real workflow and action YAML. These
#     catch the classic composite-action failure mode where an input is
#     documented and even passed by the caller, but never actually forwarded
#     into the step that needs it.
#
#   * Behavioural assertions — the real `run:` block is pulled out of the YAML
#     and executed with Maven/Docker stubbed, so the shell logic under test is
#     the shell logic that ships. Nothing is copied into this file.
#
# Java version: .sdkmanrc is the single source of truth. Section 8 reads it and
# derives every expectation from it, so bumping the Java version is a one-line
# change there and these tests follow — no version is pinned in this file.
#
# Run:  bash .github/workflows/tests/release-build-reuse.test.sh
# Deps: bash 3.2+ (macOS default is fine), awk, sed, grep, cut.
#
# Run in CI by cicd_pr_workflow-lint.yml, which executes every suite under
# .github/workflows/tests/ on PRs touching .github/**. That workflow's actionlint
# job covers the workflow YAML; these suites cover the composite actions too.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

MAVEN_JOB="$REPO_ROOT/.github/actions/core-cicd/maven-job/action.yml"
BUILD_PHASE="$REPO_ROOT/.github/workflows/cicd_comp_build-phase.yml"
RELEASE_PHASE="$REPO_ROOT/.github/workflows/cicd_comp_release-phase.yml"
DEPLOY_DOCKER="$REPO_ROOT/.github/actions/core-cicd/deployment/deploy-docker/action.yml"
DEPLOY_PHASE="$REPO_ROOT/.github/workflows/cicd_comp_deployment-phase.yml"
RELEASE_WF="$REPO_ROOT/.github/workflows/cicd_6-release.yml"
VARIANT_WF="$REPO_ROOT/.github/workflows/cicd_7-release-java-variant.yml"

pass=0
fail=0
ok()  { pass=$((pass + 1)); printf '  ok   %s\n' "$1"; }
bad() { fail=$((fail + 1)); printf '  FAIL %s\n' "$1"; [[ -n "${2:-}" ]] && printf '         -> %s\n' "$2"; return 0; }

rel() { printf '%s' "${1#"$REPO_ROOT"/}"; }

# ---------------------------------------------------------------- assertions --

# Whole file matches an ERE.
has()   { if grep -Eq -- "$2" "$1"; then ok "$3"; else bad "$3" "/$2/ not found in $(rel "$1")"; fi; }
lacks() { if grep -Eq -- "$2" "$1"; then bad "$3" "/$2/ unexpectedly present in $(rel "$1")"; else ok "$3"; fi; }

# Print the YAML text of the step in $1 whose `id:`, `name:` or `uses:` is $2.
# A `uses:` key is the action path without the leading `uses: ` — that is how a
# step that only has a `stage-name:` reaches its action. Scans every line of the
# step, not just the first, because `id:` usually follows `name:`.
step_block() {
  awk -v key="$2" '
    function isstep(line) { return line ~ /^[[:space:]]*-[[:space:]]/ }
    function flush() { if (m) printf "%s", buf; buf = ""; m = 0 }
    function consider(line,   s, had, ind) {
      ind = 0; if (match(line, /^[[:space:]]*/)) ind = RLENGTH
      # Only this step own id/name/uses keys: the dash column, or dash + 2.
      # Without this, `with: name: maven-repo-...` reads as a step name.
      if (ind != dash_indent && ind != dash_indent + 2) return
      s = line
      sub(/^[[:space:]]*-?[[:space:]]*/, "", s)
      had = sub(/^(id|name|uses):[[:space:]]*/, "", s)
      if (!had) return
      gsub(/^["\047]|["\047]$/, "", s)
      if (s == key) m = 1
    }
    BEGIN { dash_indent = -1 }
    isstep($0) {
      flush()
      match($0, /^[[:space:]]*/); dash_indent = RLENGTH
    }
    { if (buf != "" || isstep($0)) { buf = buf $0 "\n"; consider($0) } }
    END { flush() }
  ' "$1"
}

# A step exists and its body references an ERE (this is the "is the flag actually
# forwarded to the step that needs it" check).
block_has() {
  local blk; blk="$(step_block "$1" "$2")"
  if [[ -z "$blk" ]]; then
    bad "$4" "step '$2' not found in $(rel "$1")"
  elif grep -Eq -- "$3" <<<"$blk"; then
    ok "$4"
  else
    bad "$4" "step '$2' never references /$3/"
  fi
}

# As block_has, but the match must be on an `if:` line. A comment or a `with:`
# value that merely mentions the flag is not a gate.
block_has_if() {
  local blk; blk="$(step_block "$1" "$2")"
  if [[ -z "$blk" ]]; then
    bad "$4" "step '$2' not found in $(rel "$1")"
  elif grep -E '^[[:space:]]*if:' <<<"$blk" | grep -Eq -- "$3"; then
    ok "$4"
  else
    bad "$4" "no 'if:' in step '$2' matches /$3/"
  fi
}

# The inverse: a step must NOT reference something.
block_lacks() {
  local blk; blk="$(step_block "$1" "$2")"
  if [[ -z "$blk" ]]; then
    bad "$4" "step '$2' not found in $(rel "$1")"
  elif grep -Eq -- "$3" <<<"$blk"; then
    bad "$4" "step '$2' unexpectedly references /$3/"
  else
    ok "$4"
  fi
}

# The `default:` belonging to an input, bounded to that input's own block (the
# next key at the same indent ends it) rather than the next `default:` in the
# file, which may belong to the following input. $3 = indent (2 for a composite
# action input, 6 for a reusable workflow's workflow_call inputs).
input_default() {
  awk -v k="$2" -v ind="${3:-2}" '
    BEGIN { pad = sprintf("%*s", ind, "") }
    $0 ~ ("^" pad k ":[[:space:]]*$") { f = 1; next }
    f && $0 ~ ("^" pad "[a-zA-Z0-9_-]+:") { exit }
    f && $0 ~ ("^" pad "  default:") { sub(/^[[:space:]]*default:[[:space:]]*/, ""); gsub(/["\047]/, ""); print; exit }
  ' "$1"
}

# Extract a `run: |` block by step id, dedented, with Actions expressions for the
# two suffix-related inputs rewritten to plain env vars.
extract_run_body() {
  awk -v want="$2" '
    # A new step ends the search: an `id:` on a uses-only step (no run:) must not
    # fall through and extract the next step run block.
    /^[[:space:]]*-[[:space:]]/ { if (collecting) exit; found = 0 }
    # `id:` may sit on the list dash ("- id: run-maven-build") or alone. Some
    # steps (the release-coordinate tripwire) carry no id, so `name:` selects too.
    $0 ~ "^[[:space:]]*-?[[:space:]]*(id|name):[[:space:]]*" want "[[:space:]]*$" { found = 1; next }
    found && !collecting && /^[[:space:]]*run:[[:space:]]*\|[[:space:]]*$/ {
      match($0, /^[[:space:]]*/); run_indent = RLENGTH; collecting = 1; next
    }
    collecting {
      if ($0 ~ /^[[:space:]]*$/) { print ""; next }
      match($0, /^[[:space:]]*/)
      if (RLENGTH <= run_indent) exit
      print substr($0, run_indent + 3)
    }
  ' "$1" | sed -E \
      -e 's/\$\{\{ *inputs\.artifact-suffix *\}\}/$IN_ARTIFACT_SUFFIX/g' \
      -e 's/\$\{\{ *inputs\.java-version *\}\}/$IN_JAVA_VERSION/g' \
      -e 's/\$\{\{ *inputs\.release_version *\}\}/$IN_RELEASE_VERSION/g'
}

# Run an extracted body. cwd is the repo root so a body that reads .sdkmanrc sees
# the real project file rather than a copy that could drift from it. Truncates $2
# first: these files are reused by every case, so a body that writes nothing must
# not inherit the previous case's value. Returns the body status, and callers
# assert it — a body that dies on an un-substituted ${{ }} writes nothing, and
# "empty" must never be allowed to read as "correct".
# $1 = path to body, $2 = GITHUB_OUTPUT, $3 = cwd (defaults to the repo root so
# a body that reads .sdkmanrc sees the real project file).
run_body() {
  : > "$2"
  local cwd="${3:-$REPO_ROOT}"
  ( cd "$cwd" \
    && GITHUB_OUTPUT="$2" GITHUB_ENV="$WORKDIR/gh_env" \
       bash "$1" ) >/dev/null 2>&1
}

out_val() { grep -E "^$2=" "$1" | tail -n 1 | cut -d= -f2-; }

# =============================================================== 1. Maven job ==
# The build job currently couples three unrelated things to `generate-docker`:
# producing the deployment context, building a throwaway test image, and saving
# that image as a ~1GB artifact. Release downloads the context and never looks at
# the test image. Split the flags so release can keep the context and drop the
# rest.

echo "== 1. maven-job: independent artifact flags =="
has "$MAVEN_JOB" '^  generate-test-image:'      "maven-job declares a generate-test-image input"
has "$MAVEN_JOB" '^  generate-build-classes:'   "maven-job declares a generate-build-classes input"

# Defaults must stay permissive: dozens of callers rely on today's behaviour.
if [[ "$(input_default "$MAVEN_JOB" generate-test-image)" == "true" ]]; then
  ok "generate-test-image defaults to true (shared callers unaffected)"
else
  bad "generate-test-image defaults to true (shared callers unaffected)"
fi
if [[ "$(input_default "$MAVEN_JOB" generate-build-classes)" == "true" ]]; then
  ok "generate-build-classes defaults to true (shared callers unaffected)"
else
  bad "generate-build-classes defaults to true (shared callers unaffected)"
fi

block_has_if "$MAVEN_JOB" build-docker-image-from-archive "generate-test-image == 'true'" \
  "test-image build is gated on generate-test-image"
block_has_if "$MAVEN_JOB" wait-docker-image-build "generate-test-image == 'true'" \
  "test-image build wait (which follows the background build and save) is gated on generate-test-image"
block_has_if "$MAVEN_JOB" upload-docker-image "generate-test-image == 'true'" \
  "test-image upload is gated on generate-test-image"
block_has_if "$MAVEN_JOB" persist-build-classes "generate-build-classes == 'true'" \
  "build-classes upload is gated on generate-build-classes"

# The deployment context is NOT the test image: it has to survive the split. Gating
# it on generate-test-image as well would delete the artifact the deployment phase
# downloads, so assert both directions.
block_has_if "$MAVEN_JOB" persist-docker-build-context "inputs.generate-docker == 'true'" \
  "docker-build-context upload still follows generate-docker"
block_lacks "$MAVEN_JOB" persist-docker-build-context 'generate-test-image' \
  "docker-build-context is not gated on generate-test-image"

# ============================================================= 2. Build phase ==
echo "== 2. build phase: forwards the new flags =="
has "$BUILD_PHASE" '^ {6}generate-test-image:'    "build phase declares generate-test-image"
has "$BUILD_PHASE" '^ {6}generate-build-classes:' "build phase declares generate-build-classes"
block_has "$BUILD_PHASE" './.github/actions/core-cicd/maven-job' 'generate-test-image' \
  "build phase forwards generate-test-image to maven-job"
block_has "$BUILD_PHASE" './.github/actions/core-cicd/maven-job' 'generate-build-classes' \
  "build phase forwards generate-build-classes to maven-job"

# Release-mode changelist: the initial install must produce the *suffixed* Maven
# coordinates itself, because the second install that does that today is removed.
has "$BUILD_PHASE" 'release-build' "build phase exposes a release-build input"
block_has "$BUILD_PHASE" './.github/actions/core-cicd/maven-job' 'release-build' \
  "maven-job receives release-build"

# ======================================== 3. Artifact suffix is published once ==
echo "== 3. resolved suffix is exposed for downstream jobs =="
has "$MAVEN_JOB" '^outputs:' "maven-job declares outputs (so the resolved suffix can be reused)"
has "$MAVEN_JOB" 'value:.*steps\.artifact-suffix\.outputs\.raw_suffix' \
  "maven-job maps its artifact-suffix output to the step raw_suffix"
block_has "$MAVEN_JOB" artifact-suffix 'raw_suffix' \
  "suffix step emits raw_suffix (separator-free) alongside the Maven suffix"
# `artifact-suffix` was already an input of this workflow, so matching the bare
# string proves nothing — assert the actual output wiring.
has "$BUILD_PHASE" 'value:.*jobs\.build-jdk11\.outputs\.artifact-suffix' \
  "build phase re-exports the build job resolved artifact suffix"
has "$BUILD_PHASE" 'artifact-suffix:.*steps\.maven-build\.outputs\.artifact-suffix' \
  "build job exposes the maven-job step artifact-suffix output"

# ========================================================== 4. Release phase ==
echo "== 4. release phase: reuse instead of rebuild =="
has "$RELEASE_PHASE" '^ {6}reuse-build-artifacts:' "release phase declares reuse-build-artifacts"

default_reuse="$(input_default "$RELEASE_PHASE" reuse-build-artifacts 6)"
if [[ "$default_reuse" == "false" ]]; then
  ok "reuse-build-artifacts defaults to false (legacy callers keep rebuilding)"
else
  bad "reuse-build-artifacts defaults to false (legacy callers keep rebuilding)" \
      "got '$default_reuse'"
fi

# The restore must target the selected run and fail closed on a digest mismatch.
block_has "$RELEASE_PHASE" "Restore Maven Repository" 'run-id:' \
  "restore maven repo pins the source run id"
block_has "$RELEASE_PHASE" "Restore Maven Repository" 'github-token:' \
  "restore maven repo passes a token (cross-run download)"
block_has "$RELEASE_PHASE" "Restore Maven Repository" 'digest-mismatch: error' \
  "restore maven repo fails closed on digest mismatch"

# The second reactor build must be conditional on NOT reusing. Assert the
# polarity too: `if: ... && inputs.reuse-build-artifacts` (wrong sign) would
# still "reference" the flag and pass a presence-only check.
block_has_if "$RELEASE_PHASE" "Install Release Artifacts" '!inputs\.reuse-build-artifacts' \
  "Install Release Artifacts is skipped when reuse-build-artifacts is on"

# In reuse mode the restored repository IS the publication source, so it has to
# start empty: a warm runner must not be able to contribute artifacts to a
# release. This is what makes "we published what the build produced" true.
block_has_if "$RELEASE_PHASE" "Prepare Maven Repository" 'reuse-build-artifacts == true' \
  "clearing the Maven repository is gated on reuse-build-artifacts"
block_has "$RELEASE_PHASE" "Prepare Maven Repository" 'rm -rf.*\.m2/repository' \
  "reuse mode clears the local Maven repository before restoring"

# A missing/empty artifact set must fail before publishing, not publish nothing.
has "$RELEASE_PHASE" 'id: validate-release-artifacts' \
  "release phase validates the restored artifact set before publishing"
block_has_if "$RELEASE_PHASE" validate-release-artifacts 'deploy_artifact' \
  "validation is gated on deploy_artifact (only guards an actual publish)"

# Order is load-bearing. In legacy mode it is Install's -Dchangelist that CREATES
# the suffixed coordinates, so validating before Install rejects every
# Java-variant release. Nothing else here pins step order.
line_of() { grep -n -- "$1" "$RELEASE_PHASE" | head -n1 | cut -d: -f1; }
l_restore="$(line_of 'name: Restore Maven Repository')"
l_install="$(line_of 'name: Install Release Artifacts')"
l_validate="$(line_of 'name: Validate Release Artifacts')"
l_publish="$(line_of 'name: Publish Release Artifacts to S3')"
if [[ -n "$l_restore" && -n "$l_install" && -n "$l_validate" && -n "$l_publish" ]] \
   && (( l_restore < l_install && l_install < l_validate && l_validate < l_publish )); then
  ok "step order: restore < install < validate < publish"
else
  bad "step order: restore < install < validate < publish" \
      "restore=$l_restore install=$l_install validate=$l_validate publish=$l_publish"
fi

# ==================================================== 5. Deploy Docker cache ==
echo "== 5. deploy-docker: explicit, stable cache scope =="
has "$DEPLOY_DOCKER" '^  cache-scope:' "deploy-docker declares a cache-scope input"
# Assert the value is wired through, not merely that 'scope=' appears — a
# hard-coded scope would satisfy the weaker check.
has "$DEPLOY_DOCKER" 'cache-from: type=gha,scope=\$\{\{ inputs\.cache-scope \}\}' \
  "cache-from uses the cache-scope input"
has "$DEPLOY_DOCKER" 'cache-to: type=gha,scope=\$\{\{ inputs\.cache-scope \}\}' \
  "cache-to uses the cache-scope input"

if [[ "$(input_default "$DEPLOY_DOCKER" cache-scope)" == "buildkit" ]]; then
  ok "cache-scope defaults to BuildKit's implicit 'buildkit' scope"
else
  bad "cache-scope defaults to BuildKit's implicit 'buildkit' scope" \
      "got '$(input_default "$DEPLOY_DOCKER" cache-scope)'"
fi

# ================================================== 6. Deployment phase scope ==
echo "== 6. deployment phase: optional release cache isolation =="
has "$DEPLOY_PHASE" 'docker-cache-scope-prefix' \
  "deployment phase accepts a docker-cache-scope-prefix"
block_has "$DEPLOY_PHASE" "Build/Push Docker Image" 'cache-scope' \
  "main image build passes a cache scope"
block_has "$DEPLOY_PHASE" "Build/Push Docker Dev Image" 'cache-scope' \
  "dev image build passes a cache scope"

# ...and each image gets its OWN scope. Both reading outputs.main would re-create
# the very eviction problem the input exists to fix, yet still pass the checks
# above.
block_has "$DEPLOY_PHASE" "Build/Push Docker Image" 'outputs\.main' \
  "main image uses the main scope"
block_has "$DEPLOY_PHASE" "Build/Push Docker Dev Image" 'outputs\.dev' \
  "dev image uses the dev scope"
block_lacks "$DEPLOY_PHASE" "Build/Push Docker Dev Image" 'outputs\.main' \
  "dev image does not reuse the main scope"

# Behavioural: run the real scope-computing body.
extract_run_body "$DEPLOY_PHASE" docker-cache-scopes > "$WORKDIR/scopes.sh"
if [[ ! -s "$WORKDIR/scopes.sh" ]]; then
  bad "docker-cache-scopes body is extractable" "not found in $(rel "$DEPLOY_PHASE")"
else
  ok "docker-cache-scopes body is extractable"

  scope_case() {  # desc, prefix, variant, sdkman, expect-main
    local rc=0 main dev
    PREFIX="$2" VARIANT="$3" SDKMAN_JAVA="$4" \
      run_body "$WORKDIR/scopes.sh" "$WORKDIR/scopes.out" || rc=$?
    main="$(out_val "$WORKDIR/scopes.out" main)"
    dev="$(out_val "$WORKDIR/scopes.out" dev)"
    if [[ $rc -ne 0 ]]; then
      bad "$1" "body exited $rc"
    elif [[ "$main" != "$5" ]]; then
      bad "$1" "main scope '$main', expected '$5'"
    elif [[ "$main" == "$dev" ]]; then
      bad "$1" "main and dev scopes are identical ('$main')"
    else
      ok "$1"
    fi
  }

  # No prefix must be a genuine no-op: BuildKit's implicit scope on both images.
  PREFIX="" VARIANT="" SDKMAN_JAVA="25.0.4-1-ms" \
    run_body "$WORKDIR/scopes.sh" "$WORKDIR/scopes0.out"
  if [[ "$(out_val "$WORKDIR/scopes0.out" main)" == "buildkit" \
        && "$(out_val "$WORKDIR/scopes0.out" dev)" == "buildkit" ]]; then
    ok "empty prefix leaves both scopes at buildkit (no-op for existing callers)"
  else
    bad "empty prefix leaves both scopes at buildkit (no-op for existing callers)" \
        "main='$(out_val "$WORKDIR/scopes0.out" main)' dev='$(out_val "$WORKDIR/scopes0.out" dev)'"
  fi

  scope_case "release primary scope is stable and namespaced" \
    release '' '25.0.4-1-ms' 'release-main-primary-25.0.4-1-ms'
  scope_case "a java variant gets its own scope" \
    release 'java-25' '25.0.4-1-ms' 'release-main-java-25-25.0.4-1-ms'

  # The scope must not bake in anything per-run, or it would be cold every time.
  # Asserted on the step's env block: the body cannot use a value it is never
  # handed. (Grepping the body for 'github' would be wrong — it writes
  # $GITHUB_OUTPUT, which is not a scope input.)
  block_lacks "$DEPLOY_PHASE" docker-cache-scopes \
    'github\.run_id|github\.sha|github\.run_number|release_version' \
    "the scope key excludes per-run identifiers"
fi

# The scope step has to run BEFORE the builds that read its outputs, and it needs
# the SDKMAN version for the scope key. Placing it after the builds (which is what
# a careless insert does) silently passes empty scopes and looks like it works.
dpl_line() { grep -n -- "$1" "$DEPLOY_PHASE" | head -n1 | cut -d: -f1; }
l_sdk="$(dpl_line 'id: get-sdkman-version')"
l_scope="$(dpl_line 'id: docker-cache-scopes')"
l_main="$(dpl_line 'id: docker_build$')"
l_dev="$(dpl_line 'id: docker_build_dev')"
if [[ -n "$l_sdk" && -n "$l_scope" && -n "$l_main" && -n "$l_dev" ]] \
   && (( l_sdk < l_scope && l_scope < l_main && l_scope < l_dev )); then
  ok "cache scopes are computed before the builds that consume them"
else
  bad "cache scopes are computed before the builds that consume them" \
      "sdkman=$l_sdk scopes=$l_scope main=$l_main dev=$l_dev"
fi

# ==================================================== 7. Release workflows opt ==
echo "== 7. release workflows: suffix single-source and opt-in =="
for wf in "$RELEASE_WF" "$VARIANT_WF"; do
  name="$(basename "$wf")"
  has "$wf" 'generate-test-image: false'    "$name skips the unused test image"
  has "$wf" 'generate-build-classes: false' "$name skips the unused build classes"
  has "$wf" 'release-build: true'           "$name builds publication-ready coordinates on the first install"
  has "$wf" 'docker-cache-scope-prefix:'    "$name isolates its Docker cache scope"
  # The suffix must come from the build in BOTH consumers, not be re-derived
  # independently per job. A single occurrence would still pass a bare `has`
  # check, which is exactly the three-derivations divergence this fixes.
  n="$(grep -cE 'artifact-suffix:.*needs\.[a-z-]+\.outputs\.' "$wf")"
  if [[ "$n" == "2" ]]; then
    ok "$name takes the suffix from the build for both deployment and release"
  else
    bad "$name takes the suffix from the build for both deployment and release" \
        "found $n occurrence(s), expected 2"
  fi
done

# ================================================ 8. Suffix behaviour (real) ==
# The three jobs each derived the Java suffix themselves, and they disagreed:
# maven-job and the release phase derived "java25" while the deployment phase
# derived "java-25", so a derived-suffix run looked for a maven-repo artifact
# under a name the build never published.
#
# "java-25" is canonical — RELEASE_JAVA_VARIANT_SUFFIX is set to exactly that in
# the repository variables, and it is what the deployment phase, deploy-docker
# and the published 25.x variant coordinates already use. The other two were
# brought into line rather than the reverse, because changing this changes
# published Maven coordinates and Docker tags.
#
# Note the suffix is DERIVED as "java-<major>"; an explicitly supplied suffix is
# passed through verbatim whatever its shape.
echo "== 8. suffix resolution is identical across build / deploy / release =="

extract_run_body "$MAVEN_JOB"    artifact-suffix       > "$WORKDIR/mj_suffix.sh"
extract_run_body "$DEPLOY_PHASE" get-sdkman-version   > "$WORKDIR/dp_suffix.sh"
extract_run_body "$RELEASE_PHASE" java-suffix         > "$WORKDIR/rp_suffix.sh"

# .sdkmanrc is the project's single source of truth for the Java version. There
# is no pinned literal and no bundled copy here: every expectation below is read
# from that file, so bumping the Java version is a one-line change to .sdkmanrc
# and the pipeline — and these tests — follow it.
SDKMAN_JAVA="$(awk -F= '/^java=/ {print $2}' "$REPO_ROOT/.sdkmanrc")"
SDKMAN_MAJOR="$(printf '%s' "$SDKMAN_JAVA" | grep -oE '^[0-9]+')"
if [[ -n "$SDKMAN_JAVA" && -n "$SDKMAN_MAJOR" ]]; then
  ok ".sdkmanrc is the version source of truth ($SDKMAN_JAVA -> java$SDKMAN_MAJOR)"
else
  bad ".sdkmanrc is the version source of truth" "no java= major in $(rel "$REPO_ROOT/.sdkmanrc")"
fi

# desc, java-version input, artifact-suffix input, expected suffix
suffix_case() {
  local desc="$1" java_version="$2" artifact_suffix="$3" want="$4"
  export IN_JAVA_VERSION="$java_version" IN_ARTIFACT_SUFFIX="$artifact_suffix"

  local mj rp dp rc=0
  run_body "$WORKDIR/mj_suffix.sh" "$WORKDIR/mj.out" || rc=$?
  mj="$(out_val "$WORKDIR/mj.out" suffix)"
  run_body "$WORKDIR/rp_suffix.sh" "$WORKDIR/rp.out" || rc=$?
  rp="$(out_val "$WORKDIR/rp.out" suffix)"
  run_body "$WORKDIR/dp_suffix.sh" "$WORKDIR/dp.out" || rc=$?
  dp="$(out_val "$WORKDIR/dp.out" maven_suffix)"

  if [[ $rc -ne 0 ]]; then
    bad "$desc" "an extracted body exited $rc — the script is broken, not just the value"
  elif [[ "$mj" == "$want" && "$rp" == "$want" && "$dp" == "$want" ]]; then
    ok "$desc -> '$want'"
  elif [[ "$mj" == "$rp" && "$rp" == "$dp" ]]; then
    bad "$desc" "all agree on '$mj' but expected '$want'"
  else
    bad "$desc" "build='$mj' release='$rp' deploy='$dp' (must all be '$want')"
  fi
}

suffix_case "derived from .sdkmanrc"                "$SDKMAN_JAVA" "" "-java-$SDKMAN_MAJOR"
suffix_case "explicit suffix wins"                  "$SDKMAN_JAVA" "java-${SDKMAN_MAJOR}-ms" "-java-${SDKMAN_MAJOR}-ms"
suffix_case "leading separator normalised"          "$SDKMAN_JAVA" "-java-$SDKMAN_MAJOR" "-java-$SDKMAN_MAJOR"
suffix_case "no override (baseline, no suffix)"     ""            "" ""

# An explicitly supplied suffix is passed through verbatim. That holds even for a
# legacy shape: 'java25' without the dash is no longer used, and quietly
# normalising it would repoint the artifact lookup for anyone still passing it.
suffix_case "a legacy explicit suffix is not rewritten" "$SDKMAN_JAVA" "java${SDKMAN_MAJOR}" "-java${SDKMAN_MAJOR}"

# artifact-suffix is honoured with NO java-version override. maven-job always
# worked this way, but the deployment and release phases only looked at
# artifact-suffix inside their java-version branch, so a caller supplying only a
# suffix had the build publish maven-repo-<suffix> while they looked for an
# unsuffixed artifact. All three now resolve it the same way.
suffix_case "explicit suffix with no java-version override" "" "java-${SDKMAN_MAJOR}" "-java-${SDKMAN_MAJOR}"

# Deliberately not the project's version: the next major up, to prove a
# non-baseline version is handled by the same rule rather than special-cased.
HYPOTHETICAL_MAJOR=$((SDKMAN_MAJOR + 1))
suffix_case "a non-baseline major is honoured"      "${HYPOTHETICAL_MAJOR}.0.1-tem" "" "-java-${HYPOTHETICAL_MAJOR}"

# The SDKMAN id becomes the dotcms/java-base tag and, per plan §6, part of the
# Docker cache-scope key, so '+' must collapse to '-'.
export IN_JAVA_VERSION="$SDKMAN_JAVA" IN_ARTIFACT_SUFFIX=""
run_body "$WORKDIR/dp_suffix.sh" "$WORKDIR/sdk.out"
EXPECTED_SDK="${SDKMAN_JAVA//+/-}"
if [[ "$(out_val "$WORKDIR/sdk.out" SDKMAN_JAVA_VERSION)" == "$EXPECTED_SDK" ]]; then
  ok "SDKMAN id normalises '+' for the docker tag ($SDKMAN_JAVA -> $EXPECTED_SDK)"
else
  bad "SDKMAN id normalises '+' for the docker tag ($SDKMAN_JAVA -> $EXPECTED_SDK)" \
      "got '$(out_val "$WORKDIR/sdk.out" SDKMAN_JAVA_VERSION)'"
fi

# With no override, the version must come from .sdkmanrc, not a built-in default.
export IN_JAVA_VERSION="" IN_ARTIFACT_SUFFIX=""
run_body "$WORKDIR/dp_suffix.sh" "$WORKDIR/sdk2.out"
if [[ "$(out_val "$WORKDIR/sdk2.out" SDKMAN_JAVA_VERSION)" == "$EXPECTED_SDK" ]]; then
  ok "no override resolves to .sdkmanrc ($SDKMAN_JAVA), not a built-in default"
else
  bad "no override resolves to .sdkmanrc ($SDKMAN_JAVA), not a built-in default" \
      "got '$(out_val "$WORKDIR/sdk2.out" SDKMAN_JAVA_VERSION)'"
fi

# ===================================== 9. Publication validation (contract) ==
# Contract for the step `validate-release-artifacts`:
#   env REQUIRED_COORDINATES — space-separated `groupId:artifactId` (the caller
#                              sets this; a default of com.dotcms:dotcms-core)
#   env VERSION              — release version including any suffix (e.g. 25.02.16-01-java-25)
#   env MAVEN_REPO           — repository root (default $HOME/.m2/repository)
#   exit non-zero when a coordinate's version directory is missing, or has no
#   .pom, or has neither a .jar nor a .war — dotcms-core packages a jar while
#   dotcms-core-web packages a war, and publish.sh uploads pom/jar/war/zip/aar/
#   module. `publish.sh maven` publishes whatever it finds and exits 0 on an
#   empty selection, so this is the only thing between a partial artifact set
#   and a release that publishes nothing.
echo "== 9. release publication validates artifacts before publishing =="

extract_run_body "$RELEASE_PHASE" validate-release-artifacts > "$WORKDIR/validate.sh"
if [[ ! -s "$WORKDIR/validate.sh" ]]; then
  bad "validate-release-artifacts step exists" "step not found in $(rel "$RELEASE_PHASE")"
else
  ok "validate-release-artifacts step exists"

  export VERSION="25.02.16-01"
  export REQUIRED_COORDINATES="com.dotcms:dotcms-core"
  repo="$WORKDIR/repo"
  coord_dir="$repo/com/dotcms/dotcms-core/$VERSION"

  # Fixture builders. Functions, not command strings: `"$snippet"` would treat
  # the whole string as one command name and silently create nothing, which is
  # exactly the kind of vacuous fixture that makes a validator test meaningless.
  seed_none()    { :; }
  seed_empty()   { mkdir -p "$coord_dir"; }
  seed_pom()     { seed_empty; : > "$coord_dir/dotcms-core-$VERSION.pom"; }
  seed_pom_jar() { seed_pom; : > "$coord_dir/dotcms-core-$VERSION.jar"; }
  seed_pom_war() { seed_pom; : > "$coord_dir/dotcms-core-$VERSION.war"; }

  # Each case starts from an empty repo so a previous case's files cannot make
  # the next one pass.
  validation_case() {  # desc, expect_rc, seed-fn
    local desc="$1" expect_rc="$2" seed="${3:-seed_none}" rc=0
    rm -rf "$repo"; mkdir -p "$repo"
    "$seed"
    MAVEN_REPO="$repo" run_body "$WORKDIR/validate.sh" "$WORKDIR/v.out" || rc=$?
    if [[ "$expect_rc" == "nonzero" && "$rc" -ne 0 ]] || [[ "$expect_rc" == "zero" && "$rc" -eq 0 ]]; then
      ok "$desc"
    else
      bad "$desc" "expected exit $expect_rc, got $rc"
    fi
  }

  validation_case "missing version directory fails" nonzero seed_none
  validation_case "empty version directory fails"   nonzero seed_empty
  validation_case "pom with no archive fails"       nonzero seed_pom
  validation_case "pom + jar passes (dotcms-core)"  zero    seed_pom_jar
  validation_case "pom + war passes (dotcms-core-web)" zero seed_pom_war

  # A coordinate that is absent entirely must fail even when another is fine — a
  # release that ships dotcms-core without dotcms-core-web is not a release.
  rm -rf "$repo"; mkdir -p "$repo"
  seed_pom_jar
  if REQUIRED_COORDINATES="com.dotcms:dotcms-core com.dotcms:dotcms-core-web" \
     MAVEN_REPO="$repo" run_body "$WORKDIR/validate.sh" "$WORKDIR/v2.out"; then
    bad "a partially complete artifact set fails" "exited 0 with dotcms-core-web missing"
  else
    ok "a partially complete artifact set fails"
  fi

  # Comma-separated must be honoured in full. With only the FIRST element
  # missing, a script that silently kept just the last token would pass.
  rm -rf "$repo"; mkdir -p "$repo"
  seed_pom_jar
  if REQUIRED_COORDINATES="com.dotcms:dotcms-nope,com.dotcms:dotcms-core" \
     MAVEN_REPO="$repo" run_body "$WORKDIR/validate.sh" "$WORKDIR/v3.out"; then
    bad "comma-separated coordinates are all checked" "exited 0 with dotcms-nope missing"
  else
    ok "comma-separated coordinates are all checked"
  fi

  # A -sources.jar must not stand in for the primary archive.
  rm -rf "$repo"; mkdir -p "$repo"; seed_pom
  : > "$coord_dir/dotcms-core-$VERSION-sources.jar"
  if MAVEN_REPO="$repo" run_body "$WORKDIR/validate.sh" "$WORKDIR/v4.out"; then
    bad "a -sources.jar does not satisfy the primary archive" "exited 0"
  else
    ok "a -sources.jar does not satisfy the primary archive"
  fi

  # The built-in default requires both modules, so an unset variable cannot
  # weaken the guard.
  rm -rf "$repo"; mkdir -p "$repo"; seed_pom_jar
  if ( unset REQUIRED_COORDINATES
       MAVEN_REPO="$repo" run_body "$WORKDIR/validate.sh" "$WORKDIR/v5.out" ); then
    bad "the built-in default requires dotcms-core and dotcms-core-web" "exited 0"
  else
    ok "the built-in default requires dotcms-core and dotcms-core-web"
  fi

  # An empty VERSION must fail rather than look for an empty-string path.
  rm -rf "$repo"; mkdir -p "$repo"; seed_pom_jar
  if VERSION="" MAVEN_REPO="$repo" run_body "$WORKDIR/validate.sh" "$WORKDIR/v6.out"; then
    bad "an empty VERSION fails" "exited 0"
  else
    ok "an empty VERSION fails"
  fi
fi

# ================================ 10. release-build Maven args (behavioural) ==
# Step 3 of the plan lives entirely in the argument construction of
# `run-maven-build`: when release-build is on, the initial install must apply the
# same -Dchangelist the release phase will publish under, because the second
# reactor install that used to create it is being removed. Verified against
# Maven 3.9.2 / Java 25.0.4.1 that a CLI -Dchangelist overrides the
# -Dchangelist= that release-prepare writes into .mvn/maven.config.
echo "== 10. release-build applies -Dchangelist to the initial install =="

# Wiring: the changelist must come from the dash-prefixed `suffix`, not the
# separator-free `raw_suffix`. The behavioural cases below inject IN_SUFFIX
# directly, so without this they would still pass if the action switched.
has "$MAVEN_JOB" 'DEFAULT_ARGS.*-Dchangelist=.*steps\.artifact-suffix\.outputs\.suffix' \
  "changelist is built from the resolved Maven suffix (dash-prefixed)"

extract_run_body "$MAVEN_JOB" run-maven-build | sed -E \
  -e 's/\$\{\{ *inputs\.maven-args *\}\}/$IN_MAVEN_ARGS/g' \
  -e 's/\$\{\{ *inputs\.generate-docker *\}\}/$IN_GENERATE_DOCKER/g' \
  -e 's/\$\{\{ *inputs\.native *\}\}/$IN_NATIVE/g' \
  -e 's/\$\{\{ *inputs\.release-build *\}\}/$IN_RELEASE_BUILD/g' \
  -e 's/\$\{\{ *inputs\.maven-compiler-release *\}\}/$IN_COMPILER_RELEASE/g' \
  -e 's/\$\{\{ *inputs\.java-version *\}\}/$IN_JAVA_VERSION/g' \
  -e 's/\$\{\{ *steps\.artifact-suffix\.outputs\.suffix *\}\}/$IN_SUFFIX/g' \
  -e 's/\$\{\{ *runner\.os *\}\}/$RUNNER_OS/g' \
  > "$WORKDIR/mvnbody.sh"

# Stub ./mvnw to print its arguments, one per line, so the assembled FINAL_ARGS
# can be asserted on without invoking Maven.
mkdir -p "$WORKDIR/mvnrun/.mvn"
printf '#!/usr/bin/env bash\nprintf "%%s\\n" "$@"\n' > "$WORKDIR/mvnrun/mvnw"
chmod +x "$WORKDIR/mvnrun/mvnw"
# The guardrail under test requires -Drevision= here, exactly as release-prepare
# writes it on a release branch.
printf -- '-Dprod=true\n-Drevision=25.02.16-01\n-Dchangelist=\n' > "$WORKDIR/mvnrun/.mvn/maven.config"

mvn_args_run() {  # release-build, suffix
  ( cd "$WORKDIR/mvnrun" \
    && IN_MAVEN_ARGS="clean install -Dprod=true -DskipTests=true" \
       IN_GENERATE_DOCKER="true" IN_NATIVE="false" \
       IN_RELEASE_BUILD="$1" IN_COMPILER_RELEASE="" \
       IN_JAVA_VERSION="" IN_SUFFIX="$2" RUNNER_OS="Linux" \
       bash "$WORKDIR/mvnbody.sh" ) 2>/dev/null
}

mvn_args_case() {  # desc, release-build, suffix, expected-arg
  local out rc=0
  out="$(mvn_args_run "$2" "$3")" || rc=$?
  if [[ $rc -ne 0 ]]; then
    bad "$1" "extracted body exited $rc"
  elif grep -qxF -- "$4" <<<"$out"; then
    ok "$1"
  else
    bad "$1" "expected arg '$4'; got: $(tr '\n' ' ' <<<"$out")"
  fi
}

mvn_args_case "release primary passes an empty changelist" true "" "-Dchangelist="
mvn_args_case "release variant passes the java changelist" \
  true "-java-${SDKMAN_MAJOR}" "-Dchangelist=-java-${SDKMAN_MAJOR}"

# Non-release builds keep .mvn/maven.config in charge; adding a changelist here
# would rewrite their version to a release coordinate. Assert the positive marker
# first: an empty output also contains no -Dchangelist.
out="$(mvn_args_run false "-java${SDKMAN_MAJOR}")"
if ! grep -qxF -- "install" <<<"$out"; then
  bad "non-release build does not set a changelist" "the body never reached mvnw"
elif grep -q -- "-Dchangelist" <<<"$out"; then
  bad "non-release build does not set a changelist" "found -Dchangelist in the args"
else
  ok "non-release build does not set a changelist"
fi

# Guardrail: without -Drevision the version collapses to the parent default
# (1.0.0), which is not a SNAPSHOT so it escapes delete-built-artifacts-from-cache
# and would be saved into the shared Maven cache as a bogus release coordinate.
#
# These assert on the guardrail MESSAGE, not merely a non-zero exit: a syntax
# error from an un-substituted ${{ }} also exits non-zero, and a bare rc check
# would report that as a pass.
mvn_args_stderr() {  # release-build, suffix -> stderr only
  ( cd "$WORKDIR/mvnrun" \
    && IN_MAVEN_ARGS="clean install -Dprod=true -DskipTests=true" \
       IN_GENERATE_DOCKER="true" IN_NATIVE="false" \
       IN_RELEASE_BUILD="$1" IN_COMPILER_RELEASE="" \
       IN_JAVA_VERSION="" IN_SUFFIX="$2" RUNNER_OS="Linux" \
       bash "$WORKDIR/mvnbody.sh" ) 2>&1 >/dev/null
}

mv "$WORKDIR/mvnrun/.mvn/maven.config" "$WORKDIR/mvnrun/.mvn/maven.config.bak"
err="$(mvn_args_stderr true "-java-${SDKMAN_MAJOR}")"; rc=$?
if [[ $rc -eq 0 ]]; then
  bad "release-build fails closed when .mvn/maven.config is absent" "exited 0"
elif ! grep -q 'requires a non-empty -Drevision=' <<<"$err"; then
  bad "release-build fails closed when .mvn/maven.config is absent" \
      "exited $rc but not via the guardrail: $(tr '\n' ' ' <<<"$err" | head -c 200)"
else
  ok "release-build fails closed when .mvn/maven.config is absent"
fi

# Present but without -Drevision= must trip the same guardrail.
printf -- '-Dprod=true\n-Dchangelist=\n' > "$WORKDIR/mvnrun/.mvn/maven.config"
err="$(mvn_args_stderr true "-java-${SDKMAN_MAJOR}")"; rc=$?
if [[ $rc -ne 0 ]] && grep -q 'requires a non-empty -Drevision=' <<<"$err"; then
  ok "release-build fails closed when .mvn/maven.config lacks -Drevision"
else
  bad "release-build fails closed when .mvn/maven.config lacks -Drevision" \
      "rc=$rc err=$(tr '\n' ' ' <<<"$err" | head -c 200)"
fi

# A BARE '-Drevision=' sets the CI-friendly revision to the empty string, which
# collapses the version just as surely as omitting it. Matching only the prefix
# would pass this guard on exactly the case it exists for.
printf -- '-Dprod=true\n-Drevision=\n-Dchangelist=\n' > "$WORKDIR/mvnrun/.mvn/maven.config"
err="$(mvn_args_stderr true "-java-${SDKMAN_MAJOR}")"; rc=$?
if [[ $rc -ne 0 ]] && grep -q 'requires a non-empty -Drevision=' <<<"$err"; then
  ok "release-build fails closed when -Drevision= is empty"
else
  bad "release-build fails closed when -Drevision= is empty" \
      "rc=$rc err=$(tr '\n' ' ' <<<"$err" | head -c 200)"
fi

# ...and the positive case still works, so the tighter match is not simply
# rejecting everything.
printf -- '-Dprod=true\n-Drevision=25.02.16-01\n-Dchangelist=\n' > "$WORKDIR/mvnrun/.mvn/maven.config"
if mvn_args_run true "-java-${SDKMAN_MAJOR}" | grep -qxF -- "-Dchangelist=-java-${SDKMAN_MAJOR}"; then
  ok "a non-empty -Drevision= still passes the guard and sets the changelist"
else
  bad "a non-empty -Drevision= still passes the guard and sets the changelist" \
      "guard rejected a valid maven.config"
fi

mv "$WORKDIR/mvnrun/.mvn/maven.config.bak" "$WORKDIR/mvnrun/.mvn/maven.config"

# ================================ 11. release opt-in hardening (Part B) ==
echo "== 11. the opt-in has an escape hatch and an early tripwire =="

# Hardcoding reuse-build-artifacts: true in cicd_7 left no way back to
# rebuild-then-publish without editing the workflow — needed when releasing a tag
# cut before release-build existed, where the tag's own maven-job installs
# unsuffixed coordinates and validation would block the publish.
#
# Asserted on the whole file, not via step_block: `release` here is a JOB, not a
# step, so there is no step block to match.
default_reuse_input="$(input_default "$VARIANT_WF" reuse-build-artifacts 6)"
if [[ "$default_reuse_input" == "true" ]]; then
  ok "cicd_7 exposes reuse-build-artifacts as a dispatch input defaulting to true"
else
  bad "cicd_7 exposes reuse-build-artifacts as a dispatch input defaulting to true" \
      "got '$default_reuse_input'"
fi
has "$VARIANT_WF" 'reuse-build-artifacts: \$\{\{ inputs\.reuse-build-artifacts \}\}' \
  "cicd_7 release job honours the input rather than hardcoding true"
has "$RELEASE_WF" 'reuse-build-artifacts: true' \
  "cicd_6 sets reuse-build-artifacts (it creates its own tag from main, so no escape hatch needed)"

# The tripwire belongs in the build job: validation in the release phase runs
# AFTER deployment has pushed images, so on its own it cannot prevent a bad
# release from shipping images.
block_has_if "$BUILD_PHASE" "Verify release coordinates were installed" 'release-build == true' \
  "the coordinate tripwire is in the build job, gated on release-build"
block_has "$BUILD_PHASE" "Verify release coordinates were installed" 'dotcms-core-\$version\.jar' \
  "the tripwire asserts the exact expected primary artifact"
# Trunk reuses the merge queue's maven-repo for the CLI native build and deploy, so only
# pull requests may leave modules out of the build.
block_has "$BUILD_PHASE" maven-build "github.event_name == 'pull_request' && '-pl !:" \
  "only pull request builds exclude modules from the reactor"
INIT_PHASE="$REPO_ROOT/.github/workflows/cicd_comp_initialize-phase.yml"
block_has "$INIT_PHASE" data-download 'workflow: cicd_2-merge-queue\.yml' \
  "trunk and nightly reuse only merge-queue builds, never a slimmed PR build"
block_has "$INIT_PHASE" data-download 'workflow_search: false' \
  "the previous-build search does not fall back to every workflow"
block_has "$INIT_PHASE" data-download-trunk 'workflow: cicd_3-trunk\.yml' \
  "a commit that bypassed the queue can reuse trunk's own complete build"
block_has "$INIT_PHASE" data-download-trunk 'workflow_search: false' \
  "the trunk fallback does not search every workflow"
block_has_if "$INIT_PHASE" data-download-trunk "data-download.outputs.found_artifact != 'true'" \
  "the trunk fallback runs only when no merge-queue build was found"
for wf in cicd_1-pr cicd_4-nightly; do
  if grep -Eq "workflow: ${wf}\.yml" "$INIT_PHASE"; then
    bad "previous-build reuse never searches ${wf}.yml" "found 'workflow: ${wf}.yml' in $(rel "$INIT_PHASE")"
  else
    ok "previous-build reuse never searches ${wf}.yml"
  fi
done

# ================================================= 12. SAML ships with core ==
# Issue #38104. SAML used to be released separately, so a core release could
# ship an older bundle than the one under review. It is now built from source
# under the core version like Tika: a reactor module inheriting the CI-friendly
# version, a system-bundles dependency on that same version, the existing
# publisher/credentials, a CI JVM suite, and the two real fail-closed gates
# (build-coordinate tripwire and release validation) extended to require it.
#
# A missing module is reported as an assertion failure rather than aborting the suite.
echo "== 12. SAML plugin is built, bundled and validated with the release =="

SAML_POM="$REPO_ROOT/independent-projects/core-plugins/saml-plugin/pom.xml"
CORE_PLUGINS_POM="$REPO_ROOT/independent-projects/core-plugins/pom.xml"
SYSTEM_BUNDLES_POM="$REPO_ROOT/osgi-base/system-bundles/pom.xml"
TEST_MATRIX="$REPO_ROOT/.github/test-matrix.yml"
TEST_PHASE="$REPO_ROOT/.github/workflows/cicd_comp_test-phase.yml"
SAML_COORD_RE='com\.dotcms\.samlbundle:com\.dotcms\.samlbundle'
SAML_RELEASE_VERSION="25.02.16-01"

# 12a. The module and its reactor registration.
if [[ -f "$SAML_POM" ]]; then
  ok "SAML plugin module pom exists ($(rel "$SAML_POM"))"
  has "$SAML_POM" '<groupId>com\.dotcms\.samlbundle</groupId>' \
    "SAML module keeps the com.dotcms.samlbundle groupId"
  has "$SAML_POM" '<artifactId>com\.dotcms\.samlbundle</artifactId>' \
    "SAML module keeps the com.dotcms.samlbundle artifactId"
  has "$SAML_POM" '\$\{revision\}\$\{sha1\}\$\{changelist\}' \
    "SAML module inherits the CI-friendly project version"
  has "$SAML_POM" '<packaging>bundle</packaging>' \
    "SAML is still packaged as an OSGi bundle"
  has "$SAML_POM" '<Bundle-SymbolicName>\$\{project.artifactId\}</Bundle-SymbolicName>' \
    "SAML preserves its OSGi symbolic name"
  has "$SAML_POM" '<Bundle-Activator>com\.dotcms\.saml\.osgi\.Activator</Bundle-Activator>' \
    "SAML preserves its existing activator"
else
  bad "SAML plugin module pom exists" "missing $(rel "$SAML_POM")"
fi

has "$CORE_PLUGINS_POM" '<module>saml-plugin</module>' \
  "core-plugins reactor registers the saml-plugin module"

# 12b. system-bundles must depend on the core version, not a pinned SAML release.
saml_bundle_version() {
  awk '
    /<artifactId>com\.dotcms\.samlbundle<\/artifactId>/ { found = 1 }
    found && /<version>/ { sub(/.*<version>/, ""); sub(/<\/version>.*/, ""); print; exit }
  ' "$SYSTEM_BUNDLES_POM"
}
if [[ "$(saml_bundle_version)" == '${project.version}' ]]; then
  ok "system-bundles references SAML at \${project.version}"
else
  bad "system-bundles references SAML at \${project.version}" \
      "got '$(saml_bundle_version)', expected the core version"
fi

# 12c. SAML rides the existing publisher and its existing credentials.
block_has "$RELEASE_PHASE" "Publish Release Artifacts to S3" 'MAVEN_BUNNY_RW_USERNAME' \
  "release publish keeps the existing Bunny username secret"
block_has "$RELEASE_PHASE" "Publish Release Artifacts to S3" 'MAVEN_BUNNY_RW_PASSWORD' \
  "release publish keeps the existing Bunny password secret"
block_has "$RELEASE_PHASE" "Publish Release Artifacts to S3" 'publish-to-s3/publish\.sh maven' \
  "release publish uses the same generic publisher for SAML"
lacks "$RELEASE_PHASE" 'secrets\.[A-Za-z_]*[Ss][Aa][Mm][Ll]' \
  "release phase needs no SAML-specific secret"

# 12d. CI must actually run the imported SAML tests, and not let the Maven build
# cache satisfy the suite without executing it (a cached module reports no tests).
jvm_section="$(awk '
  /^  [a-zA-Z0-9_-]+:[[:space:]]*$/ {
    if (started) exit
    if ($0 ~ /^  jvm:[[:space:]]*$/) started = 1
  }
  started { print }
' "$TEST_MATRIX")"
saml_suite_line="$(grep -E -- 'maven_args:.*:com\.dotcms\.samlbundle' <<<"$jvm_section" | head -n1)"
if [[ -n "$saml_suite_line" ]]; then
  ok "CI runs the SAML plugin tests in the JVM suite"
  if grep -q 'maven\.build\.cache\.enabled=false' <<<"$saml_suite_line"; then
    ok "the SAML suite disables the Maven build cache"
  else
    bad "the SAML suite disables the Maven build cache" "maven_args: $saml_suite_line"
  fi
else
  bad "CI runs the SAML plugin tests in the JVM suite" \
      "no maven_args with :com.dotcms.samlbundle in the jvm section of $(rel "$TEST_MATRIX")"
  bad "the SAML suite disables the Maven build cache" "suite absent"
fi

# 12e. Build tripwire (real shell): with core installed but SAML missing, wrong
# versioned, or only a -sources.jar, the build must fail before images ship.
extract_run_body "$BUILD_PHASE" "Verify release coordinates were installed" > "$WORKDIR/saml-tripwire.sh"
trip_root="$WORKDIR/saml-trip"
trip_home="$WORKDIR/saml-trip-home"
mkdir -p "$trip_root/.mvn" "$trip_home/.m2/repository" "$trip_root/independent-projects/core-plugins/saml-plugin"
printf -- '-Dprod=true\n-Drevision=%s\n-Dchangelist=\n' "$SAML_RELEASE_VERSION" > "$trip_root/.mvn/maven.config"

tripwire_seed() {  # kind, version
  local base="$trip_home/.m2/repository" kind="$1" v="$2" d
  # core is installed and correct in every case: the point is that a missing or
  # bad SAML must still fail even though core alone satisfied the old check.
  mkdir -p "$base/com/dotcms/dotcms-core/$v"
  : > "$base/com/dotcms/dotcms-core/$v/dotcms-core-$v.pom"
  : > "$base/com/dotcms/dotcms-core/$v/dotcms-core-$v.jar"
  d="$base/com/dotcms/samlbundle/com.dotcms.samlbundle/$v"
  case "$kind" in
    missing) ;;
    wrong-version)
      d="$base/com/dotcms/samlbundle/com.dotcms.samlbundle/26.08.21"
      mkdir -p "$d"
      : > "$d/com.dotcms.samlbundle-26.08.21.pom"
      : > "$d/com.dotcms.samlbundle-26.08.21.jar" ;;
    sources-only)
      mkdir -p "$d"
      : > "$d/com.dotcms.samlbundle-$v.pom"
      : > "$d/com.dotcms.samlbundle-$v-sources.jar" ;;
    correct)
      mkdir -p "$d"
      : > "$d/com.dotcms.samlbundle-$v.pom"
      : > "$d/com.dotcms.samlbundle-$v.jar" ;;
  esac
}

tripwire_case() {  # desc, raw suffix ("java-25" or empty), kind, expect
  local desc="$1" raw="$2" kind="$3" expect="$4" rc=0
  local v="$SAML_RELEASE_VERSION"
  [[ -n "$raw" ]] && v="$v-$raw"
  rm -rf "$trip_home/.m2/repository"; mkdir -p "$trip_home/.m2/repository"
  if [[ "$kind" == legacy ]]; then
    rm -f "$trip_root/independent-projects/core-plugins/saml-plugin/pom.xml"
    tripwire_seed missing "$v"
  else
    : > "$trip_root/independent-projects/core-plugins/saml-plugin/pom.xml"
  fi
  if [[ "$kind" == primary-only ]]; then
    tripwire_seed correct "$SAML_RELEASE_VERSION"
    tripwire_seed missing "$v"
  elif [[ "$kind" != legacy ]]; then
    tripwire_seed "$kind" "$v"
  fi
  SUFFIX="$raw" HOME="$trip_home" \
    run_body "$WORKDIR/saml-tripwire.sh" "$WORKDIR/saml-trip.out" "$trip_root" || rc=$?
  if [[ "$expect" == nonzero && $rc -ne 0 ]] || [[ "$expect" == zero && $rc -eq 0 ]]; then
    ok "$desc"
  else
    bad "$desc" "expected exit $expect, got $rc"
  fi
}

if [[ -s "$WORKDIR/saml-tripwire.sh" ]]; then
  ok "build tripwire body is extractable"
  tripwire_case "tripwire rejects missing SAML although core is installed" "" missing nonzero
  tripwire_case "tripwire rejects wrong-version SAML" "" wrong-version nonzero
  tripwire_case "tripwire rejects sources-only SAML" "" sources-only nonzero
  tripwire_case "tripwire accepts a correct SAML build (primary)" "" correct zero
  tripwire_case "tripwire rejects missing SAML (java-25)" "java-25" missing nonzero
  tripwire_case "tripwire rejects an unsuffixed SAML build (java-25)" "java-25" primary-only nonzero
  tripwire_case "tripwire rejects sources-only SAML (java-25)" "java-25" sources-only nonzero
  tripwire_case "tripwire accepts a correct SAML build (java-25)" "java-25" correct zero
  tripwire_case "tripwire preserves historical tags without the SAML module" "" legacy zero
  tripwire_case "tripwire preserves historical Java-variant tags" "java-25" legacy zero
else
  bad "build tripwire body is extractable" "step not found in $(rel "$BUILD_PHASE")"
fi

# 12f. Release validation (real shell): the built-in default must fail when
# core+web are present but SAML is missing, wrong-versioned or sources-only, and
# pass only for a complete set.
block_has "$RELEASE_PHASE" validate-release-artifacts "$SAML_COORD_RE" \
  "release validation requires the SAML coordinate"
extract_run_body "$RELEASE_PHASE" validate-release-artifacts > "$WORKDIR/saml-validate.sh"
# Exercise the actual step env as well as its fallback, without repository variables.
saml_required_coordinates="$(step_block "$RELEASE_PHASE" validate-release-artifacts | awk '
  /^[[:space:]]+REQUIRED_COORDINATES:/ {
    sub(/^[[:space:]]+REQUIRED_COORDINATES:[[:space:]]*/, "")
    sub(/\$\{\{[[:space:]]*vars\.RELEASE_REQUIRED_COORDINATES[[:space:]]*\}\}/, "")
    print; exit
  }
')"
saml_repo="$WORKDIR/saml-validate-repo"

saml_validation_seed() {  # kind, version
  local base="$saml_repo" kind="$1" v="$2" d
  mkdir -p "$base/com/dotcms/dotcms-core/$v" "$base/com/dotcms/dotcms-core-web/$v"
  : > "$base/com/dotcms/dotcms-core/$v/dotcms-core-$v.pom"
  : > "$base/com/dotcms/dotcms-core/$v/dotcms-core-$v.jar"
  : > "$base/com/dotcms/dotcms-core-web/$v/dotcms-core-web-$v.pom"
  : > "$base/com/dotcms/dotcms-core-web/$v/dotcms-core-web-$v.war"
  d="$base/com/dotcms/samlbundle/com.dotcms.samlbundle/$v"
  case "$kind" in
    missing) ;;
    wrong-version)
      d="$base/com/dotcms/samlbundle/com.dotcms.samlbundle/26.08.21"
      mkdir -p "$d"
      : > "$d/com.dotcms.samlbundle-26.08.21.pom"
      : > "$d/com.dotcms.samlbundle-26.08.21.jar" ;;
    sources-only)
      mkdir -p "$d"
      : > "$d/com.dotcms.samlbundle-$v.pom"
      : > "$d/com.dotcms.samlbundle-$v-sources.jar" ;;
    correct)
      mkdir -p "$d"
      : > "$d/com.dotcms.samlbundle-$v.pom"
      : > "$d/com.dotcms.samlbundle-$v.jar" ;;
  esac
}

saml_validation_case() {  # desc, kind, expect, optional version, source checkout and coordinates source
  local desc="$1" kind="$2" expect="$3" v="${4:-$SAML_RELEASE_VERSION}" cwd="${5:-$REPO_ROOT}" rc=0
  local coordinates_source="${6:-default}"
  rm -rf "$saml_repo"; mkdir -p "$saml_repo"
  if [[ "$kind" == primary-only ]]; then
    saml_validation_seed correct "$SAML_RELEASE_VERSION"
    saml_validation_seed missing "$v"
  else
    saml_validation_seed "$kind" "$v"
  fi
  ( unset REQUIRED_COORDINATES
    if [[ "$coordinates_source" == workflow ]]; then
      export REQUIRED_COORDINATES="$saml_required_coordinates"
    fi
    VERSION="$v" MAVEN_REPO="$saml_repo" \
    run_body "$WORKDIR/saml-validate.sh" "$WORKDIR/saml-v.out" "$cwd" ) || rc=$?
  if [[ "$expect" == nonzero && $rc -ne 0 ]] || [[ "$expect" == zero && $rc -eq 0 ]]; then
    ok "$desc"
  else
    bad "$desc" "expected exit $expect, got $rc"
  fi
}

if [[ -s "$WORKDIR/saml-validate.sh" ]]; then
  ok "release validation body is extractable"
  if [[ -n "$saml_required_coordinates" ]]; then
    ok "release validation workflow coordinates are extractable"
  else
    bad "release validation workflow coordinates are extractable" "REQUIRED_COORDINATES env is missing"
  fi
  saml_validation_case "validation workflow env rejects core+web without SAML" missing nonzero "" "" workflow
  saml_validation_case "validation workflow env accepts a complete set" correct zero "" "" workflow
  saml_validation_case "validation default rejects core+web without SAML" missing nonzero
  saml_validation_case "validation default rejects wrong-version SAML" wrong-version nonzero
  saml_validation_case "validation default rejects sources-only SAML" sources-only nonzero
  saml_validation_case "validation default accepts a complete core+web+SAML set" correct zero
  saml_validation_case "validation rejects unsuffixed SAML for a Java variant" primary-only nonzero "$SAML_RELEASE_VERSION-java-25"
  saml_validation_case "validation accepts matching Java-variant SAML" correct zero "$SAML_RELEASE_VERSION-java-25"
  mkdir -p "$WORKDIR/historical-source"
  saml_validation_case "validation preserves historical tags without the SAML module" missing zero "$SAML_RELEASE_VERSION" "$WORKDIR/historical-source"
  saml_validation_case "validation preserves historical Java-variant tags" missing zero "$SAML_RELEASE_VERSION-java-25" "$WORKDIR/historical-source"
else
  bad "release validation body is extractable" "step not found in $(rel "$RELEASE_PHASE")"
fi

block_has_if "$TEST_PHASE" "Install native libvips (image engine parity tests)" "matrix.name == 'JVM Unit Tests'" \
  "native libvips setup stays with core tests, not the SAML suite"

SAML_SRC="${SAML_POM%/pom.xml}/src/main/java"
if [[ -d "$SAML_SRC" ]]; then
  lacks "$SAML_SRC/com/dotcms/saml/osgi/Activator.java" 'System\.(out|err)\.' \
    "SAML startup diagnostics use the application logger"
  has "$SAML_SRC/com/dotcms/saml/osgi/Activator.java" 'context\.getBundle\(\)\.getVersion\(\)' \
    "SAML startup reports the actual bundle version, not a standalone version pin"
  lacks "$SAML_SRC/org/opensaml/xml/util/Base64.java" 'System\.(out|err)\.' \
    "legacy decoder diagnostics use the application logger"
fi

# ================================================ 13. slim maven-repo artifact ==
# The maven-repo artifact used to be the whole ~/.m2/repository (2.7 GB, downloaded
# by every test job). It now holds only what the build installed; third-party jars
# travel through the Maven cache, which test jobs restore on top of it. Three
# things have to stay true: "installed" means any version (a release build has no
# -SNAPSHOT directory), the cache never receives installed artifacts, and a suite
# that needs no dependencies can skip the cache. (Reads the matrix with yq, which
# the runner image ships.)

echo "== 13. maven-job: slim maven-repo artifact =="

TEST_PHASE="$REPO_ROOT/.github/workflows/cicd_comp_test-phase.yml"
TEST_MATRIX="$REPO_ROOT/.github/test-matrix.yml"

block_has "$MAVEN_JOB" persist-maven-repo 'built-artifacts' \
  "the maven-repo artifact is uploaded from the staged built artifacts, not ~/.m2/repository"
block_lacks "$MAVEN_JOB" persist-maven-repo '~/\.m2/repository' \
  "the maven-repo artifact no longer uploads the whole repository"
block_has_if "$MAVEN_JOB" restore-cache-maven "needs-maven-repo == 'true'" \
  "the dependency cache restore is controlled by needs-maven-repo, not by artifacts-from"
block_has_if "$MAVEN_JOB" restore-artifact-maven-repo "needs-maven-repo == 'true'" \
  "a suite that needs no dotCMS module does not download the maven-repo artifact"
block_has_if "$MAVEN_JOB" restore-artifact-docker-image "needs-docker-image == 'true'" \
  "the docker image is restored on its own condition, independent of the maven-repo artifact"
block_has_if "$MAVEN_JOB" restore-cache-maven-plugins "needs-maven-repo != 'true'" \
  "suites that skip the dependency cache restore the plugin cache instead"
block_has_if "$MAVEN_JOB" save-cache-maven-plugins "needs-maven-repo != 'true'" \
  "only those suites save the plugin cache"
block_has_if "$MAVEN_JOB" save-cache-maven-plugins "restore-cache-maven-plugins.outputs.cache-hit != 'true'" \
  "the plugin cache is saved only on a miss"
block_has_if "$MAVEN_JOB" delete-built-artifacts-from-cache "release-build != 'true'" \
  "a release build does not delete its release coordinates (the caller verifies them afterwards)"
block_has_if "$MAVEN_JOB" save-cache-maven "release-build != 'true'" \
  "a release build does not write the shared dependency cache"
for step in delete-built-artifacts-from-cache save-cache-maven save-cache-maven-plugins; do
  block_has_if "$MAVEN_JOB" "$step" "cache-matched-key == '' \|\| \(github.event_name != 'pull_request' && github.event_name != 'merge_group'\)" \
    "$step: PR and merge-queue runs save only when they restored nothing (ref-scoped copies evict main's entry)"
done
if [[ "$(input_default "$MAVEN_JOB" needs-maven-repo)" == "true" ]]; then
  ok "needs-maven-repo defaults to true (shared callers keep their dependencies)"
else
  bad "needs-maven-repo defaults to true (shared callers keep their dependencies)"
fi
has "$TEST_PHASE" 'needs-maven-repo: \$\{\{ matrix\.needs_maven_deps == true \}\}' \
  "test phase forwards needs_maven_deps to the cache restore"
# The exact set, not "these three are false": another suite switched off would pass a
# weaker check, and so would a suite-level override that the test types' defaults hide.
OFF_TYPES="$(yq '[.test_types | to_entries | .[] | select(.value.defaults.needs_maven_deps == false) | .key] | sort | join(",")' "$TEST_MATRIX")"
SUITE_OVERRIDES="$(yq '[.test_types[] | (.suites // [])[] | select(has("needs_maven_deps"))] | length' "$TEST_MATRIX")"
if [[ "$(yq '.defaults.needs_maven_deps' "$TEST_MATRIX")" == "true" \
   && "$OFF_TYPES" == "e2e,karate,postman" && "$SUITE_OVERRIDES" == "0" ]]; then
  ok "exactly Postman, Karate and E2E skip the dependency cache and the maven-repo artifact"
else
  bad "exactly Postman, Karate and E2E skip the dependency cache and the maven-repo artifact" \
    "types with needs_maven_deps: false = '$OFF_TYPES' (want e2e,karate,postman); suite-level overrides = $SUITE_OVERRIDES (want 0)"
fi
# needs_node restores the Node binary (60 MB) and core-web's pnpm store (495 MB). Only the
# suites that run Node get them: Postman (newman), E2E (Playwright) and the frontend unit
# tests. Karate, the integration suites, JVM and CLI run none, and the war carries core-web.
NODE_TYPES="$(yq '[.test_types | to_entries | .[] | select(.value.defaults.needs_node == true) | .key] | sort | join(",")' "$TEST_MATRIX")"
NODE_OVERRIDES="$(yq '[.test_types[] | (.suites // [])[] | select(has("needs_node"))] | length' "$TEST_MATRIX")"
if [[ "$(yq '.defaults.needs_node // false' "$TEST_MATRIX")" == "false" \
   && "$NODE_TYPES" == "e2e,frontend,postman" && "$NODE_OVERRIDES" == "0" ]]; then
  ok "exactly Postman, E2E and the frontend unit tests restore Node and the pnpm store"
else
  bad "exactly Postman, E2E and the frontend unit tests restore Node and the pnpm store" \
    "types with needs_node: true = '$NODE_TYPES' (want e2e,frontend,postman); suite-level overrides = $NODE_OVERRIDES (want 0)"
fi
has "$TEST_PHASE" 'requires-node: \$\{\{ matrix\.needs_node == true \}\}' \
  "test phase forwards needs_node to the Node and pnpm steps"

# The Javadoc step runs Maven against ~/.m2/repository, which in reuse mode holds only
# the release coordinates. It needs the dependency cache, restored AFTER publishing so
# the cache cannot take part in the publication source.
block_has_if "$RELEASE_PHASE" "Restore Maven Dependency Cache for Javadocs" 'reuse-build-artifacts == true' \
  "release phase restores the dependency cache for the Javadoc build when it reused the artifact"
block_has_if "$RELEASE_PHASE" "Restore Maven Dependency Cache for Javadocs" 'upload_javadocs == true' \
  "the Javadoc dependency cache is restored only when Javadocs are generated"
step_line() { grep -nE -- "- name: $2\$" "$1" | head -1 | cut -d: -f1; }
L_PUBLISH="$(step_line "$RELEASE_PHASE" 'Publish Release Artifacts to S3')"
L_RESTORE="$(step_line "$RELEASE_PHASE" 'Restore Maven Dependency Cache for Javadocs')"
L_JAVADOC="$(step_line "$RELEASE_PHASE" 'Generate and Upload Javadocs')"
if [[ -n "$L_PUBLISH" && -n "$L_RESTORE" && -n "$L_JAVADOC" && "$L_PUBLISH" -lt "$L_RESTORE" && "$L_RESTORE" -lt "$L_JAVADOC" ]]; then
  ok "the Javadoc dependency cache is restored after the S3 publish and before the Javadoc build"
else
  bad "the Javadoc dependency cache is restored after the S3 publish and before the Javadoc build" \
    "publish=$L_PUBLISH restore=$L_RESTORE javadoc=$L_JAVADOC"
fi

# Behaviour: run the real stage and delete bodies against a fake repository. Maven
# records where each file came from in _remote.repositories: "x.jar>=" for an
# installed file, "x.jar>central=" for a download. The fake holds a third-party jar,
# an installed snapshot, an installed release version, a stale installed release
# beside the snapshot (what the old cache contained), and a DOWNLOADED com.dotcms
# release in the same artifactId directory as the installed snapshot, like the SAML
# plugin's pinned com.dotcms:dotcms-core:26.03.27-01. Run as GitHub runs `shell:
# bash`, with -e and pipefail.
SLIM="$WORKDIR/slim"
mkdir -p "$SLIM/home/.m2/repository" "$SLIM/tmp"
SLIM_REPO="$SLIM/home/.m2/repository"
fake_version_dir() {  # <dir> <repository id, empty for installed>
  local d="$SLIM_REPO/$1" base; base="$(basename "$(dirname "$d")")-$(basename "$d")"
  mkdir -p "$d"; : > "$d/$base.jar"; printf '%s.jar>%s=\n' "$base" "$2" > "$d/_remote.repositories"
}
fake_version_dir org/example/lib/1.0 central
fake_version_dir com/dotcms/app/1.0.0-SNAPSHOT ""
fake_version_dir com/dotcms/app/26.10.26-01 ""
fake_version_dir com/dotcms/app/26.03.27-01 dotcms-libs
fake_version_dir com/dotcms/rel/26.01.01-01 ""
fake_version_dir "com/dotcms/odd dir/1.0" ""  # a space in a path must not split a list entry
: > "$SLIM_REPO/com/dotcms/app/1.0.0-SNAPSHOT/maven-metadata-local.xml"
: > "$SLIM_REPO/com/dotcms/app/maven-metadata-local.xml"
: > "$SLIM_REPO/com/dotcms/rel/maven-metadata-local.xml"
extract_run_body "$MAVEN_JOB" stage-built-artifacts > "$SLIM/stage.sh"
extract_run_body "$MAVEN_JOB" delete-built-artifacts-from-cache > "$SLIM/delete.sh"
STAGE_DIR="$SLIM/tmp/built-artifacts"

if [[ -s "$SLIM/stage.sh" ]] && HOME="$SLIM/home" RUNNER_TEMP="$SLIM/tmp" bash -eo pipefail "$SLIM/stage.sh" >/dev/null 2>&1; then
  [[ -f "$STAGE_DIR/com/dotcms/app/1.0.0-SNAPSHOT/app-1.0.0-SNAPSHOT.jar" ]] \
    && ok "staging keeps a snapshot build at its repository path" \
    || bad "staging keeps a snapshot build at its repository path"
  [[ -f "$STAGE_DIR/com/dotcms/rel/26.01.01-01/rel-26.01.01-01.jar" ]] \
    && ok "staging keeps a release-versioned build (no -SNAPSHOT directory needed)" \
    || bad "staging keeps a release-versioned build (no -SNAPSHOT directory needed)"
  [[ -f "$STAGE_DIR/com/dotcms/odd dir/1.0/odd dir-1.0.jar" ]] \
    && ok "staging handles a path containing a space" \
    || bad "staging handles a path containing a space"
  [[ ! -e "$STAGE_DIR/com/dotcms/app/26.03.27-01" ]] \
    && ok "staging leaves a downloaded com.dotcms release beside an installed build to the cache" \
    || bad "staging leaves a downloaded com.dotcms release beside an installed build to the cache"
  [[ ! -e "$STAGE_DIR/org" ]] \
    && ok "staging leaves third-party artifacts to the cache" \
    || bad "staging leaves third-party artifacts to the cache"
  [[ -f "$SLIM_REPO/com/dotcms/rel/26.01.01-01/rel-26.01.01-01.jar" ]] \
    && ok "staging copies: the repository still holds the release build for the caller's check" \
    || bad "staging copies: the repository still holds the release build for the caller's check"
else
  bad "stage-built-artifacts runs against a repository holding builds" "body missing or exited non-zero"
fi

# An empty stage must fail loudly: upload-artifact only warns on an empty path.
mkdir -p "$SLIM/empty-home/.m2/repository/org/example" "$SLIM/empty-tmp"
if HOME="$SLIM/empty-home" RUNNER_TEMP="$SLIM/empty-tmp" bash -eo pipefail "$SLIM/stage.sh" >/dev/null 2>&1; then
  bad "staging fails when the build installed nothing" "exited 0"
else
  ok "staging fails when the build installed nothing"
fi

if [[ -s "$SLIM/delete.sh" ]] && HOME="$SLIM/home" RUNNER_TEMP="$SLIM/tmp" bash -eo pipefail "$SLIM/delete.sh" >/dev/null 2>&1; then
  [[ -f "$SLIM_REPO/org/example/lib/1.0/lib-1.0.jar" ]] \
    && ok "cleaning the cache keeps third-party artifacts" \
    || bad "cleaning the cache keeps third-party artifacts"
  [[ -f "$SLIM_REPO/com/dotcms/app/26.03.27-01/app-26.03.27-01.jar" ]] \
    && ok "cleaning the cache keeps a downloaded com.dotcms release beside an installed build" \
    || bad "cleaning the cache keeps a downloaded com.dotcms release beside an installed build"
  [[ ! -e "$SLIM_REPO/com/dotcms/app/1.0.0-SNAPSHOT" && ! -e "$SLIM_REPO/com/dotcms/app/26.10.26-01" \
     && ! -e "$SLIM_REPO/com/dotcms/rel/26.01.01-01" && ! -e "$SLIM_REPO/com/dotcms/odd dir/1.0" \
     && ! -e "$SLIM_REPO/com/dotcms/app/maven-metadata-local.xml" ]] \
    && ok "cleaning the cache removes installed artifacts of every version, and their local metadata" \
    || bad "cleaning the cache removes installed artifacts of every version, and their local metadata"
else
  bad "delete-built-artifacts-from-cache runs against a repository holding builds" "body missing or exited non-zero"
fi

# ---------------------------------------------------------------------- done --
echo
echo "pass=$pass fail=$fail"
echo
if [[ $fail -eq 0 ]]; then
  echo "All green: Phase 1 steps 2-6 are implemented."
else
  echo "$fail assertion(s) failing."
fi
echo
echo "Suffix convention: the derived Java suffix is 'java-<major>' everywhere."
echo "'java-25' is canonical because RELEASE_JAVA_VARIANT_SUFFIX is set to exactly"
echo "that in the repository variables; maven-job and cicd_comp_release-phase used"
echo "to derive 'java25', which made the build publish maven-repo-java25 while the"
echo "deployment phase asked for maven-repo-java-25."
echo "An explicitly supplied suffix is still passed through verbatim and never"
echo "rewritten, so a legacy non-dashed form is not silently repointed."
[[ $fail -eq 0 ]]
