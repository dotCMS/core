# Phase 1 Data Model: Diff-scoped strict typecheck gate

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-09-07

These are the harness's in-memory entities and the shape they take in the JSON report. There is
no database and no persisted state; the report is the only durable artifact.

---

## ChangedFile

One file the pull request added, copied, modified or renamed. Deleted files never become a
`ChangedFile` — the gate has nothing to check in a file that no longer exists.

| Field | Type | Notes |
|---|---|---|
| `path` | string | Repository-relative, forward slashes. For a rename, the **new** path. |
| `status` | `"A"` \| `"C"` \| `"M"` \| `"R"` | From the diff filter. |
| `changedLines` | array of `[start, end]` | 1-based, inclusive. Added and modified lines only, from hunk headers with zero context. Empty for a pure rename with no content change. |
| `kind` | `"source"` \| `"template"` | Drives which mode can produce diagnostics for it. |

**Rules**

- A path is normalized once, at construction, so every later comparison is a plain string match.
- `changedLines` is only consulted under line-level granularity; whole-file granularity ignores
  it entirely.

---

## ProjectTarget

One unit of compilation: an owning project paired with one of its configurations that actually
includes at least one changed file.

| Field | Type | Notes |
|---|---|---|
| `project` | string | Nx project name. |
| `root` | string | Project root, repository-relative. The longest matching root wins ownership. |
| `configPath` | string | The configuration whose resolved file list contains the changed file. |
| `mode` | `"typescript"` \| `"template-aware"` | Selected per project and always reported (FR-016). |
| `files` | array of string | The `ChangedFile` paths this target is responsible for. |

**Rules**

- A project may produce more than one `ProjectTarget` (for example a library configuration and a
  spec configuration), and a changed file may appear in more than one of them. Diagnostics are
  deduplicated afterwards, by file, line and code.
- A configuration resolving to zero files never becomes a `ProjectTarget`.

---

## UnmappedFile

A changed file that matched no project root. Reported, never discarded (FR-002).

| Field | Type | Notes |
|---|---|---|
| `path` | string | Repository-relative. |
| `reason` | string | Why nothing claimed it — no matching project root, or an owning project with no configuration that includes it. |

An unmapped file is **not** a gate failure on its own. It is a visible gap: workspace-root files
and tooling scripts belong here legitimately, and the report is what lets a reader tell those
apart from a mapping bug.

---

## Diagnostic

One reported error from either compiler, before or after filtering.

| Field | Type | Notes |
|---|---|---|
| `file` | string | Originating file, repository-relative. For an inline template this is the component source, not a template path. |
| `line` | integer | 1-based. |
| `column` | integer | 1-based. |
| `code` | string | Compiler diagnostic code. |
| `message` | string | Single line; nested explanatory chains are flattened. |
| `origin` | `"changed"` \| `"dependency"` \| `"untouched"` \| `"infrastructure"` | Why it survived or was discarded. |
| `layer` | `"source"` \| `"template"` | Which arm produced it; lets the report count them apart (FR-015). |

**Rules**

- `origin` is assigned by the filter and is the field the whole spike turns on:
  `"changed"` survives; `"dependency"` (a file from another project) and `"untouched"` (a file in
  this project the pull request did not touch) are discarded but **counted** (FR-004).
- `"infrastructure"` marks a diagnostic that is never a strictness violation whatever the flags —
  `TS2307` (cannot find module), `TS2688`, `TS6053`. Added after adjudication found one such
  diagnostic reported on a pre-registered clean pull request; it appears under plain `tsc` too, so
  a strictness gate reporting it is crying wolf. Discarded and counted like any other, never
  silently dropped.
- Under line-level granularity, a diagnostic in a changed file whose line falls outside every
  `changedLines` range is discarded as `"untouched"`.

---

## RunReport

The harness's output, one per invocation. Schema: [`contracts/report.schema.json`](./contracts/report.schema.json).

| Field | Type | Notes |
|---|---|---|
| `base` / `head` | string | The resolved commit SHAs actually compared. |
| `flagSet` | `"strict"` \| `"null-checks"` \| `"strict-max"` | Which candidate flag set ran (FR-007). `strict` is the repo convention (8 + 4); `strict-max` adds `noUncheckedIndexedAccess` and `exactOptionalPropertyTypes` and is measured for a future ratchet, never the blocking set. |
| `granularity` | `"file"` \| `"line"` | Which candidate granularity ran (FR-008). |
| `targets` | array of `ProjectTarget` | Including each one's selected mode. |
| `unmapped` | array of `UnmappedFile` | |
| `findings` | array of `Diagnostic` | Survivors only, all with `origin: "changed"`. |
| `discarded` | object | Counts by origin and by layer — the evidence that the filter, not luck, produced a pass. |
| `durationMs` | object | Wall-clock totals, split by mode so the template arm's cost is separable (FR-017). |
| `exitCode` | integer | `0` when `findings` is empty, non-zero otherwise (FR-005). |

**Invariants**

- `exitCode === 0` if and only if `findings` is empty.
- Every entry in `findings` has `origin: "changed"`.
- An empty diff yields a valid report with no targets, no findings and `exitCode: 0` — the
  no-op pass required by the spec's first edge case.

---

## SampleCase

One entry in the replay corpus. Its `expectation` is recorded **before** the harness runs
against it (D-009), which is what keeps the corpus from being fitted to the result.

| Field | Type | Notes |
|---|---|---|
| `pr` | integer | Pull request number. |
| `mergeCommit` | string | Head. Base is its first parent. |
| `expectation` | `"debt"` \| `"clean"` | The pre-registered label. |
| `knownFindings` | array | For `"debt"` cases, the violations expected — for PR #37262, the three in `sdk-create-app`. |
| `observed` | `RunReport` | Filled in by the run. |
| `adjudication` | array | Per finding: real or spurious, with the reason (SC-003). |

**Rules**

- A `"clean"` case producing any finding is a false positive and counts against SC-002 — unless
  adjudication shows the pre-registered label was wrong, in which case the label is corrected
  **and the correction is recorded**, never quietly amended.
