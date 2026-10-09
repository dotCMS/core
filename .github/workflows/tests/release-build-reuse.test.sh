#!/usr/bin/env bash
#
# Contract tests for release-artifact reuse and Docker cache isolation.
# Tracking issue: #37607
#   "CI: speed up the release GitHub Action with Maven artifact reuse and
#    Docker cache isolation"
#
# This began life as the TDD "Red" suite for Phase 1 of #37607 and is now the
# regression suite for it: every section is green on main and should stay that
# way. A non-zero exit means something regressed. The sections are independent,
# so a failure names the behaviour that broke rather than just a count.
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
    # `id:` may sit on the list dash ("- id: run-maven-build") or alone.
    $0 ~ "^[[:space:]]*-?[[:space:]]*id:[[:space:]]*" want "[[:space:]]*$" { found = 1; next }
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
# $1 = path to body, $2 = GITHUB_OUTPUT.
run_body() {
  : > "$2"
  ( cd "$REPO_ROOT" \
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
block_has_if "$MAVEN_JOB" save-docker-image "generate-test-image == 'true'" \
  "test-image save is gated on generate-test-image"
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
echo "== 7. release workflows take the suffix from the build =="
# The opt-in assertions (generate-test-image: false, release-build: true,
# reuse-build-artifacts: true, docker-cache-scope-prefix) deliberately live with
# the commit that flips them on, not here. Asserting a flag is set is only
# meaningful next to the change that sets it.
for wf in "$RELEASE_WF" "$VARIANT_WF"; do
  name="$(basename "$wf")"
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
