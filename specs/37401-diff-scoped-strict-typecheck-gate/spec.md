# Feature Specification: Diff-scoped strict typecheck gate for `core-web`

**Feature Branch**: `nicobytes/37401-strict-mode-validate-a-diff-scoped-strict-typecheck-gate-to-stop-new-non-strict-code-landing-on-main`

**Created**: 2026-09-04

**Status**: Draft

**Type**: Spike (time-boxed research)

**Related GitHub Issue**: dotCMS/core#37401

**Input**: User description: "https://github.com/dotCMS/core/issues/37401 — strict mode: validate a diff-scoped strict typecheck gate to stop new non-strict code landing on main", extended in conversation to cover Angular template strictness (`angularCompilerOptions`) as a P3 arm of the same spike.

## Problem Statement *(mandatory)*

The `core-web` workspace is only partly strict: the shared TypeScript baseline turns strict
mode **off**, and 22 of 55 project configs opt back in locally. The workspace-wide migration
(PR #37198, 1455 files) is waiting on full-team QA and is not imminent.

While it waits, **new non-strict code keeps landing on `main`**. Every sync from `main` into
the migration branch imports fresh type errors that must be fixed by hand, so the branch's
diff grows and the QA target keeps moving. PR #37262 is the concrete, still-reproducible
example: it landed three strict violations in `sdk-create-app` that had to be repaired on the
branch after a merge.

Per-project opt-in cannot close the gap, and the reason is mechanical, not a matter of will:
the workspace path aliases point at **sources**, not built output, so a project's dependencies
become part of its own compilation program and are checked with **its** flags. Checking the 8
files of `libs/portlets/dot-locales/portlet` drags in 387 files from six dependency libs. Three
of those libs (`dotcms-models`, `data-access`, `ui`) are imported by 585–1130 files each, so
any opt-in upstream of them drowns in inherited errors.

The same shape repeats one layer up, in Angular templates. 30 project configs already declare
`strictTemplates: true`, but the **four applications** — including `dotcms-ui`, the main one —
carry `strictTemplates: false` behind a `TODO(#35930): re-enable once Angular 22 template errors
are fixed per app`. Those apps cannot flip the flag wholesale, so today there is no gate at all
on the layer where the most user-facing code lives.

**The question this spike answers**: can the gate stop *counting* dependency and pre-existing
errors instead of waiting for them to be fixed? That is, run each project's existing
configuration with strictness forced on, then discard every diagnostic whose file is not part of
the pull request's diff. If that works, `main` stops accumulating strict debt today, decoupled
from the migration PR's timeline — and the same filter may extend to template diagnostics, which
is the secondary question this spike also probes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The gate catches new strict debt (Priority: P1)

A contributor opens a pull request that adds TypeScript with a strict-mode violation — an
implicit `any`, a possibly-null dereference, an index-signature access. The evaluation harness,
run over that pull request's diff, reports the violation and fails, naming the file, line and
diagnostic code.

**Why this priority**: This is the whole premise. If diff-scoped filtering cannot surface a
known-real violation on a known-real pull request, the spike ends here with a documented no-go
and nothing else in this spec matters.

**Independent Test**: Replay PR #37262 (base = its merge-base on `main`, head = its merge
commit) through the harness and confirm the three known violations in `sdk-create-app` are all
reported. Delivers a yes/no answer to the research question on its own.

**Acceptance Scenarios**:

1. **Given** PR #37262 replayed at its merge-base and merge commit, **When** the harness runs,
   **Then** it reports all three known violations (one in `src/index.ts`, two in
   `src/utils/readiness.spec.ts`) and exits non-zero.
2. **Given** at least two further merged pull requests that changed TypeScript inside
   non-strict libraries, **When** the harness runs over each, **Then** every reported finding is
   recorded and individually judged real or spurious, with the judgement written down.
3. **Given** any run that reports findings, **When** the output is read, **Then** each finding
   identifies its file, line and diagnostic code, so a contributor can act on it without
   re-running anything.

---

### User Story 2 - The gate does not cry wolf (Priority: P1)

A contributor opens a pull request that introduces no strict debt — a rename, a test-only
change, a change confined to already-strict code. The harness passes silently and costs them
nothing.

**Why this priority**: A gate that blocks merges on noise is worse than no gate; it will be
disabled within a week. Precision is what decides whether this can block on day one, so it is
equal in priority to detection.

**Independent Test**: Replay at least three merged pull requests known to carry no strict debt
and confirm all three pass. Yields the false-positive rate that the day-one blocking decision
turns on.

**Acceptance Scenarios**:

1. **Given** at least three merged pull requests that introduced no strict debt, **When** the
   harness runs over each, **Then** all three pass with zero findings.
2. **Given** any harness run, **When** it completes, **Then** it reports how many diagnostics
   originated in files outside the diff and were therefore discarded — evidencing that the
   filter, not luck, is what makes the run pass.
3. **Given** the full sample of replayed pull requests, **When** the results are tallied,
   **Then** a false-positive rate is recorded and an explicit **go / no-go for blocking on day
   one** is stated, with a named fallback posture if the answer is no-go.

---

### User Story 3 - The operating decisions are settled with measurements (Priority: P2)

Whoever implements the real gate inherits three choices already made and backed by numbers,
rather than having to re-litigate them: which strictness flags to turn on, whether a finding is
scoped to the whole changed file or only the changed lines, and what the gate costs per pull
request.

**Why this priority**: Detection and precision decide *whether* to build the gate; these
decide *what shape* it takes. Getting them wrong makes the gate either toothless or so
unadoptable that touching one line of a legacy file becomes a day's work.

**Independent Test**: Re-run the sample pull requests under each candidate flag set and each
candidate granularity, and confirm the write-up carries a per-option finding count, a wall-clock
measurement, and a single recommendation for each of the three decisions.

**Acceptance Scenarios**:

1. **Given** the sample pull requests, **When** they are run under full strict and again under
   the narrower null-checks/implicit-any subset, **Then** the finding count for each is recorded
   and one flag set is recommended.
2. **Given** the sample pull requests, **When** findings are scoped whole-file and again
   line-level, **Then** the cost of each is quantified — how many extra findings whole-file
   inherits from untouched legacy code — and one granularity is recommended.
3. **Given** a pull request touching one to three projects, **When** the harness runs, **Then**
   wall-clock time is measured and reported against the 2.4s single-project baseline.

---

### User Story 4 - Angular template strictness is assessed and decided (Priority: P3)

A contributor changes an Angular component template in one of the four applications where
template strictness is currently switched off. The harness reports the template's own strict
violations — and only those, not the app's accumulated template debt.

Reaching this requires a second execution mode. Angular's strictness settings are **not**
TypeScript compiler options and cannot be forced from the command line the way `--strict` can;
they must be supplied through configuration the compiler reads. This story establishes whether
that second mode is worth its cost.

**Why this priority**: Templates are where the most user-facing code lives and where there is
currently no gate at all, so the upside is real. But it is a distinct mechanism from the
TypeScript arm, its runtime cost is unmeasured and expected to be materially higher, and the
TypeScript arm must stand on its own regardless of how this resolves. It carries its own
go/no-go and may be deferred to the follow-up task without weakening Stories 1–3.

**Independent Test**: Run the template-aware mode against a merged pull request that changed a
template in one of the four non-strict applications, and confirm it reports that template's
violations while discarding the app's pre-existing template debt. Produces the cost figure and
the recommendation on its own.

**Acceptance Scenarios**:

1. **Given** a project whose configuration disables template strictness, **When** the harness
   runs in template-aware mode, **Then** template strictness is in force for that run **without
   any edit to a version-controlled configuration file**.
2. **Given** a pull request that changed a template in one of the four non-strict applications,
   **When** the harness runs, **Then** violations in the changed template are reported and the
   application's pre-existing template debt is discarded, with the discarded count reported.
3. **Given** the template-aware mode, **When** it runs on a project that is not an Angular
   project, **Then** it falls back to the TypeScript-only mode explicitly, never silently.
4. **Given** the sample pull requests, **When** the template-aware mode runs, **Then** its
   wall-clock cost is measured against the TypeScript-only mode, and the additional Angular
   strictness options are each assessed for whether they belong in a blocking gate.
5. **Given** the measurements, **When** the write-up is produced, **Then** it states an explicit
   go / no-go on including templates in the gate, separate from the Story 2 decision.

---

### User Story 5 - The finding is handed off (Priority: P3)

The strict-mode effort's owner reads a single write-up on the issue and knows whether to build
the gate, in what shape, and where the work is tracked — without re-deriving anything.

**Why this priority**: A spike whose result lives only in a throwaway script is a spike that
gets re-run in three months. The write-up is the deliverable that outlives the timebox.

**Independent Test**: Read the issue after the spike closes and confirm it carries the
recommendation, the measurements behind it, and either a follow-up task link or a documented
reason the approach cannot work.

**Acceptance Scenarios**:

1. **Given** the spike is complete, **When** issue #37401 is read, **Then** it carries the
   findings, every decision with its measurements, and an explicit recommendation.
2. **Given** the recommendation is "build it", **When** the issue is closed, **Then** a
   follow-up task exists covering the production gate — the durable script, the continuous
   integration hook, and the local pre-commit hook — and states whether templates are in or out
   of its first version.
3. **Given** the recommendation is "do not build it", **When** the issue is closed, **Then** it
   states the specific reason the approach fails, in enough detail that nobody re-opens the same
   question blind.

---

### Edge Cases

- **A pull request changes no TypeScript and no template at all** → the gate is a no-op and
  passes; it must not fail, and must not spend meaningful time deciding there is nothing to do.
- **The diff contains deleted or renamed files** → no crash and no phantom failure against a
  path that no longer exists at the head commit.
- **The diff touches a shared configuration file** (the workspace TypeScript baseline, or the
  workspace task configuration) → these are declared shared inputs, so the affected-project
  calculation expands to all 56 projects. The gate must stay scoped to the projects that own
  changed files rather than fanning out to the whole workspace; the chosen behavior is recorded
  either way.
- **A changed file belongs to a project with no conventional library config** — applications,
  `.tsx` projects, framework-specific projects → the gate resolves the correct config or skips
  the project **loudly**, never silently.
- **A changed file maps to no project at all** (workspace-root files, tooling scripts) →
  explicitly reported as unmapped rather than dropped.
- **A shallow checkout, and the merge-queue context** → the base ref the diff is computed
  against may not be present locally and must be fetched before use; the gate must not
  mistakenly report "nothing changed" when the base ref is missing.
- **The same file is claimed by more than one project config** (a source file included by both
  a library and a spec config) → the finding is reported once, not duplicated per config.
- **A component's template is inline rather than a separate file** → the diagnostic's
  originating file is the component source, not a template file, and must still be matched
  against the changed-file set correctly.
- **A pull request changes only a template file and no source file** → the owning project is
  still identified and checked; a template-only change must not slip through as "no TypeScript
  changed".
- **A framework upgrade introduces new diagnostics** → the gate must not start failing pull
  requests for diagnostics unrelated to what they changed; the chosen configuration is assessed
  for this fragility (see FR-018).

## Requirements *(mandatory)*

### Functional Requirements

#### Core gate (Stories 1–3)

- **FR-001**: The evaluation harness MUST determine the set of changed files for a pull request
  by comparing its head against its merge-base with the target branch, including added, copied,
  modified and renamed files, and excluding deleted ones. The set MUST cover TypeScript sources,
  and MUST cover Angular template files when the template-aware mode is in use.
- **FR-002**: The harness MUST map each changed file to the workspace project that owns it, and
  MUST report any changed file it cannot map rather than discarding it.
- **FR-003**: The harness MUST type-check each owning project under strict settings **without
  requiring any edit to any version-controlled configuration file in the repository** —
  strictness is imposed at invocation time only.
- **FR-004**: The harness MUST discard every diagnostic whose originating file is not in the
  changed-file set, and MUST report the number discarded per run.
- **FR-005**: The harness MUST exit non-zero when at least one diagnostic survives the filter,
  and zero otherwise, so it is usable as a gate.
- **FR-006**: The harness MUST report each surviving diagnostic with its file path, line and
  diagnostic code.
- **FR-007**: The harness MUST support being run under both candidate flag sets — full strict,
  and the narrower null-checks/implicit-any subset — so the two can be compared on the same
  sample.
- **FR-008**: The harness MUST support both candidate granularities — every diagnostic in a
  changed file, and only diagnostics on changed lines — so the two can be compared on the same
  sample.
- **FR-009**: The harness MUST measure and report its own wall-clock runtime per run.
- **FR-010**: The harness MUST scope its work to the projects owning changed files, and MUST
  NOT expand to the entire workspace when only a shared configuration file changed.
- **FR-011**: The harness MUST run correctly when the target branch ref is not already present
  locally, fetching it if required.

#### Template arm (Story 4)

- **FR-014**: The harness MUST be able to impose Angular's template-strictness settings on a
  project whose own configuration disables them, satisfying FR-003 — no version-controlled
  configuration file is edited.
- **FR-015**: The harness MUST apply FR-004's filter to template diagnostics on the same terms
  as source diagnostics, and MUST report the discarded count separately for them.
- **FR-016**: The harness MUST detect whether a project is an Angular project and select the
  template-aware or TypeScript-only mode accordingly, reporting the choice rather than making
  it silently.
- **FR-017**: The harness MUST measure the template-aware mode's wall-clock cost separately
  from the TypeScript-only mode's, on the same sample.
- **FR-018**: The spike MUST assess each candidate Angular strictness option for whether it
  belongs in a blocking gate, explicitly including whether promoting a whole category of
  diagnostics to errors makes the gate fragile across framework upgrades.

#### Deliverable

- **FR-012**: The spike MUST produce a written record covering: the per-pull-request results,
  the false-positive rate, the discarded-diagnostic counts, every decision with its
  measurements, and an explicit go/no-go on blocking merges from day one.
- **FR-013**: The spike MUST end with either a follow-up task for the production gate — stating
  whether templates are in scope for its first version — or a documented reason the approach
  does not work.

### Out of Scope

- Shipping the production gate itself — the durable script, its continuous-integration hook and
  its local pre-commit hook. This spike produces the evidence and the decision; the build is the
  follow-up task (FR-013).
- Any change to version-controlled configuration files, to the workspace build definition, or to
  continuous-integration workflow files.
- Migrating any library or application to strict mode, re-enabling template strictness in the
  four applications that disabled it, and any dependency on PR #37198 landing.
- Fixing the accumulated template debt that `TODO(#35930)` refers to. The gate's purpose is to
  stop that debt growing, not to pay it down.
- Catching loose types that *flow in* from non-strict dependencies. While the high-fan-in
  libraries stay non-strict, sloppy types cross into strict files unflagged — and the same
  weakness suppresses template findings, since a value typed loosely upstream satisfies a strict
  template check. This is a known, accepted limitation of the approach, not a defect of it, and
  it means the template arm's signal is weakest in exactly the applications that need it most.
- Detecting pre-existing strict debt in files a pull request does not touch.
- Framework settings that are not about strictness (message-identifier formats, emit behavior),
  even where they appear alongside strictness settings in existing configuration.

### Key Entities

- **Changed-file set**: the files a pull request added, copied, modified or renamed, relative to
  its merge-base with the target branch. The unit the whole gate is scoped by.
- **Owning project**: the workspace project whose configuration includes a given changed file;
  the unit that checking is actually invoked on.
- **Diagnostic**: a single reported error, carrying an originating file, a line and a code.
  Either survives the filter (its file is in the changed-file set) or is discarded (it came from
  a dependency or from untouched code).
- **Execution mode**: TypeScript-only, or template-aware. Determined per project by whether it
  is an Angular project, and reported per run.
- **Sample pull request**: a merged pull request replayed at its merge-base and merge commit,
  labelled up-front as carrying strict debt or not, and used as the evidence base for both the
  detection and the false-positive claims.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The harness reports 3 of the 3 known strict violations that PR #37262 introduced
  into `sdk-create-app`, and fails on that pull request.
- **SC-002**: Across at least 3 replayed pull requests that carry no strict debt, the harness
  produces **zero** findings — a measured false-positive rate of 0 on that sample.
- **SC-003**: Every finding reported across the full sample is individually adjudicated real or
  spurious, with the adjudication written down; no finding is left unexplained.
- **SC-004**: For every run, the count of diagnostics discarded as dependency-origin or
  untouched-code-origin is reported, and at least one run demonstrates a program dominated by
  dependency files (on the order of the measured 387-from-6-libs case) passing because of the
  filter.
- **SC-005**: A pull request touching 1–3 projects completes in **10 seconds or less** of
  wall-clock time in TypeScript-only mode.
- **SC-006**: All 10 edge cases listed above are exercised and their observed behavior recorded;
  none produces a crash, and none produces a silent skip.
- **SC-007**: Each of the three core decisions — flag set, granularity, runtime cost — carries a
  single stated recommendation backed by a number measured on the sample.
- **SC-008**: An explicit go / no-go on blocking merges from day one is recorded, with a named
  fallback posture if the answer is no-go.
- **SC-009**: The spike is delivered within its timebox, or the overrun and its cause are
  recorded on the issue.
- **SC-010**: The repository's version-controlled configuration files are byte-identical before
  and after the spike.
- **SC-011**: Template strictness is demonstrated in force on at least one of the four
  applications that currently disable it, with SC-010 still holding.
- **SC-012**: For at least one pull request that changed a template in a non-strict application,
  the changed template's violations are reported and the application's pre-existing template
  debt is fully discarded, with both counts recorded.
- **SC-013**: The template-aware mode's wall-clock cost is recorded against the TypeScript-only
  mode's on the same sample, and an explicit go / no-go on including templates in the gate is
  stated — separate from SC-008, so a no-go here does not block the core gate.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: None at runtime. This is developer-tooling research against the
  `core-web` frontend workspace; it produces no product behavior change and ships nothing to
  users. The area it informs — the frontend build and validation pipeline — already carries
  comparable gates for linting and formatting.
- **Backward-compatibility expectations**: Absolute. Nothing in this spike may alter existing
  configuration, build definitions or workflows (SC-010). The eventual gate, when built, must not
  block pull requests that do not introduce new strict debt (User Story 2).
- **Known related decisions**: The repository already accepts baselining accumulated debt rather
  than blocking on it — eight lint-suppression files exist, declared as inputs to the lint task.
  A type-checking equivalent would follow an established precedent, not introduce a new one. A
  project-scoped typecheck gate already exists on the strict-mode branch but has never landed on
  `main`. Template strictness was deliberately switched off in the four applications during the
  framework 22 upgrade, tracked as `TODO(#35930)`; this spike must not disturb that decision,
  only measure whether a diff-scoped gate can coexist with it. The plan phase will formally
  consult `dotCMS/platform-adrs`.

## Assumptions

- **Spike scope ends at evidence and a decision.** Issue #37401 describes the deliverable as a
  throwaway script plus a write-up, with the production gate handed to a follow-up task. This
  spec follows that framing; the durable script, CI hook and pre-commit hook are out of scope
  here.
- **Command-line strictness overrides inherited configuration for TypeScript options.** This was
  verified against a synthetic project before the spike and is treated as a given; FR-003's
  TypeScript arm depends on it, and confirming it on a real workspace project is the first thing
  the spike does.
- **The same is *not* true of Angular's strictness settings.** They are not TypeScript compiler
  options and are rejected by the compiler's command-line parser, which accepts only a small
  fixed set of non-TypeScript options. FR-014 therefore requires a different mechanism —
  supplying the settings through configuration the compiler reads, without editing any
  version-controlled file. Two viable approaches are known; choosing between them is plan-phase
  work, not spec-phase.
- **The template arm raises the timebox.** The issue's 4 hours cover Stories 1–3. Story 4 is
  expected to add roughly 2 hours. If the core arm consumes the original budget, Story 4 is
  deferred to the follow-up task with its findings-to-date recorded — it is P3 precisely so this
  is possible without weakening the deliverable.
- **The PR #37262 case is still reproducible.** The three violations are reported as still
  present on `main`. If they have since been repaired, an equivalent regression case is
  substituted and the substitution recorded.
- **Sample pull requests are chosen from recently merged work** touching `core-web` TypeScript
  and templates, labelled as debt-carrying or clean *before* the harness is run against them, so
  the sample is not selected to fit the result.
- **A sample of six or so pull requests is sufficient** for a time-boxed spike to support a
  go/no-go recommendation. It is not a statistical claim, and the write-up says so.
- **The base ref for diff computation is available or fetchable in every context the gate would
  eventually run in.** The existing pipeline already fetches it for the affected-project
  calculation, so no workflow change is anticipated.
- **Both candidate granularities are evaluated on the same sample**, so the whole-file adoption
  cost is measured rather than argued.
