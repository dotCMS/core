# design-fix-test-plan

A Claude Code skill that generates structured, dotCMS-aware **test plans** for bug fixes and features. Each plan is a numbered list of `TC-###` test cases with risk level, self-contained reproduction steps, expected result, and test type (Unit / Integration / E2E / Manual).

The skill operates in **three modes** depending on where you are in the fix lifecycle. It auto-detects the mode from the inputs you provide.

---

## When it triggers

Claude auto-invokes this skill when your message matches any of these phrases (or close variants):

- "TDD plan" / "test plan for the fix" / "plan tests before coding"
- "what tests should I write" / "what tests do I need"
- "QA plan for PR #X" / "what should I QA on this PR" / "verify this fix"
- "regression checklist" / "what to QA when this lands"
- "acceptance tests for issue #X" / "scope the testing for issue #X"

You can also force-invoke it via the slash command:

```
/design-fix-test-plan
```

---

## The three modes

| Mode | Use when… | Source of truth | First TC prefix |
|---|---|---|---|
| **`tdd`** | Fix approach is decided, **no code is written yet** | Issue + proposed fix approach | `Repro:` (must fail before fix, pass after) |
| **`qa-postfix`** | The PR is **already merged** — you need to know what to QA on the post-merge build (release candidate, nightly, staging, master) | Issue + merged PR diff + the deployed/built environment QA will exercise | `Verify:` (the original bug no longer reproduces against the post-merge build) |
| **`qa-prefix`** | **No fix yet** — you want acceptance criteria up-front | Issue + expected behavior | `Acceptance:` (must fail today, must pass after fix) |

### Mode 1 — `tdd` (drive the implementation)

**Use when:** you've agreed on the fix approach but haven't written code. The plan becomes the failing tests you write first.

**Example invocations:**
- *"Design a TDD plan for issue #35458 — I'll add cache eviction in `PermissionCacheImpl#remove`."*
- *"Plan tests before I touch `RoleAPIImpl#removeRoleFromUser`."*

**You should provide:**
- Issue # or description
- Proposed fix approach (file/method-level)

### Mode 2 — `qa-postfix` (verify a merged fix on the post-merge build)

**Use when:** the fix is **already merged** and you need to know what to QA on the build that contains it (release candidate, nightly, staging, master). This is **post-merge** verification, not a pre-merge gate. The PR's own automated tests are already in CI on `main` — QA's job is the hand-check list.

**Example invocations:**
- *"QA plan for the merged PR #35458."*
- *"What should I QA post-merge for issue #35458?"*
- *"Regression checklist for the release candidate covering PR #35460."*
- *"Verify the merged fix in nightly build."*

**You should provide:**
- A merged PR number, merge commit SHA, or release/build identifier
- (Optional but useful) which environment the QA pass will run against — RC, nightly, staging, master, etc.

**What's special in this mode:**
- **All TCs are `Manual`.** No `Unit` / `Integration` / `E2E` cases — those are the PR author's job and already running in CI. The QA plan is strictly the hand-check list.
- Skill reads the merged-PR diff via `gh pr diff <num>` (works post-merge) or `git show <merge-sha>` to find which tests the PR contributed. Those tests are now in `main`'s CI — they show up as Out-of-Scope reasons in the plan (`covered by PR-added test FooTest#bar (now on main)`), never as duplicate TCs.
- The **Verify** TC re-runs the **original bug's repro steps** against the post-merge build and confirms the bug no longer reproduces — concrete environment, user, site, screen, click sequence.
- Mandatory TCs (Verify, cache invalidation, REST 401/403/contract) are written as runbook-style manual steps — log in, click, observe — including ready-to-paste `curl` / Postman GUI commands where applicable.
- If a behavior really should have automated coverage but the PR didn't add it, it goes into a dedicated **Recommended Automation (follow-up)** subsection (suggested test type + module + rationale), **not** a Unit/IT TC the QA engineer can't run. This keeps the manual hand-check list and the "automation to add" list as two distinct deliverables instead of losing the gap in prose.

### Mode 3 — `qa-prefix` (define acceptance up-front)

**Use when:** you have an issue but no fix yet, and you want to lock in *what the fix must satisfy* before anyone codes it.

**Example invocations:**
- *"Acceptance tests for issue #35460."*
- *"Scope the QA for issue #35460 in advance."*
- *"What do I need to QA when this lands? No fix yet."*

**You should provide:**
- Issue # or description
- Expected behavior after the fix (or be ready to answer Section 0 clarifications about it)

---

## What the skill produces

Every plan output starts with the detected mode, e.g.:

```
Mode: qa-postfix (PR #35458 detected)
```

Then four sections:

1. **Summary** — bug, fix in one line, testing strategy, doc citations.
2. **Existing Coverage** — what tests already cover this (main-branch and, in `qa-postfix`, PR-added); tests that assert the buggy behavior and need updating; disabled tests worth re-enabling; reusable fixtures.
3. **Out of Scope** — every Coverage Matrix axis marked out-of-scope with a one-line reason. Reviewers use this to challenge silent gaps.
4. **Test Cases** — one block per TC:
   ```
   - Test ID: TC-001
   - Test Name: <imperative phrase>
   - Risk: Critical | High | Medium | Low
   - Scenario: Happy Path | Negative | Edge | Boundary
   - Steps To Reproduce:
     1. <step that creates everything needed>
     2. ...
   - Expected Result: <observable assertion>
   - Type of Test: Unit | Integration | E2E | Manual
   - Replaces: <optional — existing test this hardens or supersedes>
   ```
5. **Recommended Automation (follow-up)** — *`qa-postfix` only.* Missing automated coverage the merged PR didn't add, as proposed Unit/IT/E2E tests (type + module + rationale) for a follow-up issue — kept separate from the manual TCs QA executes.

---

## Pipeline (what runs before output)

1. **Section 0-Pre — Mode Detection** → emits the `Mode: …` line.
2. **Section 0 — Pre-flight Clarification** → batched `Q1`/`Q2`/… with defaults; halts until you answer (or skip).
3. **Section 0-B — Documentation Ingestion** → fetches every link you provide (docs, Figma, screenshots) in full; treats them as authoritative; flags conflicts with the PR.
4. **Section 0-C — Existing Test Coverage Audit** → greps every test module; in `qa-postfix` also diffs the PR for added tests.
5. **Section 0-D — Coverage Matrix** → walks 9 dotCMS axes and forces in/out-of-scope decisions.
6. **Mandatory TCs** auto-add by trigger.
7. **Section 1 — Output**.

---

## The 9 Coverage Matrix axes (always evaluated)

For each axis, the skill must decide **In scope** (≥1 TC) or **Out of scope** (with one-line reason):

- **Permissions** — anonymous, back-end user, front-end user, CMS Anonymous role, CMS Owner, individual vs. inherited, role revocation
- **Sites / hosts** — `SYSTEM_HOST`, default host, secondary host, cross-host
- **Languages** — default, non-default, `DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE` on/off, missing translation
- **Content version state** — working only, live, archived, working ≠ live
- **Cache layers** — cold, warm, **invalidation after mutation**, **cross-node invalidation in a cluster**
- **Workflow** — default scheme, non-default, required action
- **Push Publish** — bundle generation, receiver replay
- **Persistence** — PostgreSQL only (CI does not run H2)
- **UI / UX** — rendering, keyboard nav, focus management, ARIA / screen reader, error/loading/empty states, responsive breakpoints, i18n, theme/contrast, copy. Triggered when the fix touches `core-web/`, the issue mentions a screen/dialog/form, or screenshots / Figma are attached. Each user-visible **acceptance criterion** in the issue must map to ≥1 TC.

---

## Mandatory test cases (auto-added when triggered)

- **First TC** — mode-specific (`Repro:` / `Verify:` / `Acceptance:`).
- **Cache invalidation** — when the fix mutates cached state.
- **Cross-node cache invalidation** — when the fix touches a `*Cache*` layer or the cache-transport path; verify the mutation propagates to other cluster nodes (mutate on A → read on B), not just the local JVM.
- **REST 401** — when the fix touches `com.dotcms.rest.*`.
- **REST 403** — when the fix touches `com.dotcms.rest.*`.
- **REST `@Schema` contract** — assert response JSON shape matches declared `@Schema` / `@ApiResponse`.
- **Upgrade / startup-task on a populated DB** — when the diff adds/modifies a startup or upgrade `*Task*` or any DDL; verify it runs cleanly and idempotently against an existing populated DB (see `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`).

---

## dotCMS-specific behaviors

### Test-module routing

| Surface area | Module |
|---|---|
| Pure POJO / util / no I/O | `dotCMS` unit test (JUnit) |
| DAO / DB / cache / transactions | `dotcms-integration` (PostgreSQL-backed) |
| REST contract (status, headers, JSON, `@Schema`) | `dotcms-postman` **+** Java IT in `dotcms-integration` |
| OSGi / plugin lifecycle | `dotcms-integration` + Manual lifecycle case |
| Angular component / signal / service | Jest + Spectator in `core-web/` |
| Full UI flow | Playwright |

REST endpoints **always** require both a Java IT (logic + permissions) and a Postman case (contract). A unit test on the resource class is insufficient.

### Test-data generators (used in `Steps To Reproduce`)

The plan resolves "create a user / host / role / contentlet / …" to canonical generators:

`UserDataGen`, `RoleDataGen`, `SiteDataGen`, `LanguageDataGen`, `ContentTypeDataGen`, `FieldDataGen`, `ContentletDataGen`, `TemplateDataGen`, `ContainerDataGen`, `FolderDataGen`, `WorkflowDataGen`, `PermissionDataGen`.

Always `nextPersisted()` (not `next()`) when the test crosses a transaction or cache.

### Frontend specifics (`core-web/`)

- Modern Angular only: `@if` / `@for` / `input()` / `output()` / `signal()` — never `*ngIf` / `@Input()`.
- HTTP via `HttpTestingController`, never live `fetch`/`HttpClient`.
- `byTestId` / `byText` selectors, not CSS selectors coupling to PrimeNG/DaisyUI internals.

---

## Inputs to provide for best results

| Input | `tdd` | `qa-postfix` | `qa-prefix` |
|---|---|---|---|
| Bug / feature description | required | required | required |
| Proposed fix approach | **required** | derived from PR | not required |
| Expected behavior after fix | inferred | confirmed against PR | **required** |
| PR / branch / diff | n/a | **required** | n/a |
| Documentation links | optional but valued | optional but valued | optional but valued |

If a required input is missing, the skill asks **once** in Section 0 (numbered batch with defaults) — never one question at a time.

---

## Where the plan is saved

After printing to the conversation, the skill writes the plan to `local/test-plans/` at the repo root:

| Inputs | Filename |
|---|---|
| Issue # + PR # | `local/test-plans/<issue>-<pr>-test-plan.md` |
| Issue # only | `local/test-plans/<issue>-test-plan.md` |
| PR # only | `local/test-plans/pr<pr>-test-plan.md` |
| Description only | `local/test-plans/<short-kebab-slug>-test-plan.md` |

If the target file already exists, the skill asks before overwriting (offers `-v2`, `-v3` suffix or abort).

## What this skill does NOT do

- It does not write the test code itself — only the plan. Hand the plan to a coding agent, or write the tests yourself following the steps.
- It does not run the tests. (For `qa-postfix`, you execute the Manual TCs yourself against the post-merge build / RC / staging environment.)
- It does not estimate effort or schedule.
- It does not modify the GitHub issue or PR. The plan is conversation output + a saved file; attach the file to the issue/PR if you want it persisted there.

---

## Tips

- **Provide doc links** at invocation time (Confluence, Figma, screenshots, dotCMS docs). The skill fetches them in full and treats them as authoritative — they often reveal edge cases the issue/PR didn't mention.
- **Provide a PR number** when the fix exists. The PR-diff audit is the single biggest quality lift for `qa-postfix` plans.
- **Skip clarification questions you don't care about** — each one has a default that the skill will use. Just answer the ones that matter.
- **Out of Scope is a feature, not laziness** — challenge or accept each axis the skill marks out-of-scope. Silent gaps are the goal you're avoiding.

---

## Related files

- `SKILL.md` — the skill instructions Claude actually reads. Edit there to change behavior.
- This `README.md` — human-facing reference. Edit when behavior changes so the docs don't drift.
