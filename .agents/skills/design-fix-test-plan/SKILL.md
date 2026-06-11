---
name: design-fix-test-plan
description: >
  Generates a structured test plan for a bug fix or feature in dotCMS. Operates in three modes — (1) **TDD** (no code yet, drive the implementation with failing tests first), (2) **QA post-fix** (PR is **already merged**; produce the manual hand-check list QA runs against the post-merge build / release candidate / staging — Manual TCs only, no Unit/IT/E2E), (3) **QA pre-fix** (no fix yet, define acceptance tests so we know when the fix is done). Each test case is tagged with a unique ID (TC-###) and includes risk level, self-contained reproduction steps, expected result, and test type (Unit / Integration / E2E / Manual). Trigger on phrases like "TDD plan", "test plan for the fix", "what tests should I write", "QA plan for PR #X", "verify this merged fix", "regression checklist for the merged PR", "what should QA exercise post-merge", "acceptance tests for issue #X", "scope the testing for issue #X", "QA plan for the release candidate". Auto-detects mode from the inputs the user provides (issue only / issue+fix / issue+merged-PR-or-build).
---

# Design Test Plan for a Fix

You are producing a **structured test plan** for a dotCMS bug fix or feature. Depending on what the user provides, the plan can drive a TDD implementation (write tests before code), scope QA verification of an existing PR, or define acceptance criteria before any fix exists. The plan must be specific enough that a developer or QA engineer can execute each case without asking follow-up questions.

## Section 0-Pre — Mode Detection (MANDATORY — do this first)

Before anything else, classify which **mode** you are operating in. Output the mode as a single line at the top of every response (e.g. `Mode: qa-postfix (PR #35458 detected)`).

| Mode | When to pick it | Source of truth |
|---|---|---|
| `tdd` | Fix approach is described or implied (e.g. "I'll change X to Y", "we plan to add a guard in `RoleAPIImpl`"), but **no code is written yet** | Issue + proposed fix approach |
| `qa-postfix` | The PR is **already merged** and the fix is in a built / deployed environment (release candidate, nightly, staging, master). QA is verifying the merged change, not gate-keeping a merge. | Issue + merged PR diff + deployed build the QA will exercise |
| `qa-prefix` | Only an issue / bug description exists — no fix approach, no PR | Issue + behavior expectations |

### Detection rules (priority order)

1. **User explicitly names the mode** ("give me a TDD plan", "QA plan", "acceptance tests") → use it.
2. **A merged PR / merge commit / built artifact / deployed environment is provided or implied** → `qa-postfix`. Read the diff (`gh pr diff <num>` works for merged PRs too; alternatively `git show <merge-sha>`) before continuing.
3. **A concrete fix approach is provided** (file/method-level intent) → `tdd`.
4. **Only the issue / behavior description is provided** → `qa-prefix`.
5. **Ambiguous** → ask one question in Section 0: *"Are you planning the fix (TDD), QA-ing an existing PR (qa-postfix), or scoping acceptance before any fix (qa-prefix)?"* Default to `qa-prefix` if skipped.

### What changes per mode (preview — full rules below)

| Aspect | `tdd` | `qa-postfix` | `qa-prefix` |
|---|---|---|---|
| Required input | Issue + fix approach | Issue + PR/diff | Issue + expected behavior |
| Section 0-C audit also greps | main-branch tests | **PR-added tests** + main-branch tests | main-branch tests, by behavior surface |
| Repro TC name prefix | `Repro:` (must fail before fix, pass after) | `Verify:` (must pass on PR, would have failed on main) | `Acceptance:` (must fail today, pass after fix) |
| Allowed `Type of Test` values | Unit, Integration, E2E, Manual | **Manual only** (no Unit/IT/E2E) | Unit, Integration, E2E, Manual |
| TC bias when an axis has zero automated coverage | Add a Unit/IT TC | Manual hand-check (mode rule) | Add a Unit/IT TC |
| Section 1 Summary framing | "drives the implementation" | "validates the merged change on the post-merge build" | "scopes acceptance criteria for the fix" |

State the detected mode and proceed to Section 0.

## Section 0 — Pre-flight Clarification (MANDATORY — do this before writing anything else)

After fetching and reading the issue and PR(s), **scan for ambiguities**.

Trigger a clarification question for ANY of the following:

- Expected behavior after the fix/feature is not explicitly stated
- Unclear whether a behavior is intentional or a side effect
- Affected dotCMS areas (multi-tenant, multi-lingual, permissions, push publish) not clearly scoped
- PR touches code paths that could affect multiple areas without clarifying which are in scope
- Acceptance criteria are missing or vague
- Multiple possible interpretations of how a feature should work
- An edge case mentioned but unresolved ("TBD", "needs discussion", "to be confirmed")
- Documentation links referenced but not included
- Any assumption you would otherwise make silently

### Rules

- Collect **ALL** ambiguities first — ask everything in a **single message**, never one question at a time.
- Number each question (`Q1`, `Q2`, …) so the user can answer by number.
- For each question, briefly state **why it matters** for the test plan (one line).
- After each question, provide a **default assumption** you will use if the user skips it.
- **Wait for the user's response** before proceeding to Section 1.
- If the user shares documentation links (at invocation time or in their answers), fetch and read them in full before continuing — see Section 0-B below.
- If there are no ambiguities, state exactly: `No clarifications needed — proceeding with the test plan.`

### Format

```
## Clarification Needed

Before I write the test plan, I need to clarify a few things:

**Q1:** <question>
_Why it matters:_ <one line>
_Default if skipped:_ <assumption>

**Q2:** <question>
_Why it matters:_ <one line>
_Default if skipped:_ <assumption>

**Q[last] (Optional — always include this as the final question):**
Do you have any relevant documentation, design specs, images, or external links I should
use as reference when writing this test plan? This includes official dotCMS docs, Figma
designs, Confluence pages, Notion docs, screenshots, or any other resource that describes
how this feature or fix is supposed to behave.
_Why it matters:_ Documentation is treated as the authoritative source of truth and may
reveal additional test cases, constraints, or preconditions not mentioned in the issue/PR.
_Default if skipped:_ No additional documentation — test plan is based solely on the issue,
PR description, and PR diff.

Please answer by number (e.g. "Q1: yes, Q3: see link"). You can skip any and I'll use the default.
```

## Section 0-B — Documentation Ingestion

If the user provides documentation links (in the original prompt or in answers to clarifications):

1. **Fetch every link in full** — use `WebFetch` for URLs, `Read` for local paths, image-reading for screenshots/Figma exports. Do not summarize from the URL alone.
2. **Treat documentation as authoritative** — if docs and the PR description disagree, the docs win for behavior; flag the conflict to the user before producing the plan.
3. **Extract test-relevant signals**: stated invariants, edge cases, error states, permission rules, supported locales/tenants, version constraints, UI states.
4. **Cite the source** in the Summary section of the plan (e.g. "behavior per <doc title> §3.2").
5. If a link is unreachable or paywalled, tell the user before proceeding — don't silently skip it.

## Inputs you need (after clarification)

Before generating the plan, confirm you have the items required by your detected mode:

| # | Input | `tdd` | `qa-postfix` | `qa-prefix` |
|---|---|---|---|---|
| 1 | Bug or feature description | required | required | required |
| 2 | Proposed fix approach (file/method-level) | **required** | derived from PR diff | not required — replaced by item 2b |
| 2b | Expected behavior after fix | inferred from fix | confirmed against PR | **required** (drives acceptance) |
| 3 | Affected surface area | required | **read from PR diff** | required (by behavior surface) |
| 4 | PR / branch / diff | n/a | **required** | n/a |
| 5 | Resolved ambiguities (Section 0) | required | required | required |
| 6 | Documentation read (Section 0-B) | required | required | required |
| 7 | Existing coverage audited (Section 0-C) | required | required | required |

If any required item is missing, return to Section 0 with one targeted question — do not proceed with assumptions.

## Section 0-C — Existing Test Coverage Audit (MANDATORY)

Before deciding what new tests to write, find what already exists. The goal is to **reuse, strengthen, or replace** — not duplicate. A TDD plan that ignores existing coverage produces redundant tests and misses brittle ones that should be hardened by the fix.

### Steps

1. **Identify the changed/affected production classes & methods** — from the PR diff or fix approach (e.g. `RoleAPIImpl#removeRoleFromUser`, `PermissionCacheImpl#remove`).

2. **Search every test module** for direct references and naming conventions (run on `main` for `tdd` / `qa-prefix`, on the PR branch for `qa-postfix`):

   ```bash
   # Java unit + integration tests — anchor on the QUALIFIED symbol, never a bare method name.
   # A bare `get`/`save`/`remove`/`find` returns thousands of hits and makes the audit useless.
   grep -rnE "ClassName|\.methodName\(|new ClassName\(" \
     dotCMS/src/test/java \
     dotcms-integration/src/test/java

   # Test classes that mirror the production class name (Foo.java → FooTest.java / FooIntegrationTest.java)
   find dotCMS dotcms-integration -name "<ClassName>Test.java" -o -name "<ClassName>IntegrationTest.java"

   # Postman REST collections
   grep -rln "<endpoint-path>" dotcms-postman/src/main/resources/postman

   # Frontend tests (Jest/Spectator)
   grep -rn "<ComponentName>\|<serviceName>" core-web --include="*.spec.ts"

   # Playwright E2E — TWO projects plus Stencil component e2e; grep all of them
   grep -rn "<feature-name>" e2e core-web/apps/dotcms-ui-e2e
   ```

   > **Grep hygiene:** if any symbol returns more than ~50 hits, it is too generic to classify —
   > narrow it by class qualifier (`ClassName#method`, `\.method(`, `new ClassName(`) or fall back to
   > the `<ClassName>Test.java` file-name search above. Do **not** classify coverage from a truncated
   > or noisy result set; an unanchored match is not evidence of coverage.

   **Additional step for `qa-postfix` only — enumerate tests that the merged PR contributed.** The PR is already merged, so the diff is read off the merge commit itself, not a working branch:

   ```bash
   # Files the merged PR introduced or modified — works whether you have the PR # or just the merge SHA
   gh pr diff <num> --name-only \
     | grep -E '(Test\.java$|IT\.java$|\.spec\.ts$|\.e2e\.ts$|postman.*\.json$)'

   # Or, if you only have the merge commit:
   git show --name-only --pretty=format: <merge-sha> \
     | grep -E '(Test\.java$|IT\.java$|\.spec\.ts$|\.e2e\.ts$|postman.*\.json$)'

   # Inspect the actual added test methods (look for @Test additions and `it(`/`test(` adds in TS)
   gh pr diff <num> -- '**/*Test.java' '**/*IT.java' '**/*.spec.ts' \
     | grep -E '^\+.*(@Test|void test|it\(|test\()'
   ```

   For each PR-added test, decide: does it cover one of the Coverage Matrix axes that's in scope? Mark the axis as `covered by PR-added test <name> (now on main)` in the Out-of-Scope block — these are **already in CI** post-merge and do **not** become new TCs in your output.

3. **For each hit, classify**:
   - **Covers behavior X correctly** → reference it in Section 1 (Out of Scope, with `already covered by FooTest#barTest`).
   - **Covers it but is brittle / asserts wrong invariant / would still pass with the bug** → produce a TC that **replaces or strengthens** it. Mark the TC with `Replaces: FooTest#barTest`.
   - **Tangentially related, leaves a real gap** → produce a fresh TC.
   - **Disabled / `@Ignore` / `@Disabled` / `it.skip`** → flag in Section 1; ask whether the fix should re-enable it.

4. **Reuse existing fixtures** — note any test base class, `@BeforeAll` setup, helper, or data generator already used by the existing tests so new TCs match the codebase conventions instead of inventing parallel scaffolding.

5. **Confirm the bug isn't already "tested"** — if a test asserts the buggy behavior as expected, the fix must update *that* test. Call this out explicitly; never leave a test asserting the old wrong behavior.

6. **Audit downstream UI consumers** — when the fix changes a REST endpoint, a service, or a public API surface, the audit must include the callers, not just the changed unit. Run:

   ```bash
   # Frontend callers of a REST endpoint (Angular services, components, templates)
   grep -rn "<endpoint-path>" core-web --include="*.ts" --include="*.html"

   # Playwright specs that exercise the endpoint or its UI surface (both E2E projects)
   grep -rn "<endpoint-path>\|<surface-name>" e2e core-web/apps/dotcms-ui-e2e
   ```

   For each caller hit, name it explicitly in the *Existing Coverage* block (file path + symbol) and treat the surface it renders (Content Drive, Host Folder Field, Page Editor, etc.) as **in scope** for the UI / UX axis. If no Playwright spec exists for that surface, record `No E2E scaffold for <surface>` — that is a finding, not a reason to skip. A backend-only diff is **not** sufficient grounds to mark the UI / UX axis Out of Scope when a caller exists.

### Output of this section

Produce a short audit block (kept inside your working notes, surfaced in Section 1's Summary) listing, for each affected class/method:

```
- <Class>#<method>
  - Existing tests: <FooTest#bar (passes), BazIT#qux (asserts buggy behavior — must update)>
  - Coverage gap: <one line, e.g. "no test for role-revocation cache propagation">
  - Reusable fixtures: <e.g. "RoleDataGen + APILocator.getPermissionAPI() pattern in PermissionAPITest">
```

If no existing tests are found, state explicitly: `No existing coverage found for <surface area>.` That is itself a finding worth flagging.

## Section 0-D — Coverage Matrix (MANDATORY)

Now that you know what exists, walk the matrix below against the fix's surface area. For **each axis**, decide **In scope** (must produce ≥1 TC) or **Out of scope** (record in the "Out of Scope" block of Section 1 with a one-line reason). Do not silently skip an axis.

| Axis | What to vary | Typical "in scope" trigger |
|---|---|---|
| **Permissions** | anonymous, authenticated back-end user, authenticated front-end user, CMS Anonymous role, CMS Owner, individual vs inherited permissions, role revocation | fix touches `Permission*`, `Role*`, REST endpoints, or any `checkPermission` path |
| **Sites / hosts** | `SYSTEM_HOST`, default host, secondary host, cross-host references | fix touches `Host*`, `Identifier*`, multi-tenant content, or queries scoped by host |
| **Languages** | default language, non-default language, `DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE` on/off, missing translation | fix touches `Contentlet`, `Language*`, search, rendering, or APIs that take `languageId` |
| **Content version state** | working only, live, archived, multiple versions, working ≠ live | fix touches `Versionable`, content publish/unpublish, workflow |
| **Cache layers** | cold cache, warm cache, **invalidation after mutation** (CMS, Identifier, Permission, Role, Layout, Short-Term, ES), **cross-node invalidation in a cluster** | any mutation of cached state (regression vector — see #35458) |
| **Workflow** | default scheme, non-default scheme, required action, mandatory step | fix touches `Workflow*` or content state transitions |
| **Push Publish** | bundle generation, receiver replay, integrity checks | fix touches publishable entities (Content, Templates, Containers, ContentTypes, Workflows, Categories) |
| **Persistence** | **PostgreSQL only** (CI does not run H2) | any DB-touching code; no in-memory shortcuts |
| **UI / UX** | rendering, keyboard navigation, focus management, screen-reader / ARIA, error & loading & empty states, responsive breakpoints (mobile/tablet/desktop), i18n display, dark/light theme, copy/microcopy | **Any one** of: fix touches `core-web/`; the PR diff includes `*.html` / `*.scss` / `*.component.ts` / `*.spec.ts`; the issue or an AC bullet names a UI surface (screen, dialog, button, form, selector, drawer — e.g. "Content Drive", "Site Browser", "Host Folder Field", "edit contentlet", "Page Editor"); the fix changes a REST endpoint consumed by `core-web/` or `e2e/` (verify by grepping the endpoint path); the issue carries a UI-area label (e.g. `dotCMS: New Edit Contentlet`, `Page Editor`); screenshots, Figma, or video are attached. **A backend-only diff does not exempt this axis** when an AC names a user-visible surface. |

### UI / UX — when to expand into multiple TCs

If the **UI / UX** axis is in scope, the plan **must** include at least one TC for each of the following sub-aspects that applies (skip a sub-aspect only if it provably doesn't apply, and record it in *Out of Scope*):

- **Visual rendering** — the component shows the correct content for the happy path and for each non-default state (empty, loading, error, disabled). Type: Jest + Spectator (`core-web/`) or Playwright screenshot.
- **Keyboard navigation** — tab order is logical, every interactive element is reachable, `Escape` closes modals/menus, `Enter`/`Space` activate buttons. Type: Playwright (`page.keyboard.press(...)`) or Spectator.
- **Focus management** — opening a dialog moves focus into it; closing returns focus to the trigger; route changes move focus to the main heading. Type: Playwright or Spectator.
- **Screen reader / ARIA** — interactive elements have accessible names, dynamic state changes are announced (`aria-live`, `aria-expanded`, `role="alert"`). Type: Playwright + axe-core, or manual screen-reader pass.
- **Error / loading / empty states** — each is rendered with the correct copy and is recoverable (retry button works, error doesn't strand the user). Type: Spectator.
- **Responsive layout** — at mobile / tablet / desktop breakpoints the component remains usable (no overflow, no overlap, no hidden actions). Type: Playwright with `page.setViewportSize(...)`.
- **i18n / locale** — copy resolves through dotCMS message bundles for at least the default language and one non-default language; long translations don't break layout. Type: Spectator with mocked bundle, or Playwright with locale switch.
- **Theme / contrast** — dark/light themes render correctly; text passes WCAG AA contrast for the components touched. Type: Playwright + axe-core.
- **Copy / microcopy** — labels, button text, error messages, and tooltips match the design spec or the issue's acceptance criteria verbatim. Type: Spectator (assert literal text).

**Acceptance-criteria-first (applies to every plan, not only when UI / UX is in scope)**: before walking the Coverage Matrix, **extract every bullet** from the issue's "Acceptance Criteria" / "AC" / "Definition of Done" section (and from any linked design doc). Each AC bullet that names a UI surface, a user-observable outcome, or a parity claim ("matches X", "the screen shows Y", "the field returns Z") **must** map to at least one TC, and the TC name must reference the bullet (e.g. `AC-3: Content Drive shows all subfolders matching Site Browser`).

An AC bullet with no TC is a coverage gap. It is **not** acceptable to drop it into *Out of Scope* with the reason "no automation framework exists" — that is a reason to make the TC **Manual** (see the carve-out in *Test-type bias by mode* below), not to skip it. The only acceptable Out-of-Scope reasons for an AC bullet are: it is already covered by an existing test (cite it), it has been explicitly de-scoped by the user during Section 0 clarifications (cite the Q#), or it is provably unreachable from the changed code path (explain why).

**Stack-specific routing for UI / UX TCs**:
- Logic-only assertions on a single component → **Jest + Spectator** (`core-web/`).
- Anything requiring a real browser, real DOM, real CSS, viewport sizing, or accessibility tooling → **Playwright** (E2E).
- Visual-only checks that can't be reliably automated → **Manual** with explicit "expected screenshot" and the path to a comparison image if available.

### Surface deduplication — when NOT to split into separate TCs

The rules above describe *what* to cover. This rule governs *how many TCs* to generate when the same scenario must be exercised on multiple rendering surfaces (e.g. route mode vs. dialog mode, embedded widget vs. standalone page, desktop vs. mobile breakpoint).

**Rule**: if two candidate TCs share the same scenario, the same action sequence, and the same expected result — and differ **only** in the surface on which they are executed — write **one TC** with clearly labelled sub-steps per surface:

```
- Steps To Reproduce:
  [Route mode]
  1. Navigate to /dotAdmin/#/content/<inode>.
  2. ...
  [Dialog mode]
  1. Open the same item via the Content listing → Edit modal overlay.
  2. ...
```

**Split into separate TCs only when** the two surfaces have genuinely different expected results OR touch different code paths that could fail independently (e.g. route guard vs. `DynamicDialogRef` intercept with distinct behaviour under the same scenario).

Applying this rule: for a fix that affects both route mode and dialog mode of the same feature, you should produce at most **one TC per scenario** (not one per scenario × one per mode).

### Mandatory test cases (auto-add when triggered)

The plan **must** include the following TCs when the trigger applies — do not omit. The **first** mandatory TC has a mode-specific name prefix:

| Mode | Prefix | Meaning of the TC |
|---|---|---|
| `tdd` | `Repro: <issue#>` | Failing test that becomes green after the fix |
| `qa-postfix` | `Verify: <issue#>` | The original bug no longer reproduces on the post-merge build. Run the bug's exact repro steps against the deployed/built environment and confirm the fixed behavior. |
| `qa-prefix` | `Acceptance: <issue#>` | Must fail today and must pass after any fix |

Other mandatory TCs (mode-independent in *what to verify*; the **Type of Test** depends on mode — see the rule below):

- **Cache invalidation** — when the fix mutates cached state, verify *mutate → read → new value*. High risk.
- **Cross-node cache invalidation** — when the fix touches a `*Cache*` layer or anything on the cache-transport path, verify the mutation propagates to **other cluster nodes**, not just the local JVM. dotCMS runs clustered; an entry that evicts correctly in-process can stay stale on a peer node (this is the failure mode behind regression #35458). Verify *mutate on node A → read from node B → node B returns the new value within the invalidation window*. High risk. (In-process IT cannot truly cluster — emit this as a Manual/flagged TC outside `qa-postfix`, and as a standard Manual TC in `qa-postfix`.)
- **REST: 401 unauthenticated** — when the fix touches `com.dotcms.rest.*`. High risk.
- **REST: 403 authenticated but unauthorized** — when the fix touches `com.dotcms.rest.*`. High risk.
- **REST: response contract** — verify response JSON shape matches the declared `@Schema`/`@ApiResponse` (per CLAUDE.md rule). Medium risk.
- **Upgrade / startup-task on a populated DB** — when the diff adds or modifies a startup/upgrade `*Task*` (e.g. an `UpgradeTask`/`StartupTask`) or any DDL. Verify the task runs cleanly against an **existing, populated** database (not a fresh install), is idempotent (a second startup does not re-run or error), and that the resulting schema matches expectation. Cross-reference [`docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`](../../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md) for rollback implications. High risk. Example steps: *restore a pre-fix DB snapshot → deploy the build → confirm the task runs once with no error and schema matches → restart and confirm it does not re-run.*

### Test-type rules by mode

#### `qa-postfix` — **Manual only**

In `qa-postfix` mode, **every** TC's `Type of Test` must be `Manual`. Do **not** emit `Unit`, `Integration`, or `E2E` TCs in this mode.

Rationale: the PR is **already merged** and its automated tests are now part of CI on `main` — they don't need to be re-listed in a QA plan. The QA engineer's deliverable is the **hand-check list** they execute against the post-merge build (release candidate, nightly, staging, master) — what they need to click, log in as, exercise across browsers, retry under flaky network, etc. Mixing Unit/IT/E2E specs into a post-merge QA hand-off plan creates work the QA engineer cannot execute and noise that hides the items they actually must do.

Concrete consequences in `qa-postfix`:
- **Verify TC** — Manual repro of the original bug **against the post-merge build** (log in, navigate, perform the action that previously failed, observe that the bug no longer reproduces). Steps must be runbook-style: which environment / build, which user, which site, which screen, which click.
- **Cache invalidation** — Manual: perform the mutating action via UI/API, refresh / re-query, confirm new value.
- **REST 401 / 403 / contract** — Manual via Postman GUI, `curl`, or in-app exercise (not Postman *collection* runner — that's automation). Provide the exact `curl` command in the steps.
- **Coverage Matrix axes** — every in-scope axis becomes a Manual TC. Use the data generators from `Stack-specific hints` only as *example commands the QA engineer can paste into a console*, not as `Type: Integration`.
- **Out-of-Scope reasons** referencing PR-added or main-branch tests are still valid (and encouraged — they shrink the manual list). An axis is `Out of Scope: covered by PR-added test FooTest#bar` does not become a Manual TC.

If you find yourself wanting to write a Unit/Integration/E2E TC in `qa-postfix`, instead either:
1. Mark the axis as `Out of Scope: should be added by the PR — flag to author` (and call it out in *Existing Coverage* as a missing automation gap), **or**
2. Convert the TC to a Manual hand-check that exercises the same behavior end-to-end.

The QA plan never asks the QA engineer to run a JUnit class.

##### Recommended Automation (follow-up) — `qa-postfix` only

The Manual-only rule governs the **Test Cases** section (what QA executes now). It does **not** mean the missing-automation signal should be lost. When the audit (Section 0-C) finds an in-scope behavior that the merged PR left **without** adequate automated coverage, record it in a separate **Recommended Automation (follow-up)** subsection at the end of the plan — *not* as a `TC-###` the QA engineer runs.

This keeps the two deliverables distinct:
- **Test Cases** = the manual hand-check list QA executes against the build.
- **Recommended Automation (follow-up)** = proposed Unit/IT/E2E coverage for the PR author / a follow-up issue, so the gap is actionable instead of buried in prose.

Each follow-up entry should name the uncovered behavior, the suggested test type and module (per *Test-module routing*), and a one-line rationale. Example:

```
- Gap: cross-node permission-cache eviction has no integration coverage.
  Suggested: Integration test in `dotcms-integration` exercising mutate-on-A → read-on-B.
  Why: regression-prone (see #35458); manual cluster checks don't run in CI.
```

If the PR's automation is complete, write `Recommended Automation (follow-up): none — PR coverage is adequate.` Do not pad this section with tests that already exist (those belong in *Existing Coverage*).

#### `tdd` and `qa-prefix` — automation-first

- When an axis has zero existing automated coverage, prefer **Unit** or **Integration** TCs — the goal is to grow the suite.
- **Carve-out — UI parity / user-visible behavior**: when the AC is a UI parity claim or user-observable outcome and no Playwright (or Spectator) scaffold covers the surface, a **Manual** TC is the correct output. Do not silently downgrade the AC to *Out of Scope* because automation is missing; the absence of a scaffold is itself the finding.

A Manual TC produced under any rule above must:
- Reference the AC bullet by ID in the Test Name when applicable (e.g. `AC-4: Host Folder Field shows all subfolders matching Site Browser`).
- State *why* it is Manual in one phrase (e.g. "Manual — no Playwright spec for Content Drive folder browsing", or in `qa-postfix` simply "Manual — qa-postfix mode").
- Spell out concrete repro data (counts, names, URLs) so QA can execute without re-reading the issue.
- Where useful, suggest a follow-up issue to add automated coverage for the surface — but do not block the current plan on it.

## Section 1 — Test Plan Output

Once Section 0 is resolved, output a markdown document with the parts below. The first line of the output is always the detected mode (e.g. `Mode: qa-postfix (PR #35458)`).

Section order: **Summary → Existing Coverage → Out of Scope → Test Cases**. In `qa-postfix` mode, append a final **Recommended Automation (follow-up)** subsection after Test Cases (see the qa-postfix Test-type rules above).

### Summary

Two or three sentences. Open with the framing for the detected mode:

- `tdd` — "These tests **drive the implementation** of the fix for issue #X."
- `qa-postfix` — "These manual checks **verify the merged fix** for issue #X on the post-merge build — confirming the bug no longer reproduces and surfacing regressions the merge could have introduced."
- `qa-prefix` — "These are the **acceptance criteria** for issue #X — they must fail today and pass after the fix lands."

Then state the bug in one line, the testing strategy in one line (e.g. "covered by 2 unit tests for the validator, 1 integration test for the REST endpoint, 1 manual cross-host check"), and cite any documentation from Section 0-B.

### Existing Coverage

Audit findings from Section 0-C — for each affected class/method, list:
- Existing tests that already pass and cover relevant behavior (these become Out-of-Scope reasons, not new TCs).
- **In `qa-postfix` only**: tests **added or modified by the PR itself** — list each by file + test name and which Coverage Matrix axis it covers.
- Existing tests that **assert the buggy behavior** and must be updated by the fix (call out by name).
- Disabled / `@Ignore` / `it.skip` tests in the area, with a question on whether the fix should re-enable them.
- Reusable fixtures/data-gens/base classes already in use.
- If nothing exists: state `No existing coverage found for <surface area>.`

### Out of Scope
Bulleted list of every Coverage Matrix axis (Section 0-D) marked Out of scope, each with a one-line reason. Acceptable reasons include `already covered by FooTest#barTest`, `not reachable from this code path`, or `non-goal per Q2 clarification`. If everything is in scope, write "None — full matrix covered." Reviewers use this section to challenge silent gaps.

### Test Cases

**Each test case MUST be wrapped in its own fenced code block** (triple
backticks). Do not emit TCs as a flat bullet list — without the fence the
plan renders as one wall of text and reviewers can't visually separate cases.
Leave a blank line between consecutive code blocks.

Use this exact format for every TC:

````
```
- Test ID: TC-###
- Test Name: <short imperative phrase, e.g. "rejects request when role is revoked mid-session">
- Risk: Critical | High | Medium | Low
- Scenario: Happy Path | Negative | Edge | Boundary
- Steps To Reproduce:
  1. <step that creates any required state — user, content, role, config>
  2. <step>
  3. <step>
- Expected Result: <observable outcome — assertion target, status code, UI state, log entry>
- Type of Test: Unit | Integration | E2E | Manual
- Replaces: <optional — existing test this hardens or supersedes, e.g. "PermissionAPITest#testRoleRemoval">
```
````

Omit the `Replaces` line when the TC is net-new. Always include it when Section 0-C identified an existing test that asserts buggy behavior or is too weak.

#### TC Consolidation Check (MANDATORY — do this before the Rendering check)

After generating all TCs, scan every pair and ask:
1. Do they test the **same scenario** (same user action, same expected result)?
2. Do they differ **only** in execution surface (route vs. dialog mode, browser, viewport, etc.)?

If yes to both → **merge** them into one TC:
- Move each surface into a labelled sub-step (`[Route mode]`, `[Dialog mode]`, etc.) inside Steps To Reproduce.
- Update the TC name to reflect both surfaces (e.g. "… in route and dialog mode").
- Delete the duplicate TC and renumber all subsequent IDs.
- Update the Summary's TC count.

Keep separate TCs only when the surfaces have different expected results **or** touch code paths that could fail independently. If you merged anything, note it as `(merged from <old IDs>)` in a comment on the surviving TC name for one revision — reviewers use this to audit the consolidation.

#### Rendering check (do this before finishing the plan)

Before printing/saving, confirm the output meets all of the following:

- Every TC starts with a line containing only ` ``` ` and ends with a line
  containing only ` ``` `.
- The TC body is **not** prefixed with `> ` quote markers, nor indented
  beneath a bullet — it sits at column 0 inside the fence.
- There is a blank line between the closing fence of one TC and the opening
  fence of the next.
- The same wrapping is used both in the conversation output (Section 1) and
  in the persisted file (Section 2) — they must be byte-identical.

If any of these fail, regenerate the Test Cases section before saving.

## Section 2 — Save the Plan (MANDATORY final step)

After printing the plan to the conversation, **persist it to disk** so it can be referenced later, attached to the GitHub issue/PR, or handed to a coding agent.

### Location

Save to `local/test-plans/` at the **repository root** (create the directory if it doesn't exist — `mkdir -p local/test-plans`). The `local/` directory is already gitignored, so plans stay out of version control by default.

### Filename rules

| Inputs you have | Filename |
|---|---|
| Issue # **and** PR # | `<issue>-<pr>-test-plan.md` (e.g. `35458-35459-test-plan.md`) |
| Issue # only (`tdd` or `qa-prefix`) | `<issue>-test-plan.md` (e.g. `35460-test-plan.md`) |
| PR # only (orphan PR — rare) | `pr<pr>-test-plan.md` (e.g. `pr35459-test-plan.md`) |
| Neither (description only) | `<short-kebab-slug>-test-plan.md` derived from the bug title (e.g. `permission-cache-revocation-test-plan.md`) |

If a file already exists at that path, **do not overwrite silently**. Read it first, then ask the user whether to overwrite, append a numeric suffix (`-v2`, `-v3`), or abort the save.

### File contents

Write the **exact same markdown** that you printed in Section 1, with two additions at the top:

```markdown
<!--
Generated by design-fix-test-plan skill
Mode: <tdd | qa-postfix | qa-prefix>
Issue: #<issue> (<issue-title>)
PR: #<pr>  (omit line if no PR)
Generated: <YYYY-MM-DD>
-->

# Test Plan — <one-line bug/feature description>

<rest of Section 1 output: Mode line, Summary, Existing Coverage, Out of Scope, Test Cases>
```

### Confirmation

After writing, output **one line** to the conversation telling the user where it went:

```
Saved: local/test-plans/35458-35459-test-plan.md
```

Do not re-print the plan body after saving — the user already saw it once.

## Rules

- **IDs are sequential and unique**: `TC-001`, `TC-002`, … Never reuse or skip.
- **Tests must be self-contained**: Steps must create everything needed to reproduce the bug — fixtures, users, permissions, content, config flags. Do not assume "a user exists" or "the system is in state X." Spell it out.
- **Risk and Scenario are two separate fields** — do not conflate them. `Risk` is *how bad it is if this fails*; `Scenario` is *what kind of input/flow the test exercises*. A happy-path flow can still be `Critical` risk (e.g. the login or publish path), so it must be expressible — that is exactly why these are split.
  - **Risk guidance**:
    - **Critical** — a security or data-integrity invariant; data loss/corruption; auth bypass; the failure blocks a core publish/render/login flow for all users.
    - **High** — the exact failure mode the bug describes, or a regression vector for an important invariant.
    - **Medium** — adjacent code paths, error handling, permission variations.
    - **Low** — cosmetic or low-blast-radius edge behavior.
  - **Scenario guidance**:
    - **Happy Path** — the normal successful flow with valid inputs.
    - **Negative** — invalid input, unauthorized actor, error path (expects a rejection/error response).
    - **Edge** — null/empty/missing/unusual-but-valid state (no translation, archived version, cold cache).
    - **Boundary** — limits and off-by-one conditions (min/max length, first/last page, count = 0 / 1 / N).
  - Every plan must include at least one `Happy Path` scenario and at least one `Critical`-or-`High` risk case.
- **Type of Test guidance**:
  - **Unit** — pure logic, single class/function, no I/O.
  - **Integration** — multiple components together, hits DB / cache / real services in-process (e.g. `dotcms-integration`).
  - **E2E** — full user flow through UI or external API (e.g. Playwright, Postman collections).
  - **Manual** — only when automation is genuinely impractical; explain why in the test name.
- **Expected Result is an assertion, not a description**: prefer "returns HTTP 403 with body `{error: "permission denied"}`" over "user cannot access".
- **One behavior per test**: if a step list contains "and then also verify…", split it into two test cases.
- **Order**: list `Happy Path` scenario cases first (smoke before deep), then order the remainder by descending `Risk` — `Critical`, `High`, `Medium`, `Low`.

## Anti-patterns to avoid

- Vague names like "test the fix" or "edge case test".
- Reproduction steps that start mid-flow ("Given a user with role X…") without creating the user.
- Combining unrelated assertions in one Expected Result.
- Inventing test types (no "smoke", "regression", "sanity" — map them to Unit/Integration/E2E/Manual).
- Producing a plan with only `Happy Path` scenario cases for a bug fix — the bug itself defines a `High`-or-`Critical` risk case.
- Collapsing `Risk` and `Scenario` back into one field, or marking a critical happy-path flow as low-risk just because it is the happy path.

## Stack-specific hints (dotCMS)

### Test-module routing (pick the narrowest module that exercises the change)

| Surface area touched | Module / framework |
|---|---|
| Pure POJO, util, validator, parser (no I/O) | `dotCMS` unit test (JUnit) |
| DAO, DB query, cache layer, transaction boundary, service composition | `dotcms-integration` (PostgreSQL-backed) |
| REST contract (status, headers, JSON shape, `@Schema`) | `dotcms-postman` **plus** a Java IT in `dotcms-integration` |
| OSGi / plugin lifecycle, classloader, hot reload | `dotcms-integration` + a Manual case for the lifecycle event |
| Angular component, signal, service, directive | Jest + Spectator in `core-web/` (see `docs/frontend/TESTING_FRONTEND.md`) |
| Full UI flow / end-to-end click path | Playwright (see `docs/testing/E2E_TESTS.md`) |

If the fix touches a REST endpoint, the plan **must** include both a Java IT (logic + permissions) and a Postman case (contract). A unit test on the resource class alone is insufficient.

### Test-data generators — use these in `Steps To Reproduce`

When a step says "create a user / host / role / content type / contentlet / language / template / workflow", resolve it to the canonical generator so the test is idiomatic:

| Need | Generator (or builder) |
|---|---|
| User | `new UserDataGen().nextPersisted()` |
| Role | `new RoleDataGen().nextPersisted()` |
| Host / Site | `new SiteDataGen().nextPersisted()` |
| Language | `new LanguageDataGen().nextPersisted()` |
| Content Type | `new ContentTypeDataGen().nextPersisted()` |
| Field | `new FieldDataGen().nextPersisted()` |
| Contentlet | `new ContentletDataGen(contentTypeId).nextPersisted()` |
| Template | `new TemplateDataGen().nextPersisted()` |
| Container | `new ContainerDataGen().nextPersisted()` |
| Folder | `new FolderDataGen().nextPersisted()` |
| Workflow Scheme | `new WorkflowDataGen().nextPersisted()` |
| Permission | `new PermissionDataGen()` + `APILocator.getPermissionAPI().save(...)` |

Steps must persist via `nextPersisted()` (not `next()`) when the test crosses a transaction or hits a cache. Use unique-suffix names (the generators do this by default) — never hard-code identifiers that could collide in a shared DB.

### Frontend specifics (`core-web/`)

- Modern syntax only: `@if`/`@for` blocks, `input()`/`output()`, `signal()` — tests must not match `*ngIf`/`@Input()` markup.
- HTTP: assert via `HttpTestingController`, never live `fetch`/`HttpClient` to a real backend.
- Selectors: prefer `byTestId` / `byText` over CSS selectors that couple to PrimeNG/DaisyUI internals.
- Effects/signals: drive the test via `TestBed.flushEffects()` or `fixture.detectChanges()` deliberately — don't rely on Zone autorun.