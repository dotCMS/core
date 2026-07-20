# Implementation Plan: Automated Release Changelog Publishing to dev.dotcms.com

**Branch**: `36605-changelog-site-publish` | **Date**: 2026-07-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/36605-changelog-site-publish/spec.md`

**Note**: This is the dotCMS override of the plan template (resolved from
`.specify/templates/overrides/plan-template.md`). It adds a **Legacy Impact**
section and a mandatory **ADR Alignment (Gate)** to the stock Spec-Kit plan.

## Summary

When a current-track dotCMS GA release is published, deliver its changelog entry to the
public site (dev.dotcms.com/docs/changelogs) with no human step: generate site-format
markdown release notes from the same data the GitHub release notes already use, then
upsert-by-version a `Dotcmsbuilds` contentlet on the corpsites-headless backend and fire
the System Workflow Publish action so it goes live.

Two responsibilities, cleanly split:

1. **Generation** — site-format markdown, produced from the *existing* deterministic
   release-data gatherer (`.github/scripts/gather-release-data`) via a new site-specific
   prompt template. One data source feeds both GitHub and the site (the spec's
   drift-prevention strategy).
2. **Delivery** — a new Python CLI package `changelog-publisher` (mirroring the
   `evergreen-tracks` house pattern) that does the idempotent search → upsert → publish
   against the Workflow API, with human-edit protection and a non-zero exit that drives a
   Slack failure notification.

Wiring is a new reusable phase called from `cicd_6-release.yml` after `release-notes`,
gated on the release-prepare `is_latest` output (which already encodes "current-track GA,
not CLI, not LTS") and marked non-blocking so a publish failure never fails the release.

## Technical Context

**Language/Version**: Python ≥3.12 (delivery tool); TypeScript 5.x (existing generation
data-gatherer, reused unchanged); GitHub Actions YAML (wiring). No Java, no Angular.

**Primary Dependencies**: `requests` (HTTP to the Workflow API — same dependency and call
style as `evergreen-tracks/executor.py`). Dev: `pytest` + `responses` (HTTP mocking, the
same test stack `evergreen-tracks` uses). Generation reuses `anthropics/claude-code-action@v1`
and the Node data-gatherer already in the repo.

**Storage**: None local. Remote state is the `Dotcmsbuilds` content type on
`https://corpsites-headless.dotcms.cloud` (one contentlet per released version; the same
record set also drives the site downloads listing — hence strict upsert, no duplicates).

**Testing**: `uv run pytest` unit tests with `responses`-mocked HTTP (search hit / miss,
upsert vs create decision, human-edit-protection skip, `disabledWYSIWYG` payload shape,
idempotent re-run, exclusion filter, error → non-zero exit). No JUnit/Postman/Karate/e2e —
this feature ships no dotCMS server code and no REST endpoint (see Test Strategy for the
explicit "cannot implement" declaration and why).

**Target Platform**: GitHub Actions ubuntu runner inside the `dotCMS/core` release
pipeline. Runs as CI tooling, not inside the dotCMS server.

**Project Type**: CI/release tooling (Python package under `.github/actions/core-cicd/` +
a reusable workflow). Not a backend or frontend feature.

**Performance Goals**: SC-001 — entry live within 30 minutes of release. The publish step
is a handful of HTTP calls; the binding latency is the release pipeline itself. No perf
tuning needed.

**Constraints**: Idempotent / self-healing (FR-004); zero duplicate rows (FR-003,
downloads registry shares the record set); markdown preserved via `disabledWYSIWYG`
(FR-005); never overwrite human-edited entries (FR-011); publish only the triggering
release, no backfill (FR-012); never block the release on failure (FR-008); no hardcoded
secrets, never log the token (Constitution III).

**Scale/Scope**: ~470 existing `Dotcmsbuilds` rows; one new/updated row per release
(roughly weekly cadence, plus hotfixes). Single-tenant, single content type.

## Legacy Impact

*dotCMS is a mixed-age codebase — see Constitution Principle I.*

- **Touches legacy?**: No. This feature adds nothing under `com.dotmarketing.*` or
  `com.dotcms.*` — no dotCMS server/Java code at all. It is CI/release tooling
  (`.github/actions/core-cicd/`, `.github/scripts/`, `.github/workflows/`) that talks to an
  already-deployed dotCMS instance over its public Workflow REST API.
- **Modern vs legacy placement**: New Python package sits beside the existing
  `evergreen-tracks` package under `.github/actions/core-cicd/`, matching that established
  layout (src/ + tests/ + pyproject + uv). Generation extends the existing
  `.github/scripts/gather-release-data` tooling rather than introducing a parallel path.
- **Backward compatibility / migration**: No DB schema, no ES/OpenSearch mapping, no dotCMS
  REST contract changes — the `Dotcmsbuilds` content type and the site's rendering are
  untouched; we write into the record set the site already reads. The manual CMS-entry path
  keeps working unchanged (the automation is additive, never a gate — Legacy Considerations
  in the spec). Not a rollback-unsafe change (per docs/core/ROLLBACK_UNSAFE_CATEGORIES.md):
  no persistent core state is altered; a bad run is corrected by an idempotent re-run.
- **Progressive enhancements**: The one file touched in-place — the release workflow
  `cicd_6-release.yml` — gets only an additive job + the new phase call; no rewrite of
  existing phases. The Node data-gatherer is reused as-is (only a new prompt-template file
  is added alongside it).

## Test Strategy (TDD — mandatory)

*Constitution Principle V: no implementation code before tests are written,
developer-approved, and confirmed failing (Red).*

| Component / behavior | Test type(s) | Where | Notes |
|----------------------|--------------|-------|-------|
| Search-by-version parsing (locate existing entry from `_search` response, `_dotraw` exact match) | Unit (pytest + `responses`) | `.github/actions/core-cicd/changelog-publisher/tests/test_client.py` | Covers hit, miss, and multi/ambiguous results |
| Upsert decision (create vs update-in-place; exactly one row) | Unit | `tests/test_publisher.py` | FR-003, FR-004, SC-003 |
| Human-edit protection (skip when `modUserName` ≠ service account; `--force` overrides) | Unit | `tests/test_publisher.py` | FR-011, SC-006 |
| Publish payload shape (`disabledWYSIWYG: ["releaseNotes"]`, correct `minor`/`dockerImage`/`releasedDate`/`showInChangeLog`/`lts`, fire Publish action id) | Unit | `tests/test_publisher.py` | FR-005, FR-006 — the disabledWYSIWYG regression guard |
| Idempotent re-run (two runs + a notes edit → one row, latest notes) | Unit | `tests/test_publisher.py` | FR-004, US2 |
| Exclusion filter defense-in-depth (CLI / LTS / non-current-track version string rejected even if invoked) | Unit | `tests/test_cli.py` | FR-007 — the tool refuses out-of-scope versions independent of the workflow guard |
| Failure → non-zero exit / no secret in logs | Unit | `tests/test_cli.py` | FR-008 drives Slack; Constitution III |
| Site-format markdown template (headings, per-item `[#N](url)` links, anchors, no emoji) | Golden-file assertion | `.github/scripts/gather-release-data/` jest suite OR a fixture check in `changelog-publisher` | FR-002, FR-010, SC-004 — the generation half; see open question on placement |

- **Tests that cannot be implemented**: **JUnit integration, Postman, Karate, and e2e are
  not applicable** and are deliberately omitted. Rationale (Constitution V requires this be
  stated explicitly, not silent): this feature ships no dotCMS server code, no JAX-RS
  endpoint, and no `com.dotcms.*`/`com.dotmarketing.*` change — those suites exercise the
  running dotCMS server, which this feature does not modify. The behavior lives entirely in
  CI Python + workflow YAML and is fully covered by `responses`-mocked pytest unit tests
  (the same layer `evergreen-tracks` is tested at). A true end-to-end publish against the
  live corpsites backend is validated once manually via the quickstart dry-run then
  `--apply` against a throwaway version, documented as an operational check rather than an
  automated test (there is no non-prod corpsites instance to target from CI). **This
  omission requires developer sign-off at the task gate.**

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Legacy-Aware Development** — PASS. No legacy touched; new code placed in the modern
  CI-tooling location beside `evergreen-tracks`. Legacy Impact section completed above.
- **II. Configuration & Logging Discipline** — PASS (with scope note). The Java-specific
  rules (`Config.getStringProperty`, `Logger`, `bom/pom.xml`) do not apply — no Java here.
  The spirit (no ad-hoc config, no secret leakage) is honored: the site token arrives only
  as a GitHub secret → env var, is read once, never logged, and is not written to
  `GITHUB_OUTPUT`/step summary. Python logging mirrors `evergreen-tracks` (`logging`, info
  level, no payload/token dumps).
- **III. Security by Default** — PASS. No hardcoded secrets (new managed secret
  `DOTCMS_DEVSITE_TOKEN`, FR-009); token never logged; PR body/title text that flows into
  generated notes is already treated as untrusted by the existing prompt template and stays
  data-only on the site (stored, not executed). Input (version string) validated before any
  network call.
- **IV. Contract Correctness** — N/A. No dotCMS REST endpoint added or changed; no
  `openapi.yaml`/`@Schema` surface. We are a *client* of the existing Workflow API.
- **V. Test-First / TDD** — PASS. Test Strategy above is authored first; `/speckit-tasks`
  will order every story Tests → Approval GATE → Red GATE → Implementation, and the
  "cannot implement" declaration for JUnit/Postman/Karate/e2e is explicit and slated for
  developer sign-off (not a silent default).

No unjustified violations. Complexity Tracking table is empty.

## ADR Alignment (Gate)

*GATE: completed before Phase 1 design. ADRs live ONLY in `dotCMS/platform-adrs`.*

**Step 1 — Consult existing ADRs (mandatory).** The mandatory `before_plan` hook
`.specify/scripts/bash/adr-context.sh` ran successfully (exit 0) and returned the full
`dotCMS/platform-adrs` INDEX (19 ADRs). Keywords considered: release, changelog, versioning,
evergreen, secrets, cicd, external-integration, content, publish. The index was reviewed in
full; the entries below are the only ones with any bearing on this plan.

### Relevant existing ADRs

| ADR | Title | Status | How it constrains / informs this plan |
|-----|-------|--------|----------------------------------------|
| [ADR-0019](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0019-sdk-cms-date-lockstep-versioning.md) | Date-Lockstep Versioning for the dotCMS SDKs | accepted | Confirms the CalVer `yy.mm.dd-##` current-track vs `_lts_` split this plan keys its exclusion on. The `minor` field value = release-prepare `release_version` (no `v` prefix); the CLI/LTS exclusion (FR-007) reuses the pipeline's existing `is_latest` regex rather than inventing tag parsing. No conflict. |
| [ADR-0003](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0003-secret-naming-conventions-deprecated.md) | AWS Secrets Manager Path Naming Conventions | accepted | Governs *AWS Secrets Manager / K8s ESO* secret paths, not GitHub Actions repo/org secrets, so it does not bind this plan's credential. Recorded to show it was checked: the site token is a GitHub Actions secret and follows the repo's existing SCREAMING_SNAKE convention (`CI_MACHINE_TOKEN`, `DEV_REQUEST_TOKEN`, `ANTHROPIC_API_KEY`) → `DOTCMS_DEVSITE_TOKEN`. |
| [ADR-0013](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0013-skip-integration-and-postman-tests-for-frontend-only-changes-in-merge-queue-to-increase-flow.md) | Skip Integration and Postman Tests for Frontend-Only Changes in Merge Queue | accepted | Reinforces that not every change owes integration/Postman coverage; supports this plan's explicit, justified omission of those suites for CI-only tooling (see Test Strategy). Informative, not constraining. |

### Conflicts with accepted ADRs

- None. No accepted ADR governs release-note site publishing, the corpsites Workflow-API
  integration, or GitHub Actions secret naming.

### Proposed ADRs (propose only — never created here)

| Proposed title | One-line rationale | Suggested next step |
|----------------|--------------------|--------------------|
| ADR: Automation service-account identity + human-edit-protection convention for CMS-writing pipelines | Establishes "automation writes as a dedicated service account; last-modifier ≠ service account means hands-off" as a reusable pattern (this is the first pipeline to write customer-facing CMS content from CI). | Open in `dotCMS/platform-adrs` via `new-adr.sh` if a second CMS-writing automation appears. |

*One decision is worth recording but is not blocking; proposed only, not authored here.*

## Project Structure

### Documentation (this feature)

```text
specs/36605-changelog-site-publish/
├── plan.md              # This file
├── research.md          # Phase 0 — decisions & alternatives (created this phase)
├── data-model.md        # Phase 1 — Dotcmsbuilds field mapping + API contract (created this phase)
└── tasks.md             # Phase 2 — created by /speckit-tasks, NOT here
```

### Source Code (repository root)

```text
# Delivery — new Python CLI package (mirrors .github/actions/core-cicd/evergreen-tracks)
.github/actions/core-cicd/changelog-publisher/
├── pyproject.toml                        # requests dep; pytest+responses dev; console_script
├── README.md
├── src/changelog_publisher/
│   ├── __init__.py
│   ├── cli.py                            # `changelog-publisher publish` — dry-run default, --apply, --force
│   ├── client.py                         # corpsites HTTP: _search by minor_dotraw, fire workflow action
│   ├── publisher.py                      # upsert decision + human-edit protection + disabledWYSIWYG payload
│   └── version.py                        # current-track validation (defense-in-depth exclusion, FR-007)
└── tests/
    ├── test_cli.py
    ├── test_client.py
    ├── test_publisher.py
    └── fixtures/                         # sample _search hit/miss JSON

# Generation — extend existing deterministic gatherer (no new tool)
.github/scripts/gather-release-data/
└── prompt-template-site.md               # NEW: site editorial format (headings, anchors, no emoji, prose intro)

# Wiring — new reusable phase + one additive call in the release workflow
.github/workflows/
├── cicd_comp_changelog-site-publish-phase.yml   # NEW reusable phase (generate site notes → publish → Slack on fail)
└── cicd_6-release.yml                            # EDIT: add `changelog-site-publish` job after `release-notes`
```

**Structure Decision**: CI/release tooling, not a dotCMS module. The delivery tool is a
standalone `uv`/`pytest` Python package placed beside the proven `evergreen-tracks` package
under `.github/actions/core-cicd/`, reusing its `requests`-based HTTP + `responses` test
idiom. Generation is not a new tool — it adds a single site-format prompt template next to
the existing `gather-release-data` Node gatherer so both GitHub and site notes derive from
one deterministic data source. A new reusable workflow phase mirrors the repo's modular
phase pattern (`cicd_comp_*`) and is invoked non-blocking from `cicd_6-release.yml`.

## Architecture Flow (reference for tasks)

1. **Trigger**: `cicd_6-release.yml` calls a new `changelog-site-publish` job after
   `release-notes` succeeds, gated on `needs.release-prepare.outputs.is_latest == 'true'`
   `&& github.repository == 'dotcms/core'`. That `is_latest` flag is `true` only for the
   current-track `yy.mm.dd-##` format and `false` for `_lts_`/CLI — the deliberate FR-007
   filter, reused not reinvented. The job is `continue-on-error` / `allow_failure: true`
   (FR-008: never blocks the release).
2. **Generate**: reuse `gather-release-data` (deterministic given the two tags) → same JSON
   the GitHub notes used; run `claude-code-action` against `prompt-template-site.md` to
   write `/tmp/site-release-notes.md` in the site's editorial format.
3. **Upsert**: `uv run changelog-publisher publish --version <release_version>
   --notes-file /tmp/site-release-notes.md --docker-image <deployment tag>
   --released-date <today> --apply`. The tool: `POST /api/content/_search` with
   `+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:<version>`; if a row exists and its
   `modUserName` ≠ the service account → **skip + emit skip reason** (FR-011); otherwise fire
   `PUT /api/v1/workflow/actions/b9d89c80-3d88-4311-8365-187323c96436/fire` with the field
   set + `"disabledWYSIWYG": ["releaseNotes"]` (create when absent, update-in-place when
   present — FR-003/004). `showInChangeLog=true`; `lts` set to the current-track value read
   from an existing current-track entry (open question — confirm the exact value before
   hardcoding).
4. **Publish semantics**: System Workflow (scheme d61a59e1-a49c-46f2-a929-db2b4bfa88b2) has
   no approval step, so the fire call makes the entry live immediately (FR-001, US1
   scenario 3).
5. **Human-edit protection**: last-modifier check above; `--force` is the explicit operator
   override, exposed only via manual re-run, never set by the release pipeline (FR-011).
6. **Slack**: on non-zero exit (failure) OR a protective skip, post to `#dot-releases` via
   the existing `notify-slack` composite action (naming version + reason). The publish job's
   failure is caught and converted to a notification without failing the release (FR-008,
   US3).
7. **Secret**: `DOTCMS_DEVSITE_TOKEN` (new managed GitHub secret; provisioning is an
   operational prerequisite, not code — FR-009). Passed as job env, read once by the tool,
   never logged. (Confirmed the existing `DEV_REQUEST_TOKEN` is a Docker build-arg for the
   dev image, unrelated — a new secret is genuinely required.)

## Complexity Tracking

> Fill ONLY if Constitution Check or ADR Alignment has violations that must be justified.

None. No principle violations; no accepted-ADR conflicts.
