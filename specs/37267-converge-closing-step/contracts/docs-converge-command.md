# Contract: `/speckit-docs-converge`

**Feature**: `37267-converge-closing-step`
**Artifact**: `.claude/skills/speckit-docs-converge/SKILL.md` (NEW, dotCMS-authored)

The command interface of the new companion skill. Delivers AC-003 and AC-004.

---

## Identity

| Property | Value | Why |
|---|---|---|
| Directory | `.claude/skills/speckit-docs-converge/` | Must equal frontmatter `name` — skill-lint enforces this even for grandfathered skills |
| Frontmatter `name` | `"speckit-docs-converge"` | |
| Invocation | `/speckit-docs-converge` | Derived from `speckit.docs-converge` by `.` → `-` |
| `metadata.author` | `"dotcms"` | Follows `speckit-adr-context` — this is authored, not vendored |
| `metadata.source` | `"dotcms customization (see .specify/CUSTOMIZATIONS.md)"` | |
| `user-invocable` | `true` | Runnable standalone, not only as a hook |
| `disable-model-invocation` | `false` | |
| Governance | **must** be added to `grandfathered` in `.claude/skills/skills.config.json` | Otherwise `cicd_pr_skill-lint` fails the naming rule (research R3) |

---

## Inputs

| Input | Source | Required |
|---|---|---|
| `FEATURE_DIR` | `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks` | Yes |
| `spec.md`, `plan.md`, `tasks.md` | `FEATURE_DIR` | Yes — stop with the prerequisite command named if any is missing, exactly as converge does |
| Constitution | `.specify/memory/constitution.md` | Optional; skip its checks gracefully if it is an unfilled template |
| Working tree | `docs/**`, `CLAUDE.md` files, JAX-RS sources, `openapi.yaml`, Javadoc on changed files | Read-only |
| `$ARGUMENTS` | User | Optional narrowing hint |

**Scope bound (mandatory):** assessment is limited to files the artifacts name, plus documents
reachable from them by reverse lookup. Unbounded doc sweeps over a repo this size produce noise
that trains developers to ignore the step — the failure mode AC-004's control case exists to
detect.

---

## Behavior

0. **Incomplete-run guard** *(clarification 2026-08-28)* — scan `tasks.md` for unchecked
   non-`[GATE]` tasks. If any remain, the implement run halted rather than finished (parked at a
   TDD gate, or stopped on a failure). Report `implementation_incomplete` — *"implementation
   incomplete — finish the task list first"* — write **nothing**, and stop. This is the normal
   state after every gate approval, so it must be quiet; without the guard, converge would append
   duplicates of tasks already open on every single approval.
1. **Initialize** — resolve `FEATURE_DIR`, load the three artifacts.
2. **Build the documentation-intent inventory** — for each requirement/plan decision, determine
   which of the four `DocSurface`s it implicates.
3. **Assess each surface** per [`data-model.md`](../data-model.md) → `DocSurface`.
4. **Classify** every gap: `gap-type` × `severity` per the data model's two mapping tables,
   reusing the existing vocabulary (AC-003 forbids a new one).
4b. **Dedupe** *(clarification 2026-08-28)* — drop any finding already represented by a task in a
   prior Convergence phase, **checked or unchecked**. Checked means the developer consciously
   accepted it; unchecked means it is already queued. Either way, re-emitting it is noise, and
   this rule is what makes the loop terminate: every pass yields strictly fewer new findings than
   the last.
5. **Present findings in-session before writing anything** — same table shape as converge:

   ```markdown
   ## Documentation Convergence Findings

   | ID | Surface | Gap Type | Severity | File | Source | Evidence | Remaining Work |
   |----|---------|----------|----------|------|--------|----------|----------------|
   | D1 | openapi | contradicts | CRITICAL | .../openapi.yaml | Constitution IV | committed yaml lacks the `siteId` parameter added to FolderResource | Regenerate and commit |
   ```

   Plus summary metrics: surfaces checked, findings by gap type, findings by severity.
6. **Append or report converged** — per [`tasks-append.md`](./tasks-append.md).
7. **Handoff** — on `tasks_appended`, state the count and phase and recommend
   `/speckit-implement`; on `converged`, state that documentation matches and PR 2 is unblocked
   *provided converge also reported converged*.

---

## Output contract

| Outcome | Condition | `tasks.md` |
|---|---|---|
| `implementation_incomplete` | Unchecked non-gate tasks remain (step 0) | **Byte-for-byte unchanged** |
| `converged` | Zero **new** actionable findings after dedupe | **Byte-for-byte unchanged** — no empty header |
| `tasks_appended` | ≥1 new finding after dedupe | One `## Phase N: Documentation Convergence` section appended |

---

## Prohibitions (AC-004 — the invariant that makes this safe)

The skill's **only** write is appending to `tasks.md`. It MUST NOT:

- edit `docs/**`, any `CLAUDE.md`, `openapi.yaml`, Javadoc, or any source file;
- edit `spec.md` or `plan.md` — **under any circumstance**, including when it detects the
  implementation diverged from them. Editing an approved spec to match what was built rewrites
  the contract a reviewer signed off on and inverts the two-PR flow. Divergence is *reported*,
  and a divergence from an already-approved spec is flagged as needing re-approval per Quick
  Start §3;
- rewrite, renumber, reorder, or delete any existing task, including one from a prior
  Documentation Convergence phase;
- **run a build.** No `./mvnw`, no `pnpm`, no code generation. The chain runs automatically
  after every implement; a Maven compile in that path is unacceptable, and regenerating
  `openapi.yaml` is out of scope. For a suspected stale yaml it emits a task instructing the
  developer to run `./mvnw compile -pl :dotcms-core -DskipTests` and commit the result;
- create or edit an ADR, in this repo or `dotCMS/platform-adrs` (Constitution ADR section,
  ADR-0001);
- make network calls.

---

## Hook dispatch the skill must implement

To keep the family consistent and leave room for a future link in the chain, `SKILL.md` includes
the standard blocks, copied structurally from `speckit-converge/SKILL.md`:

- **Pre-Execution**: `hooks.before_docs_converge`
- **Post-Execution**: `hooks.after_docs_converge`

Neither key is declared in `extensions.yml` by this change, so both skip silently today.

---

## Conformance checks

```bash
# name matches directory; grandfathered; catalog fresh — all three or CI fails
just skills-lint
just skills-catalog && git diff --exit-code .claude/skills/CATALOG.md

# the prohibition that defines the route: no shipped skill was touched
python3 - <<'EOF'
import json, hashlib
m = json.load(open('.specify/integrations/claude.manifest.json'))['files']
bad = [p for p, h in m.items() if hashlib.sha256(open(p,'rb').read()).hexdigest() != h]
assert not bad, f"shipped skill modified: {bad}"
print("all 10 shipped skills pristine")
EOF
```
