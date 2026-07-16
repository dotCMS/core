---

description: "Task breakdown — Automated Release Changelog Publishing to dev.dotcms.com (#36605)"
---

# Tasks: Automated Release Changelog Publishing to dev.dotcms.com

**Input**: Design documents from `/specs/36605-changelog-site-publish/`

**Prerequisites**: plan.md (APPROVED — architecture is fixed, do not redesign), spec.md, research.md, data-model.md

**Feature**: When a current-track dotCMS GA release is published, generate site-format markdown release
notes from the existing release-data gatherer and upsert-by-version a `Dotcmsbuilds` contentlet on the
corpsites-headless backend, firing the System Workflow Publish action so it goes live — idempotently,
protecting human edits, never blocking the release, and announcing failures/skips in `#dot-releases`.

**Tests (TDD — MANDATORY)**: Per Constitution Principle V, **no implementation code is written before**
(1) tests are written, (2) the developer validates and approves them — or explicitly states which test
type cannot be implemented and why — and (3) the tests are confirmed to **FAIL (Red)**. Every user-story
phase encodes these three gates as tasks. Do not delete the gate tasks and do not reorder implementation
ahead of them.

**Test-type applicability (SIGNED OFF by the session lead — record, do not re-ask)**: This feature ships
**no dotCMS server code, no JAX-RS endpoint, and no `com.dotcms.*`/`com.dotmarketing.*` change**. It is CI
tooling: a Python CLI package plus workflow YAML plus one generation prompt template. Therefore **JUnit
integration, Postman, Karate, and e2e are not applicable and are deliberately omitted** — those suites
exercise a running dotCMS server this feature does not touch. All behavior is covered by `pytest` +
`responses`-mocked HTTP unit tests (the layer `evergreen-tracks` is tested at) plus a golden-file check
for the generated markdown. A true end-to-end publish against the live corpsites backend is validated once
manually (quickstart dry-run then `--apply` against a throwaway version) as an operational check, since
there is no non-prod corpsites instance reachable from CI. This omission is the developer decision
Constitution V requires; it is recorded here so the approval gates below reference it rather than blocking.

**Organization**: Tasks are grouped by user story (US1 auto-publish P1, US2 idempotency + human-edit
protection P2, US3 Slack notifications P3) to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3), or SETUP/FOUND/POLISH
- **[GATE]**: A blocking checkpoint that requires developer action; never auto-satisfy it
- Exact file paths are given relative to the repository root

## Path Conventions (this feature — CI/release tooling, not a dotCMS module)

- **Delivery tool (Python)**: `.github/actions/core-cicd/changelog-publisher/`
  (`src/changelog_publisher/`, `tests/`, `pyproject.toml`) — mirrors the `evergreen-tracks` package.
- **Generation**: `.github/scripts/gather-release-data/` — reused unchanged; one new prompt template
  `prompt-template-site.md` added alongside `prompt-template.md`.
- **Wiring**: `.github/workflows/cicd_comp_changelog-site-publish-phase.yml` (new reusable phase) and
  `.github/workflows/cicd_6-release.yml` (edited: one additive job).
- **Slack**: reuse `.github/actions/core-cicd/notification/notify-slack/`.

---

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 [SETUP] Scaffold the Python package `.github/actions/core-cicd/changelog-publisher/`
        mirroring `.github/actions/core-cicd/evergreen-tracks/`: create `pyproject.toml` (requires-python
        `>=3.12`, runtime dep `requests`, dev deps `pytest`+`responses`, a `[project.scripts]`
        `changelog-publisher = "changelog_publisher.cli:main"` console entrypoint), `README.md`, the
        `src/changelog_publisher/__init__.py` package marker, and an empty `tests/` with `tests/fixtures/`.
        No logic yet — just the layout + build config so `uv run` resolves.
- [x] T002 [P] [SETUP] Confirm the test harness runs green-empty: `uv run pytest` from
        `.github/actions/core-cicd/changelog-publisher/` collects zero tests and exits 0 (proves the uv
        env + pytest + responses install, matching how the release workflow invokes `evergreen-tracks`).
- [x] T003 [P] [SETUP] Add `.gitignore` coverage for `.venv/`, `.pytest_cache/`, and `__pycache__/` under
        the new package (mirror `evergreen-tracks`) so the venv/cache dirs are never committed.

**Checkpoint**: Empty but runnable package; no product logic.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete. These four tasks resolve the
plan's three open questions plus the exit-code contract — all of which gate implementation *details* and
must be settled by **inspection of ground truth, not assumption**.

- [x] T004 [FOUND] **[Open question 1 — exact contentlet field values]** Query the live corpsites-headless
        backend for a recent current-track `Dotcmsbuilds` entry (e.g. `minor` = `26.06.30-01`) via
        `POST /api/content/_search` with `+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:<version>`.
        Record the **exact stored values** of `lts`, `released`, `download`, and `showInChangeLog` for a
        current-track row (the frontend filters `+Dotcmsbuilds.lts:3` for CURRENT — confirm whether `3` is
        the stored contentlet value or only the GraphQL filter value, since they can differ). Also capture
        the `modUserName` string of an automation-written vs hand-authored entry so the service-account
        identity used by human-edit protection (US2) is exact. Write the confirmed values into
        `specs/36605-changelog-site-publish/data-model.md`'s field table (replacing the "confirm" / "open"
        placeholders) so the publisher hardcodes verified constants, not guesses. Do NOT proceed to US1
        implementation with placeholder values.
- [x] T005 [FOUND] **[Open question 3 — skip-vs-failure signaling contract]** Define and document the
        tool's exit-code / stdout contract in `.github/actions/core-cicd/changelog-publisher/README.md`
        *before* any CLI code: **exit 0** for success (created/updated/published) **and** for a protective
        skip, where a skip is distinguished by a machine-readable marker on stdout (e.g. a single line
        `::changelog-skip::<version> reason=<reason>` or a JSON `{"result":"skipped",...}` object);
        **non-zero exit** only for real failures (network/auth/payload error, `>1` search hit). This split
        is what lets the workflow post *different* Slack wording for skips vs failures (FR-008, US3) while
        a skip keeps the run green. The contract must be authored here so US2's human-edit-protection tests
        and US3's Slack-branching tests assert against a fixed spec, not an ad-hoc one.
- [x] T006 [FOUND] **[Open question 2 — golden-file placement decision]** Decide and record (in the new
        package `README.md` and as a one-line note in `research.md` D2) where the site-format markdown
        golden-file test lives: **either** the existing jest suite at `.github/scripts/gather-release-data/`
        (which already has `categorize.test.ts` / `github.test.ts`, so the fixture sits with the generator
        it tests) **or** a fixture check inside `.github/actions/core-cicd/changelog-publisher/tests/`.
        Recommendation to record unless a reason emerges otherwise: place it in the `gather-release-data`
        jest suite, because generation (the prompt template) is what the golden file guards and it keeps
        the Python tool free of Node/markdown-generation concerns. State the chosen location and the why;
        US1's generation test (T016) targets exactly that path.
- [x] T007 [P] [FOUND] Establish shared tool scaffolding used by all stories: `logging` setup mirroring
        `evergreen-tracks` (info level, **never** log the bearer token or full payloads — Constitution II/III),
        base-URL/`DOTCMS_DEVSITE_TOKEN` env read-once helper, and a `requests.Session` with the
        `Authorization: Bearer` header, in `src/changelog_publisher/client.py`. Skeleton only (no request
        methods yet) so US1 tests have a seam to mock via `responses`.

**Checkpoint**: All three open questions resolved from ground truth; exit-code contract fixed; shared
config/logging seam in place. Stories may begin.

---

## Phase 3: User Story 1 - Release notes appear on the site automatically (Priority: P1) 🎯 MVP

**Goal**: A published current-track GA release produces a live changelog entry on
dev.dotcms.com/docs/changelogs — correct version, availability date, docker tag, and site-format
categorized notes — with no human step. This is the create-and-publish happy path plus the generation
half and the workflow wiring. (Update-in-place, idempotency, human-edit protection, and Slack come in
US2/US3.)

**Independent Test**: Replay a release event for a known version with no pre-existing entry; verify a live
changelog entry appears with correct version/date/docker-tag/formatted notes and no manual CMS step.

### Tests for User Story 1 (MANDATORY — write FIRST)

- [x] T010 [P] [US1] `responses`-mocked unit test for search-by-version parsing in
        `.github/actions/core-cicd/changelog-publisher/tests/test_client.py`: `_search` returns 0 hits
        (create path) — assert the query string is `+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:<version>`
        with `limit: 2`, and that results are read from `entity.jsonObjectView.contentlets`. Include a
        1-hit fixture in `tests/fixtures/` for later stories.
- [x] T011 [P] [US1] Unit test for the **create-path publish payload shape** in `tests/test_publisher.py`:
        firing `PUT /api/v1/workflow/actions/b9d89c80-3d88-4311-8365-187323c96436/fire` sends
        `contentlet` with `contentType:Dotcmsbuilds`, `minor`, `releaseNotes`, `dockerImage`,
        `releasedDate`, `showInChangeLog`, `lts` (the confirmed T004 values), **no `identifier`** (create),
        and — the regression guard — **`"disabledWYSIWYG": ["releaseNotes"]` always present** (FR-005).
- [x] T012 [P] [US1] Unit test for **exclusion filter defense-in-depth** in `tests/test_cli.py`
        (`src/changelog_publisher/version.py`): a current-track `yy.mm.dd-##` string is accepted; `_lts_`
        forms and CLI (`dotcms-cli-*`) forms are **rejected before any network call** with a non-zero exit
        (FR-007). The tool refuses out-of-scope versions independently of the workflow guard.
- [x] T013 [P] [US1] Unit test for **dry-run-by-default / `--apply`** in `tests/test_cli.py`: without
        `--apply` no fire call is issued (only `_search` may run) and the intended action is printed;
        with `--apply` the fire call is issued. Mirrors the `evergreen-tracks` safety convention.
- [x] T014 [P] [US1] Golden-file test for the **site-format markdown** at the location chosen in T006
        (default: `.github/scripts/gather-release-data/` jest suite): assert the rendered notes use the
        site editorial format — section headings (Features / Enhancements & Adjustments / Fixes) with
        anchors like `{#Fixes-<version>}`, per-item `[#NNNNN](github-url)` links, a short prose intro, and
        **no emoji** (FR-002, FR-010, SC-004).

  > **Ground-truth deviation (approved by lead):** live corpsites entries (verified 26.06.30-01 / 26.06.02-01) use a prose intro, `### Section {#Anchor-<version>}` headings, and double-bracket **issue** links `[[#N](https://github.com/dotCMS/core/issues/N)]` — not the `[#N](.../pull/N)` wording above. The tool/template follow the live format so published entries are visually indistinguishable from hand-authored ones (SC-004).

- [x] T015 [US1] [GATE] **Developer approval** — present the US1 test set (T010–T014) for review. The
        JUnit/Postman/Karate/e2e omission is already signed off (see the header block); record that
        acknowledgement here and proceed on explicit approval of the unit + golden-file coverage. Do not
        continue without an explicit decision.
- [x] T016 [US1] [GATE] **Red** — run the approved tests and confirm they **FAIL for the intended reason**
        (missing implementation, not import/collection errors). Record the failing output. Do NOT write
        implementation until Red is confirmed.

### Implementation for User Story 1 *(only after T015 + T016 pass)*

- [x] T017 [P] [US1] Implement `src/changelog_publisher/version.py`: current-track CalVer validation
        (`^[0-9]{2}\.[0-9]{2}\.[0-9]{2}-[0-9]{1,2}$`), rejecting `_lts_` and CLI forms — make T012 green.
- [x] T018 [US1] Implement `_search` in `src/changelog_publisher/client.py` (query build, `limit: 2`,
        parse `entity.jsonObjectView.contentlets`, return 0/1/>1 with identifier + `modUserName` when
        present) — make T010 green.
- [x] T019 [US1] Implement the **create + fire-Publish** path in `src/changelog_publisher/publisher.py`:
        build the contentlet payload with the confirmed field values (T004) and the always-present
        `disabledWYSIWYG: ["releaseNotes"]`, fire the System Workflow Publish action, no `identifier` on
        create — make T011 green. (Update-in-place is deferred to US2.)
- [x] T020 [US1] Implement `src/changelog_publisher/cli.py` `publish` subcommand: args `--version`,
        `--notes-file`, `--docker-image`, `--released-date`, dry-run default + `--apply`; wire
        version-validation → search → create-publish — make T013 green.
- [x] T021 [P] [US1] Add `.github/scripts/gather-release-data/prompt-template-site.md`: the site editorial
        format (headings + anchors, `[#N](url)` links, prose intro, no emoji) distinct from the GitHub
        `prompt-template.md` — make T014 green.
- [x] T022 [US1] Create the reusable phase `.github/workflows/cicd_comp_changelog-site-publish-phase.yml`:
        (a) run `gather-release-data` → JSON, (b) run `claude-code-action` against `prompt-template-site.md`
        to write `/tmp/site-release-notes.md` — resolve **open question D2a** here (recommended: a second
        lightweight `claude-code-action` step, leaving the existing GitHub-notes step untouched and the site
        path independently retryable; record the choice in a workflow comment), (c) `uv run
        changelog-publisher publish ... --apply` with `DOTCMS_DEVSITE_TOKEN` passed as job env (read once,
        never echoed to `GITHUB_OUTPUT`/step summary). Slack wiring is added in US3 (T033).
- [x] T023 [US1] Edit `.github/workflows/cicd_6-release.yml`: add a `changelog-site-publish` job after
        `release-notes`, gated on `needs.release-prepare.outputs.is_latest == 'true' && github.repository
        == 'dotcms/core'` (the deliberate FR-007 current-track filter, reused not reinvented), calling the
        new phase. Additive only — no rewrite of existing phases.
- [x] T024 [US1] Confirm T010–T014 now **PASS**; refactor for clarity; verify no token/payload is logged.

**Checkpoint**: A release with no pre-existing entry publishes a correctly-formatted, live changelog entry
automatically. US1 is independently testable (create + publish happy path + wiring).

---

## Phase 4: User Story 2 - Re-runs are safe and self-healing (Priority: P2)

**Goal**: Re-running publish for a version updates the existing entry in place (never a second row), the
latest notes win, and an entry last modified by a human is protected — skipped, not overwritten (override
only via explicit `--force`).

**Independent Test**: Run publish twice for the same version, then again with modified notes; verify
exactly one entry exists reflecting the latest notes. Separately, seed an entry whose `modUserName` is a
human and verify the tool skips with the skip marker instead of overwriting.

### Tests for User Story 2 (MANDATORY — write FIRST)

- [x] T025 [P] [US2] Unit test for the **update-in-place** decision in `tests/test_publisher.py`: a 1-hit
        search whose `modUserName` = the service account fires Publish **with the found `identifier`** in
        the payload — asserting no create/second row (FR-003, SC-003).
- [x] T026 [P] [US2] Unit test for **idempotent re-run** in `tests/test_publisher.py`: two successive runs
        plus a notes edit converge to a single row carrying the latest notes; the second run takes the
        update path, not create (FR-004, US2).
- [x] T027 [P] [US2] Unit test for **human-edit protection** in `tests/test_publisher.py`: a 1-hit search
        whose `modUserName` ≠ service account → **skip** (no fire call), emitting the skip marker from the
        T005 contract; with `--force`, the same case updates in place (FR-011, SC-006).
- [x] T028 [P] [US2] Unit test for the **`>1` hit guard** in `tests/test_publisher.py`: a search returning
        two contentlets → error / non-zero exit (never guess which to update) — feeds the US3 failure path.

- [ ] T029 [US2] [GATE] **Developer approval** of the US2 test set (T025–T028). Test-type omission already
        signed off (header block); record acknowledgement and proceed on explicit approval.
- [ ] T030 [US2] [GATE] **Red** — confirm US2 tests FAIL for the intended reason before implementing.
        Record the failing output.

### Implementation for User Story 2 *(only after T029 + T030 pass)*

- [ ] T031 [US2] Extend `src/changelog_publisher/publisher.py` with the full decision table from
        `data-model.md`: 0 hits → create (US1); 1 hit + service account → update-in-place with
        `identifier`; 1 hit + other user + no `--force` → skip + emit skip marker; 1 hit + other user +
        `--force` → update; `>1` hit → error. Make T025–T028 green.
- [ ] T032 [US2] Add `--force` to `src/changelog_publisher/cli.py`, reachable only via manual operator
        re-run (the release workflow in T022/T023 never passes it). Confirm T025–T028 **PASS**.

**Checkpoint**: US1 AND US2 work; exactly one row per version across any number of runs; human edits safe.

---

## Phase 5: User Story 3 - Failures and skips are announced in Slack, not silent (Priority: P3)

**Goal**: A publish failure OR a protective skip posts to `#dot-releases` (naming the version and reason,
with different wording per the T005 contract), and the publish job never blocks or fails the product
release.

**Independent Test**: Run publish with an invalid credential → a failure notice lands in `#dot-releases`
while the release process is unaffected. Separately, trigger a human-edit skip → a skip notice (distinct
wording) lands in the same channel.

### Tests for User Story 3 (MANDATORY — write FIRST)

- [ ] T033 [P] [US3] Unit test for the **failure exit contract** in `tests/test_cli.py`: a network/auth
        error (e.g. mocked 401) and a payload-rejected (`4xx`) response each produce a **non-zero exit**,
        and — Constitution III guard — the bearer token never appears in captured stdout/stderr/logs
        (FR-008).
- [ ] T034 [P] [US3] Unit test asserting the **skip vs failure distinction** on stdout per the T005
        contract in `tests/test_cli.py`: a protective skip exits 0 with the skip marker; a real failure
        exits non-zero with no skip marker — so the workflow can branch to different Slack wording.

- [ ] T035 [US3] [GATE] **Developer approval** of the US3 test set (T033–T034). Test-type omission already
        signed off; record acknowledgement and proceed on explicit approval.
- [ ] T036 [US3] [GATE] **Red** — confirm US3 tests FAIL for the intended reason before implementing.

### Implementation for User Story 3 *(only after T035 + T036 pass)*

- [ ] T037 [US3] Finalize CLI exit/stdout behavior in `src/changelog_publisher/cli.py` per the T005
        contract (non-zero on real failure with no secret leakage; exit 0 + skip marker on protective
        skip) — make T033–T034 green.
- [ ] T038 [US3] **[Open question D7a — Slack channel id]** Resolve the numeric channel-id for
        `#dot-releases` (or the repo/org variable that holds it) and record it. The `notify-slack` composite
        action takes an id, not a name.
- [ ] T039 [US3] Wire Slack into `.github/workflows/cicd_comp_changelog-site-publish-phase.yml`: mark the
        publish job `continue-on-error` / `allow_failure: true` (FR-008 — never fails the release), then in
        a following step branch on the tool's exit code + skip marker (T005) to call
        `.github/actions/core-cicd/notification/notify-slack` with the `#dot-releases` channel-id (T038),
        `SLACK_BOT_TOKEN`, and a payload naming the version and reason — **failure wording** for non-zero
        exit, **skip wording** for the skip marker (FR-008, US3 scenarios 1 & 3, SC-005).
- [ ] T040 [US3] Confirm T033–T034 **PASS** and that the release job stays green when the publish step
        fails or skips (verify the `continue-on-error` semantics in the workflow).

**Checkpoint**: All three stories work; failures/skips are never silent and never block the release.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T041 [P] [POLISH] Flesh out `.github/actions/core-cicd/changelog-publisher/README.md`: usage
        (`publish` args, dry-run vs `--apply`, `--force` operator override), the exit-code/skip contract
        (T005), the manual backfill procedure (explicit per-version, no auto-backfill — FR-012), and the
        one-time manual end-to-end validation (quickstart dry-run → `--apply` against a throwaway version).
- [ ] T042 [P] [POLISH] Additional edge-case unit tests surfaced during implementation: "internal
        maintenance only" empty-changes note still produces an entry; hotfix `-02`/`-03` same-day suffixes
        each publish their own row; older-line `-04` patch carries today's date and touches no other row
        (FR-013, spec Edge Cases).
- [ ] T043 [POLISH] Run `uv run pytest` for the full `changelog-publisher` suite and the
        `gather-release-data` jest suite (if the golden file landed there per T006); confirm all green.
- [ ] T044 [POLISH] Security/hygiene pass on touched files: confirm no secret is ever logged or written to
        `GITHUB_OUTPUT`/step summary, input (version string) is validated before any network call, and the
        `.venv`/cache dirs are gitignored (Constitution II/III).

---

## Dependencies & Execution Order

### Within Each User Story (TDD — enforced)

1. **Tests written** (unit + golden-file — the layer-appropriate types; JUnit/Postman/Karate/e2e omitted
   with signed-off rationale in the header) — before any implementation.
2. **[GATE] Developer approval** — explicit; the test-type omission is pre-signed and re-acknowledged, not
   re-litigated.
3. **[GATE] Red** — tests confirmed failing for the right reason.
4. Implementation (Green): version validation → client (`_search`) → publisher (payload/decision) → CLI →
   generation template → workflow wiring.
5. Confirm tests pass; refactor.
6. Story complete before moving to the next priority.

`/speckit-implement` MUST halt at each `[GATE]` task and not write implementation code until the approval
and Red gates are satisfied.

### Phase Dependencies

- Setup (Phase 1) → Foundational (Phase 2, **BLOCKS all stories** — resolves the 3 open questions + exit
  contract) → US1 (Phase 3, MVP) → US2 (Phase 4) → US3 (Phase 5) → Polish (Phase 6).
- US2 depends on US1's `_search` + create path and publisher scaffold. US3 depends on US2's skip signalling
  (the skip marker) and US1's workflow phase.

### Parallel Opportunities

- `[P]` tasks touch different files with no dependencies and may run together (e.g. all US1 test files
  T010–T014; implementation T017/T021).
- Tests within a story marked `[P]` can be written in parallel — but the Approval and Red gates are single
  blocking checkpoints for the whole story.

---

## Notes

- The `[GATE]` tasks are non-negotiable (Constitution Principle V). Never mark them complete on the
  developer's behalf. The JUnit/Postman/Karate/e2e omission is an explicit, recorded developer decision
  (see header) — not a silent default.
- The three plan open questions are front-loaded as blocking Foundational tasks (T004 field values, T005
  exit contract, T006 golden-file placement) plus two wiring-time questions (T022 D2a one-vs-two Claude
  calls, T038 D7a Slack channel-id) — each resolved by inspection, not assumption.
- Builder watch-outs: (1) `disabledWYSIWYG: ["releaseNotes"]` must be present on **every** fire payload —
  T011 is the regression guard; without it markdown collapses to one `<p>`. (2) The publish job must be
  `continue-on-error`/`allow_failure` so a changelog hiccup never fails the software release (FR-008).
  (3) `lts`/`released`/`download` must use the **verified** stored values from T004, not the frontend
  GraphQL filter value (`lts:3` may be a filter artifact, not the stored field). (4) Never log the bearer
  token or write it to `GITHUB_OUTPUT`/step summary.
- Commit after each task or logical group (Red commit, then Green commit encouraged).
