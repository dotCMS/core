---
name: "speckit-docs-converge"
description: "Extend the convergence gap analysis to documentation drift — docs/, CLAUDE.md, openapi.yaml and REST annotations, spec/plan divergence, and Javadoc. Append-only: findings become tasks in tasks.md, never direct edits. Runs as an after_converge hook."
argument-hint: "Optional narrowing hint (a doc area or file to focus on). Defaults to the feature's artifacts."
compatibility: "Requires spec-kit project structure with .specify/ directory and a feature with spec.md, plan.md and tasks.md"
metadata:
  author: "dotcms"
  source: "dotcms customization (see .specify/CUSTOMIZATIONS.md)"
user-invocable: true
disable-model-invocation: false
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Pre-Execution Checks

**Check for extension hooks (before documentation convergence)**:

- Check if `.specify/extensions.yml` exists in the project root.
- If it exists, read it and look for entries under the `hooks.before_docs_converge` key
- If the YAML cannot be parsed or is invalid, skip hook checking silently and continue normally
- Filter out hooks where `enabled` is explicitly `false`. Treat hooks without an `enabled` field as enabled by default.
- For each remaining hook, do **not** attempt to interpret or evaluate hook `condition` expressions:
  - If the hook has no `condition` field, or it is null/empty, treat the hook as executable
  - If the hook defines a non-empty `condition`, skip the hook and leave condition evaluation to the HookExecutor implementation
- When constructing slash commands from hook command names, replace dots (`.`) with hyphens (`-`). For example, `speckit.git.commit` → `/speckit-git-commit`.
- For each executable hook, output the following based on its `optional` flag:
  - **Optional hook** (`optional: true`):

    ```text
    ## Extension Hooks

    **Optional Pre-Hook**: {extension}
    Command: `/{command}`
    Description: {description}

    Prompt: {prompt}
    To execute: `/{command}`
    ```

  - **Mandatory hook** (`optional: false`):

    ```text
    ## Extension Hooks

    **Automatic Pre-Hook**: {extension}
    Executing: `/{command}`
    EXECUTE_COMMAND: {command}

    Wait for the result of the hook command before proceeding to the Goal.
    ```
    After emitting the block above you MUST actually invoke the hook and wait for it to finish before continuing. Run it the same way you would run the command yourself in this agent/session (the invocation may differ from the literal `{command}` id shown above, e.g. a skills-mode agent runs it as `/skill:speckit-...` or `$speckit-...`). Emitting the block alone does not run the hook.

- If no hooks are registered or `.specify/extensions.yml` does not exist, skip silently

## Goal

`/speckit-converge` closes the gap between the feature's artifacts and the **code**. This command
closes the gap between the artifacts and the **documentation** — and in dotCMS that is not a
nicety, because several documents here are normative or build-verified:

- `openapi.yaml` is auto-generated at compile and **CI fails when the committed file does not
  match the build** (root `CLAUDE.md` → OpenAPI / Swagger; Constitution Principle IV).
- `docs/` is the single source of truth for backend and frontend standards; `CLAUDE.md` is the
  always-loaded navigation hub.
- The `@Schema` / return-type correspondence is a Critical Rule.

So a feature can converge "clean" on code while `docs/` still describes the old behavior and
`openapi.yaml` still advertises the old contract. This command finds that, and — exactly like
converge — **appends the remaining work as tasks** rather than fixing it itself.

It runs automatically as an `after_converge` hook. You can also run it by hand.

## Operating Constraints

**APPEND-ONLY, NEVER REWRITE**: The command's **only** write is appending a new
`## Phase N: Documentation Convergence` section to `tasks.md`. It MUST NOT:

- edit `docs/`, any `CLAUDE.md`, `openapi.yaml`, Javadoc, or any source file — completing the
  appended tasks is the job of `/speckit-implement`;
- edit `spec.md` or `plan.md` **under any circumstance**, including when it detects that the
  implementation diverged from them. Editing an approved spec to match what was built rewrites
  the contract a reviewer signed off on and inverts the whole point of the two-PR flow. Report
  the divergence; let a human reconcile it;
- rewrite, renumber, reorder, or delete any existing task, including tasks from a prior
  Documentation Convergence phase;
- **run a build.** No `./mvnw`, no `pnpm`, no code generation. This command runs automatically
  after every implement pass; a Maven compile in that path is unacceptable. For a suspected
  stale `openapi.yaml` it emits a *task* telling the developer to regenerate it;
- create or edit an ADR, here or in `dotCMS/platform-adrs` (Constitution → ADRs);
- make network calls.

When documentation already matches, the command MUST leave `tasks.md` **byte-for-byte
unchanged** (no empty phase header) and report a clean result.

**Constitution Authority**: `.specify/memory/constitution.md` is non-negotiable. Documentation
that contradicts a MUST principle — most often Principle IV's contract-correctness rules — is
the highest-severity finding. If the constitution is an unfilled template, skip constitution
checks gracefully rather than failing.

## Execution Steps

### 1. Initialize Context

Run `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks` once
from repo root and parse JSON for FEATURE_DIR. Derive absolute paths:

- SPEC = FEATURE_DIR/spec.md
- PLAN = FEATURE_DIR/plan.md
- TASKS = FEATURE_DIR/tasks.md
- CONSTITUTION = `.specify/memory/constitution.md` (if present)

If `spec.md`, `plan.md`, or `tasks.md` is missing, STOP with a clear message naming the
prerequisite command (`/speckit-specify` or `/speckit-specify-fix`, `/speckit-plan`,
`/speckit-tasks`). Do not produce partial output.

For single quotes in args like "I'm Groot", use escape syntax: e.g 'I'\''m Groot' (or
double-quote if possible: "I'm Groot").

### 2. Incomplete-Run Guard (do this before anything else)

Scan `tasks.md` for unchecked tasks that are **not** `[GATE]` tasks.

**Match `[GATE]` in the label position only** — immediately after the task ID and any `[P]` /
`[US#]` labels — never anywhere in the line. A task whose *description* mentions `[GATE]` (for
instance, one about gate handling) is an ordinary task. A naive `grep -v '[GATE]'` misclassifies
it, and if it were the last one left the guard would wave an incomplete run through. Concretely:

```bash
grep -E '^- \[ \] T[0-9]+ (\[P\] )?(\[US[0-9]+\] )?\[GATE\]'   # gate tasks
grep -E '^- \[ \] T[0-9]+ (\[P\] )?(\[US[0-9]+\] )?(\[GATE\])?' # all unchecked; subtract the above
```

If any remain, `/speckit-implement` halted rather than finished — parked at a TDD gate awaiting
approval, or stopped on a failed task. Assessing documentation now is meaningless: the feature
is half-built, and every not-yet-written doc would be reported as `missing`, duplicating tasks
that are already open.

In that case:

- report outcome **`implementation_incomplete`** with the message *"implementation incomplete —
  finish the task list first"*, naming how many non-gate tasks remain;
- **write nothing**;
- skip to Step 8 (hooks).

This is the normal state after every `[GATE]` approval, so it must be quiet, not noisy.

### 3. Load Artifacts (Progressive Disclosure)

Load only what is needed:

**From spec.md**: Functional Requirements (FR-###), Success Criteria (SC-###), user-story
acceptance scenarios, and any documentation commitments the spec makes explicitly.

**From plan.md**: the file paths and components the plan says will be created or edited; the
architecture decisions a reader of `docs/` would need to know about.

**From tasks.md**: task IDs (to compute the next ID and phase number), descriptions, phase
grouping, referenced file paths, and **every task in any prior Convergence or Documentation
Convergence phase** — needed for dedupe in Step 5.

**From the constitution**: principle names and MUST/SHOULD statements bearing on documentation
(chiefly Principle IV).

### 4. Assess the Four Documentation Surfaces

Derive the set of source files the artifacts name as changed. **Bound the assessment to those
files and the documents that describe them.** Do not sweep the repo — an unbounded documentation
pass over a codebase this size produces findings about everything and gets ignored.

For each surface, produce a `Finding` only where there is a real, evidenced gap.

**A. `docs/` and `CLAUDE.md`**
Reverse-lookup from each changed file/behavior to the documents that describe it: `docs/**/*.md`,
root `CLAUDE.md`, `core-web/CLAUDE.md`, and any `CLAUDE.md` inside a touched module. A document
that describes a behavior, command, or pattern the implementation changed, and no longer matches
it, is a finding.

**B. `openapi.yaml` and REST contracts**
Only when the feature touched a JAX-RS resource. Check that `@Operation`, `@Parameter` and
`@Schema` annotations describe what was actually built (`@Schema` must match the real return
type — Critical Rule), and that the committed
`dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml` reflects the current annotations.
**Never run the build** to determine this; reason from the annotations and the committed yaml.
The emitted task instructs the developer to run
`./mvnw compile -pl :dotcms-core -DskipTests` and commit the regenerated yaml.

**C. `spec.md` / `plan.md` back-annotation**
Where the final implementation diverged from the approved intent, report it so a human can
reconcile it. **Never edit either file.** If the spec was already approved on PR 1, say so and
flag that the divergence needs **re-approval** per Quick Start §3.

**D. Javadoc and code comments**
Comments on code the feature changed that still describe its previous behavior.

Classify each finding with the **existing** vocabulary — do not invent a new one:

- **`missing`** — behavior was built and nothing documents it where conventions say it should be.
- **`partial`** — a doc covers the area but is incomplete for what was built.
- **`contradicts`** — a doc actively states something now false. Highest-value class: the reader
  is misled, not merely uninformed.
- **`unrequested`** — documentation describing behavior nothing in spec, plan, or tasks asked
  for. Surfaced for awareness; never deleted.

Every finding records: a stable `D`-prefixed id, the surface, the **repo-relative file path**
(mandatory), the `source-ref` it traces to (`FR-003`, `US1/AC2`, `plan: storage decision`,
`Constitution IV`), the gap type, a severity, and the **evidence** — what the document currently
says versus what was built. **No evidence, no finding.**

### 5. Dedupe Against Prior Convergence Phases

Drop any finding already represented by a task in a prior `## Phase N: Convergence` or
`## Phase N: Documentation Convergence` section — **whether that task is checked or unchecked**.

- *Unchecked* means the work is already queued.
- *Checked* means the developer looked at it and consciously accepted it — typically an
  `unrequested` finding they decided to keep.

Re-raising either is noise, and this rule is what makes the loop **terminate**: every pass yields
strictly fewer new findings than the last, so the sequence must reach zero. Without it, a
correct-but-declined finding would block PR 2 forever.

### 6. Assign Severity

- **CRITICAL** — a document the constitution treats as normative is wrong in a way CI or a
  Critical Rule depends on: a stale `openapi.yaml`, or an `@Schema` that no longer matches the
  return type (Principle IV).
- **HIGH** — `contradicts` on `docs/` or `CLAUDE.md`; or implementation divergence from an
  **approved** spec.
- **MEDIUM** — `missing` or `partial` on `docs/`/`CLAUDE.md`; Javadoc describing superseded
  behavior.
- **LOW** — comment rot with no behavioral claim; `unrequested` documentation.

### 7. Present Findings, Then Append (or report converged)

Output the summary **before** writing anything:

## Documentation Convergence Findings

| ID | Surface | Gap Type | Severity | File | Source | Evidence | Remaining Work |
|----|---------|----------|----------|------|--------|----------|----------------|
| D1 | openapi | contradicts | CRITICAL | dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml | Constitution IV | committed yaml lacks the `siteId` parameter added to FolderResource | Regenerate and commit |

**Summary metrics**: surfaces checked; findings by gap type; findings by severity; findings
suppressed by dedupe.

**If there are one or more actionable findings** (`tasks_appended`):

1. Let `M` be the maximum existing task ID and `N` the highest existing phase number.
2. Append a single new section header `## Phase N+1: Documentation Convergence`.
3. Emit one checklist item per finding, ordered severity-descending, with zero-padded IDs
   `T{M+1:03d}, T{M+2:03d}, …`:

   ```markdown
   - [ ] T042 <imperative description> in <file-path> per <source-ref> (<gap-type>)
   ```

   `<file-path>` is **mandatory** — a documentation task the developer cannot act on without
   re-deriving the target is not actionable.
4. Never reuse or renumber existing IDs. If a prior Documentation Convergence phase exists,
   append a new, separately-numbered one below it — do not touch the old one.

Documentation findings land in a **later phase** than converge's code findings, which is what
keeps them ordered after constitution violations without any cross-command sorting.

**If there are no actionable findings** (`converged`):

- Do **not** modify `tasks.md` at all — no empty phase header.
- Report: **"✅ Documentation converged — `docs/`, `CLAUDE.md`, REST contracts and comments match
  what was built."**
- Include the summary counts of what was checked.

**Handoff**: on `tasks_appended`, state how many tasks were appended under which phase and
recommend `/speckit-implement`. On `converged`, note that documentation is clear — and that PR 2
is unblocked provided `/speckit-converge` also reported `converged` (or its remaining findings
were consciously accepted; see Quick Start §3 and §9).

### 8. Check for extension hooks

After producing the result, check if `.specify/extensions.yml` exists in the project root.

- If it exists, read it and look for entries under the `hooks.after_docs_converge` key
- If the YAML cannot be parsed or is invalid, skip hook checking silently and continue normally
- Filter out hooks where `enabled` is explicitly `false`. Treat hooks without an `enabled` field as enabled by default.
- For each remaining hook, do **not** attempt to interpret or evaluate hook `condition` expressions:
  - If the hook has no `condition` field, or it is null/empty, treat the hook as executable
  - If the hook defines a non-empty `condition`, skip the hook and leave condition evaluation to the HookExecutor implementation
- Report the outcome (`converged`, `tasks_appended`, or `implementation_incomplete`) in-session
  before listing any hooks, so users can decide whether to run optional follow-up commands.
- When constructing slash commands from hook command names, replace dots (`.`) with hyphens (`-`). For example, `speckit.git.commit` → `/speckit-git-commit`.
- For each executable hook, output the following based on its `optional` flag:
  - **Optional hook** (`optional: true`):

    ```text
    ## Extension Hooks

    **Optional Hook**: {extension}
    Command: `/{command}`
    Description: {description}

    Prompt: {prompt}
    To execute: `/{command}`
    ```

  - **Mandatory hook** (`optional: false`):

    ```text
    ## Extension Hooks

    **Automatic Hook**: {extension}
    Executing: `/{command}`
    EXECUTE_COMMAND: {command}
    ```
    After emitting the block above you MUST actually invoke the hook and wait for it to finish before continuing. Run it the same way you would run the command yourself in this agent/session (the invocation may differ from the literal `{command}` id shown above, e.g. a skills-mode agent runs it as `/skill:speckit-...` or `$speckit-...`). Emitting the block alone does not run the hook.

- If no hooks are registered or `.specify/extensions.yml` does not exist, skip silently
