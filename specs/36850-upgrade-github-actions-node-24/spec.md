# Issue Resolution Specification: Upgrade GitHub Actions to Node 24 runtime majors

**Feature Branch**: `36850-upgrade-github-actions-node-24`

**Created**: 2026-08-24

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#36850](https://github.com/dotCMS/core/issues/36850) (blocked by #35930, closed by [#36838](https://github.com/dotCMS/core/pull/36838) — merged 2026-08-04)

**Input**: User description: "GitHub Actions runners deprecate the Node 20 action runtime and force Node 24. CI jobs (e.g. Initial Artifact Build) emit a deprecation annotation naming `actions/cache/restore@v4`, `actions/checkout@v4`, `actions/upload-artifact@v4`, `pnpm/action-setup@v4`. This is the Actions runtime (JS inside official actions), not the project Node version. Do not use `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION`."

## Problem Statement *(mandatory)*

GitHub has deprecated the Node 20 action runtime on Actions runners. Every action pinned to a major
whose `runs.using` is `node20` (or older) is now silently force-migrated onto Node 24 at execution
time, and each affected job emits a deprecation annotation. dotCMS/core's CI is broadly affected:
**124 references to 8 in-scope actions across 51 files** under `.github/workflows/**` and
`.github/actions/**` are still on `node20`-era majors, including legacy pins as old as
`actions/checkout@v2` and `actions/setup-node@v2-beta`.

Two distinct harms:

1. **Now** — every PR, merge-queue, trunk, nightly and release run is annotated with deprecation
   warnings. This is noise that trains reviewers to ignore annotations, which is exactly where a real
   failure would surface.
2. **When the shim is removed** — GitHub's force-migration is temporary. Once the runner drops the
   Node 20 compatibility shim, every one of these 124 references stops working, taking the entire
   build, test, and release pipeline with it. There is no partial-failure mode: `actions/checkout`
   alone appears in 62 places.

There is a further structural cause behind this issue existing at all: **the repository has no
automated guard against action-version drift.** There is no `.github/dependabot.yml` for the
`github-actions` ecosystem, no `actionlint` in CI, and `.github/filters.yaml`'s `backend` filter only
matches `cicd_comp_*.yml`, `cicd_1-pr.yml`, and `.github/actions/core-cicd/**/action.yml` — so edits
to roughly 38 of the 51 affected files trigger no build, no test, and no lint on a PR.

**Severity / Impact**: Medium now, critical on the runner's removal date. Affects **every engineer on
every PR** (`cicd_1-pr.yml`: ~3,140 runs/90d), the merge queue (~741), trunk (~452),
post-run reporting (~4,043), and the release pipeline. `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION` is a
temporary opt-out, not a fix, and is explicitly excluded — it appears nowhere in the repository today
and must stay that way.

## Reproduction *(mandatory)*

**Environment**: `dotCMS/core` `main` at or after `88af0bad55`. GitHub-hosted runners
(`ubuntu-${{ vars.UBUNTU_RUNNER_VERSION || '24.04' }}` ×80, `ubuntu-latest` ×18,
`macos-*` ×1). Actions runner ≥ 2.327.1. No `container:` jobs and no Windows runners.

**Steps to Reproduce**:

1. Open any pull request against `main`, or observe any recent run of `cicd_1-pr.yml`.
2. Open the run and select the **Initial Artifact Build** job (from `cicd_comp_build-phase.yml` →
   `.github/actions/core-cicd/maven-job`).
3. Read the job's **annotations** (not just its pass/fail status).
4. Repeat for the `initialize`, `label-pr`, `test`, and `finalize` jobs.

**Expected Behavior**: The run completes with no Node-runtime deprecation annotations. Every action it
invokes declares `runs.using: node24`.

**Actual Behavior**: Each job carries an annotation of the form:

> Node.js 20 is deprecated. The following actions target Node.js 20 but are being forced to run on
> Node.js 24: `actions/cache/restore@v4`, `actions/checkout@v4`, `actions/upload-artifact@v4`,
> `pnpm/action-setup@v4`

Observed example: <https://github.com/dotCMS/core/actions/runs/30649369328/job/91220403674>

**Reproducibility**: **Always**, on every run of every affected workflow. Deterministic — it is a
function of the pinned version strings, not of timing, data, or state.

## Scope of Investigation *(mandatory)*

- **Affected area**: CI/CD only — GitHub Actions workflow and composite-action definitions under
  `.github/`. No dotCMS product surface, no Java, no Angular, no database, no API.
- **Suspected surface**: Neither `com.dotcms.*` nor `com.dotmarketing.*`. This defect lives entirely
  in declarative YAML (`.github/workflows/**`, `.github/actions/**`). The modern/legacy axis does not
  apply; the analogous axis here is **hot-path CI** (`maven-job`, `setup-java`, the `cicd_comp_*`
  phases — validated by every PR) versus **cold-path CI** (release, nightly, scheduled, issue-driven,
  and two workflows that are `disabled_manually` on GitHub) which no PR build can exercise.
- **Related known decisions**: `cicd_2-merge-queue.yml:12` cites ADR-0013 (change detection); this
  change must not alter change-detection behavior. The repo's documented Action-security rules
  (`docs/core/CICD_PIPELINE.md` §Action Security, `docs/core/GIT_WORKFLOWS.md` §Security Patterns) —
  pin versions/SHAs, never `master`/`main`, minimal explicit `permissions` — must be preserved, not
  regressed. `/speckit-plan` will formally consult `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Not a code defect — a **maintenance-drift** defect with a confirmed external cause: GitHub's
[deprecation of Node 20 on Actions runners](https://github.blog/changelog/2025-09-19-deprecation-of-node-20-on-github-actions-runners/).
Each affected `uses:` line names a major whose `action.yml` declares `runs.using: node20` (or
`node16`/`node12`). The annotation is emitted by the runner, per job, listing that job's stale actions.

The drift persisted because nothing enforces a floor: no Dependabot config for the `github-actions`
ecosystem, no `actionlint` job, and `.github/filters.yaml` gives most workflow files no PR validation
at all. **The fix is therefore two-part: bump the pins, and add the guard that keeps them bumped.**

Version research is complete and recorded in the implementation plan. Target majors (all verified
`node24`, all verified latest as of 2026-08-24, and their breaking changes read from upstream release
notes and source):

| Action | Current pins | Target | Refs |
|---|---|---|---|
| `actions/checkout` | v4 ×54, v3 ×2, v2 ×1, SHA ×5 | v7.0.1 | 62 |
| `actions/download-artifact` | v4 ×13, SHA ×1 | v8.0.1 | 14 |
| `actions/cache/restore` | v4 ×12, v3 ×1 | v6.1.0 | 13 |
| `actions/upload-artifact` | v4 ×11 | v7.0.1 | 11 |
| `actions/cache/save` | v4 ×8 | v6.1.0 | 8 |
| `actions/setup-node` | v4 ×6, v2-beta ×1, SHA ×1 | v7.0.0 | 8 |
| `actions/cache` | v4 ×5, SHA ×1 | v6.1.0 | 6 |
| `pnpm/action-setup` | v4 ×2 | v6.0.10 | 2 |

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Bump all **124** references to the 8 actions above to their `node24` majors, across
  `.github/workflows/**` and `.github/actions/**`. Hot path first
  (`.github/actions/core-cicd/maven-job/action.yml` — 19 refs, the densest file;
  `.github/actions/core-cicd/setup-java/action.yml` — 6; then the `cicd_comp_*` phases), then the
  remaining composites, then the cold-path workflows.
- Bump the **4 third-party `node20` actions that execute on the PR pipeline**, because the annotation
  is emitted *per job* and would otherwise persist on jobs the issue's own AC does not name:
  `actions/github-script` v7 → **v8** (14 refs), `dorny/paths-filter` v3.0.1 → **v4.0.3**,
  `dawidd6/action-download-artifact` v6 → **v24**, `docker/login-action` v3.0.0/v3 → **v4.6.0**.
- **Preserve each site's existing pinning style**: float tag → float tag; SHA pin → new SHA **with a
  corrected `# vX.Y.Z` comment**. Eight sites are SHA-pinned, and two of them
  (`issue_autodoc.yml:100,103` vs `dotbot-review.yml:45`/`dotbot-act.yml:40`) currently pin the *same*
  `actions/checkout` SHA under *different* version comments — one is wrong today; both get fixed.
- Update the 5 `.github/actions/**/README.md` usage examples that show `actions/checkout@v2`
  (permitted by the issue's AC: "pin docs in action READMEs if needed").
- **Add the drift guard**: `.github/scripts/check-action-versions.sh` plus a standalone
  `cicd_pr_actions-lint.yml` job modeled on the existing `cicd_pr_skill-lint.yml`. This is the
  executable, confirmed-failing test required by Constitution Principle V, and it is the only
  automated validation the ~38 unfiltered files will ever get. Delivered as **one self-contained
  commit** so a reviewer can drop it without touching the migration.

**Explicitly out of scope / non-goals**:

- `.nvmrc`, `core-web`, Angular, and app/tooling Node versions — delivered by #36838. **No application
  code in this change.**
- `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION` or any equivalent opt-out, in any form.
- **The 4 first-party JS actions still on `runs.using: 'node16'`** —
  `.github/actions/issues/issue-fetcher`, `issues/issue-labeler`,
  `legacy-release/changelog-report`, `legacy-release/rc-changelog`. These are not YAML edits: each
  requires a `dist/index.js` rebuild via `.github/actions/buildActions.sh` plus TypeScript/`@types/node`/
  `@actions/core`/`ncc`/`eslint` upgrades. A regenerated bundle cannot be diff-reviewed the way YAML
  can, so mixing it in would make this change unreviewable. **File as the immediate next issue** —
  `node16` is more deprecated than `node20`.
- The remaining `node20`/`node16`/`node12` third-party actions on release/nightly/scheduled/issue-only
  paths (`docker/setup-buildx-action`, `docker/build-push-action`, `docker/metadata-action`,
  `docker/setup-qemu-action`, `astral-sh/setup-uv`, `actions/stale`, `actions/setup-python`,
  `slackapi/slack-github-action`, `peter-evans/*`, `jfrog/setup-jfrog-cli`,
  `phoenix-actions/test-reporting`, `whelk-io/maven-settings-xml-action`,
  `JamesIves/github-pages-deploy-action`, `ad-m/github-push-action@master`). Two are not mechanical at
  all — `aws-actions/configure-aws-credentials` v1 → v6 needs OIDC/`role-to-assume` plus org-level
  trust-policy work, and `slackapi/slack-github-action` v1 → v2+ changes the payload format. Follow-up
  issue, split per vendor.
- **`actions/github-script` v9.** v8 is the correct target: verified to be a pure `node24` bump (no
  `"type": "module"`, still `@actions/github ^6.0.0` / `@octokit/core ^5.0.1`, README still documents
  `require()`). v9 is the ESM break — `require('@actions/github')` fails and `getOctokit` becomes an
  injected parameter. Three sites use `require()` (`cicd_comp_test-phase.yml:115`,
  `cicd_comp_pr-area-labeler.yml:43`, `cicd_scheduled_qa-stuck-check.yml:76`), so v9 needs its own audit.
- **Repairing `publish_docs.yml`.** It is `disabled_manually` with 0 runs/90d, and its
  `cd core-web && npm install` cannot work against a pnpm workspace regardless. Its `setup-node` pin
  gets bumped for consistency and nothing more; converting it to pnpm would be a functional change to
  a dead pipeline hidden inside a runtime bump. Same for `legacy-release_sbom-generator.yaml` (also
  disabled). Separate cleanup issue to delete or resurrect them.
- Extending `.github/filters.yaml` so `.github/**` triggers real validation, and adding
  `.github/dependabot.yml` for the `github-actions` ecosystem. Both are the *structural* fix for this
  class of drift and both are strongly recommended — as a follow-up, so this change stays a runtime bump.
- Dead code noticed in passing and deliberately left alone: the `runner.os == 'Windows'` branches in
  `maven-job/action.yml` (no Windows runner exists in any matrix), and `.github/main.workflow`
  (Actions-v1 HCL, never executed).

## Regression Risk *(mandatory)*

- **Blast radius**: The entire CI/CD surface — but **no product surface**. `maven-job/action.yml` and
  `setup-java/action.yml` are shared by PR, merge-queue, trunk, nightly, and release, so a defect
  there blocks all merges. Concentration of risk, ranked:
  1. **`actions/download-artifact` v8 changes `digest-mismatch` from warn to `error`** — the only
     change in this whole set that can turn a green build red. Exposure: `maven-job:266`
     (`maven-repo` → `~/.m2/repository`, multi-GB, cross-run via `run-id`), `:289` (`build-classes`),
     `:276` (`docker-image`), and `cicd_comp_finalize-phase.yml:39` (`pattern: build-reports-*` — N
     artifacts, N chances to fail, on every PR and merge-queue run). **Resolved 2026-08-24 (dev decision):** land v8 **with an
     explicit `digest-mismatch: warn`** at all 14 sites so the bump is provably behavior-neutral —
     exactly the pure runtime change this issue asks for — then flip to `error` in a separate
     ~14-line commit that can be reverted alone. The value is written explicitly either way, so a
     future major cannot silently move it again.
  2. **`actions/checkout` v7's fork-PR block — assessed and ruled out.** v7 refuses to check out fork
     PR code from `pull_request_target` / `workflow_run`. Reading
     `src/unsafe-pr-checkout-helper.ts` and `src/input-helper.ts` at tag `v7.0.1`, two independent
     reasons it cannot fire here: the guard is only *reached* when a custom `repository:` or explicit
     `ref:` is supplied (a default self-checkout is skipped outright), **and** the throw additionally
     requires a genuine fork head repo **and** inputs that point at the fork's code. Site by site:
     `cicd_post-workflow-reporting.yml:58` is a bare `- uses: actions/checkout@v4` with no `with:`
     block at all (and on `workflow_run`, `github.ref`/`github.sha` resolve to the default branch, not
     the fork head); `cicd_publish-pr-test-image.yml` has **no checkout step**, and is gated
     `head.repo.full_name == github.repository`; `cicd_comp_publish-pr-test-image.yml` never checks
     out; `dotbot-review.yml`/`dotbot-act.yml` fire on `pull_request`/`issue_comment`, which the guard
     does not cover. **Do not pin anything to v6, and do not add `allow-unsafe-pr-checkout: true`** —
     that would permanently disarm a real protection against a risk this repo does not have. If it
     ever trips, the failure is loud and instant, the blast radius is reporting only, and it can only
     manifest on a fork PR.
  3. **`pnpm/action-setup` v6 — silent-regression watchpoint.** `steps.pnpm-info.outputs.version`
     feeds every pnpm store cache key (`maven-job:245`, `deploy-javascript-sdk:123`). The resolved
     pnpm is pinned by `core-web/package.json`'s `packageManager` (`pnpm@10.17.1`), so it will not
     change — but if it did, every pnpm cache would cold-miss once and surface as a *slow* build, not
     a failure. Verify `cache-hit` on the **second** PR build, not the first.
  4. **`dawidd6/action-download-artifact` v6 → v24** spans 18 majors. All inputs in use
     (`github_token`, `workflow_search`, `commit`, `workflow_conclusion`, `search_artifacts`,
     `dry_run`, `name`, `name_is_regexp`, `path`, `run_id`, `if_no_artifact_found`) and the
     `found_artifact` output are verified present in v24's `action.yml`; all three call sites already
     set `if_no_artifact_found: warn`, so they are non-fatal by construction.
- **Backward compatibility**:
  - **Cache keys are unchanged everywhere.** No cache is invalidated; existing caches stay warm and a
    revert re-reads the same keys. `actions/cache/restore@v6` still emits `cache-hit`,
    `cache-primary-key`, `cache-matched-key`, so the restore→save handoff via
    `${{ steps.restore-*.outputs.cache-primary-key }}` (`maven-job:429,437,445,453`;
    `setup-java:91,171,179`) works untouched.
  - **Artifact names and shapes are unchanged.** `upload-artifact@v7`'s new `archive` input defaults
    to `'true'`, i.e. exactly v4 behavior. `archive: false` **must not** be set anywhere:
    `maven-job:402` is consumed by `docker load < /tmp/docker-image/image.tar`, and
    `cicd_comp_finalize-phase.yml:230` is consumed by `dawidd6/action-download-artifact`, which both
    expect the zipped shape. `download-artifact@v5`'s `artifact-ids:` path change does not apply —
    there are **zero** uses of `artifact-ids:` in the repo.
  - **`checkout` v6+ moves credentials out of `.git/config`** into a file under `$RUNNER_TEMP`. All
    ~10 `git push` sites run in the same job and as the same user as their checkout; grep confirms
    nothing reads `.git/config` or `extraheader` or extracts the token, and no step runs `git` under
    `sudo` or inside `docker run` against the workspace.
  - **Runner minimum (2.327.1) is moot.** Only `security_scheduled_pentest.yml` uses self-hosted
    runners, and its sole `uses:` is `ad-m/github-push-action@master` — a Docker action, unaffected.
    Everything else is GitHub-hosted. Relatedly, checkout v6's only hard runner requirement
    (≥ 2.329.0 for Docker container actions) does not apply: there are no `container:` jobs and no
    `uses: docker://` anywhere.
  - **`package-manager-cache` is a no-op here.** `setup-node` reads only
    `$GITHUB_WORKSPACE/package.json`, and this repo has **no root `package.json`** (only a stray root
    `package-lock.json`). `core-web/package.json`'s `packageManager: pnpm@10.17.1` is invisible to it,
    and v6+ limits auto-caching to npm regardless. Auto-caching cannot fire at any of the 8 sites.
    See AC-005 — this is a deliberate deviation from the issue's written AC.
- **Data considerations**: **None.** No DB schema, no Elasticsearch mapping, no API contract, no
  serialized state, no data repair. Nothing here falls into any category in
  `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`. Rollback is a `git revert` of the PR — or of any single
  commit, since action versions do not interact (the one pairing to keep together is a
  `cache/restore` and its matching `cache/save`).

## Acceptance & Verification *(mandatory)*

- **AC-001**: A pull-request run of `cicd_1-pr.yml` completes with **no Node-runtime deprecation
  annotation** on any of its jobs — `initialize`, `label-pr`, **`build` / Initial Artifact Build**,
  `publish-test-image`, `test`, `finalize`. Residual annotations from the deliberately deferred
  out-of-scope actions must be **enumerated in the PR body** and attributable to a named follow-up
  issue, so the remainder is documented rather than looking like a miss.
- **AC-002**: No occurrence of `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION` (or equivalent) anywhere in
  the repository.
- **AC-003**: Every reference to the 8 in-scope actions under `.github/` is at or above its target
  major; every SHA-pinned reference matches its `# vX.Y.Z` comment. Enforced mechanically by AC-007's
  guard, not by eye.
- **AC-004**: Caching and artifacts continue to work with **unchanged keys and names** — Maven
  repository restore/save, Maven-wrapper cache, node-binary (`installs`) cache, pnpm store cache,
  SDKMAN caches, Sonar cache; and the `maven-repo`, `docker-image`, `build-classes`,
  `build-reports-*`, and `workflow-data` artifacts upload and download. Evidence: `cache-hit` true on
  a **second** consecutive PR build (the first legitimately re-primes nothing, but a cold miss on the
  second indicates a key regression).
- **AC-005**: Where the issue's AC calls for `package-manager-cache: false`, the change instead
  **documents that the precondition does not exist** in this repo (no root `package.json`, so
  `setup-node` auto-caching cannot fire) and adds no dead configuration.
  **Resolved 2026-08-24 (dev decision):** document the deviation, add no dead configuration. Rationale
  recorded for the reviewer — a no-op `package-manager-cache: false` would imply a hazard that does not
  exist here and would be copied by cargo-cult into workflows where it also does nothing.
- **AC-006 (sad path)**: No cache or artifact step is silently skipped or silently degraded. Every
  step that changed behavior on its new major carries an **explicit** input rather than relying on a
  default — specifically `digest-mismatch` on all 14 `download-artifact` sites. If a required input
  changed on a target major, the workflow is updated so the job still succeeds.
- **AC-007 (the Red gate)**: `.github/scripts/check-action-versions.sh` **fails on `main`** with 124
  violations across 51 files, and **passes** after the sweep. It asserts three properties, each
  catching a different failure class: (1) no in-scope reference below its target major; (2) every
  in-scope SHA pin is a known target SHA *and* its trailing version comment agrees; (3) every
  `download-artifact` reference declares `digest-mismatch` explicitly.
  **Resolved 2026-08-24 (dev decision):** both the guard script and the `cicd_pr_actions-lint.yml` job
  ship in this PR as **batch 0, one self-contained droppable commit**, modeled on
  `cicd_pr_skill-lint.yml`. It is **not** marked a required status check in this PR — land it, let it
  run green for a week, flip required in a follow-up.
- **Verification method** — no unit/integration/Postman/Jest layer exists for workflow YAML, so
  Principle V is satisfied by naming what does apply and recording, here, why the classic layers
  cannot:
  1. **Red → Green (the primary test):** `.github/scripts/check-action-versions.sh` — exits 1 with 124
     violations on `main`, exits 0 after the sweep. Committed, reproducible, dev-approvable. The
     failing run is linked in the PR body.
  2. **Regression net:** `actionlint` over `.github/workflows/` — catches malformed `uses:`, broken
     `${{ }}`, invalid `needs:`/`if:` across 51 hand-edited files. Gate on **no *new* findings**
     against a baseline captured on `main`, not on zero findings. `actionlint` is deliberately *not*
     the version test: its popular-actions database is keyed `owner/repo@vN` and silently skips
     versions it does not know. `.mise.toml:23-26` declares `actionlint` + `shellcheck` but neither
     is installed by default — `brew install actionlint shellcheck` or
     `docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint`. `shellcheck` findings are
     pre-existing noise; this change touches no `run:` block.
  3. **Acceptance assertion:** scan the PR run's annotations via `gh api` for
     `Node\.js (16|20) .* deprecated`. Red against the last pre-change `cicd_1-pr.yml` run on `main`;
     Green (in-scope actions only) against this PR's run.
  4. **`workflow_dispatch` dry-runs** for cold-path workflows a PR cannot reach, via
     `gh workflow run <file> --ref 36850-…`: `utility_discover-docker-tags.yml`,
     `cicd_manual_build-docker-context.yml`, `cicd_manual_build-java-base.yml`,
     `issue_manual_label-issues.yml`, `cicd_scheduled_qa-stuck-check.yml`,
     `cicd_scheduled_opensearch-phase-sweep.yml`, `cicd_scheduled_image-cve-scan.yml`, and
     **`cicd_manual_publish-starter.yml`** — the repo's only macOS job and the least-tested path for
     `node24` actions.
  5. **Explicitly unverifiable pre-merge, and why** — recorded on the record as Principle V requires:
     - **`cicd_publish-pr-test-image.yml`**: `pull_request_target` reads its workflow definition from
       the **base branch**, so a branch edit is inert until merge. No testing on this branch can reach
       it; its callee's `download-artifact` bump only becomes reachable post-merge, via the
       "PR: docker image" label on a future PR.
     - **`cicd_post-workflow-reporting.yml`**: needs a real completed `workflow_run` for 'PR Check';
       its fork path needs a fork PR.
     - **Release/LTS/nightly** (`cicd_6-release.yml`, `cicd_5-lts.yml`, `cicd_4-nightly.yml`,
       `cicd_release-cli.yml`, `cicd_release-sdk.yml`, `cicd_comp_release-*`, `legacy-release_*`):
       tag/schedule-gated. **Must be human-reviewed by someone who knows the release process** —
       automation cannot cover them.
     - **`publish_docs.yml`, `legacy-release_sbom-generator.yaml`**: `disabled_manually` on GitHub,
       structurally unverifiable.
  6. **Post-merge watch window, 72 hours, no release cut inside it:** first `cicd_2-merge-queue.yml`
     (~741 runs/90d), first `cicd_3-trunk.yml` (~452), first `cicd_4-nightly.yml`, and the first
     fork PR.

## Assumptions

- Verified 2026-08-24: the sequencing gate is clear. #36838 merged 2026-08-04 (`55a3965701`) and
  #35930 closed 2026-08-06; this branch is cut from `main` at `88af0bad55`.
- Verified 2026-08-24 via `gh api`: the target versions above are the **latest** majors of each
  action, and each declares `runs.using: node24`. If a newer `node24` major ships before
  implementation, the newer one is acceptable per the issue's own wording — but its release notes must
  be read first, since three of the majors in this set carry real behavior changes
  (`download-artifact` v8, `checkout` v7, `setup-node` v5/v6).
- **Re-resolve all target SHAs immediately before editing** (`gh api repos/actions/checkout/commits/v7.0.1 --jq .sha`).
  124 edits inherit any transcription error, and a wrong-but-valid SHA fails at runtime in a
  maximally confusing way.
- Verified 2026-08-24: `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION` appears nowhere in the repository.
- Verified 2026-08-24: `publish_docs.yml` and `legacy-release_sbom-generator.yaml` are
  `disabled_manually` with 0 runs in 90 days.
- `legacy-release_comp_maven-build-docker-image.yml:124` pins `actions/cache/restore@v3`, which targets
  the v1 cache service shut down in February 2025 — it is already dead code, so bumping it is free.
- The PR ships as **Spec-Kit PR 2**; this `spec.md` ships alone as **PR 1** and needs another
  developer's **approval** (not merge) before `/speckit-plan` runs. From batch 0 until the sweep
  completes, PR 2 is **intentionally red** — the Red gate requires it. This must be stated in the PR
  description so the guard is not "helpfully" reverted.
- PR title (squash-merged, becomes the `main` commit):
  `chore(ci): upgrade GitHub Actions to Node 24 runtime majors (#36850)`.
