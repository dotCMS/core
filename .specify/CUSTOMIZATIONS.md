# dotCMS Spec-Kit Customizations

This project uses [GitHub Spec-Kit](https://github.com/github/spec-kit) (the `specify` CLI)
for spec-driven development, pinned to **v1.0.9** and initialized for Claude Code (skills).
It was previously pinned to v0.12.4; the upgrade is dotCMS/core#37649.

Beyond the stock install, dotCMS adds customizations for **legacy-awareness**,
**ADR integration**, an **ADR-creation guardrail**, and a **separate issue-resolution flow**.
Most of them live under names upstream never ships, including **three net-new skills** —
`speckit-adr-context`, `speckit-specify-fix` and `speckit-docs-converge` — which is what lets
them survive a regeneration untouched. This file records what changed, why, and how to
re-apply anything after a Spec-Kit upgrade.

## How Spec-Kit was installed

```bash
uv tool install specify-cli --from git+https://github.com/github/spec-kit.git@v1.0.9
# or, from an existing install: specify self upgrade --tag v1.0.9
# from the repo root:
specify init --here --force --non-interactive --integration claude --script sh
```

Pin the CLI to the exact tag rather than taking the latest: a bare upgrade silently picks up
whatever was released since, and the tree then matches a version nobody recorded.

## Upgrading: why `init --here --force` and not `specify integration upgrade`

From v1.0.9 the CLI ships `specify integration upgrade <key>` and `specify extension update`,
and upstream's own `docs/upgrade.md` calls `init --here --force` an "escape hatch". For this
repo the escape hatch is the correct tool, and the reason is worth knowing before the next
upgrade re-opens the question.

`.specify/integrations/claude.manifest.json` records a sha256 for exactly **ten files — the ten
shipped skill `SKILL.md` files** — and for nothing else. The shared scripts we patch
(`common.sh`, `setup-plan.sh`, `setup-tasks.sh`, `check-prerequisites.sh`) are not tracked in
it, and upstream refreshes shared scripts only when they still match a previously recorded
managed copy. The manifest-aware path would therefore leave those scripts at the old version
underneath ten freshly regenerated skills — the drifted state the upgrade exists to end. The
regenerated v1.0.9 manifest still tracks only those ten files, so this holds next time too.

`specify integration status` is still the right thing to run *after* an upgrade: it reports
modified and missing managed files, and should say zero of each.

This created `.specify/` and the `/speckit-*` skills under `.claude/skills/`. The install is
additive — it did not modify any pre-existing `.claude/` files.

## The standard flow

`/speckit-specify` (feature) **or** `/speckit-specify-fix` (issue) → `/speckit-plan` →
`/speckit-tasks` → `/speckit-implement` → `/speckit-converge`. Optional: `/speckit-clarify`,
`/speckit-checklist`, `/speckit-analyze`. Both spec flows funnel through `/speckit-plan`, so the
ADR and legacy gates apply to features and fixes alike.

`/speckit-converge` is the **mandatory closing step**, not an optional tool: `/speckit-implement`
fires it automatically (customization #7). Loop implement → converge until it reports
`converged`, then open PR 2.

## Spec-folder commit policy

What ships in a PR vs. stays local is decided by one test — **durable reference vs.
process artifact**:

- **Durable reference** — useful to a reviewer or future dev *after* the PR merges,
  without re-reading the code → **commit**.
- **Process artifact** — value is entirely *during* development → **keep local**
  (gitignored; the files still exist on disk, so `/speckit-implement` and
  `/speckit-converge` are unaffected).

| Artifact | Commit? | Why |
|----------|---------|-----|
| `spec.md` | Always | the reviewed contract (FRs, user stories, success criteria) |
| `data-model.md` | When it carries verified contracts | concrete entity→field/type, relationships, validation rules, real payload/DB shapes confirmed while building — the field-level ground truth `spec.md` stays above |
| `contracts/` | Same test as `data-model.md` | committed API specs are durable; scaffolding is not |
| `plan.md`, `research.md`, `tasks.md`, `quickstart.md`, `checklists/` | Never | pure process — how / what-order / decisions-in-flight |

The never-commit set is enforced by `.gitignore` (`specs/*/plan.md`, `research.md`,
`tasks.md`, `quickstart.md`, `checklists/`).

**`data-model.md` commit-worthiness bar:** commit it only if a future dev would need it
to know the shapes without reading the code. Its structure follows the plan template's
Phase 1 spec — *entity name, fields, relationships, validation rules from requirements*.
If a feature's `data-model.md` just restates entities already obvious from `spec.md`, it's
as ephemeral as the rest — don't commit it (same for `contracts/`).

## Customizations

### 1. Constitution — `.specify/memory/constitution.md` (AUTHORED)

Replaced the placeholder with dotCMS project law: Legacy-Aware Development, Config/Logger
discipline, security, contract correctness, **Test-First/TDD (Principle V, NON-NEGOTIABLE)**,
and the **ADR section** (mandatory consultation in the plan phase + the "never create ADRs"
guardrail). Loaded by every `/speckit-*` skill. Keep it in sync with the repo root `CLAUDE.md`.

**TDD gate (Principle V):** no implementation code is written before (1) tests are written
(unit/Postman/integration/Karate/e2e as applicable), (2) the developer validates and approves
them — or explicitly states which type can't be implemented and why, and (3) tests are
confirmed to FAIL (Red). Enforced via the constitution + the `tasks-template` override's
per-story `[GATE]` tasks + the plan's Test Strategy section. `/speckit-implement` reads the
constitution and executes tasks in order, halting at each gate.

### 2. ADR consultation in the plan phase — UPGRADE-SAFE (no shipped files edited)

ADRs live only in the private repo `dotCMS/platform-adrs`. Three additive pieces make the
plan phase ADR-aware:

- **`.specify/scripts/bash/adr-context.sh`** — read-only helper. Fetches `platform-adrs/INDEX.md`
  via the authenticated `gh` CLI and prints ADRs matching supplied keywords. GET-only; exits 0
  even with no matches (never blocks planning).
- **`.claude/skills/speckit-adr-context/SKILL.md`** — a skill that runs the helper, reads
  relevant ADR bodies, and summarizes them for the plan's ADR Alignment section.
- **`.specify/extensions.yml`** — registers `speckit.adr-context` as a **mandatory
  `before_plan` hook**, so `/speckit-plan` auto-invokes it before planning. (The shipped
  `/speckit-plan` skill already checks `.specify/extensions.yml` for `before_plan` hooks — we
  did not edit it.)
- **`.specify/templates/overrides/plan-template.md`** — adds the **ADR Alignment (Gate)** and
  **Legacy Impact** sections (see #3). This is the durable backstop: even if the hook does not
  fire, the plan template forces ADR consultation and the run of `adr-context.sh`.

### 3. Template overrides — UPGRADE-SAFE (`.specify/templates/overrides/`)

`resolve_template()` in `.specify/scripts/bash/common.sh` resolves
`overrides/ → presets/ → extensions/ → core`, so files here win without touching core
templates:

- **`overrides/plan-template.md`** — stock plan + **Legacy Impact** (touches `com.dotmarketing.*`?
  back-compat/migration, progressive enhancements) and **ADR Alignment (Gate)** (relevant ADRs,
  conflicts with accepted ADRs, **Proposed ADRs = propose-only**).
- **`overrides/spec-template.md`** — feature spec + a **Legacy Considerations** section. Used by
  `/speckit-specify` automatically.
- **`overrides/tasks-template.md`** — makes tests **mandatory** (stock template marks them
  optional) and bakes the TDD gates into every user story: Tests → `[GATE]` developer approval
  → `[GATE]` Red (confirmed failing) → Implementation. Includes a dotCMS test-type table
  (unit/integration/Postman/Karate/e2e).

### 4. Separate issue-resolution flow — ADDITIVE (one of the three net-new skills)

- **`.specify/templates/spec-issue-template.md`** — defect-framed spec: Problem, Reproduction,
  Scope of Investigation, Root-Cause Hypothesis, Fix Scope & Non-Goals, Regression Risk,
  Acceptance & Verification.
- **`.claude/skills/speckit-specify-fix/SKILL.md`** — `/speckit-specify-fix` command. Reuses
  `create-new-feature.sh` for numbering/dir, then swaps in the issue template. Keeps
  `/speckit-specify` = new features, `/speckit-specify-fix` = issue/bug resolution. Its output
  flows into `/speckit-plan`, so ADR + legacy gates apply to fixes too.

  **It must pass `--timestamp` itself.** `create-new-feature.sh` does not read
  `init-options.json`; it only accepts the flag. The shipped `/speckit-specify` honours
  `feature_numbering` because it builds the directory name itself, but this command delegates to
  the script — so without the flag, fixes would keep numbering sequentially while features
  numbered by timestamp, with nothing to report the divergence.

### 5. Timestamp feature numbering — CONFIGURATION ONLY (patch retired in #37649)

**`.specify/init-options.json`** sets `"feature_numbering": "timestamp"`, so a feature created
without an explicit `--number` is identified by `YYYYMMDD-HHMMSS`, which is collision-free by
construction. No script is patched.

Until #37649 this was a patch to `.specify/scripts/bash/create-new-feature.sh` adding
`get_highest_from_branches()`, a scan of branch names to stop two unmerged branches claiming the
same sequential number. It was retired rather than re-applied, for two reasons found by reading
the tree rather than the code:

- **The patch was inert here.** Its scan matched only Spec-Kit's exact zero-padded three-digit
  branch prefix. Of the 60 directories under `specs/`, zero carry one — every feature is named
  for its GitHub issue, as [Quick Start](../docs/core/SPEC_KIT_QUICK_START.md) instructs. The
  function returned 0 on every invocation, and the `max()` consuming it did nothing.
- **Sequential numbering was worse than useless here.** Upstream's `get_highest_from_specs()`
  matches prefixes of three digits *or more*, so it read the issue numbers as feature numbers:
  two unpushed branches both produced `37650`, a value that looks like a GitHub issue number and
  is not one.

The usual objection to timestamps — unreadable directory names — does not apply, because the
documented convention is to pass the issue number with `--number`. Auto-numbering is the
fallback for someone who skips that, and there a visibly machine-generated name is a signal
rather than a cost.

**Residual risk, accepted:** two features created within the same second still collide, and a
branch that exists only in another developer's clone cannot be observed at all.

### 6. eval-free feature-path resolution — EDITS SHIPPED SCRIPTS

Still necessary at v1.0.9 — upstream has not fixed this, so the patch was re-applied after the
upgrade. Re-derive it against the current function body rather than applying the old diff
textually: the body around it changed between 0.12.4 and v1.0.9, notably the `--no-persist`
branch that keeps read-only resolution from writing `feature.json`.

Stock Spec-Kit resolves feature paths by having `get_feature_paths()` (in `common.sh`)
print `%q`-quoted `KEY=value` lines that callers capture and `eval`. Semgrep flags that
`eval` as a blocking command-injection risk. Since every consumer sources `common.sh`
anyway, our patch has `get_feature_paths()` assign `REPO_ROOT`, `CURRENT_BRANCH`,
`FEATURE_DIR`, `FEATURE_SPEC`, `IMPL_PLAN`, `TASKS`, `RESEARCH`, `DATA_MODEL`,
`QUICKSTART`, and `CONTRACTS_DIR` directly in the caller's shell, and the three call
sites (`setup-plan.sh`, `setup-tasks.sh`, `check-prerequisites.sh`) call it plainly —
no output string is ever re-parsed as code.

### 7. Convergence as the closing step, developer-triggered — UPGRADE-SAFE (no shipped files edited)

Upstream ships `/speckit-converge` as step 9 of its quickstart, but nothing in our flow invoked
it, so the question *"does the code actually match the spec approved in PR 1?"* was never asked
before PR 2 — it was left for the reviewer to reconstruct from the diff, the exact failure mode
the two-PR flow exists to avoid.

One additive piece closes that:

- **`.specify/extensions.yml`** — registers `speckit.converge` as an **`after_implement` hook with
  `optional: true`**, so `/speckit-implement` *recommends* it on completing the task list,
  printing the command without running it. The shipped `/speckit-implement` skill **already**
  dispatches `after_implement` (its "Mandatory Post-Execution Hooks" section) — we did not edit
  it, or any other shipped skill.

**Why recommended and not mandatory.** An automatic run fires at the end of the task list, which
is the *least* final state of the work: in practice a developer finishes the tasks and then makes
several manual corrections. Converge would sign off on the code as it stood before that polishing,
and a stale `converged` verdict is worse than no verdict, because nobody re-reads it — while the
docs telling you it "runs automatically" actively train you not to. Only the developer knows when
their edits have stopped. So convergence stays part of the flow (Quick Start §1, §2, §3) with the
*timing* left to the developer. Note this diverges from dotCMS/core#37267's AC B, which specified
`optional: false`; the divergence and its reason are recorded in that issue.

Append-only by construction: converge's only write is a `## Phase N: Convergence` section at the
end of `tasks.md`, which is gitignored (see the commit policy above) and therefore never reaches
PR 2. Documented for developers in [Quick Start](../docs/core/SPEC_KIT_QUICK_START.md) §1, §3
and §9.

**Alternative considered — editing the shipped skill.** Extending
`.claude/skills/speckit-converge/SKILL.md` directly was rejected. It would have been the first
shipped `/speckit-*` skill we ever edited, and it creates a lose-lose with
`.specify/integrations/claude.manifest.json`: that file records a sha256 per shipped skill and is
read by the upstream `specify` CLI **on upgrade, to detect locally-modified files**. Refresh the
hash after editing and upgrade silently overwrites our customization; leave it stale and the
manifest is invalid. The additive route avoids the choice entirely — all ten hashes stay valid.

### 8. Documentation drift in the convergence step — UPGRADE-SAFE (no shipped files edited)

Upstream's `/speckit-converge` reads `spec.md`, `plan.md`, `tasks.md` and the codebase. It has no
notion of documentation, because a vanilla Spec-Kit repo has no normative documentation contract.
dotCMS does — `openapi.yaml` is build-verified and CI-enforced, `docs/` is the single source of
truth per domain, and `@Schema`/return-type correspondence is a Critical Rule. So a feature could
converge clean while the docs still described the old behavior.

Two additive pieces close that:

- **`.claude/skills/speckit-docs-converge/SKILL.md`** — a dotCMS-authored companion command. Same
  classification vocabulary (`missing`/`partial`/`contradicts`/`unrequested`), same severity
  scale, same append-only contract as converge. Assesses four surfaces: `docs/` + `CLAUDE.md`,
  `openapi.yaml` + REST annotations, `spec.md`/`plan.md` divergence, and Javadoc. It **never**
  edits a document, never edits `spec.md`/`plan.md`, and never runs a build — a stale
  `openapi.yaml` becomes a task carrying the `./mvnw compile` command, not a regeneration.
- **`.specify/extensions.yml`** — registers it as a **mandatory** `after_converge` hook. Mandatory
  here is not a contradiction of #7: by the time it fires, the developer has already chosen to
  converge, so extending that one decision to documentation needs no second prompt. The shipped
  `/speckit-converge` skill **already** dispatches `after_converge` (its Execution Step 9) — we
  did not edit it.

Because it is a new skill, two governance files must move with it or the required
`cicd_pr_skill-lint` check fails: `.claude/skills/skills.config.json` (its name is added to
`grandfathered`, as with every other `speckit-*` skill) and `.claude/skills/CATALOG.md`
(regenerated with `just skills-catalog`, never hand-edited).

**Two behaviors that make a mandatory hook safe to run automatically:**

- **Incomplete-run guard** — if `tasks.md` still has unchecked non-`[GATE]` tasks, implement
  halted rather than finished; it reports `implementation_incomplete` and writes nothing.
  Without this, every TDD gate approval would append duplicates of already-open tasks.
- **Dedupe** — a finding already represented by a task in a prior Convergence phase, *checked or
  unchecked*, is never re-emitted. Each pass yields strictly fewer new findings, so the loop
  provably terminates; and a finding the developer consciously accepted (ticked `[X]`) is never
  raised again. **Known asymmetry:** the shipped `/speckit-converge` has neither behavior and we
  won't edit it to add them, so its own findings can recur. v1.0.9 makes this upstream's explicit
  intent rather than an oversight: converge is now told to include every existing task in its
  inventory "regardless of checkbox state or Convergence phase", because "completion claims are
  not evidence". That is handled in documentation —
  [Quick Start](../docs/core/SPEC_KIT_QUICK_START.md) §3 and §9 define the gate as *`converged`,
  or every remaining finding consciously accepted*, and §10 carries the symptom row.

### 9. An executable check that the customizations survived — UPGRADE-SAFE

**`.specify/scripts/bash/verify-customizations.sh`** asserts, in one run, the things above that
can be observed from disk: that the pin, the installed CLI and the version floor agree; that no
vendored script re-parses a produced string as code; that the three template overrides still
*win* resolution rather than merely existing; that the constitution still carries Principle V and
the never-create-ADRs guardrail; that all three hooks name skills that exist and keep their
mandatory/optional flag; that the net-new files survived; that a pre-upgrade spec directory still
resolves without being written to; that numbering is collision-free in both spec flows; and that
this document still describes the tree.

Why it exists: every customization here fails by **absence**. A regeneration that drops an
override or flips a mandatory hook to optional produces no error and no failing build — the flow
keeps working and quietly stops asking for the dotCMS content. "It ran without error" is not
evidence; this is. Run it after any upgrade, and before trusting a regenerated tree.

What it cannot assert, by construction: whether an agent *obeys* a skill. That
`/speckit-plan` fires the ADR hook unasked, that `/speckit-specify` emits Legacy Considerations,
that `/speckit-tasks` orders the TDD gates and that `/speckit-implement` halts at them are
properties of an agent interpreting Markdown — no process to invoke, no exit code to read.
Grepping a skill for the word "hook" would assert that the text exists, which is the weaker claim.
Those stay manual; [Quick Start](../docs/core/SPEC_KIT_QUICK_START.md) carries the walkthrough.

## `.specify/feature.json` — assessed, and a no-op for us

Upstream's per-checkout pointer to the active feature directory. It is worth a paragraph only
because it is easy to mistake for new surface arriving with v1.0.9, and then to go looking for a
migration that does not exist.

**It is not new.** The 0.12.4 tree already read it: `read_feature_json_feature_directory()` and
`_persist_feature_json()` are in that version's `common.sh`, and resolution order was already
`SPECIFY_FEATURE_DIRECTORY` → `.specify/feature.json` → error. What was true before the upgrade is
that the **file** did not exist in most checkouts, not that the mechanism was absent. v1.0.9
changes nothing about it: `create-new-feature.sh` persists it, and `check-prerequisites.sh` passes
`--no-persist` so read-only path resolution never dirties the working tree.

**What did change** is that v1.0.9 ships a managed `.specify/.gitignore` carrying `feature.json`,
so the rule that used to live in the repo root `.gitignore` was removed in #37649 — its own comment
had said it lived there only "until the upgrade".

**Verified, not assumed**: a spec directory created *before* the upgrade still resolves to itself
afterwards, and resolving it writes nothing. `verify-customizations.sh` asserts both, fingerprinting
the directory before and after so the read-only property is checked rather than merely not
violated. No in-flight work is orphaned and no migration step is needed for anyone's open branch.

## Guardrail: Spec-Kit must never create ADRs

Enforced in the constitution, the `adr-context.sh` output, the `speckit-adr-context` and
`speckit-specify-fix` skills, and the plan template's "Proposed ADRs" section. Spec-Kit only
**proposes** ADRs; they are authored solely in `dotCMS/platform-adrs` via its `new-adr.sh`.

## Re-applying after a `specify` upgrade

A future `specify init --force` / upgrade can overwrite files **shipped by Spec-Kit**. Our
customizations are split so that most survive automatically:

| Path | Survives upgrade? | Action after upgrade |
|------|-------------------|----------------------|
| `.specify/memory/constitution.md` | Usually (not overwritten unless re-init) | Verify still present; re-author if reset |
| `.specify/templates/overrides/*` | ✅ Yes (overrides dir is ours) | None |
| `.specify/templates/spec-issue-template.md` | ✅ Yes (net-new name) | None |
| `.specify/extensions.yml` | ✅ Yes (net-new; not shipped) | Verify all three hooks still match skill names: `before_plan`, `after_implement`, `after_converge` |
| `.specify/scripts/bash/adr-context.sh` | ✅ Yes (net-new name) | None |
| `.claude/skills/speckit-adr-context/`, `.claude/skills/speckit-specify-fix/`, `.claude/skills/speckit-docs-converge/` | ✅ Yes (net-new skills) | Confirm not clobbered; re-run `just skills-lint` |
| `.claude/skills/skills.config.json`, `.claude/skills/CATALOG.md` | ✅ Yes (ours, not shipped) | Verify all 13 `speckit-*` names are still in `grandfathered`; re-run `just skills-catalog`, then `just skills-lint` |
| `.specify/scripts/bash/adr-context.sh`, `verify-customizations.sh` | ✅ Yes (net-new names) | None — but run `verify-customizations.sh` first thing after the upgrade |
| `.specify/init-options.json` | ❌ No (regenerated) | Re-set `"feature_numbering": "timestamp"` (#5); confirm `speckit_version` matches the CLI you used |
| `.specify/scripts/bash/common.sh`, `setup-plan.sh`, `setup-tasks.sh`, `check-prerequisites.sh` | ❌ No (shipped scripts, patched in-place) | Re-apply the eval-free path resolution patch (#6): direct assignment in `get_feature_paths()` + plain calls at the three call sites. **Re-derive it against the new function bodies; do not apply the old diff textually.** |

**One** shipped file is patched in place — `common.sh` and the three call sites of patch #6,
counted as one patch. `create-new-feature.sh` left that list in #37649 when patch #5 was retired.
We do **not edit** any shipped `/speckit-*` skill or core template, and a gate that an upgrade
weakens must be re-imposed from a file we own, never by editing the shipped skill.

If a future Spec-Kit version changes the `before_plan` hook contract or `resolve_template`
precedence, re-verify items #2 and #3 above — and note that from v1.0.9 the constitution scaffold
is also resolved through that stack, so an `overrides/constitution-template.md` is available if
`/speckit-constitution` ever needs to be stopped from reshaping our authored constitution. We do
not ship one today; that was a conscious decision in #37649, not an oversight.

### What the last upgrade found

The individual review of the ten regenerated skills for the v1.0.9 upgrade is committed at
`specs/37649-speckit-upgrade-latest/shipped-skills-review.md`. Start the next upgrade by diffing
against it rather than repeating the investigation. Its headline: no shipped skill weakened either
gate, and `speckit-plan` strengthened the ADR gate — a malformed `extensions.yml` used to skip
hook checking *silently*, which would have disabled our mandatory ADR consultation with no
symptom at all.

## Alternative considered: the native `bug` extension

Spec-Kit ships a `bug` extension (`/speckit-bug-assess|fix|test`, `specify extension add bug`).
It is a parallel assess→fix→test workflow that stores reports under `.specify/bugs/<slug>/` and
**does not pass through `/speckit-plan`** — so it would bypass our ADR Alignment gate. We chose
the `/speckit-specify-fix` approach instead so issue-resolution plans are still ADR- and
legacy-checked. Revisit if the bug extension gains a planning/ADR step.

**Re-checked at v1.0.9 (2026-09-22): verdict unchanged.** `extensions/bug/extension.yml` still
provides exactly `speckit.bug.assess`, `speckit.bug.fix` and `speckit.bug.test`, still stores
reports under `.specify/bugs/<slug>/`, and still has no route into `/speckit-plan`. The single
occurrence of "plan" in the extension is "Confirm the plan" in `speckit.bug.fix.md`, referring to
the remediation the assess step proposed, not to Spec Kit's plan phase. Adopting it would still
bypass the ADR Alignment gate.
