# Data Model: Wire the diff-scoped strict typecheck gate into CI

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-09-14

This feature stores nothing. It has no database table, no index mapping, no serialized state, and no
persisted record of any kind. What it does have is a small set of entities that move between the
harness, the build and the reader — and one of them, **Run Outcome**, is the whole feature's
correctness condition. That is why this file exists rather than being skipped.

The harness's own data shapes were defined by PR #37403 and are unchanged here
(`specs/37401-diff-scoped-strict-typecheck-gate/data-model.md`,
`contracts/report.schema.json`). What follows is only what *this* feature adds or constrains.

---

## Run Outcome — three states that must never collapse into two

The single most important structure in the feature. Every requirement about blocking, failing and
reporting is a statement about this enum.

| State | Harness exit | Build result | What the reader sees |
|---|---|---|---|
| **Clean** | `0` | pass | Nothing. No annotations, no findings in the summary. Includes the no-op case where the diff touches no frontend file. |
| **Findings** | `1` | **fail** | Inline annotations on the changed lines, plus a summary listing them. The change does not merge until they are fixed (FR-010). |
| **Could not run** | `2`, or killed at the time limit (`143`) | **fail** | A failed check naming the cause (FR-011, FR-013). |

**Invariant — the one that defines the feature**: *Clean* must be distinguishable from both other
states. *Findings* and *Could not run* share a consequence today (both fail) but are different
facts, and must stay separate in the reasoning even where the configuration cannot separate them. The failure mode this guards against is
a harness that reports "clean" without having looked, which is indistinguishable from a genuinely
clean pull request and would silently defeat the gate — the same class of defect as the gate
existing and never executing, which is why issue #37536 exists.

**Encoding** (see [contracts/ci-hooks.md](./contracts/ci-hooks.md)): `<successCodes>` lists `0` and
nothing else. The absence of everything else is the mechanism; a comment in the POM must say so,
because a well-meaning "let's tolerate 1 while we clean up" would be invisible in review and would
turn the gate back into a report.

**State transitions**: none. A run produces exactly one outcome and nothing survives it.

---

## Execution Context — where the gate is allowed to run

Derived, not stored. Read from the environment at build time (R-005).

| Field | Source | Values |
|---|---|---|
| Event | `env.GITHUB_EVENT_NAME` | `pull_request` → run (annotations); `merge_group` → run (enforcement); anything else → skip |
| Validation profile | `-Pvalidate` | active → the execution exists; absent → it does not |
| Path relevance | `.github/filters.yaml` `frontend` filter | decides whether the containing job runs at all |

The three are independent and all must hold. Note the layering: the path filter is evaluated by CI
*before* the job starts, the profile decides whether the execution is declared, and the event name
decides whether a declared execution is skipped. A backend-only pull request is stopped by the first
of the three and never reaches the other two — which is why SC-002 asks for *zero* measurable cost,
not merely a fast no-op.

---

## Finding

Produced by the harness; this feature only routes it. One strict violation.

| Field | Meaning | Used by |
|---|---|---|
| `file` | Repository-relative path | annotation target, summary row |
| `line`, `column` | Position within the changed hunk | annotation target |
| `code` | The TypeScript diagnostic code (e.g. `TS4111`) | fix guidance lookup in `format.mjs` |
| `message` | What is wrong | annotation body |

**Constraint carried from the spec (FR-002)**: every finding's `line` falls inside a hunk this
branch added or modified. A finding outside the diff is a defect, not a strict violation — the spike
measured 99.1 % of diagnostics being discarded on a representative portlet precisely to make this
hold.

---

## Observed Duration — the evidence artifact

The only entity this feature *adds*, and it is added for one reason: SC-005 cannot be satisfied if
nobody can read a run's cost — and now that the gate blocks, that cost is on every frontend merge.

| Field | Source | Notes |
|---|---|---|
| Elapsed time | `report.durationMs.total`, already computed by the harness | Never printed before this feature — FR-018 surfaces it |
| Diff size | distinct paths across `report.targets[].files` **and** `report.unmapped[].path` | There is no `report.files`. The count must be of unique paths: `selectConfigs` claims a source under every eligible config, so one changed file can appear in two targets, and unmapped files were changed too — they just were not examined. |

**Lifecycle**: emitted into the job summary on every run, read by a human, transcribed onto issue
#37536 for at least five real pull requests (FR-022). Nothing is persisted by the system — the issue
comment *is* the record. That is deliberate: building storage for five numbers would cost more than
the decision they inform.

**Why it must appear on clean runs too**: a duration recorded only when findings exist would sample
the fast cases and the slow cases unevenly — and with the gate blocking, the tail is what sits on
the critical path of every frontend merge.

---

## Not modelled here

- **The report JSON** — defined and versioned by PR #37403; this feature neither extends nor reads
  it beyond the two fields above.
- **Any per-developer state.** An earlier draft carried a local push-time check with an opt-out
  environment variable; that was cut, and this change is continuous-integration verification only.
  Nothing here is per-developer, so there is nothing to model.
