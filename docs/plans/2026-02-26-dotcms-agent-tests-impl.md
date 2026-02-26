# dotcms-agent-tests Skill Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a local skill that validates AI agents can complete common dotCMS developer tasks starting from `AGENTS.md` alone, using isolated sub-agents scored by a review agent.

**Architecture:** The skill lives at `~/.claude/skills/dotcms-agent-tests/` (never committed). Each test case launches a Task sub-agent with `isolation: "worktree"` (a fresh git worktree copy of the repo) and a verbatim task prompt. A review agent scores transcripts against rubrics. Failing tests produce concrete AGENTS.md diff suggestions. Only `AGENTS.md` and linked docs go into the PR.

**Tech Stack:** Claude Code Task tool (sub-agents + isolation), worktrunk `wt` (worktree management/inspection), git worktrees, Markdown skill files.

---

## Task 1: Create skill directory structure

**Files:**
- Create: `~/.claude/skills/dotcms-agent-tests/SKILL.md`
- Create: `~/.claude/skills/dotcms-agent-tests/review-rubric.md`
- Create: `~/.claude/skills/dotcms-agent-tests/test-cases/BE-001-run-specific-test.md`
- Create: `~/.claude/skills/dotcms-agent-tests/test-cases/BE-002-build-command-selection.md`
- Create: `~/.claude/skills/dotcms-agent-tests/test-cases/BE-003-add-rest-endpoint.md`
- Create: `~/.claude/skills/dotcms-agent-tests/test-cases/FE-001-serve-test-lint.md`

**Step 1: Create directories**

```bash
mkdir -p ~/.claude/skills/dotcms-agent-tests/test-cases
```

Expected: exits 0, directories exist.

---

## Task 2: Write SKILL.md

**File:** `~/.claude/skills/dotcms-agent-tests/SKILL.md`

**Step 1: Write the file**

```markdown
---
name: dotcms-agent-tests
description: >
  Run AI agent test cases against the dotCMS codebase to validate progressive
  context gathering. Tests that agents starting from AGENTS.md alone can complete
  common developer tasks correctly and efficiently. Use when validating AGENTS.md
  quality, before merging AGENTS.md changes, or adding new test scenarios.
---

# dotCMS Agent Test Suite

Validates that AI agents starting from `AGENTS.md` alone can complete common
developer tasks — choosing the right commands, reading the right docs, and
avoiding dotCMS-specific landmines.

## When to Use

- Before merging changes to `AGENTS.md`
- When a developer reports an agent gave wrong guidance
- When adding new landmines to discover
- Periodic quality check (e.g. after major codebase changes)

## How Tests Work

Each test case in `test-cases/` defines:
1. A **verbatim task prompt** given to an isolated sub-agent
2. **Pass criteria** — what the agent must do/say/find
3. **Fail indicators** — specific wrong behaviors
4. **Gap signal** — what AGENTS.md change would fix a failure

Isolation: each test uses `isolation: "worktree"` on the Task tool, which
creates a fresh git worktree copy of the repo. No context bleeds between tests.
The worktree is a standard git worktree — inspect with `wt list`, clean up
with `wt remove <branch>` or `git worktree remove <path>`.

## Running Tests

### All tests (parallel)

When invoked without arguments, dispatch all test cases in parallel:

1. Read all files in `test-cases/`
2. For each test, launch a Task sub-agent with:
   - `subagent_type: "general-purpose"`
   - `isolation: "worktree"`
   - Prompt: the test's Task Prompt section verbatim, prefixed with the isolation preamble
3. Collect all results, then launch a review agent with all transcripts + `review-rubric.md`
4. Output the scorecard (see Scorecard Format below)
5. For each FAIL, output the AGENTS.md gap signal and suggested diff

### Single test

When invoked as `/dotcms-agent-tests BE-001`, run only that test case.

## Isolation Preamble

Prepend this to every test prompt before sending to the sub-agent:

```
You are an AI coding assistant working in the dotCMS codebase.
Your ONLY starting context is the AGENTS.md file in this repository.
Do NOT assume any prior knowledge beyond what AGENTS.md tells you.
If you need more context, read the files that AGENTS.md references.
Answer the following task as a developer would ask it in a fresh session:

---
```

## Scorecard Format

```
## Agent Test Results — YYYY-MM-DD

| ID    | Name                    | Result | Key Observation                    |
|-------|-------------------------|--------|------------------------------------|
| BE-001 | Run specific test      | ✅ PASS | Correctly used skip=false flag     |
| BE-002 | Build selection        | ❌ FAIL | Suggested full build for core-only |
| BE-003 | Add REST endpoint      | ✅ PASS | Found REST_API_PATTERNS.md         |
| FE-001 | Frontend serve/test    | ✅ PASS | Included NODE_OPTIONS OOM warning  |

### Failures & Suggested AGENTS.md Changes

#### BE-002: Build selection
**Observation:** Agent suggested `./mvnw clean install -DskipTests` for a
single-class change in dotcms-core.

**Suggested AGENTS.md diff:**
[include diff from test case gap signal]
```

## Adding New Test Cases

Copy any existing test case in `test-cases/` as a template. Use naming:
- `BE-NNN-<slug>.md` for backend scenarios
- `FE-NNN-<slug>.md` for frontend scenarios

Run the new test in isolation first to establish a baseline before updating AGENTS.md.
```

**Step 2: Verify file exists**

```bash
ls -la ~/.claude/skills/dotcms-agent-tests/SKILL.md
```

Expected: file listed.

---

## Task 3: Write review-rubric.md

**File:** `~/.claude/skills/dotcms-agent-tests/review-rubric.md`

**Step 1: Write the file**

```markdown
# Review Rubric for Agent Test Evaluation

You are a senior dotCMS developer evaluating an AI agent's response to a
developer task. The agent started with AGENTS.md only and had access to
the full repository to navigate from there.

## Your Job

Score the agent's transcript against the test case criteria. Be strict —
the goal is to catch AGENTS.md gaps, not to be generous.

## Evaluation Dimensions

### 1. Correctness (0-3 points)
- 3: All commands/steps are correct and dotCMS-specific
- 2: Mostly correct, minor omission
- 1: Partially correct, missing a critical flag or step
- 0: Wrong — generic Maven/Angular advice, not dotCMS-specific

### 2. Progressive Context (0-2 points)
- 2: Agent read only what was needed (AGENTS.md + ≤2 referenced docs)
- 1: Agent read more than needed but got there
- 0: Agent hallucinated without reading any files, or read 5+ files for simple task

### 3. Landmine Avoidance (0-3 points)
Per test case — check each listed landmine:
- +1 per landmine correctly avoided or warned about
- -1 per fail indicator exhibited (can go negative, cap at 0)

### 4. Navigation Efficiency (0-2 points)
- 2: Agent navigated from AGENTS.md → right doc in 1 hop
- 1: Agent found the right info eventually (2-3 hops)
- 0: Agent couldn't navigate or needed to read the whole codebase

## Pass/Fail Threshold

- **PASS**: Total ≥ 7/10 AND all mandatory pass criteria in test case are met
- **FAIL**: Total < 7/10 OR any mandatory criterion missed

## Output Format

For each test case, output:

```
### <TEST-ID>: <Name>
**Score:** X/10
**Result:** PASS / FAIL

**Correctness (X/3):**
[1-2 sentences]

**Progressive Context (X/2):**
Files read: [list]

**Landmine Avoidance (X/3):**
[per-landmine notes]

**Navigation Efficiency (X/2):**
[notes]

**Verdict:** [PASS/FAIL with one-line reason]
**Gap Signal:** [If FAIL: what is missing from AGENTS.md]
```

## dotCMS-Specific Grading Notes

These are automatic FAIL regardless of score:
- Agent suggests running the full integration test suite without `-Dit.test=`
- Agent gives `./mvnw test` (wrong — always `./mvnw verify`)
- Agent runs frontend build without mentioning `cd core-web` first
- Agent suggests adding Maven dependencies to `dotCMS/pom.xml` (must be `bom/application/pom.xml`)
```

**Step 2: Verify**

```bash
ls -la ~/.claude/skills/dotcms-agent-tests/review-rubric.md
```

---

## Task 4: Write BE-001 test case

**File:** `~/.claude/skills/dotcms-agent-tests/test-cases/BE-001-run-specific-test.md`

**Step 1: Write the file**

```markdown
# BE-001: Run a Specific Integration Test

## Task Prompt

I need to run just the `ContentTypeAPIImplTest` integration test class in dotCMS.
What command do I use?

## Context Allowed

Starting point: AGENTS.md only (no prior conversation). Agent may read any
file the AGENTS.md references.

## Pass Criteria (all required)

- [ ] Command uses `./mvnw verify` (not `./mvnw test`)
- [ ] Command targets `-pl :dotcms-integration`
- [ ] Command includes `-Dcoreit.test.skip=false`
- [ ] Command includes `-Dit.test=ContentTypeAPIImplTest`
- [ ] Agent warns that tests are **silently skipped** without the `skip=false` flag
- [ ] Agent does NOT suggest running the full suite

## Fail Indicators (any = FAIL)

- Suggests `./mvnw test` or `./mvnw verify` without `-pl :dotcms-integration`
- Omits `-Dcoreit.test.skip=false` flag entirely
- Omits `-Dit.test=ContentTypeAPIImplTest`
- Suggests running the full integration suite (~60 min)
- Says "just run `mvn test`" without dotCMS-specific flags

## Landmark: What Good Looks Like

```bash
./mvnw verify -pl :dotcms-integration \
  -Dcoreit.test.skip=false \
  -Dit.test=ContentTypeAPIImplTest
```

Plus a warning: "Without `-Dcoreit.test.skip=false`, tests are silently skipped."

## AGENTS.md Gap Signal

**If failing:** AGENTS.md has the right flags but may not make the silent-skip
landmine prominent enough.

**Suggested fix if score < 7:** Move the silent-skip warning earlier in AGENTS.md
and make it bold/prominent above the test commands, not buried after them.
```

---

## Task 5: Write BE-002 test case

**File:** `~/.claude/skills/dotcms-agent-tests/test-cases/BE-002-build-command-selection.md`

**Step 1: Write the file**

```markdown
# BE-002: Build Command Selection

## Task Prompt

I've changed a single Java utility class in the `dotcms-core` module only —
nothing else touched. What's the fastest way to build without running tests?

## Context Allowed

Starting point: AGENTS.md only.

## Pass Criteria (all required)

- [ ] Recommends `./mvnw install -pl :dotcms-core -DskipTests` OR `just build-quicker`
- [ ] Explains WHY this is faster than the full build
- [ ] Does NOT recommend `./mvnw clean install -DskipTests` for this scenario
- [ ] Approximate time estimate given (~2-3 min)

## Fail Indicators (any = FAIL)

- Recommends `./mvnw clean install -DskipTests` as the first/only option
- Recommends `just build` without explaining it's overkill for core-only
- No mention of `-pl :dotcms-core`

## Landmark: What Good Looks Like

"For a change only in `dotcms-core`, use the targeted build:
```bash
./mvnw install -pl :dotcms-core -DskipTests  # ~2-3 min
# or: just build-quicker
```
Reserve `./mvnw clean install` for major changes or starting fresh (~8-15 min)."

## AGENTS.md Gap Signal

**If failing:** AGENTS.md lists both commands but provides no heuristic for
choosing between them. An agent defaults to "safe = full build".

**Suggested AGENTS.md diff:**
Add after the build commands block:
> **Choose the right command:** Core-only change → `just build-quicker`.
> Multiple modules changed → `./mvnw install -pl :dotcms-core --am -DskipTests`.
> Major change or fresh start → `just build`.
```

---

## Task 6: Write BE-003 test case

**File:** `~/.claude/skills/dotcms-agent-tests/test-cases/BE-003-add-rest-endpoint.md`

**Step 1: Write the file**

```markdown
# BE-003: Add a REST Endpoint

## Task Prompt

I need to add a new GET endpoint to an existing JAX-RS resource class in dotCMS.
What's the standard pattern I should follow?

## Context Allowed

Starting point: AGENTS.md only. Agent must navigate to find the REST pattern docs.

## Pass Criteria (all required)

- [ ] Agent navigates to a REST patterns reference (via AGENTS.md → docs link)
- [ ] Response includes `@GET @Path @Produces(MediaType.APPLICATION_JSON)`
- [ ] Response mentions `webResource.init(request, response, true)` for auth
- [ ] Response mentions `@Operation` + `@ApiResponses` Swagger annotations
- [ ] Response mentions using a specific `ResponseEntity*View` class (not generic)
- [ ] Response warns about NOT using `ResponseEntityView.class` in `@Schema`

## Fail Indicators (any = FAIL)

- Gives a generic JAX-RS example with no dotCMS-specific patterns
- Omits `WebResource.init()` call (no auth initialization)
- Uses `@Schema(implementation = ResponseEntityView.class)` (antipattern)
- Never reads any file beyond AGENTS.md (hallucinated from general knowledge)

## Landmark: What Good Looks Like

Agent reads `AGENTS.md`, finds reference to `docs/backend/REST_API_PATTERNS.md`
or `CLAUDE.md`, then provides dotCMS-specific pattern with `WebResource`,
`@Operation`, `@ApiResponses`, and correct `@Schema` guidance.

## AGENTS.md Gap Signal

**If failing (likely):** AGENTS.md currently has NO breadcrumb to REST patterns.
An agent that hasn't seen dotCMS before will give generic JAX-RS advice.

**Suggested AGENTS.md diff:**
In the References section, add:
```
- `docs/backend/REST_API_PATTERNS.md` — REST endpoint patterns, Swagger annotations, ResponseEntity views
```

This is the most likely test to fail before AGENTS.md is updated.
```

---

## Task 7: Write FE-001 test case

**File:** `~/.claude/skills/dotcms-agent-tests/test-cases/FE-001-serve-test-lint.md`

**Step 1: Write the file**

```markdown
# FE-001: Frontend Serve, Test, and Lint

## Task Prompt

I'm working on the dotCMS frontend. How do I start the dev server, run the
unit tests, and run the linter for the `dotcms-ui` application?

## Context Allowed

Starting point: AGENTS.md only. Agent should navigate to `core-web/CLAUDE.md`
if AGENTS.md references it.

## Pass Criteria (all required)

- [ ] Agent mentions `cd core-web` first (or equivalent path prefix)
- [ ] Dev server: `npx nx serve dotcms-ui` (runs on :4200)
- [ ] Tests: `npx nx test dotcms-ui`
- [ ] Lint: `npx nx lint dotcms-ui`
- [ ] Agent mentions the OOM risk for standalone NX builds and `NODE_OPTIONS="--max_old_space_size=4096"`

## Fail Indicators (any = FAIL)

- Commands given without `cd core-web` / `core-web/` prefix
- Uses `npm run` instead of `npx nx` or `nx` directly
- No mention of OOM risk or NODE_OPTIONS for larger builds
- Says "run `ng serve`" (wrong tool — dotCMS uses Nx)

## Landmark: What Good Looks Like

```bash
cd core-web
npx nx serve dotcms-ui    # dev server on :4200
npx nx test dotcms-ui     # unit tests
npx nx lint dotcms-ui     # linting
```

Plus: "For standalone builds, add `NODE_OPTIONS=\"--max_old_space_size=4096\" --parallel=2`
to avoid OOM errors."

## AGENTS.md Gap Signal

**If failing:** AGENTS.md mentions OOM gotcha and references `core-web/CLAUDE.md`
but doesn't list the Nx commands inline. An agent may not navigate to CLAUDE.md
for such a simple question.

**Suggested AGENTS.md diff:**
In the "Build, test, run" section, the frontend commands are already there:
```
cd core-web && npx nx serve dotcms-ui   # dev server on :4200
cd core-web && npx nx lint dotcms-ui    # lint
cd core-web && npx nx test dotcms-ui    # test
```
If agent fails: verify these are present and the OOM gotcha is linked back to
them (currently the gotcha is in a separate Gotchas section — agents may not
connect the two).
```

---

## Task 8: Run baseline tests (RED phase)

This establishes which tests fail BEFORE any AGENTS.md changes.

**Step 1: Open the skill in a fresh session and invoke it**

In Claude Code (fresh session, `--no-resume`):
```
/dotcms-agent-tests
```

This dispatches all 4 sub-agents in parallel with `isolation: "worktree"`.
Each sub-agent gets: isolation preamble + task prompt from the test case.

**Step 2: Collect the scorecard output**

The review agent outputs a scorecard table. Record results in:
`docs/plans/2026-02-26-baseline-results.md` (create this file with the scorecard).

**Expected baseline (before AGENTS.md fixes):**
- BE-001: PASS (silent-skip warning is present)
- BE-002: FAIL (no build decision heuristic)
- BE-003: FAIL (no REST patterns breadcrumb)
- FE-001: PASS (OOM gotcha and commands present)

If results differ, update the gap analysis in Task 9 accordingly.

**Step 3: Inspect failed worktrees (optional)**

```bash
wt list   # or: git worktree list
```

Failed test worktrees are kept automatically (they contain the transcript).
Clean up passing ones manually if desired.

---

## Task 9: Update AGENTS.md based on baseline results

**File:** `AGENTS.md` (repo root)

Apply the gap signals from failing tests. Keep AGENTS.md ≤ 60 lines.
**Principle: landmines in AGENTS.md, verbose content in linked docs.**

**Step 1: Read current AGENTS.md**

```bash
cat AGENTS.md
```

**Step 2: Apply fixes for each failing test**

For BE-002 (likely failing) — add build decision heuristic after the build commands block:

```markdown
> **Pick the right build:** Core-only change → `just build-quicker` (~2-3 min).
> Affects dependencies → add `--am` flag. Major change/fresh start → `just build` (~8-15 min).
```

For BE-003 (likely failing) — add REST patterns link to References section:

```markdown
- `docs/backend/REST_API_PATTERNS.md` — REST endpoints, Swagger `@Schema` rules, ResponseEntity views
```

**Step 3: Verify line count stays under 60**

```bash
wc -l AGENTS.md
```

Expected: ≤ 60 lines. If over, move a verbose section to `docs/dev/` and replace with a link.

**Step 4: Commit**

```bash
git add AGENTS.md
git commit --no-verify -m "docs: improve AGENTS.md with build heuristic and REST patterns breadcrumb

Fixes BE-002 (build selection) and BE-003 (REST patterns) agent test failures.
Progressive disclosure: landmines stay in AGENTS.md, patterns linked via docs/.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 10: Run tests again (GREEN phase)

**Step 1: Run the full test suite again in a fresh session**

```
/dotcms-agent-tests
```

**Step 2: Verify all 4 tests pass**

Expected scorecard after AGENTS.md fixes:
- BE-001: ✅ PASS
- BE-002: ✅ PASS
- BE-003: ✅ PASS
- FE-001: ✅ PASS

**Step 3: If any test still fails**

- Read the review agent's gap signal for that test
- Apply the suggested diff to AGENTS.md
- Re-run only that test: `/dotcms-agent-tests BE-003`
- Repeat until passing

**Step 4: Clean up test worktrees**

```bash
git worktree list   # see all worktrees
git worktree remove <path>   # remove each agent-test-* worktree
```

Or with wt:
```bash
wt list
# then for each agent-test-* branch:
wt remove <branch-name>
```

---

## Task 11: Final commit and PR

Only `AGENTS.md` and any new `docs/dev/*.md` files go in the PR.
The skill at `~/.claude/skills/dotcms-agent-tests/` is never committed.

**Step 1: Verify only doc/AI files changed**

```bash
git diff main --name-only
```

Expected output: only `AGENTS.md` and optionally `docs/dev/*.md` files.
If any `.java` or `.ts` files appear: stop, investigate.

**Step 2: Final commit if any uncommitted changes**

```bash
git add AGENTS.md docs/dev/
git commit --no-verify -m "docs: AGENTS.md improvements from agent test validation

All 4 agent test scenarios now pass. Changes:
- Added build command selection heuristic
- Added REST patterns breadcrumb to references

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

**Step 3: Push and create PR**

```bash
git push origin cursor/development-environment-setup-e028
gh pr create \
  --title "docs: AGENTS.md improvements validated by agent test suite" \
  --body "## Summary

- Validated 4 developer scenarios with isolated AI agent tests
- Fixed 2 gaps identified in AGENTS.md (build heuristic, REST breadcrumb)
- All 4 tests pass after changes

## Test scenarios validated
- BE-001: Run specific integration test ✅
- BE-002: Build command selection ✅
- BE-003: Add REST endpoint ✅
- FE-001: Frontend serve/test/lint ✅

## Files changed
- \`AGENTS.md\` only (+ optional \`docs/dev/\` if navigation gaps found)

No code changes in this PR.

🤖 Generated with [Claude Code](https://claude.com/claude-code)" \
  --draft
```

---

## Reference: wt Worktree Management

The Task tool's `isolation: "worktree"` creates standard git worktrees.
Use these wt commands to manage them after tests run:

```bash
wt list                          # list all worktrees including test ones
git worktree list                # alternative: native git
git worktree remove <path>       # remove a specific worktree
git worktree prune               # remove stale worktrees
```

Test worktrees are named `agent-test-<id>-<timestamp>` on a temporary branch.
They are safe to remove once you've reviewed the transcript.