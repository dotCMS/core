# Feature Specification: Migrate all core-web unit tests from Jest and Karma to Vitest

**Feature Branch**: `nicobytes/37444-migrate-all-core-web-unit-tests-from-jest-and-karma-to-vitest`

**Created**: 2026-09-07

**Status**: Draft

**Type**: New Feature (test-infrastructure migration)

**Input**: GitHub issue [#37444](https://github.com/dotCMS/core/issues/37444) — *Migrate all core-web unit tests from Jest and Karma to Vitest*. Parent epic [#32713](https://github.com/dotCMS/core/issues/32713), whose *Reliability* objective covers unit tests: "the testing strategy is successfully migrated to Vitest for unit tests". **This feature delivers the unit-test half only** — the epic's E2E objective is separate work and is explicitly out of scope here (see [Out of scope](#out-of-scope)).

---

## Context

The `core-web` Nx workspace runs its unit tests on three runners at once. This feature consolidates
them onto one, without changing what the product does.

**Verified state of the workspace at `31c3672aa5`** (measured, not quoted from the issue):

| Runner | Wiring | Projects | Spec files |
|---|---|---|---|
| Jest | `@nx/jest/plugin`, explicit 41-glob `include` in `nx.json`, `targetName: "test"` | 41 | **977** |
| Karma | `@angular/build:karma` executor | 2 (`libs/dotcms-js`, `apps/dotcms-block-editor`) | 3 and **0** |
| Vitest *(already migrated)* | `@nx/vitest`, `testTargetName: "test"` | **1** (`libs/sdk/vue` only) | 14 |
| Stencil *(out of scope)* | `@nxext/stencil:test` on Stencil's own bundled Jest | 1 (`libs/dotcms-webcomponents`) | 7 |
| E2E *(out of scope)* | `@nx/playwright/plugin` | 1 (`apps/dotcms-ui-e2e`) | 35 |
| Elsewhere | not in any migrated target | — | 57 |
| | | | **1,093 total** |

> **Corrected against the issue.** The issue lists four projects as already on Vitest. Only
> `libs/sdk/vue` is: `libs/sdk/analytics`, `libs/sdk/experiments` and `libs/edit-content-bridge` each
> have a `jest.config.ts` **and** appear in the Jest `include` list — their `vite.config.mts` is a
> *build* config with no `test` block. Their 26 specs were counted as done; they are part of the
> work. See [data-model.md](./data-model.md) for the per-project inventory and
> [research.md](./research.md) R-1 for why this matters more than the arithmetic.

Vitest and `@nx/vitest` are already installed and already registered in `nx.json` under the **same
target name** Jest uses (`test`). Migrating a project is therefore mechanical: drop it from the Jest
`include` list, add a Vitest config, delete its `jest.config.ts`. The `nx test` target name never
changes, which is what keeps the GitHub Actions layer untouched.

Also verified:

- **No real Jasmine remains.** All 4 `jasmine.*` matches in the workspace are inside *comments*
  describing an earlier migration. Neither `jasmine-core` nor `karma` is in `package.json` — the two
  `karma.conf.js` files are orphaned config.
- **No snapshot tests exist** — 0 `.snap` files, 0 `toMatchSnapshot`/`toMatchInlineSnapshot` call
  sites. The `snapshotFormat` pin in `jest.preset.js` is dead config, and snapshot-format drift is
  not a risk for this migration.
- **`@openng/spectator@1.0.1` already ships a `./vitest` entry point** alongside `./jest`. 593 files
  import `@openng/spectator/jest`; the substitution is a supported, published entry point, not a
  shim. This resolves what the issue flagged as its single highest-risk unknown.

### Out of scope

Nothing below is touched by this feature. These are boundaries, not intentions — FR-001 makes the
product-code boundary machine-enforced, and FR-002a does the same for the E2E boundary.

- **End-to-end tests, entirely.** The 35 E2E specs under `apps/dotcms-ui-e2e/`, their
  `playwright.config.ts`, their page objects, fixtures and helpers, the two `@nx/playwright/plugin`
  entries in `nx.json`, the `e2e` target and its `targetDefaults` entry, and the
  `e2eTestRunner: "playwright"` generator default are all left exactly as they are. **This feature
  changes unit tests only.**

  Worth stating explicitly because it is a live trap rather than a hypothetical one: E2E specs are
  also named `*.spec.ts`, so the FR-001 allowlist pattern would *permit* editing them even though
  nothing here should. FR-002a closes that gap by requiring the enforcement check to exclude the E2E
  project outright, which turns "we did not mean to touch E2E" into a failing build if anyone does.

  Note also that FR-010 changes only `unitTestRunner` in the generator defaults. The sibling
  `e2eTestRunner` key sits in the same block and is not modified.

- **`libs/dotcms-webcomponents`.** Runs on `@nxext/stencil:test` using Stencil's own bundled Jest and
  its own TypeScript version. It is not in the `@nx/jest/plugin` include list. Migrating it means
  migrating off Stencil's test harness — its own issue. Every "no Jest remaining" assertion in this
  spec excepts it explicitly. **No file inside this project is edited**, which is why the one package
  it reaches into the root manifest for is retained rather than relocated (FR-005, Context
  correction 9).
- **Backend tests** — JUnit, integration, Karate and Postman suites.
- **Any product-code change.** See FR-002.
- **Rewriting what tests assert.** Test files themselves *may* be edited where the runner change
  requires it, but assertions and the behavior under test are preserved, and improving weak tests
  stays separate work. See FR-012.

### Corrections to premises stated in the issue

These were verified against the working tree and **change what the work must deliver**. They are
recorded here so the plan phase inherits the corrected picture rather than the issue's text.

1. **The Maven invocation is not what AC-6 describes, and it must change.** The issue states the
   `unit-test` execution runs `nx run-many -t test --exclude=tag:skip:test` and is unchanged.
   `core-web/pom.xml` actually runs `nx affected -t test --base=${git.origin.branch}
   --exclude=tag:skip:test --detectOpenHandles --forceExit`. `--detectOpenHandles` and `--forceExit`
   are **Jest-only CLI flags** — they must be removed. The adjacent `NODE_OPTIONS=--max-old-space-size=6144`
   block exists *because* `--detectOpenHandles` forces Jest into `runInBand`, so its stated rationale
   becomes obsolete too. The `core-web/pom.xml` path is already inside the issue's own AC-1 allowlist,
   so this is permitted — but the claim "CI needs no pipeline edit" must be restated as
   *no GitHub Actions workflow edit; the `test` target name is preserved*.

2. **AC-10's premise does not hold in this repository.** `tools/plugins/typecheck-spec.plugin.mjs`
   does not exist anywhere in the repo at `31c3672aa5`; there are **zero** `typecheck` targets in any
   `project.json`, and no such plugin in `nx.json`'s plugin list. The only `typecheck` targets are
   those inferred by three narrowly-scoped `@nx/vite/plugin` entries for individual SDK projects.
   There is consequently **no workspace-wide spec type-checking to preserve or to lose**. The real
   requirement is the inverse and much narrower (FR-011), and this migration must not be described as
   protecting a guard that is not there.

3. **CI report artifacts are an unlisted requirement.** `jest.preset.js` emits a JUnit XML report to
   `target/core-web-reports/TEST-results.xml` (via `jest-junit`), lcov + html coverage to
   `target/core-web-reports/`, and uses the `github-actions` reporter for inline annotations. No
   acceptance criterion in the issue preserves any of this. If the replacement does not produce
   equivalent artifacts at the same paths, whatever consumes them goes **silently empty** — a green
   build with no reports. Covered by FR-008.

4. **The Jest dependency list is incomplete.** Beyond the five packages the issue names, the workspace
   also carries `@jest/globals`, `@types/jest`, `babel-jest`, `jest-html-reporters`, `jest-junit`,
   `jest-util`, `jest-environment-node` and `@nx/jest`. Note that `@testing-library/jest-dom` is
   **not** Jest-specific and works under Vitest — it stays.

5. **The generator-defaults fix misses one generator.** `@nx/react` → `library` also pins
   `unitTestRunner: "jest"`, in addition to the two `@nx/angular` generators the issue names.

6. **`passWithNoTests: true` is a workspace default, not a CLI flag.** It is set in `nx.json` under
   `targetDefaults["@nx/jest:jest"].options`. Removing the Jest CLI flag is not enough; the default
   must not be recreated for Vitest.

7. **Counts drift slightly from the issue, so the environment mapping must be derived, not copied.**
   Actual: 49 `jest.config.ts` (51 `jest.config.*`), and the `testEnvironment` distribution is
   7 `@happy-dom/jest-environment`, 1 `jsdom`, 4 `node`, with the remainder inheriting the preset
   default — not the "7 / 1 / 9" the issue states.

8. **Documentation scope is wider than AC-12.** Jest-prescriptive guidance also lives in
   `docs/frontend/TESTING_FRONTEND.md` (19 mentions), `docs/frontend/TESTING_REVIEW_RULES.md` (3),
   `docs/frontend/README.md` (2), `docs/frontend/ANGULAR_STANDARDS.md` (1), and the Cursor rules
   `.cursor/rules/test-context.mdc` and `.cursor/rules/frontend-context.mdc`.

9. **Stripping Jest packages breaks the out-of-scope Stencil project.** `libs/dotcms-webcomponents/stencil.config.ts`
   names `jest-html-reporters` in its `testing.reporters`, and that project's `package.json` is a
   build manifest with **no dependency block at all** — it resolves the package from the root
   `core-web/package.json`, which is exactly where AC-4 wants it deleted from. Satisfying AC-4
   literally therefore breaks the one project the issue declares out of scope, and it breaks
   *silently*: the reporter fails to load, the build does not. `jest-html-reporters` MUST stay
   (FR-005). Scope of the exception verified as exactly one package: `@stencil/core@4.39.0` declares
   no `dependencies`, `peerDependencies` or `peerDependenciesMeta`, so it bundles its own test
   runner and reaches no other Jest package in the root manifest.

---

## Clarifications

### Session 2026-09-07

- Q: A spec cannot be migrated without editing product source — what is its end state in the workspace? → A: Migrate it to the new runner but mark it skipped, with the linked issue in an inline comment. Single-runner end state is preserved; "zero new skips" becomes "zero *unexplained* skips" backed by a per-skip justification table.
- Q: A spec becomes flaky or order-dependent purely because of runner differences — what is the sanctioned fix? → A: Edit the test file as needed (order dependence, mock reset, timer handling), enumerated per edit. Product files stay untouched — that is the only hard boundary, and the required check enforces it. Assertions and behavior under test are still not rewritten.
- Q: Removing Jest packages from the root manifest breaks the out-of-scope Stencil project, which resolves `jest-html-reporters` from there — how is that resolved? → A: Keep `jest-html-reporters` in the root `package.json`, annotated as belonging to Stencil rather than to the migrated suite, and except it in FR-005. Zero files in the out-of-scope project are touched.
- Q: The FR-001 allowlist covers test files only, so it would reject the enforcement script and codemod that this pull request itself adds — how is migration tooling treated? → A: Both are committed under a dedicated tooling path and the allowlist gains a separate, explicitly named tooling category. Both stay in the tree afterwards: the codemod as the review artifact for ~1,050 rewritten files, the check as reusable infrastructure.
- Q: What is the source of truth for the FR-003 test-count parity, given the baseline stops being reproducible once the old configs are deleted? → A: Derive counts programmatically from the JUnit report on both sides, via a committed script, at per-project granularity. Baseline capture becomes an explicit prerequisite step that runs before any config deletion.

## User Scenarios & Testing *(mandatory)*

The beneficiaries here are the people who build and review dotCMS, plus the CI system that gates
their work. There is no end-user-facing journey — and that absence is the point of User Story 1.

### User Story 1 - A reviewer approves a thousand-file change without auditing it (Priority: P1)

A reviewer opens a pull request touching over a thousand files. Rather than reading them, or running
a regression pass against the admin UI, they look at one required check that mechanically proves the
diff contains no product source file. Because no shipped code changed, no product behavior can have
changed, and the reviewer's remaining job is to confirm the tests still all run — which a second
check answers. They approve on evidence, not on a promise in the description.

**Why this priority**: This is the precondition for the whole migration being reviewable at all.
PR [#37198](https://github.com/dotCMS/core/pull/37198) (strict-mode rollout, 1,455 files) stalled not
on a defect but on the cost of manually QA-ing a diff that large — and there the concern was correct,
because strict mode *did* edit product code. Here nothing does. Without the enforcing check, that
distinction is an unverifiable assertion and this pull request inherits #37198's fate. Built first,
so every later commit is verified by it.

**Independent Test**: Land the check on its own, then push a commit that deliberately edits one
product source file — the check must fail. Revert it — the check must pass. Delivers value
immediately and independently: the same script is reusable by the next test-infra migration.

**Acceptance Scenarios**:

1. **Given** a pull request whose diff against the merge base contains only test files and test
   configuration, **When** CI runs, **Then** the enforcement check passes and reports the file
   categories it allowed.
2. **Given** a pull request that modifies any product source file, **When** CI runs, **Then** the
   check fails and names the offending path(s).
3. **Given** a pull request that modifies any file in the end-to-end suite — an E2E spec, its runner
   configuration, or its workspace wiring — **When** CI runs, **Then** the check fails, even though
   E2E specs share the `*.spec.ts` naming that the allowlist otherwise admits.
4. **Given** a reviewer looking at the merged evidence, **When** they read the enforcement check and
   the test-count parity report, **Then** they can answer "did product code change?", "was E2E
   touched?" and "are all unit tests still running?" without opening a single spec file.

---

### User Story 2 - A developer runs the whole suite on one runner (Priority: P1)

A developer runs `nx test` for any project in the workspace and it executes on Vitest. The same
command works everywhere; there is no per-project question of which runner applies, no Jest config to
reason about alongside a Vitest one, and no Karma executor surviving in two corners.

**Why this priority**: The migration's actual deliverable. Equal priority to Story 1 because the two
ship together in one pull request — Story 1 is what makes Story 2 mergeable.

**Independent Test**: For each migrated project, run its test target and compare executed-test count
and pass/fail state against the recorded Jest baseline.

**Acceptance Scenarios**:

1. **Given** any project that ran on Jest, **When** its test target is invoked, **Then** it runs on
   Vitest, and the number of executed tests equals the Jest baseline apart from skips declared in
   the justification table, each with a linked issue.
2. **Given** `libs/dotcms-js`, **When** its test target is invoked, **Then** its 3 specs run on Vitest
   and pass.
3. **Given** `apps/dotcms-block-editor` (which has no spec files), **When** the workspace is
   inspected after the change, **Then** its dead test target, `karma.conf.js`, `src/test.ts` and
   test polyfills entry are gone rather than migrated.
4. **Given** the full workspace, **When** the unit-test suite runs, **Then** it is green with no
   project passing vacuously on an empty suite and no retry flag concealing flakiness.

---

### User Story 3 - CI keeps reporting what it reported before (Priority: P2)

The pipeline continues to publish unit-test results and coverage from `core-web` exactly where it
published them before: a JUnit XML report and lcov/html coverage under `target/core-web-reports/`,
plus inline GitHub annotations on failures. Nothing downstream of the test run notices the runner
changed.

**Why this priority**: A silent failure mode, and the one this feature is most likely to introduce
unnoticed. A build that runs every test correctly but writes no report still shows green, so it can
merge and stay broken for weeks. Not P1 only because it cannot make the product wrong — but it can
make CI stop telling the truth about the product.

**Independent Test**: Inspect the pull request's own CI run: assert the report artifacts exist at the
expected paths and are non-empty, and that a deliberately failed test still surfaces as a GitHub
annotation.

**Acceptance Scenarios**:

1. **Given** a completed unit-test run in CI, **When** the report locations are inspected, **Then** a
   populated JUnit-format test report and lcov coverage exist at the same paths as before.
2. **Given** a failing test in CI, **When** the run completes, **Then** the failure is annotated
   inline on the pull request as it was under the previous runner.
3. **Given** the GitHub Actions workflow definitions, **When** the diff is inspected, **Then** none
   of them changed; the only build-layer edit is the removal of runner-specific flags from the Maven
   invocation, itself inside the enforcement allowlist.

---

### User Story 4 - The migration does not erode (Priority: P2)

A developer generates a new Angular application, Angular library or React library. It arrives
configured for Vitest, with no manual config step and no reintroduction of Jest.

**Why this priority**: Cheap to do and it protects the whole result. Without it the next generated
project silently re-adds a second runner and the workspace drifts back toward the state this feature
exists to end.

**Independent Test**: Generate one project of each affected type into a scratch location and confirm
it is wired to Vitest.

**Acceptance Scenarios**:

1. **Given** the workspace generator defaults, **When** an Angular application, Angular library or
   React library is generated, **Then** its unit-test runner is Vitest.
2. **Given** developer-facing guidance (`core-web/CLAUDE.md`, `docs/frontend/*`, `.cursor/rules/*`),
   **When** it is read after the change, **Then** it prescribes Vitest and contains no instruction
   that is only correct under Jest.

---

### Edge Cases

- **A spec passes only because of runner-specific behavior.** Module-registry semantics, mock
  hoisting order, fake-timer details and intra-file execution order can all differ between runners,
  so some specs will pass in isolation and fail in suite with no product bug involved. Resolution is
  two-tier: **fix it in the test file** (FR-012) — that is expected, sanctioned work, not an
  exception. Only when no test-file edit can work because product source would have to change is the
  spec **marked skipped** with a linked issue and a justification-table entry (FR-002, FR-004). The
  enforcement check (Story 1) fails the build rather than letting a product edit slip in, so the
  fallback is always skip-and-track, never quietly patch the product.
- **A project has zero spec files.** It is deleted, not migrated (`apps/dotcms-block-editor`). A
  migration that carries dead config forward is not a migration.
- **The memory ceiling that Jest needed comes back.** `jest.preset.js` sets
  `workerIdleMemoryLimit: '1536MB'` to stop V8 aborting on `libs/ui` — a real, measured OOM
  (issue #37245), not a precaution. Vitest has no identical knob. Worker-memory behavior on the
  heaviest projects (`libs/ui`, `apps/dotcms-ui`) must be observed, not assumed, before the Maven
  `NODE_OPTIONS` ceiling is treated as unnecessary.
- **`main` moves under a 1,000-file diff.** Any concurrent merge that touches a spec file conflicts.
  Per-project commits keep rebasing tractable and make a split-by-project fallback cheap.
- **A project's DOM environment is implicit.** Most `jest.config.ts` files do not name a
  `testEnvironment` and inherit the preset default. Reading only the explicit declarations would
  silently move those projects to a different environment, so the mapping is derived per project from
  effective configuration, not from declared values.
- **The out-of-scope Stencil project.** `libs/dotcms-webcomponents` is not in the Jest `include` list
  and runs Stencil's own bundled Jest on its own TypeScript version. Completeness assertions about
  "no Jest remaining" must explicitly except it, or they will fail against work that was never in
  scope.

## Requirements *(mandatory)*

### Functional Requirements

**Enforcement — what replaces manual QA**

- **FR-001**: A required CI check MUST inspect the pull request diff against its merge base and MUST
  fail the build if any changed path falls outside a committed allowlist of test files and test
  configuration. Any product source path MUST fail. The check MUST be committed as a reusable script
  so subsequent test-infrastructure migrations inherit it.
- **FR-001a**: The allowlist MUST carry a **separate, explicitly named category for migration
  tooling**, distinct from the test-file categories, covering the enforcement script itself and the
  FR-013 codemod under a dedicated tooling path. Without it the check rejects the very pull request
  that introduces it. Both scripts MUST be committed and MUST remain in the tree after the
  migration: the codemod is the pull request's most useful review artifact — it lets a reviewer
  verify ~1,050 mechanically rewritten files by reading one transformation instead of a thousand
  diffs — and the enforcement script is reusable by the next test-infrastructure migration. Neither
  is product code, so neither weakens FR-001's guarantee; keeping them in their own category is what
  keeps that visible rather than smuggling tooling in under a test-file glob.
- **FR-002**: The change MUST NOT modify any product source file. If a spec cannot be migrated
  without editing product source, that spec MUST still be migrated to the new runner and marked
  **skipped**, carrying an inline comment that links the issue raised for it. It MUST NOT be left on
  a retired runner (that would violate FR-005 and recreate the mixed-runner end state this feature
  exists to remove) and MUST NOT be deleted (a deleted test cannot fail, so the coverage loss would
  be silent). The skip MUST be declared under FR-004.
- **FR-002a**: The change MUST NOT modify anything belonging to the end-to-end test suite. The
  FR-001 allowlist MUST therefore **exclude** the E2E project (`apps/dotcms-ui-e2e/`) rather than
  permit it, since its specs share the `*.spec.ts` naming that the allowlist otherwise admits.
  E2E runner configuration, the E2E plugin entries and target defaults in workspace configuration,
  and the `e2eTestRunner` generator default MUST all be left unchanged — the check MUST fail the
  build if any of them appear in the diff.
- **FR-003**: For every migrated project, the number of executed tests MUST equal the pre-migration
  baseline, with zero newly skipped or `todo` tests **other than those declared under FR-004**.
  Baseline and post-migration counts MUST be recorded per project, and any intentional difference
  justified individually.
- **FR-003a**: Counts MUST be derived **programmatically from the JUnit-format test report** on both
  sides of the migration, by a script committed under the FR-001a tooling path — not transcribed by
  hand. The report already exists on both sides (FR-008 requires it preserved), so the comparison
  comes from an artifact CI produces anyway. Per-project granularity is mandatory: a workspace-level
  total can hide one project losing tests while another gains them.
- **FR-003b**: Capturing the baseline MUST be an explicit prerequisite step, completed **before** any
  retired-runner configuration is deleted. Once those configs are gone the baseline is no longer
  reproducible from the branch, which would reduce FR-003 to an unverifiable claim about 41 projects.
- **FR-004**: The full unit-test suite MUST pass across all migrated projects. No project may pass
  vacuously on an empty suite (the pre-existing "pass with no tests" workspace default MUST NOT be
  recreated), and no retry mechanism may mask flakiness. **Zero *unexplained* skips are permitted**:
  every newly skipped test MUST appear in a per-skip justification table in the pull request
  description, naming the test and its linked issue. A skip with no table entry fails acceptance.
  The table is the complete inventory of coverage this migration parks, so it MUST be reviewable as
  a single list rather than scattered across commit messages.

**Migration completeness**

- **FR-005**: No Jest wiring MUST remain in `core-web`, excepting `libs/dotcms-webcomponents`: no
  `jest.config.*` files, no Jest preset, no Jest plugin entry or `include` list in workspace
  configuration, and no Jest-only package in `package.json` — including the packages beyond those
  named in the issue (see Context correction 4).

  Two classes of package MUST be retained rather than removed on the strength of a Jest-sounding
  name, each annotated in `package.json` with why it stays:

  1. **Runner-agnostic packages** that work unchanged under the new runner.
  2. **`jest-html-reporters`**, which is required by the out-of-scope Stencil project — see Context
     correction 9. Removing it would break that project silently.

  The exception is exactly one package wide: the Stencil toolchain bundles its own test runner and
  declares no peer dependencies, so no other Jest package is reachable from it.
- **FR-006**: No Karma wiring MUST remain: both Karma executor targets gone, `libs/dotcms-js`
  running its specs on Vitest, `apps/dotcms-block-editor`'s dead test target and associated files
  deleted, and the stale workspace-level `karma.conf.js` input reference (which already points at a
  nonexistent file) removed.
- **FR-007**: Each project's effective DOM environment MUST be preserved, derived per project from
  effective configuration rather than from explicitly declared values only, so that no test silently
  gains or loses DOM APIs. The resulting mapping MUST be recorded in the pull request description.
- **FR-008**: CI-consumed test and coverage artifacts MUST continue to be produced in the same
  formats at the same paths: a JUnit-format test report and lcov coverage under
  `target/core-web-reports/`, plus inline GitHub failure annotations. This MUST be verified against
  the pull request's own CI run rather than asserted.
- **FR-009**: No GitHub Actions workflow definition MUST change. The `nx` test target name MUST
  remain `test`. Runner-specific CLI flags in the Maven `unit-test` invocation MUST be removed, and
  the obsolete rationale in the surrounding configuration comments corrected rather than left to
  mislead.
- **FR-010**: Workspace generator defaults MUST select Vitest as the unit-test runner for the Angular
  application, Angular library **and React library** generators. Only the unit-test runner key is
  changed; the `e2eTestRunner` default in the same configuration block MUST be left untouched
  (FR-002a).
- **FR-011**: Every project that resolves a `typecheck` target today MUST still resolve one after the
  change — project count before equals after. This requirement is deliberately narrow: contrary to
  the issue's AC-10, no workspace-wide spec type-checking exists in this repository to preserve (see
  Context correction 2), and the change MUST NOT be represented as protecting one.

**Fidelity of the port**

- **FR-012**: Test files MAY be edited where the runner change requires it — correcting order
  dependence, mock reset, timer handling, or module-registry assumptions that only held under the
  retired runner. **Product files MUST NOT be edited under any circumstances** (FR-001, FR-002): that
  boundary, not a test-file boundary, is what makes this change behavior-safe, and it is the one the
  required check enforces.

  Within a test file, what the test *asserts* MUST be preserved — assertions and the behavior under
  test are not rewritten, and improving weak tests remains separate work. Every edit beyond what the
  codemod performs MUST be enumerated per FR-013, and FR-003's count parity remains the guard
  against a stubborn test being quietly dropped rather than fixed. Skipping (FR-002, FR-004) is
  reserved for the case where no test-file edit can work because product source would have to
  change; it is not an alternative to fixing an order dependence.
- **FR-013**: Mechanical rewrites MUST be performed by a codemod so the diff is uniform and
  reviewable by pattern rather than file by file. This includes the mock/spy/timer API substitutions,
  type references, and the `@openng/spectator/jest` → `@openng/spectator/vitest` entry-point change
  across 593 files. Hand-edits MUST be limited to what the codemod cannot do and enumerated in the
  pull request description. The codemod MUST be committed and retained per FR-001a — it is the
  artifact that makes the mechanical portion of the diff reviewable by reading one transformation
  rather than a thousand files.
- **FR-014**: The change MUST be delivered as a single pull request with **one commit per project**,
  so review and bisection can proceed project by project and a split-by-project fallback stays cheap.
- **FR-015**: The 4 stale comments referencing Jasmine spy APIs MUST be removed — they describe a
  migration two runners ago and become actively misleading once Jest is gone.

**Documentation & measurement**

- **FR-016**: Developer-facing testing guidance MUST be updated wherever it prescribes runner-specific
  behavior — `core-web/CLAUDE.md`, `docs/frontend/TESTING_FRONTEND.md`,
  `docs/frontend/TESTING_REVIEW_RULES.md`, `docs/frontend/README.md`,
  `docs/frontend/ANGULAR_STANDARDS.md`, `.cursor/rules/test-context.mdc` and
  `.cursor/rules/frontend-context.mdc` (see Context correction 8). Claims inherited from the previous
  runner — notably that the test target does not type-check — MUST be **re-verified** under the new
  runner rather than copied across.
- **FR-017**: Unit-test wall time before and after MUST be recorded in the pull request description.
  No threshold gates the change: runner variance and cache state make a fixed target
  non-reproducible, so the number is reported as input to the epic's Performance objective, not as a
  pass condition.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can establish that no product behavior changed by reading **one** automated
  check result, with zero product source files to audit and zero manual regression passes required.
- **SC-002**: 100% of unit tests that executed before the change execute after it, except a declared
  set each of whose members has a linked issue and a justification-table entry; 0 *unexplained*
  skips; 0 projects passing on an empty suite. Established per project from the machine-readable test
  report on both sides, not asserted.
- **SC-003**: Exactly one unit-test runner remains across the workspace, with a single documented
  exception (the Stencil-based library, which was never in scope). 0 surviving configs, presets,
  packages or workspace wiring for the two retired runners.
- **SC-004**: Unit-test result and coverage reports remain present and non-empty at their existing
  locations, and a failing test still annotates inline — each verified on the change's own CI run.
- **SC-005**: 0 GitHub Actions workflow files change. 0 test target names change.
- **SC-005a**: 0 files belonging to the end-to-end suite change — 0 E2E specs, 0 E2E runner
  configuration, 0 E2E workspace wiring — and this is enforced by the same required check as the
  product-code boundary, not left to reviewer attention.
- **SC-006**: A newly generated project of each affected type arrives on the new runner with 0 manual
  configuration steps.
- **SC-007**: 0 statements in developer-facing testing guidance remain that are correct only under a
  retired runner.
- **SC-008**: Unit-test wall time before and after is recorded and published for the epic's
  Performance objective.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: **None.** This is the defining property of the feature, not a
  hopeful summary. The change is confined to the frontend workspace's test tooling; no backend code,
  no `com.dotmarketing.*` legacy surface, no REST contract, no database schema, no shipped bundle.
  Test files are not bundled, so no artifact delivered to a customer differs by a byte. FR-001 makes
  this machine-enforced rather than asserted.
- **Backward-compatibility expectations**: No product API, content, or admin workflow is affected.
  The compatibility surface that *does* matter is internal and developer-facing: the `nx test` target
  name (preserved, FR-009) and the CI report artifact contract (preserved, FR-008). Breaking either
  would be invisible in a green build, which is why both are explicit requirements.
- **Known related decisions**:
  - Epic [#32713](https://github.com/dotCMS/core/issues/32713) sets the direction for unit tests:
    Vitest. Its separate E2E objective is not addressed here.
  - PR [#37198](https://github.com/dotCMS/core/pull/37198) / epic
    [#35932](https://github.com/dotCMS/core/issues/35932) (strict mode) is the cautionary precedent:
    a large diff that stalled on review cost. The lesson applied here is not "write smaller pull
    requests" but "make the safety property checkable" — that pull request edited product code and
    this one provably does not.
  - Issue #37245 documents the measured CI heap OOM that produced the current worker-memory
    settings — relevant to the edge case above, and not to be discarded as a stale precaution.
  - `@angular/build@22.1.0` ships a native `unit-test` builder that defaults to Vitest, which is the
    path angular.dev documents. This feature deliberately uses the Nx plugin instead, including for
    the two Karma projects, to keep one configuration style across the workspace. The native builder
    remains the fallback if the Nx plugin proves limiting.
  - The plan phase will formally consult `dotCMS/platform-adrs` for binding decisions.

## Assumptions

- **~~The already-migrated projects are a working reference.~~ Corrected — there is no Angular
  precedent.** Phase 0 research disproved this: only `libs/sdk/vue` runs Vitest, it is a Vue project,
  and **no** project in the workspace uses `TestBed` or `@openng/spectator` under Vitest.
  Angular-plus-Vitest viability is **assumed, not established**, which is why the plan makes the
  spike a decision gate rather than a warm-up ([research.md](./research.md) R-1). The existing config
  is still a useful reference for path aliasing and output conventions (R-5) — and for nothing that
  matters here.
- **`@openng/spectator`'s Vitest entry point is behaviorally equivalent to its Jest one.** Its
  existence is verified (published in `1.0.1`); equivalence across all 593 importing files is
  assumed and MUST be proven by spiking one representative Angular project before the codemod runs
  workspace-wide. This remains the highest-risk unknown even though the entry point exists.
- **The single-pull-request delivery decision stands**, per the issue. Flagging one factor without
  disputing it: a diff this size competes for review throughput and will need rebasing against every
  `main` merge that touches a spec file. If it stalls for that reason rather than a QA reason, the
  per-project commits (FR-014) make splitting by project a cheap pivot.
- **Removing the Jest-only Maven flags is behavior-preserving for the suite** beyond the memory
  characteristics called out in the edge cases. The flags forced single-process execution; the new
  runner's default worker model is assumed acceptable and MUST be confirmed on the heaviest projects.
- **Coverage numbers will shift slightly.** The two coverage providers instrument differently; small
  deltas are expected and are not treated as regressions. Coverage is reported per project rather
  than gated.
- **No new dependency is introduced.** Vitest, its coverage provider, the Nx plugin and both DOM
  environment packages are already in `package.json`. **This assumption survived only because of the
  runner decision taken in the plan**: Angular projects run on `@angular/build:unit-test`
  (`@angular/build 22.1.2` already declares `vitest` as an optional peer). The alternative — keeping
  the issue's `@nx/vitest` approach for Angular — would have required adding
  `@analogjs/vite-plugin-angular` and broken this assumption ([research.md](./research.md) R-1).
*(The former assumption about capturing test-count baselines before migration has been promoted to a
requirement — see FR-003b — and is no longer listed here.)*
