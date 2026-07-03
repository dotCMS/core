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
`/speckit-tasks` → `/speckit-implement`. Optional: `/speckit-clarify`, `/speckit-checklist`,
`/speckit-analyze`. Both spec flows funnel through `/speckit-plan`, so the ADR and legacy
gates apply to features and fixes alike.

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
| `.specify/extensions.yml` | ✅ Yes (net-new; not shipped) | Verify hook still matches skill name |
| `.specify/scripts/bash/adr-context.sh` | ✅ Yes (net-new name) | None |
| `.claude/skills/speckit-adr-context/`, `.claude/skills/speckit-specify-fix/` | ✅ Yes (net-new skills) | Confirm not clobbered |

We intentionally **did not edit** any shipped `/speckit-*` skill or core template, so there is
no manual patch to re-apply. If a future Spec-Kit version changes the `before_plan` hook
contract or `resolve_template` precedence, re-verify items #2 and #3 above.

## Alternative considered: the native `bug` extension

Spec-Kit ships a `bug` extension (`/speckit-bug-assess|fix|test`, `specify extension add bug`).
It is a parallel assess→fix→test workflow that stores reports under `.specify/bugs/<slug>/` and
**does not pass through `/speckit-plan`** — so it would bypass our ADR Alignment gate. We chose
the `/speckit-specify-fix` approach instead so issue-resolution plans are still ADR- and
legacy-checked. Revisit if the bug extension gains a planning/ADR step.
