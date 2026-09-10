# Issue Resolution Specification: Adopt `/speckit-converge` as the mandatory closing step of the dotCMS Spec-Kit flow, covering documentation drift

**Feature Branch**: `37267-converge-closing-step`

**Created**: 2026-08-28

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37267](https://github.com/dotCMS/core/issues/37267)

**Input**: User description: "https://github.com/dotCMS/core/issues/37267 — Adopt /speckit-converge as the mandatory closing step of the dotCMS Spec-Kit flow, covering documentation drift"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). The defect here is
  in the repo's own development process tooling (Spec-Kit skills, hooks, and the docs that
  describe the flow), not in shipped product code. It is framed the same way: what is wrong,
  how to observe it, and how we will know it is fixed.
-->

## Problem Statement *(mandatory)*

The dotCMS Spec-Kit flow ends at `/speckit-implement`. Nothing in the documented process asks
**"does the code actually match the spec that was approved in PR 1?"** before PR 2 is opened.
That question is the entire justification for the two-PR, approval-gated flow (Quick Start §3);
today it is left to the PR 2 reviewer to reconstruct from the diff — precisely the failure mode
the two-PR flow exists to avoid.

`/speckit-converge` — the upstream Spec-Kit command designed to answer that question — is
already installed (`.claude/skills/speckit-converge/SKILL.md`, registered in
`.claude/skills/skills.config.json`) and already documented in
`docs/core/SPEC_KIT_QUICK_START.md` §8 and §9. **This issue is not about adding the command.**
Two gaps separate how we ship it today from what we need:

**Gap 1 — it is not positioned as the closing step of the flow.** It is described only in §9
*"The other commands"*, framed as an optional escape hatch ("Code has run ahead of `tasks.md`
and you want to know what's left"). It is absent from every place the canonical flow is written
down:

| Location | Current state (verified) |
|---|---|
| `SPEC_KIT_QUICK_START.md` §1 — flow diagram | ✗ ends at `/speckit-implement` |
| `SPEC_KIT_QUICK_START.md` §2 — tier table (Lean / Full) | ✗ not listed in either flow |
| `SPEC_KIT_QUICK_START.md` §3 — two-PR sequence (steps 1–5) | ✗ not mentioned before PR 2 |
| `SPEC_KIT_QUICK_START.md` §9 (line 337, §9.4 at line 371) | ✓ documented, but as an *optional* tool |
| `CLAUDE.md` **Flow** line (line 95) | ✗ ends at `/speckit-implement` → PR 2 |
| `.specify/CUSTOMIZATIONS.md` "The standard flow" (line 21) | ✗ ends at `/speckit-implement` |
| `.specify/extensions.yml` | ✗ only `before_plan` is declared; no post-implement hook |

**Gap 2 — it converges code, but never documentation.** The shipped skill reads `spec.md`,
`plan.md`, `tasks.md`, and the codebase. Its `SKILL.md` contains no reference to `docs/`,
`CLAUDE.md`, `openapi.yaml`, or Javadoc. A feature can therefore converge "clean" while `docs/`
still describes the old behavior and `openapi.yaml` still advertises the old contract. This
matters more in dotCMS than in a vanilla Spec-Kit repo because several documents here are
build-verified or normative:

- `openapi.yaml` is auto-generated at compile and **CI fails when the committed file does not
  match the build output** (`CLAUDE.md` → OpenAPI / Swagger).
- `docs/` is the single source of truth for backend and frontend standards; `CLAUDE.md` is the
  always-loaded navigation hub.
- `@Schema` / return-type correspondence is a Critical Rule and Constitution Principle IV.

**Severity / Impact**: Medium. Affects every developer running the Spec-Kit flow in
`dotCMS/core`, on every Tier 1 and Tier 2 change. The cost is silent and cumulative: PR 2
reviewers absorb spec-conformance checking manually, and documentation drift accrues on merges
that otherwise look complete. No product runtime impact — the blast radius is the developer
workflow and the repo's own documentation.

## Clarifications

### Session 2026-08-28

- Q: A finding can be correct but something the developer decides not to act on (an `unrequested`
  finding especially). Converge re-derives findings from code each run, so it reappears forever
  and the loop never converges. What terminates it? → A: **Dedupe + developer judgment.**
  `speckit-docs-converge` skips any finding already represented by a task in a prior Convergence
  phase (checked or unchecked), so it never re-emits. The shipped `/speckit-converge` cannot be
  given this behavior (it is a shipped file we deliberately do not edit), so §10 documents the
  operative rule: **the gate is "no NEW actionable findings"**, and a developer who has
  consciously accepted a recurring finding marks its task `[X]` and proceeds.
- Q: `/speckit-implement` often halts partway — parked at a TDD `[GATE]` awaiting approval, or
  stopped on a failed task — and its hook fires on "reporting completion". Converge would then
  see every unbuilt task as `missing` and append duplicates of tasks already open. What should
  happen? → A: **Detect the incomplete run and no-op.** `speckit-docs-converge` checks `tasks.md`
  for unchecked non-gate tasks; if any remain it reports *"implementation incomplete — finish the
  task list first"* and appends nothing (byte-for-byte no-op). The shipped converge cannot be
  made to do this, so §10 documents that its output after a halt is advisory.
- Q: How should §3 and §9 word the PR 2 gate, given a developer may accept a recurring finding
  and proceed, and nothing enforces it in CI? → A: **"Converged, or accepted."** §3 states that
  PR 2 opens once converge reports `converged`, *or* every remaining finding has been consciously
  accepted (task marked `[X]`). §9 explains the accept-and-proceed rule and why it exists. The
  docs are explicit that this gate is human-judged, not script-enforced.

#### Trigger decision (2026-08-31) — converge is recommended, not automatic

- Q: `after_implement` fires converge the instant `/speckit-implement` completes its task list.
  But a developer normally makes several **manual corrections** after that. Those corrections are
  never assessed, and the `converged` verdict the developer sees is already stale by the time they
  open PR 2. Where should the trigger live? → A: **Nowhere automatic — the developer triggers it.**
  The hook becomes `optional: true`, which makes the shipped skills *print* the recommendation
  with the command ready instead of executing it. Convergence stays part of the flow (§1, §2, §3
  keep it unparenthesized); only the **timing** moves to the developer, because only the developer
  knows when their edits have stopped.

  **Why not the alternatives.** Firing at the end of implement assesses the least-final state of
  the work, and the docs saying "runs automatically; nothing to type" actively trained the reader
  to trust a verdict nobody re-reads — worse than having no verdict. A `git push`-time hook was
  considered and rejected: it leaks (pushing from a terminal outside Claude Code skips it) and
  adds machinery the issue never asked for.

  **Scope consequence**: this contradicts issue #37267's AC B (`optional: false`, "runs without
  the developer typing the command"). `spec.md` is still Draft and PR 1 is not open, so no
  re-approval is owed — but **the issue's AC B is now out of date and must be renegotiated there
  before the issue is closed.**

#### Delivery decision (2026-08-31) — one PR, not two

- Q: Commit `a572f07be1` is already pushed and bundles `spec.md`, `data-model.md` and
  `contracts/` together with the implementation. Quick Start §3 — the very policy this feature
  documents — says PR 1 carries `spec.md` **alone** and is approved before planning starts. That
  order can no longer be followed. → A: **Ship as a single PR and say so in its description**,
  asking the reviewer to read `spec.md` first, as a spec, before the rest of the diff. Chosen
  because the history is already published and mixed, and because this change is process tooling
  rather than product code, so the risk of having skipped the up-front spec approval is low.
  Recorded here rather than quietly ignored: **this feature's own PR did not follow the two-PR
  flow it documents.**

#### Follow-up decisions (T003 gate + T002 finding)

- Q: Quick Start §9 is titled "The other commands". If converge is mandatory it is no longer one
  of them — where does its explanation live? → A: **Remove its row from the §9 "Reach for it
  when" table and retitle §9** so the heading no longer says "other"; the detailed subsection
  stays where it is. No section renumbering, and converge stops being *listed* as optional —
  which was the actual contradiction. [CHK008]
- Q: One numbered customization in `.specify/CUSTOMIZATIONS.md`, or two? → A: **Two.**
  **#7** = the `after_implement` → `speckit.converge` hook (closes Gap 1, depends on nothing
  new). **#8** = the `speckit-docs-converge` skill + the `after_converge` hook (closes Gap 2).
  This matches the US1/US2 split and keeps two pieces with different upgrade risk — a hook onto
  a vendored skill vs. a skill we author — separately reviewable and separately revertible.
  [CHK016]
- Q: Which single term names the terminal state? → A: **`converged`.** It is the outcome string
  the skill itself prints, so the developer's vocabulary matches the terminal output. The escape
  hatch is documented as an explicit **exception** in §3 and §9 ("`converged`, or every remaining
  finding consciously accepted"), not folded into the name of the state. [CHK015]
- Q: The dry run required by issue AC E cannot run where §Verification method said — neither
  `specs/36834-…` nor `specs/37176-…` has `plan.md` or `tasks.md`, and converge **stops** when
  any of the three artifacts is missing (`speckit-converge/SKILL.md` step 1). Those files are
  gitignored by design, so no feature directory but the active one ever carries them. → A: **Use
  `specs/37267-converge-closing-step` itself** — the only directory in the repo with the full
  artifact set, and a demanding target besides, since this feature really does change `docs/`,
  `CLAUDE.md`, and `.specify/`. Run it mid-implementation for the "≥1 actionable finding" case
  and again at the end for the zero-findings control. [supersedes CHK019]

## Reproduction *(mandatory)*

**Environment**: `dotCMS/core` working tree at any commit on `main` as of 2026-08-28. Claude
Code with the repo's `.claude/skills/` loaded. No build, server, or database required — this is
a process/tooling defect observable by reading repo files and running the flow.

**Steps to Reproduce**:

*Gap 1 — converge is absent from the canonical flow:*

1. Open `docs/core/SPEC_KIT_QUICK_START.md` §1 and read the flow diagram.
2. Read the §2 tier table rows for Tier 1 (Lean) and Tier 2 (Full).
3. Read the §3 numbered sequence, steps 1–5.
4. Open `CLAUDE.md` and read the **Flow** bullet under "Spec-Driven Development (Spec-Kit)".
5. Open `.specify/CUSTOMIZATIONS.md` and read "The standard flow".
6. Open `.specify/extensions.yml` and list the declared hook keys.

*Gap 2 — converge ignores documentation:*

7. `grep -niE 'docs/|CLAUDE\.md|openapi|javadoc' .claude/skills/speckit-converge/SKILL.md`
8. Run the full flow on a change that alters a documented behavior or a JAX-RS contract, then
   run `/speckit-converge` and read its findings table.

**Expected Behavior**:

- Steps 1–5: every location that states the canonical flow ends with `/speckit-converge` before
  PR 2, shown as a required step with the `implement → converge` loop.
- Step 6: an `after_implement` hook is declared, pointing at `speckit.converge`, `optional: false`.
- Step 7: the skill references the documentation surfaces it must assess.
- Step 8: documentation drift appears as findings, classified with the existing vocabulary and
  appended as ordinary tasks for `/speckit-implement`.

**Actual Behavior**:

- Steps 1–5: every location ends at `/speckit-implement` → PR 2. Converge appears only in §9's
  "other commands" table (line 337) and its §9 subsection (line 371), both framed as optional.
- Step 6: only `before_plan` → `speckit.adr-context` is declared. No `after_implement` key.
- Step 7: zero matches. The skill has no notion of documentation.
- Step 8: findings cover code only. Stale `docs/`, a stale `openapi.yaml`, or Javadoc describing
  the previous behavior all pass as converged.

**Reproducibility**: Always — deterministic, observable by reading the files listed above.

## Scope of Investigation *(mandatory)*

- **Affected area**: Developer tooling and process documentation — the dotCMS Spec-Kit
  customization layer. Concretely: `docs/core/SPEC_KIT_QUICK_START.md`, `CLAUDE.md`,
  `.specify/` (`extensions.yml`, `CUSTOMIZATIONS.md`, `integrations/claude.manifest.json`), and
  `.claude/skills/` (`speckit-converge/SKILL.md`, `skills.config.json`, `CATALOG.md`).
- **Suspected surface**: Neither modern `com.dotcms.*` nor legacy `com.dotmarketing.*` — **no
  Java or TypeScript application code is in scope.** The change is confined to Markdown, YAML,
  and JSON under `.specify/`, `.claude/`, `docs/`, and root `CLAUDE.md`. Legacy Impact in the
  plan is therefore expected to be "none"; the plan should confirm that rather than assume it.
- **Related known decisions**:
  - Constitution Principle IV (Contract Correctness) — `@Schema` must match the return type;
    `openapi.yaml` is generated, not hand-edited. This is what makes REST/OpenAPI drift a
    first-class convergence concern.
  - Constitution ADR section — Spec-Kit must never create or edit ADRs, anywhere. Converge and
    any companion skill inherit that prohibition.
  - Quick Start §3 — a spec that changes after approval needs re-approval. A convergence finding
    that the implementation diverged from an approved spec must route to that rule.
  - Quick Start §8 / `.gitignore` — `tasks.md` is a process artifact, gitignored by design.
    Converge's output stays local and never reaches PR 2.
  - `.specify/CUSTOMIZATIONS.md` customization #2 (`before_plan` → `adr-context`) is the
    upgrade-safe hook precedent; #5 and #6 are the "edits a shipped file, re-apply on upgrade"
    precedent. The plan picks which category this change lands in.

## Root-Cause Hypothesis

Two independent causes, one per gap:

1. **Gap 1 is a documentation and configuration omission.** `/speckit-converge` was installed as
   part of the upstream skill set but never wired into the dotCMS-authored narrative of the flow
   (Quick Start §1–§3, `CLAUDE.md`, `CUSTOMIZATIONS.md`) or into `extensions.yml`. Nothing
   technical blocks it — the hook mechanism it needs already exists (see below); the declaration
   was simply never written.

2. **Gap 2 is an upstream scope boundary.** `.claude/skills/speckit-converge/SKILL.md` is a
   verbatim vendored copy of upstream `templates/commands/converge.md`. Upstream scopes
   convergence to "the codebase" because a generic Spec-Kit repo has no normative documentation
   contract. dotCMS does, so the upstream scope is too narrow here — this is a customization gap,
   not an upstream bug.

**Verified during specification (reduces implementation risk, confirm in the plan):**

- `.claude/skills/speckit-implement/SKILL.md` **already implements** `after_implement` hook
  dispatch — "Mandatory Post-Execution Hooks", lines ~182–225, including the
  `optional: false` → `EXECUTE_COMMAND:` path. AC **B** is therefore expected to be satisfied by
  a declaration in `extensions.yml` alone, with **no edit to a shipped skill file**.
- `.claude/skills/speckit-converge/SKILL.md` **already implements** `before_converge` (Pre-Execution
  Checks) and `after_converge` (Execution Step 9) hook dispatch. The additive-companion route in
  the issue's Additional Context is therefore mechanically available today.
- `.specify/integrations/claude.manifest.json` line 10 records the sha256 of
  `speckit-converge/SKILL.md`; editing that file invalidates the hash and requires updating it.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Reposition `/speckit-converge` as the mandatory closing step in all five places the canonical
  flow is documented: Quick Start §1 (diagram), §2 (tier table, both tiers, unparenthesized),
  §3 (two-PR sequence, as the precondition for opening PR 2 — worded **"converged, or every
  remaining finding consciously accepted"**), §9 (rewritten from optional tool to mandatory
  closing step, including loop-until-converged semantics and the accept-and-proceed rule), and
  §10 (troubleshooting rows for "converge keeps appending tasks / never reports converged" and
  for converge firing after a halted implement run).
- Update the `CLAUDE.md` **Flow** line to `… → /speckit-tasks → /speckit-implement →
  /speckit-converge → PR 2 (implementation)`.
- Update `.specify/CUSTOMIZATIONS.md` "The standard flow" to the same sequence, and record the
  new hook as a numbered customization with its upgrade-safety classification, matching the
  style of customization #2.
- Declare an `after_implement` hook in `.specify/extensions.yml` pointing at `speckit.converge`
  with `optional: false`, following the existing schema and comment style.
- Extend the convergence gap analysis to cover documentation drift: `docs/` and `CLAUDE.md`,
  `openapi.yaml` and REST annotation correspondence, `spec.md` / `plan.md` back-annotation, and
  Javadoc / code comments — using the existing `missing` / `partial` / `contradicts` /
  `unrequested` vocabulary and the existing severity ordering.
- Keep the append-only invariant intact: documentation gaps become tasks in
  `## Phase N: Convergence`, each naming the file to update.
- **Untrack `.specify/feature.json`** (added during implementation, T050 — not in the original
  scope). It is listed in `.gitignore:238` yet still tracked, because it was committed before the
  ignore rule existed, so every developer's local feature pointer leaks into their PRs. Quick
  Start §3 and §10 already claim it is "local and untracked, never committed"; `git rm --cached`
  makes both claims true without editing either sentence.
- **Guarantee the loop terminates.** Two behaviors, both in the new companion skill:
  *dedupe* — never re-emit a finding already represented by a task in a prior Convergence phase,
  checked or unchecked; and *incomplete-run detection* — when `tasks.md` still has unchecked
  non-gate tasks, report that the implementation is incomplete and append nothing.
- Whichever implementation route the plan selects (edit the shipped skill vs. an additive
  companion skill wired as `after_converge`), carry out its bookkeeping: regenerate
  `.claude/skills/CATALOG.md` via `just skills-catalog` if a skill description or status changes,
  and update the sha256 in `.specify/integrations/claude.manifest.json` if
  `speckit-converge/SKILL.md` is edited.

**Explicitly out of scope / non-goals**:

- Changing what `/speckit-analyze` does — it remains the pre-implement, read-only artifact audit.
- Letting converge (or any companion skill) create or edit ADRs — prohibited by the constitution.
- Any CI enforcement of convergence. The gate lives in the developer loop, not in the pipeline;
  no workflow under `.github/workflows/` is touched.
- Letting converge edit `docs/`, `CLAUDE.md`, `openapi.yaml`, Javadoc, `spec.md`, `plan.md`, or
  any application code directly. Converge reports; `/speckit-implement` writes.
- Regenerating or hand-editing `openapi.yaml` as part of this change — converge only *reports*
  when it is stale.
- Rewriting the upstream `/speckit-converge` behavior beyond the documentation dimension
  (severity model, append contract, phase numbering, and the `converged` / `tasks_appended`
  outcomes all stay as they are).
- Upgrading Spec-Kit itself, or reconciling other upstream drift.
- Changing the two-PR policy, the tier model, or the gitignore policy for `tasks.md`.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - Every developer running `/speckit-implement` in this repo — the flow gains an automatic
    follow-on step. Risk: a mandatory `after_implement` hook that misfires or loops leaves the
    developer stuck at the end of every run. Mitigated four ways: dedupe and incomplete-run
    detection make the loop terminate mechanically (see Clarifications); §3/§9 give a sanctioned
    accept-and-proceed exit; and §10 gains rows for both symptoms.
  - **Developers who hit a TDD `[GATE]`** — the most common `/speckit-implement` outcome, not an
    edge case. The chain fires on every gate approval, so it must stay quiet during a partial
    run or it doubles the size of `tasks.md` over the course of a feature.
  - Anyone running `/speckit-converge` — findings volume increases once documentation is in
    scope. Risk: noisy or non-actionable doc findings train developers to ignore convergence.
    AC-004 constrains this by requiring findings to be genuinely actionable and by requiring zero
    findings on a feature known to be complete.
  - The four other `/speckit-*` skills that read `.specify/extensions.yml` parse it defensively
    (invalid YAML → skip silently), so a malformed declaration degrades rather than breaks —
    but it also fails silently, which is the harder failure to notice.
- **Backward compatibility**:
  - No product API, DB schema, ES mapping, or serialized state is touched. Not rollback-unsafe
    under `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`.
  - In-flight feature directories under `specs/` keep working: converge is additive and
    append-only, and existing `tasks.md` files gain a new phase rather than being rewritten.
  - Editing `.claude/skills/speckit-converge/SKILL.md` breaks its recorded sha256 in
    `.specify/integrations/claude.manifest.json` and creates a re-apply obligation on the next
    Spec-Kit upgrade (`CUSTOMIZATIONS.md` §"Re-applying after a `specify` upgrade"). The additive
    companion route avoids both. The plan chooses and justifies.
  - `CLAUDE.md` and the constitution must stay in sync per the constitution's Governance section;
    if this change touches normative flow statements in `CLAUDE.md`, the plan must check whether
    `.specify/memory/constitution.md` needs the matching edit and a version bump.
- **Data considerations**: None. No migration, no repair of existing data. `tasks.md` files
  already on disk are only ever appended to.

## Acceptance & Verification *(mandatory)*

The acceptance criteria describe the **behavior a developer should see**, not the implementation
route. Either route in the issue's Additional Context table can satisfy them; `/speckit-plan`
picks one and justifies it.

### AC-001 — Converge is documented as the final step of the flow

The reproduction steps 1–5 now produce the expected behavior:

- `docs/core/SPEC_KIT_QUICK_START.md` §1's flow diagram shows `/speckit-converge` after
  `/speckit-implement`, including the `implement → converge` loop and the exit to PR 2.
- §2's tier table lists `/speckit-converge` in **both** the Lean (Tier 1) and Full (Tier 2)
  flows, unparenthesized — i.e. not an optional step.
- §3's numbered sequence names the precondition for opening PR 2 as **converge reporting
  `converged`, or every remaining finding having been consciously accepted** (its task marked
  `[X]`), and says plainly that this gate is human-judged rather than script-enforced.
- §9 no longer frames `/speckit-converge` as an optional "other command"; its subsection is
  retained or relocated and rewritten to describe the mandatory closing step, including
  loop-until-converged semantics and the accept-and-proceed rule (what "accepted" means, and
  why an escape exists at all).
- The `CLAUDE.md` **Flow** line reads `… → /speckit-tasks → /speckit-implement →
  /speckit-converge → PR 2 (implementation)`.
- `.specify/CUSTOMIZATIONS.md` "The standard flow" states the same sequence.
- §10 Troubleshooting has **two** new rows: converge keeps appending tasks / never reports
  converged; and converge running after `/speckit-implement` halted at a `[GATE]` (its output is
  advisory — finish the task list, then let the chain run again).

### AC-002 — Converge is part of the flow, recommended at the end of implement, triggered by the developer

> **Supersedes the issue's AC B** (2026-08-31, developer decision). The issue specified
> `optional: false` and *"converge runs without the developer typing the command"*. Both are
> deliberately **not** implemented; see the Clarifications entry for the reasoning. This
> divergence must be renegotiated on the issue before it is closed.

- `.specify/extensions.yml` declares an `after_implement` hook pointing at `speckit.converge`
  with **`optional: true`** and a `prompt`, using the same schema and comment style as the
  existing `before_plan` → `speckit.adr-context` hook.
- Completing a `tasks.md` with `/speckit-implement` **prints the recommendation with the command
  ready to run, and does not execute it.**
- The developer runs `/speckit-converge` when they judge the work finished — after their manual
  corrections, not before. Fix, re-run, repeat; per AC-004's dedupe rule the loop **terminates**:
  each pass has strictly fewer new findings than the last.
- Running converge mid-flight is allowed and useful; the documentation pass detects an incomplete
  task list and appends nothing, so an early run is quiet rather than noisy.
- `after_converge` → `speckit.docs-converge` **stays `optional: false`**: by the time it fires the
  developer has already chosen to converge, so extending that single decision to documentation
  needs no second prompt.
- Both hooks are recorded in `.specify/CUSTOMIZATIONS.md` as numbered customizations stating
  upgrade-safety, consistent with how customization #2 is recorded.

### AC-003 — Documentation drift is part of the gap analysis

Converge's assessment covers, in addition to the code:

- **`docs/` and `CLAUDE.md`** — a doc describing behavior, a command, or a pattern the
  implementation changed, which no longer matches it, is reported as a finding.
- **`openapi.yaml` and REST contracts** — when the feature touched a JAX-RS resource, converge
  checks that `@Operation` / `@Parameter` / `@Schema` annotations describe what was actually
  built, and that the committed `openapi.yaml` matches the current build output (the CI check in
  `CLAUDE.md` → OpenAPI / Swagger).
- **`spec.md` / `plan.md` back-annotation** — where the final implementation diverged from the
  approved spec, the divergence is reported so it can be reconciled. Converge must **not** edit
  `spec.md` or `plan.md`; the reconciliation is surfaced as a task, and divergence from an
  *approved* spec is flagged as needing re-approval per Quick Start §3.
- **Javadoc and code comments** — comments left describing the previous behavior of code the
  feature changed are reported.
- Each documentation finding is classified with the existing vocabulary (`missing` / `partial` /
  `contradicts` / `unrequested`) and carries the file path it refers to.
- Documentation findings are ordered after constitution violations, consistent with the current
  severity ordering.

### AC-004 — Append-only invariant is preserved

- Converge's only write remains appending a `## Phase N: Convergence` section to `tasks.md`. It
  does not edit `docs/`, `CLAUDE.md`, `openapi.yaml`, Javadoc, `spec.md`, `plan.md`, or any
  application code.
- Documentation gaps are emitted as ordinary tasks in that section, actionable by
  `/speckit-implement`, each naming the file to update.
- **Dedupe** — a finding already represented by a task in a prior Convergence phase is not
  re-emitted, whether that task is checked or unchecked. This is what lets a consciously accepted
  finding stay accepted and the loop reach a terminal state.
- **Incomplete-run detection** — when `tasks.md` still contains unchecked non-gate tasks, the run
  reports *"implementation incomplete — finish the task list first"* and appends nothing, leaving
  `tasks.md` byte-for-byte unchanged. This is the normal state after every TDD `[GATE]` approval,
  so it must be quiet rather than noisy.
- When code **and** docs already satisfy the artifacts, `tasks.md` is left **byte-for-byte
  unchanged** and converge reports `converged`.
- Existing task IDs are never renumbered, reordered, or deleted; a second Convergence phase is
  appended as a new phase below any prior one.

### AC-005 — Regression check on the rest of the flow

- `/speckit-plan` still fires its `before_plan` → `speckit.adr-context` hook unchanged.
- `/speckit-specify`, `/speckit-specify-fix`, `/speckit-tasks`, and `/speckit-analyze` behave
  exactly as before; `/speckit-analyze` remains read-only and pre-implement.
- No `.github/workflows/` file is modified — convergence is not enforced in CI.
- No ADR file is created or edited in this repo or in `dotCMS/platform-adrs`.

### Verification method

This change ships no Java or TypeScript, so there is no JUnit / integration / Postman /
Jest surface. Constitution Principle V requires that a missing test type be stated explicitly
with its reason — that is stated here, and the executable and reviewable checks that replace it
are named below. `/speckit-plan`'s Test Strategy must confirm or override this.

1. **Static verification of the documentation ACs (AC-001):** a reviewer re-runs reproduction
   steps 1–6 and confirms each location now matches the expected behavior. Every changed
   Markdown file is checked for internal link validity.
2. **Hook wiring (AC-002):** `.specify/extensions.yml` parses as valid YAML
   (`python3 -c "import yaml,sys; yaml.safe_load(open('.specify/extensions.yml'))"`), declares
   `hooks.after_implement.command: speckit.converge` and `optional: false`. Then a live run:
   execute `/speckit-implement` on a feature directory with at least one open task and observe
   converge firing without being typed.
3. **Live dry run producing a real finding (AC-003, issue AC E):** ⚠️ **UNACHIEVABLE AS
   SPECIFIED — recorded, not silently dropped.** Two decisions taken in this feature are
   mutually incompatible:
   - The **incomplete-run guard** (Clarifications, 2026-08-28) makes `speckit-docs-converge`
     write nothing and assess nothing while any unchecked non-`[GATE]` task remains.
   - This check requires a run **mid-implementation** — precisely the state the guard forbids.

   They also conflict with check 4 below: one demands ≥1 finding on this directory, the other
   demands zero, at the same point in time. The guard is the more valuable of the two behaviors
   (it keeps a mandatory hook quiet after every TDD gate approval, which is the common case), so
   it stands and this check does not run.

   **Resolved by a different route.** The *mid-implementation* run is impossible, but the
   requirement itself was met: the clean run on 2026-08-28 produced two genuinely actionable
   findings — a stale flow sentence in `.claude/skills/speckit-specify-fix/SKILL.md:26`, and an
   implementation divergence from `plan.md`'s change surface — each naming a file and citing
   evidence. Issue AC E's substance is therefore **evidenced**; only the timing the spec
   prescribed was not. Findings on a *complete* feature turn out to be the stronger demonstration
   anyway: they are drift the implementer genuinely missed, not work that was merely not done
   yet.
   > **Why this target and not another feature.** Converge stops when `spec.md`, `plan.md`, or
   > `tasks.md` is missing, and `plan.md`/`tasks.md` are gitignored by design (Quick Start §8), so
   > they exist only in the working tree of whoever created them. No other directory under
   > `specs/` carries the full set — verified. This one does, and it is a demanding target rather
   > than a convenient one: the feature genuinely changes `docs/`, `CLAUDE.md`, and `.specify/`,
   > so there is real drift to detect.
4. **Zero-finding control (AC-004, issue AC E):** re-run against the same directory **after the
   implementation is complete**; confirm it reports `converged` and leaves `tasks.md`
   byte-for-byte unchanged — verified with `sha256sum tasks.md` before and after.
5. **Append-only control (AC-004):** with an existing `tasks.md` containing a prior Convergence
   phase, run converge again and diff: existing IDs unchanged, a new phase appended below.
5b. **Dedupe / termination control (AC-004):** on a feature with a known recurring finding, run
   the documentation pass twice without changing any code. The second run must **not** re-emit
   the finding, and must leave `tasks.md` byte-for-byte unchanged. This is the check that proves
   the loop terminates rather than merely appearing to.
5c. **Incomplete-run control (AC-002, AC-004):** on a `tasks.md` with at least one unchecked
   non-gate task, run the documentation pass and confirm it reports *"implementation incomplete"*
   and writes nothing — the state after every `[GATE]` approval, so it must be silent.
6. **Bookkeeping (issue AC E):** if `speckit-converge/SKILL.md` was edited, its sha256 in
   `.specify/integrations/claude.manifest.json` matches
   (`shasum -a 256 .claude/skills/speckit-converge/SKILL.md`). If any skill description or status
   changed, `just skills-catalog` was re-run and `.claude/skills/CATALOG.md` is up to date
   (re-running it produces no diff).
7. **Regression (AC-005):** `git diff --name-only` on the branch shows no file under
   `.github/workflows/`, no `.java`/`.ts` file, and no ADR file.

## Assumptions

- **The issue is a Tier 2 change** despite carrying `Type : Task` rather than a bug label: it
  changes the process contract every developer follows and edits the repo's normative
  documentation. `/speckit-specify-fix` is the right entry point because the work is framed as
  closing two identified gaps in existing, shipped behavior. The Full flow (with `/speckit-clarify`
  and `/speckit-analyze` available) applies.
- **The implementation route is a plan decision, not a spec decision.** The issue presents two
  routes (edit the shipped `SKILL.md` vs. an additive `speckit.docs-converge` companion wired as
  `after_converge`); the ACs above are route-neutral by design. Specification verified that both
  are mechanically available today.
- **AC-002's hook dispatch needs no shipped-file edit.** `speckit-implement/SKILL.md` already
  implements `after_implement` dispatch. If the plan finds otherwise, that materially changes
  the upgrade-safety calculus and must be called out.
- **"Documentation" means the repo's own docs**, not dotcms.com product documentation, which
  lives outside this repository and is out of reach of converge.
- **`docs/core/SPEC_KIT_QUICK_START.md` is a durable reference and ships in a PR.** Per the
  §8 / `CUSTOMIZATIONS.md` commit policy, every file changed by this work (`docs/`, `CLAUDE.md`,
  `.specify/*`, `.claude/skills/*`) is committed; only `tasks.md` and the other process artifacts
  produced *while doing* this work stay local.
- **The walkthrough video linked at the top of the Quick Start will fall out of date** once the
  flow gains a step. Re-recording it is out of scope; the plan may propose a note acknowledging
  the drift.
