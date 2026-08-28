# dotCMS Spec-Kit Customizations

This project uses [GitHub Spec-Kit](https://github.com/github/spec-kit) (the `specify` CLI)
for spec-driven development, pinned to **v0.12.4** and initialized for Claude Code (skills).

Beyond the stock install, dotCMS adds four customizations for **legacy-awareness**,
**ADR integration**, an **ADR-creation guardrail**, and a **separate issue-resolution flow**.
This file records what changed, why, and how to re-apply anything after a Spec-Kit upgrade.

## How Spec-Kit was installed

```bash
uv tool install specify-cli --from git+https://github.com/github/spec-kit.git@v0.12.4
# from the repo root:
specify init --here --integration claude --force --script sh
```

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

### 4. Separate issue-resolution flow — ADDITIVE

- **`.specify/templates/spec-issue-template.md`** — defect-framed spec: Problem, Reproduction,
  Scope of Investigation, Root-Cause Hypothesis, Fix Scope & Non-Goals, Regression Risk,
  Acceptance & Verification.
- **`.claude/skills/speckit-specify-fix/SKILL.md`** — `/speckit-specify-fix` command. Reuses
  `create-new-feature.sh` for numbering/dir, then swaps in the issue template. Keeps
  `/speckit-specify` = new features, `/speckit-specify-fix` = issue/bug resolution. Its output
  flows into `/speckit-plan`, so ADR + legacy gates apply to fixes too.

### 5. Branch-aware feature numbering — EDITS A SHIPPED SCRIPT

**`.specify/scripts/bash/create-new-feature.sh`** — stock Spec-Kit derives the next
sequential feature number only from `specs/*` directories, which exist **only on the branch
that created them**. Two unmerged feature branches would therefore both claim the same next
number. Our patch adds `get_highest_from_branches()` — a best-effort scan of branch names
(`git ls-remote --heads origin` when reachable, local + remote-tracking refs offline) — and
takes the max of both sources. The scan matches **only** Spec-Kit's exact zero-padded
3-digit prefix (`^[0-9]{3}-`) so legacy issue-numbered branches (e.g. `16227-test-branch`)
never inflate the counter. Collisions remain possible only if two devs create features
simultaneously before either pushes — rerun with `--number` to resolve, or use
`--timestamp` numbering which is collision-free by construction.

### 6. eval-free feature-path resolution — EDITS SHIPPED SCRIPTS

Stock Spec-Kit resolves feature paths by having `get_feature_paths()` (in `common.sh`)
print `%q`-quoted `KEY=value` lines that callers capture and `eval`. Semgrep flags that
`eval` as a blocking command-injection risk. Since every consumer sources `common.sh`
anyway, our patch has `get_feature_paths()` assign `REPO_ROOT`, `CURRENT_BRANCH`,
`FEATURE_DIR`, `FEATURE_SPEC`, `IMPL_PLAN`, `TASKS`, `RESEARCH`, `DATA_MODEL`,
`QUICKSTART`, and `CONTRACTS_DIR` directly in the caller's shell, and the three call
sites (`setup-plan.sh`, `setup-tasks.sh`, `check-prerequisites.sh`) call it plainly —
no output string is ever re-parsed as code.

### 7. Convergence as the mandatory closing step — UPGRADE-SAFE (no shipped files edited)

Upstream ships `/speckit-converge` as step 9 of its quickstart, but nothing in our flow invoked
it, so the question *"does the code actually match the spec approved in PR 1?"* was never asked
before PR 2 — it was left for the reviewer to reconstruct from the diff, the exact failure mode
the two-PR flow exists to avoid.

One additive piece closes that:

- **`.specify/extensions.yml`** — registers `speckit.converge` as a **mandatory `after_implement`
  hook**, so `/speckit-implement` auto-invokes it on completing the task list. The shipped
  `/speckit-implement` skill **already** dispatches `after_implement` (its "Mandatory
  Post-Execution Hooks" section) — we did not edit it, or any other shipped skill.

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
- **`.specify/extensions.yml`** — registers it as a mandatory `after_converge` hook. The shipped
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
  won't edit it to add them, so its own findings can recur. That is handled in documentation —
  [Quick Start](../docs/core/SPEC_KIT_QUICK_START.md) §3 and §9 define the gate as *`converged`,
  or every remaining finding consciously accepted*, and §10 carries the symptom row.

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
| `.claude/skills/skills.config.json`, `.claude/skills/CATALOG.md` | ✅ Yes (ours, not shipped) | Verify `speckit-docs-converge` is still in `grandfathered`; re-run `just skills-catalog` |
| `.specify/scripts/bash/create-new-feature.sh` | ❌ No (shipped script, patched in-place) | Re-apply the branch-aware numbering patch (#5): `get_highest_from_branches()` + the max() at the `BRANCH_NUMBER` computation |
| `.specify/scripts/bash/common.sh`, `setup-plan.sh`, `setup-tasks.sh`, `check-prerequisites.sh` | ❌ No (shipped scripts, patched in-place) | Re-apply the eval-free path resolution patch (#6): direct assignment in `get_feature_paths()` + plain calls at the three call sites |

Apart from the script patches (#5, #6), we did **not edit** any shipped `/speckit-*` skill
or core template. If a future Spec-Kit version changes the `before_plan` hook contract or
`resolve_template` precedence, re-verify items #2 and #3 above.

## Alternative considered: the native `bug` extension

Spec-Kit ships a `bug` extension (`/speckit-bug-assess|fix|test`, `specify extension add bug`).
It is a parallel assess→fix→test workflow that stores reports under `.specify/bugs/<slug>/` and
**does not pass through `/speckit-plan`** — so it would bypass our ADR Alignment gate. We chose
the `/speckit-specify-fix` approach instead so issue-resolution plans are still ADR- and
legacy-checked. Revisit if the bug extension gains a planning/ADR step.
