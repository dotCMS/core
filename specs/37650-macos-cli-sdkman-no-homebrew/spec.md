# Issue Resolution Specification: macOS CLI native build fails — Homebrew no longer ships a bash bottle for our runner platforms

**Feature Branch**: `37650-macos-cli-sdkman-no-homebrew`

**Created**: 2026-09-21

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37650](https://github.com/dotCMS/core/issues/37650)

**Input**: User description: "macOS CLI native build fails because Homebrew stopped publishing bash bottles for `arm64_sonoma` (macos-14) and for all x86_64 macOS (macos-15-intel). The `Install Bash 4+ (macOS)` step in `.github/actions/core-cicd/setup-java/action.yml:64-71` runs `brew install bash` with no fallback and exits 1. The failure is self-perpetuating: the step is gated on an SDKMAN cache miss, and when it fails the cache is never saved. `main` has been red for 18/18 runs since Sep 17. Key finding, verified empirically: Bash 4 is only needed to get past a preventive guard in SDKMAN's installer — neither the installer nor the 5.23.1 runtime uses Bash 4 constructs, and the installer runs to completion under Bash 3.2.57 with the guard disarmed. The proposed fix removes the Homebrew dependency."

## Problem Statement *(mandatory)*

Every CLI native image build on macOS fails. The `Build native image on macOS-Silicon` job
cannot install SDKMAN, so it never reaches the Maven build. Because the build matrix uses
`fail-fast: true`, its failure also cancels the Linux and macOS-Intel legs, which collapses the
whole CLI Build phase and turns `Finalize / Final Status` red.

No code change caused this. An upstream Homebrew policy change removed the prebuilt `bash`
bottle for both macOS runner platforms the pipeline pins, and the affected step invokes
`brew install bash` with no fallback.

The failure cannot self-heal. The step only runs on an SDKMAN cache miss; when it fails, the
steps that would repopulate that cache are skipped, so the next run misses again and fails
identically. Once the cache expired, the pipeline entered a closed loop.

**Severity / Impact**: High. Affects the entire engineering team continuously — `main` has been
red on 18 of the last 18 Trunk Workflow runs since Sep 17, 2026, masking any genuine build
failure behind a known-red signal. Blocks CLI native artifact production for `osx-aarch_64` and
`osx-x86_64`, and therefore CLI releases. No impact on the dotCMS product itself or on
customers; this is confined to CI/CD.

## Reproduction *(mandatory)*

**Environment**: GitHub Actions hosted runners. `macos-14` (Apple Silicon, `arm64_sonoma`) and
`macos-15-intel` (Intel, `sequoia` x86_64), as pinned by the runner matrix defaults in
`.github/workflows/cicd_comp_cli-native-build-phase.yml:70`. Requires a cold
`macOS-<arch>-sdkman-install` cache — which is now the permanent state.

**Steps to Reproduce**:

1. Trigger any run of the `-3 Trunk Workflow` on `main` (push to `main`, or `workflow_dispatch`).
2. Open the `CLI Build / Build native image on macOS-Silicon` job.
3. Expand the `Run ./.github/actions/core-cicd/maven-job` step, then the nested
   `Install Bash 4+ (macOS)` step.
4. Observe the job fail before any Maven work begins.

The missing bottles can be confirmed independently of CI:

```bash
curl -s https://formulae.brew.sh/api/formula/bash.json | jq -r '.bottle.stable.files | keys[]'
# arm64_golden_gate, arm64_linux, arm64_sequoia, arm64_tahoe, x86_64_linux
# → no arm64_sonoma, and no x86_64 macOS tag at all
```

**Expected Behavior**: The `setup-java` action installs SDKMAN, provisions the JDK from
`.sdkmanrc` plus GraalVM, and the job proceeds to build the native image. All three matrix legs
(Linux, macOS-Intel, macOS-Silicon) complete.

**Actual Behavior**: The `Install Bash 4+ (macOS)` step fails:

```
==> Downloading Homebrew API data
✔︎ JSON API packages.arm64_sonoma.jws.json
Error: bash: no bottle available!
If you're feeling brave, you can try to install from source with:
  brew install --build-from-source bash
This is a Tier 3 configuration
Process completed with exit code 1.
```

The subsequent `Install SDKMan` and `Save Cache SDKMan install` steps are skipped, the composite
action fails, and `fail-fast` cancels the sibling matrix legs.

**Reproducibility**: **Intermittent, and worse for it.** Reproduces on both architectures —
verified on `macos-14`/`arm64_sonoma` (run 35108639064) and independently on
`macos-15-intel`/`sequoia` (run 35276183950), with an identical error, so it is not specific to
one OS version or architecture. It has failed on 18 consecutive Trunk runs. But it does **not**
fail deterministically: it depends on which `bash` formula the runner's Homebrew index resolves.

| Formula resolved | Dependencies | `arm64_sonoma` bottle | Outcome |
|---|---|---|---|
| **5.3.20** (current) | ncurses, readline, gettext | ✗ | `no bottle available!` |
| **5.3.15** (older) | ncurses | ✓ | installs successfully |

A failing run logs `Downloading Homebrew API data` — it refreshes the index, resolves 5.3.20 and
dies. A run against a runner whose Homebrew does not auto-update resolves 5.3.15 from the image's
local index and passes. Whether the toolchain provisions is therefore a race with how stale that
runner's Homebrew index happens to be.

This strengthens rather than weakens the case for the fix: a build that passes or fails according
to a package index cache it does not control is worse than one that fails consistently, because
intermittent failure reads as flakiness and stops being investigated. Note also that the cold
SDKMAN cache is a **precondition**, not a trigger — a warm cache skips the step entirely and
masks the defect.

## Scope of Investigation *(mandatory)*

- **Affected area**: CI/CD build infrastructure — specifically the shared Java/toolchain
  provisioning composite action used across the pipeline. The failing step is reached via
  `cicd_comp_cli-native-build-phase.yml` → `core-cicd/maven-job` → `core-cicd/prepare-runner` →
  `core-cicd/setup-java`.
- **Suspected surface**: Neither modern (`com.dotcms.*`) nor legacy (`com.dotmarketing.*`) — no
  product Java code is involved. The change is confined to `.github/`. The modern/legacy split
  does not apply to this fix.
- **Related known decisions**: No known ADR governs CI toolchain provisioning; the plan phase
  will confirm against `dotCMS/platform-adrs`. Relevant prior history in this repo:
  - #35004 (open) — the original "SDKMAN 5.21.0 requires Bash 4+" report.
  - PR #35007 — ran the installer under `zsh`; reverted because the installer relies on
    bashisms (sdkman/sdkman-cli#1520).
  - PR #35039 — introduced `brew install bash`, described by its author as a *"stopgap until
    mise replaces SDKMAN in the pipeline"*. This is the stopgap that has now broken.
  - #34214 — migrate tooling to `mise`. Feasible: mise's index carries both toolchains this
    repo pins — `graalvm-community-21.0.2` (≡ `21.0.2-graalce`) and `microsoft-25.0.4`
    (≡ `25.0.4+1-ms`). Its documented GraalVM limitation is about auto-mapping `.sdkmanrc`,
    not about what it can install; explicit identifiers work. Out of scope here on grounds of
    size and validation risk, not feasibility — see Non-Goals.

## Root-Cause Hypothesis

This is confirmed rather than hypothesised; the plan phase should validate the reasoning rather
than re-derive the cause.

1. SDKMAN's installer aborts on Bash < 4 (`get.sdkman.io`, "Checking Bash version..."). macOS
   ships Bash 3.2.57 and Apple will not upgrade it (GPLv3). **No GitHub macOS runner image
   provides Bash 4+** — verified across the `macos-14`, `macos-15`, `macos-26` and Intel
   manifests, all of which report `Bash 3.2.57(1)-release`.
2. To satisfy that check, `setup-java/action.yml:64-71` runs `brew install bash`.
3. The **current** `bash` formula (5.3.20) has no bottle for `arm64_sonoma` or for any x86_64
   macOS tag — both are Tier 3, source-build only. Its published tags are `arm64_sequoia`,
   `arm64_tahoe`, `arm64_golden_gate`, `arm64_linux`, `x86_64_linux`. When a runner resolves
   that formula, the command exits 1. Older formulae (5.3.15) did have an `arm64_sonoma`
   bottle and fewer dependencies, which is why the failure is intermittent — see
   Reproducibility.
4. `-e` propagates the failure up through `setup-java` → `prepare-runner` → `maven-job` → job
   → `fail-fast` → workflow.

**The load-bearing finding for the fix**: the Bash 4 requirement is an *advisory guard*, not a
real runtime constraint. Verified empirically:

- The installer contains **zero** Bash 4 constructs (no `declare -A`, `local -A`, `mapfile`,
  `readarray`, `${var,,}`, `${var^^}`, `&>>`, `;;&`, `globstar`, `coproc`).
- The installed SDKMAN 5.23.1 runtime is equally clean — zero hits across its 22 scripts.
- Running the installer under `/bin/bash` 3.2.57 with the guard disarmed, against an isolated
  `HOME`, completes successfully: SDKMAN 5.23.1 + native 0.7.34, platform `darwinarm64`.
- Against that installation, under Bash 3.2.57: `sdk update`, `sdk list java`, `sdk home java`
  and `sdk install java` all behave correctly.
- Corroborating evidence already in the repo: the `Setup SDKMan` step
  (`setup-java/action.yml:111-113`) runs `sdk install java` / `sdk use` / `sdk default` under
  `shell: bash` with no OS conditional — i.e. under macOS's Bash 3.2 — and has always worked.

So Homebrew is dragged into every macOS CI job solely to satisfy a check that does not apply to
how the pipeline uses SDKMAN.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Remove the Homebrew dependency from macOS Java/toolchain provisioning in
  `.github/actions/core-cicd/setup-java/action.yml`, so SDKMAN installs using the Bash the
  runner already provides.
- Ensure the SDKMAN install cache is repopulated on success, breaking the self-perpetuating
  failure loop.
- Fail loudly and actionably if the assumption about SDKMAN's installer stops holding, so the
  pipeline can never silently degrade into a broken toolchain.
- Provide a temporary, `workflow_dispatch`-only validation workflow exercising both macOS
  runners, used to satisfy the Red/Green gates. Removed from the branch before merge.

**Explicitly out of scope / non-goals**:

- **Bumping `macos-14` → `macos-15`** (`cicd_comp_cli-native-build-phase.yml:70`). `macos-14` is
  deprecated by GitHub (actions/runner-images#13518, unsupported after Nov 2) and should move,
  but it is a separate concern: it does not cause this failure, it cannot fix the Intel leg
  (no x86_64 macOS bottle exists in any version), and keeping `macos-14` here means the
  verification run proves the fix on the platform *without* bottles. Separate issue/PR.
- **Migrating off SDKMAN** — to `mise` (#34214), or to the official `actions/setup-java` +
  `graalvm/setup-graalvm`. Both are viable, and one of them is likely the right end state.
  Either is a refactor of a different order, though: `.sdkmanrc` is the toolchain source of
  truth across ~20 files — including `parent/pom.xml`, `.github/filters.yaml` and the
  `dotcms/java-base` image tag — and `setup-java` reaches 8 workflows plus both release paths
  via `prepare-runner` → `maven-job`. It would also change Linux, which currently works.
  Decisively, a change that size cannot be validated against a baseline that has been red for
  five days: restoring a green `main` is a **prerequisite** for that migration. This is
  sequencing, not an argument against it. Tracked separately.
- **Reworking the CLI build matrix** or dropping the `osx-x86_64` target. Whether dotCMS still
  needs an Intel macOS CLI binary is a product decision, not part of this fix.
- **Changing `fail-fast` behavior** in the native build matrix.
- Any change to Linux provisioning, or to product code.

## Regression Risk *(mandatory)*

- **Blast radius**: `setup-java` is shared. It is reached from `core-cicd/prepare-runner:45-48`
  (and therefore from `core-cicd/maven-job`, used across most of the pipeline), plus
  `cicd_comp_release-phase.yml:142` and two call sites in
  `legacy-release_maven-release-process.yml`. However, the affected step is gated on
  `runner.os == 'macOS'` **and** an SDKMAN cache miss, so Linux jobs never execute it — their
  behavior must remain byte-for-byte unchanged. The realistic blast radius is macOS jobs only,
  all of which are currently failing, so the change cannot make the situation worse.
- **Backward compatibility**: No product API, content, serialized state, DB schema or ES mapping
  is touched — see [Rollback-Unsafe Change Categories](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md);
  none apply. The cache key `${{ runner.os }}-${{ env.ARCHITECTURE }}-sdkman-install` must remain
  unchanged so existing warm caches on other platforms stay valid. Rolling this change back
  simply restores today's broken state — it carries no rollback risk of its own.
- **Data considerations**: None. No persisted data, no migration, no repair of bad state. The
  only durable artifact is the GitHub Actions cache, which is regenerated on demand.
- **Residual risk**: The fix depends on SDKMAN's installer remaining Bash 3.2-compatible. If
  upstream genuinely adopts Bash 4, the guard stops being advisory. This is mitigated, not
  eliminated: the implementation must detect that the installer no longer matches expectations
  and fail the build with an explicit, actionable error rather than proceeding.

## Acceptance & Verification *(mandatory)*

- **AC-001**: The reproduction no longer occurs — a `setup-java` invocation on `macos-14`
  (Apple Silicon) and on `macos-15-intel` (Intel) completes successfully **on a cold SDKMAN
  cache**, provisioning both the `.sdkmanrc` JDK and GraalVM. The cold-cache path is mandatory:
  it is the only path that fails today.
- **AC-002**: No `brew` invocation occurs anywhere in the macOS toolchain setup, verifiable by
  the absence of Homebrew output in the job log. The fix must not merely add a fallback around
  `brew`; the dependency is removed.
- **AC-003**: `Save Cache SDKMan install` executes after a successful run, proving the
  self-perpetuating loop is broken. A subsequent run on the same key hits the cache and skips
  installation entirely.
- **AC-004**: Linux provisioning is unchanged — a Linux job using `setup-java` still succeeds,
  and its execution path is not altered by the change.
- **AC-005**: If SDKMAN's installer no longer matches the assumption the fix relies on, the
  build fails with an explicit error naming the cause, rather than installing a broken
  toolchain or silently falling back.

**Verification method**:

Standard dotCMS test layers do not apply — this change contains no Java, TypeScript or REST
surface, so unit, integration, Postman, Karate and e2e suites have nothing to assert against.
Per Constitution Principle V, this is stated explicitly rather than left silent: **the
layer-appropriate test here is a CI workflow that exercises the failing step on real runners.**

The complication is that `cicd_comp_cli-native-build-phase.yml` is `workflow_call`-only and
never runs on a pull request, so the fix cannot be validated by opening a PR. The test is
therefore a temporary `workflow_dispatch` workflow on the feature branch, with a matrix over
`macos-14` and `macos-15-intel`, invoking `./.github/actions/core-cicd/setup-java` with
`require-graalvm: true` and asserting `java -version` and `native-image --version` resolve.

This satisfies the TDD gates genuinely:

1. **Red** — run the validation workflow on the branch *before* the fix; both legs must fail
   with `bash: no bottle available!`, confirming the test reproduces the defect for the right
   reason.
2. **Approval** — developer reviews and approves the validation workflow as the test.
3. **Green** — apply the fix; both legs pass, and the log checks for AC-002 and AC-003 hold.

Post-merge confirmation: the first `-3 Trunk Workflow` run on `main` shows all three CLI Build
legs passing. The temporary workflow is deleted from the branch before the implementation PR is
opened; it is scaffolding for the Red/Green gates, not a permanent addition to the pipeline.

## Assumptions

- The GitHub-hosted macOS runner images continue to provide Bash 3.2.57, `curl`, `unzip`, `zip`,
  `tar`, `find` and `sed` — all of which SDKMAN's installer requires and all of which are
  present today.
- SDKMAN's installer remains Bash 3.2-compatible in substance. AC-005 converts a violation of
  this assumption into a loud build failure rather than a silent breakage.
- Homebrew will not restore bottles for `arm64_sonoma` or x86_64 macOS. Even if it did, the fix
  is still correct — it removes a dependency the pipeline never needed.
- The `osx-x86_64` CLI artifact is still required. If that turns out to be false, dropping the
  Intel leg would be a simpler answer, but that is a product decision outside this issue.
- `RUNNER_TEMP` is available for staging downloads in composite action steps (standard GitHub
  Actions behavior).
