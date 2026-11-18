# Phase 1 Data Model: Converge as the mandatory closing step

**Feature**: `37267-converge-closing-step` | **Date**: 2026-08-28
**Plan**: [`plan.md`](./plan.md)

These are not database tables or Java classes — this change ships no code. They are the
**conceptual entities the new skill reasons about**, written down so `SKILL.md` and the
contracts describe the same things with the same words, and so a reviewer can check a finding
against a definition rather than against intuition.

---

## Entity: `DocumentationFinding`

The unit of output. One per genuine gap between what was built and what the documentation says.
Deliberately mirrors the shipped `Finding` in `speckit-converge/SKILL.md` §4 so the two skills'
output reads as one vocabulary.

| Field | Type | Rules |
|---|---|---|
| `id` | string | Stable within a run. `D1`, `D2`, … — `D`-prefixed so it never collides with converge's `F1`, `F2` in a combined reading. |
| `surface` | enum | One of `docs`, `openapi`, `spec-plan`, `javadoc`. See `DocSurface` below. Exactly one — a finding spanning two surfaces is two findings. |
| `file-path` | string | **Required.** Repo-relative path of the document to change (`docs/backend/REST_API_PATTERNS.md`, not "the REST docs"). AC-003 requires every finding to carry it, and AC-004 requires the emitted task to name it. |
| `source-ref` | string | What the finding traces back to: `FR-003`, `SC-002`, `US1/AC2`, `plan: storage decision`, `Constitution IV`. Same vocabulary as converge. |
| `gap-type` | enum | `missing` \| `partial` \| `contradicts` \| `unrequested`. The **existing** vocabulary — AC-003 forbids inventing a new one. |
| `severity` | enum | `CRITICAL` \| `HIGH` \| `MEDIUM` \| `LOW`. Same scale as converge, applied per the mapping below. |
| `evidence` | string | The observed fact: which file, which section, what it currently says vs. what was built. A finding without evidence is a guess and must not be emitted. |
| `remaining-work` | string | Imperative phrasing, becomes the task body. |

### Validation rules

1. **Evidence is mandatory.** No file read → no finding. This is what makes AC-004's
   zero-findings control achievable rather than aspirational.
2. **`file-path` must exist** in the working tree, except when `gap-type = missing` and the
   remaining work is to *create* the document — in which case the path is the file to create.
3. **Scope bound.** The finding must trace to a file named in `plan.md`/`tasks.md` or reachable
   from one by the reverse-lookup in `DocSurface.docs`. Findings outside the artifacts' scope
   are not emitted (research R4) — otherwise a docs pass over a repo this size drowns the
   signal.
4. **`spec-plan` findings are report-only.** They may never produce a task that edits `spec.md`
   or `plan.md`. See the state note under `DocSurface` below.

### Gap-type mapping for documentation

The existing vocabulary was written for code. This is how it reads against a document, so two
runs classify the same drift the same way:

| Gap type | Documentation meaning |
|---|---|
| `missing` | Behavior was built and **nothing documents it** where the repo's conventions say it should be. |
| `partial` | A doc covers the area but is **incomplete** for what was built (new parameter undocumented, new option unlisted). |
| `contradicts` | A doc **actively states something now false** — the highest-value class, because a reader is misled rather than merely uninformed. |
| `unrequested` | Documentation describes behavior **nothing in the spec, plan, or tasks asked for**. Surfaced for awareness; never deleted by this skill. |

### Severity mapping

| Severity | When |
|---|---|
| `CRITICAL` | A doc the constitution treats as normative is wrong in a way CI or a Critical Rule depends on — chiefly a stale `openapi.yaml` (Principle IV; **the OpenAPI CI check will fail**) or an `@Schema` that no longer matches the return type. |
| `HIGH` | `contradicts` on `docs/` or `CLAUDE.md`; or an implementation divergence from an **approved** spec (needs re-approval per Quick Start §3). |
| `MEDIUM` | `missing` or `partial` on `docs/`/`CLAUDE.md`; Javadoc that describes superseded behavior. |
| `LOW` | Comment rot on changed code with no behavioral claim; `unrequested` documentation. |

---

## Entity: `DocSurface`

The four assessment surfaces from AC-003. Fixed set — the skill does not invent a fifth.

| Surface | Inputs read | Emits a task that… | Never |
|---|---|---|---|
| `docs` | `docs/**/*.md`, root `CLAUDE.md`, `core-web/CLAUDE.md`, and any `CLAUDE.md` under a touched module | updates the named document | edits it directly |
| `openapi` | JAX-RS sources named in the artifacts; their `@Operation`/`@Parameter`/`@Schema`; `dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml` | tells the dev to fix annotations and re-run `./mvnw compile -pl :dotcms-core -DskipTests`, then commit the regenerated yaml | runs Maven, or hand-edits `openapi.yaml` — it is generated (Principle IV) |
| `spec-plan` | `spec.md`, `plan.md` | asks a **human** to reconcile the divergence; if the spec was already approved, says so and points at Quick Start §3 re-approval | edits `spec.md` or `plan.md` under any circumstance |
| `javadoc` | Javadoc and code comments on files the artifacts name as changed | corrects the stale comment | rewrites unrelated comments |

> **Why `spec-plan` is report-only.** Editing an approved spec to match what was built would
> silently rewrite the contract a reviewer signed off on — inverting the entire point of the
> two-PR flow. The shipped converge skill already forbids it; this skill inherits the ban
> rather than relaxing it.

---

## Entity: `ConvergenceOutcome`

The result of one run. Reuses the shipped skill's two values so the chain reports in one voice,
plus one state the clarification session added.

| Value | Meaning | Effect on `tasks.md` |
|---|---|---|
| `converged` | Zero **new** actionable findings | **Byte-for-byte unchanged.** No empty phase header. |
| `tasks_appended` | ≥1 new actionable finding | Exactly one new `## Phase N: …` section appended at the end |
| `implementation_incomplete` | `tasks.md` still has unchecked non-gate tasks — implement halted rather than finished | **Byte-for-byte unchanged.** Reports *"implementation incomplete — finish the task list first"* |

`implementation_incomplete` (clarification 2026-08-28) applies to `speckit-docs-converge` only;
the shipped converge has no such state.

**State transitions across the developer loop:**

```
implementation_incomplete ──▶ developer finishes the task list ──▶ chain re-runs
tasks_appended            ──▶ developer runs /speckit-implement ──▶ chain re-runs ──▶ tasks_appended | converged
converged                 ──▶ terminal; converged (or all findings accepted) ⇒ open PR 2
```

The loop is developer-driven — neither skill re-invokes `/speckit-implement` (research R2), so
there is no runaway automation.

### Termination (clarification, 2026-08-28)

`converged` means **no *new* actionable findings**, not "no findings ever". Two rules make the
loop provably terminating rather than merely usually-terminating:

1. **Dedupe.** A finding already represented by a task in a prior Convergence phase — *checked or
   unchecked* — is never re-emitted. Each pass therefore yields strictly fewer new findings than
   the last, so the sequence must reach zero.
2. **Acceptance is durable.** Because dedupe ignores checkbox state, a developer who consciously
   accepts a finding (typically an `unrequested` one) marks its task `[X]` and is never asked
   again. Without this, a correct-but-declined finding would block PR 2 forever.

**Known asymmetry, accepted deliberately.** Both rules live in `speckit-docs-converge`. The
shipped `/speckit-converge` cannot be given them without editing a shipped file, which research
R1 rules out. Its own findings may therefore recur. That is handled in documentation rather than
code: Quick Start §3 and §9 define the gate as *converged, **or** every remaining finding
consciously accepted*, and §10 carries the symptom row.

---

## Entity: `HookDeclaration`

One entry under `hooks:` in `.specify/extensions.yml`. Schema is fixed by the shipped skills'
dispatch logic; this change adds two entries and invents nothing.

| Field | Type | This change's values |
|---|---|---|
| `command` | string, dot-separated | `speckit.converge` (under `after_implement`); `speckit.docs-converge` (under `after_converge`) |
| `optional` | boolean | **`false`** for both — mandatory is the entire point of AC-002 |
| `description` | string | One line, matching the existing `before_plan` entry's style |
| `enabled` | boolean, optional | Omitted. Absent ⇒ enabled (shipped default). |
| `condition` | string, optional | Omitted. A non-empty `condition` makes every shipped skill **skip the hook**, which would silently disable the step. |

### Validation rules

1. The file must remain valid YAML. Every shipped skill catches a parse error by *skipping hook
   checking silently* — so a malformed file disables the automation **without any error
   message**. This is the highest-consequence, lowest-visibility failure in the change, which is
   why the Test Strategy asserts parseability explicitly rather than trusting review.
2. `command` maps to a slash command by replacing `.` with `-`. `speckit.docs-converge` →
   `/speckit-docs-converge` → must match the skill **directory name** `speckit-docs-converge`
   and its frontmatter `name` (skill-lint enforces the directory/frontmatter match).
3. Existing `before_plan` stays untouched (AC-005).

---

## Entity: `ConvergencePhase`

The append target in `tasks.md`. Governed by [`contracts/tasks-append.md`](./contracts/tasks-append.md).

| Field | Rule |
|---|---|
| header | `## Phase N: Documentation Convergence` — distinct from converge's `## Phase N: Convergence` so a reader can tell which pass produced which tasks |
| `N` | highest existing phase number + 1, computed **at write time** (so it naturally lands after the code phase converge just wrote) |
| task IDs | `T{M+1:03d}` onward, where `M` is the highest existing `T###` anywhere in the file |
| ordering | severity-descending. Per AC-003, documentation findings come **after** constitution violations — satisfied structurally, since converge's constitution tasks are in the earlier phase |
| task line | `- [ ] T042 <imperative work> in <file-path> per <source-ref> (<gap-type>)` — `<file-path>` mandatory per AC-004 |

### Invariants (all of AC-004)

1. Existing task IDs are never reused, renumbered, reordered, or deleted.
2. A prior Documentation Convergence phase is left alone; a new one is appended below it.
3. Zero findings ⇒ zero bytes written.
4. No file other than `tasks.md` is ever written by this skill.
