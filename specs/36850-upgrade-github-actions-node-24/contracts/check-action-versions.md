# Contract: `.github/scripts/check-action-versions.sh`

The drift guard. This is the **only interface** this change exposes, and it is what plays the Red
role for Constitution Principle V. Its contract is fixed here so `cicd_pr_actions-lint.yml`, the
bash test, and any future caller can depend on it.

Governed data: see [`../data-model.md`](../data-model.md) (`ActionFloor`).

## Invocation

```bash
.github/scripts/check-action-versions.sh [--include-docs] [--format text|github] [PATH ...]
```

Run from the repository root. No network access, no `gh` dependency, no writes — it is a pure
read-and-report over the working tree.

| Argument | Default | Meaning |
|---|---|---|
| `PATH ...` | `.github` | Roots to scan. Files are selected by extension (`.yml`, `.yaml`, plus `.md` under `--include-docs`). |
| `--include-docs` | off | Also check `README.md` usage examples and prose references. Off by default so a doc-only lag never blocks a code sweep. |
| `--format text` | default | Human-readable report. |
| `--format github` | — | Emit `::error file=<f>,line=<n>::<message>` so violations annotate the PR diff directly. Used by the CI job. |

## Exit codes

| Code | Meaning | Consumer behavior |
|---|---|---|
| `0` | No violations. | CI job passes. |
| `1` | One or more violations. | CI job fails; each violation is annotated. |
| `2` | Usage error — unknown flag, unreadable path, or a malformed manifest entry. | CI job fails. Distinguished from `1` so "the guard is broken" is never mistaken for "the repo is clean". |

`2` is a deliberate part of the contract: a guard that exits `0` when its own manifest is malformed
would silently stop guarding, which is precisely the failure mode #36850 is about.

## Assertions

For every `uses:` reference to an action present in the manifest:

**A1 — Version floor.** `uses: <action>@vN` must satisfy `N >= min_major`. Newer majors pass (it is
a floor, so upstream releases never create false failures). A non-numeric tag (`@v2-beta`,
`@master`, `@main`) is a violation regardless.

> `checkout@v4` below floor 7 · `setup-node@v2-beta` is non-numeric · `cache/restore@v3` below floor 6

**A2 — SHA pin integrity.** `uses: <action>@<40-hex>` must have `<40-hex> == pinned_sha` **and** a
trailing `# <pinned_version>` comment on the same line.

> Fresh SHA with a stale `# v4.2.0` comment · pin not matching `pinned_sha`

A2 exists because it is the likeliest mechanical error across 124 edits and the one code review
reliably misses — and because `main` already carries two such mismatches (one checkout SHA labelled
both `# v4.2.2` and `# v4.2.0`).

**A3 — Required inputs are explicit.** For each entry in `required_inputs`, the input must appear in
that step's `with:` block.

> `download-artifact@v8` with no `digest-mismatch:` — the value must be written, not inherited

A3 makes the `digest-mismatch` decision durable: a future major cannot move the default under us
without the guard noticing.

## Output

`--format text`, one line per violation, then a summary:

```
.github/actions/core-cicd/maven-job/action.yml:195: actions/cache/restore@v4 is below the required major (v6)
.github/workflows/dotbot-act.yml:40: actions/checkout SHA pin comment says v4.2.0, expected v7.0.1
.github/actions/core-cicd/maven-job/action.yml:266: actions/download-artifact is missing required input 'digest-mismatch'

124 violations in 51 files
```

Guarantees: `file:line:` prefix on every violation so editors and `::error` annotations can both
consume it; stable ordering (path, then line) so output is diffable across runs; the summary line
last, on stdout; usage/manifest errors on stderr.

## Behavior contract (what the bash test asserts)

`.github/workflows/tests/check-action-versions.test.sh` — following the convention set on this
branch by `.github/workflows/tests/link-issue-to-pr.test.sh`:

| # | Given | Expect |
|---|---|---|
| 1 | A fixture with `actions/checkout@v4` | exit `1`, A1 violation naming file and line |
| 2 | A fixture with `actions/checkout@v7.0.1` | exit `0` |
| 3 | A fixture with a *newer* major, `actions/checkout@v8` | exit `0` — floor, not equality |
| 4 | Correct SHA, wrong `# vX.Y.Z` comment | exit `1`, A2 violation |
| 5 | Correct SHA, no comment at all | exit `1`, A2 violation |
| 6 | `download-artifact@v8.0.1` with no `digest-mismatch` | exit `1`, A3 violation |
| 7 | `download-artifact@v8.0.1` with `digest-mismatch: warn` | exit `0` — `warn` and `error` both satisfy A3 |
| 8 | `setup-node@v2-beta` | exit `1` — non-numeric tag |
| 9 | An unknown flag | exit `2`, usage on stderr |
| 10 | A malformed manifest entry | exit `2` — never `0` |
| 11 | A `README.md` with `checkout@v2`, run without `--include-docs` | exit `0` |
| 12 | Same, run with `--include-docs` | exit `1` |
| 13 | An action absent from the manifest, e.g. `docker/build-push-action@v6` | exit `0` — out of scope, deferred to #37194 |
| 14 | Two violations in different files | both reported, ordered by path then line |

Case 3 and case 13 are the ones that keep the guard from becoming a maintenance burden: it must not
fail when upstream ships a new major, and it must not fail on actions this change deliberately did
not adopt.

## CI integration

`.github/workflows/cicd_pr_actions-lint.yml`, modeled on `.github/workflows/cicd_pr_skill-lint.yml`:

- `on: pull_request` with `paths: ['.github/workflows/**', '.github/actions/**', '.github/scripts/check-action-versions.sh']`
- `permissions: contents: read` only
- `concurrency` with `cancel-in-progress: true`
- Steps: sparse checkout → run the guard with `--format github` → run the guard's own bash test →
  run `actionlint`
- **Not** a required status check in this PR. Land it, let it run green for a week, flip required in
  a follow-up.

### Expected state during delivery

| Point in delivery | Guard | Why |
|---|---|---|
| On `main` today | **exit 1** (~124 violations) | This is the recorded **Red**. Link the failing run in the PR body. |
| Batches 1–8 in progress | exit 1, count falling | The PR is **intentionally red** here. Say so in the PR description so nobody reverts the guard. |
| After the sweep | **exit 0** | **Green.** |
| `--include-docs` after the README batch | exit 0 | The 8 doc references across 7 files are fixed too. |
