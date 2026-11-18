# Contract: CI report artifacts

**Requirements**: FR-008, FR-003a · **Research**: R-2 · **Consumer**: CI reporting, Maven, reviewers

This contract exists because its failure mode is invisible. A migration that runs every test
correctly but stops writing reports still shows a green build, so it can merge and stay broken for
weeks. Nothing in the source issue protected it.

## Produced today

> **Corrected during implementation.** An earlier draft of this contract stated that coverage lands
> in `target/core-web-reports/` alongside the JUnit report. **It does not.** Measured across the 49
> `jest.config.ts` files: **41 override `coverageDirectory`**, and of those **37 write to
> `core-web/coverage/<project path>`** while only 4 write under `target/`. The preset's
> `coverageDirectory: '../../../target/core-web-reports/'` is overridden almost everywhere.
>
> "The same paths" is therefore **not one path** — it is a per-project map. Any FR-008 assertion
> written against a single coverage directory would pass while 37 projects silently stopped
> producing coverage.

| Artifact | Path | Producer | Confidence |
|---|---|---|---|
| JUnit XML | **two locations, by project depth**: `core-web/target/core-web-reports/TEST-results.xml` (depth-3 projects) and `<repo-root>/target/core-web-reports/TEST-results.xml` (depth-2). All projects share one filename, so parallel runs overwrite | `jest-junit` reporter (preset) | **Measured** — research R-12 |
| Coverage (lcov, html, text) | **per project**: `core-web/coverage/<project>` (37), under `target/` (4), preset default (8) | `coverageDirectory`, overridden per project | Measured |
| Inline PR annotations | GitHub Checks | `github-actions` reporter (preset) | Measured (config) |

**RESOLVED — and the answer is that the existing reporting is broken** (research R-12, measured):

1. `outputDirectory` (`'../../../target/core-web-reports'`) resolves relative to each project's
   `rootDir`, so **depth-2 projects write outside `core-web/` entirely**. `libs/utils`' 111 tests
   landed in `<repo-root>/target/`, while `libs/portlets/dot-usage`'s 21 landed in `core-web/target/`.
2. All projects share `outputName`, so under `nx run-many` **the last writer wins**. Running those
   two together produced a report containing 21 tests; the 111 disappeared silently.

CI therefore consumes one project's results out of ~41, from a path half the projects never write
to. **This predates the migration.** The baseline capture works around it with
`JEST_JUNIT_OUTPUT_DIR`/`NAME` plus a cross-check against the runner's own totals; the defect itself
should be raised as its own issue rather than inherited or silently fixed here.

**Per-project identity — the report has none.** An earlier draft of this contract claimed suites are
named by Jest `displayName`. They are not: `<testsuite name>` is the top-level `describe()` block.
Measured — `portlets-dot-usage` names its suite `DotUsageShellComponent`. The report therefore
**cannot say which project produced it**, so attribution must come from the caller (which project was
just run), never from parsing the file.

## Required after migration — identical paths and formats

All three are first-party features of the target runner; no package is added (R-2).

| Artifact | Mechanism | Contract |
|---|---|---|
| JUnit XML | built-in `junit` reporter, tuple form with `outputFile` | Keep the format and the `core-web/target/core-web-reports/` location, but give **each project a distinct `outputFile`** so results stop overwriting each other. Vitest supports this natively, so it costs nothing; consolidating the two depth-dependent locations onto one is a side effect of setting the path explicitly per project rather than by `../../../`. Must parse as JUnit XML with a non-zero testcase count. |
| Coverage | `@vitest/coverage-v8` — reporters `html`, `lcov`, `text`; `reportsDirectory` | **Per project**, reproducing each project's own existing `coverageDirectory`: 37 under `core-web/coverage/<project>`, 4 under `target/`, 8 on the preset default. Not one shared directory. |
| Annotations | built-in `github-actions` reporter | A failing test annotates inline on the PR. |

**Consequence for the per-project migration**: each project's `coverageDirectory` must be read from
its own `jest.config.ts` and carried across, exactly as its DOM environment is (FR-007). Applying one
workspace-wide coverage directory would be the same class of mistake as applying one workspace-wide
environment — it looks tidier and silently changes 41 projects.

`jest-junit` is removed. `jest-html-reporters` is **retained in `package.json`** — not for this
suite, but because the out-of-scope Stencil project resolves it from the root manifest (FR-005,
spec correction 9).

## Verification — on the PR's own CI run, not locally

FR-008 requires this be *verified*, not asserted. Local runs do not exercise the GitHub reporter and
can mask path-resolution differences.

| Check | Pass condition |
|---|---|
| JUnit file exists | present at the path above after the unit-test phase |
| JUnit file is populated | parses, `tests` attribute > 0 — an empty well-formed file is the exact silent failure this guards |
| Coverage present | `lcov.info` exists and is non-empty |
| Annotation path works | a deliberately failed test annotates inline, then is reverted |

## Coupling to count parity

FR-003a derives per-project test counts **from this JUnit report** on both sides of the migration.
The two contracts therefore share a dependency, and it runs one way:

> If the JUnit report is missing or empty, count parity cannot be computed — and must report
> **failure**, never "0 == 0, parity holds".

A parity check that silently succeeds against two empty reports would defeat both requirements at
once, which is the single worst outcome available in this feature.

## Deliberately not required

- **Coverage thresholds.** `coverageThresholds` exists as a builder option and is left unset. The
  provider changes from istanbul to V8, so small deltas are expected and are not regressions
  (spec Assumptions). Coverage is reported per project, not gated.
- **Report format modernisation.** Anything beyond path-and-format parity is out of scope; changing
  what CI consumes is a separate decision from changing what produces it.
