# Contract: strict-gate command line

**Feature**: [spec.md](../spec.md) | **Plan**: [plan.md](../plan.md) | **Date**: 2026-09-07

Two entry points. Both live in `core-web/tools/scripts/strict-gate/` and are run with the
workspace's pinned Node.

---

## `run.mjs` — check one range

```
node tools/scripts/strict-gate/run.mjs --base <ref> --head <ref> [options]
```

| Option | Values | Default | Requirement |
|---|---|---|---|
| `--base` | git ref | *required* | Fetched if absent locally (FR-011) |
| `--head` | git ref | `HEAD` | |
| `--flags` | `strict` \| `null-checks` \| `strict-max` | `strict` | FR-007. `strict` is the repo convention (8+4) — the same yardstick as PR #37198. `strict-max` adds `noUncheckedIndexedAccess` + `exactOptionalPropertyTypes`: measured for a future ratchet, not the blocking set. |
| `--granularity` | `file` \| `line` | `line` | FR-008. `line` blames only lines the pull request wrote; `file` makes whoever touches a legacy file inherit its history. |
| `--templates` | `on` \| `off` | `off` | FR-014; `off` keeps the core arm independent of the template arm |
| `--report` | path | *(none)* | Writes the JSON record, conforming to [`report.schema.json`](./report.schema.json) |
| `--format` | `text` \| `github` \| `markdown` \| `json` | `text` | What goes to stdout |
| `--scope` | path prefix \| `none` | `core-web` | Hard scope; a diff outside it is a no-op pass |

**Exit codes**

| Code | Meaning |
|---|---|
| `0` | No surviving diagnostic. Includes the empty-diff no-op. |
| `1` | At least one surviving diagnostic — the gate failure the whole thing exists to produce. |
| `2` | The harness could not run: base ref unresolvable after fetch, project graph unreadable, configuration unparseable. **Never conflated with `1`** — a broken harness reporting "clean" is the one failure mode that would quietly defeat the gate. |

**Output formats**

| Format | For | Behavior |
|---|---|---|
| `text` | humans and **coding agents reading raw CI logs** | States why the gate failed, the scope rule, each violation with a concrete fix hint, and the local repro command |
| `github` | the pull request diff | `::error file=,line=,col=::` annotations, rendered inline on the changed lines; also appends a Markdown job summary when `GITHUB_STEP_SUMMARY` is set |
| `markdown` | job summary / comment | Table of violations with the scope rule stated first |
| `json` | machines | The full report |

The `text` and `github` outputs deliberately lead with the **scope rule** — that only changed
lines (or changed files) are checked and that dependency diagnostics were ignored on purpose. An
agent that does not know this will "fix" an entire legacy file and produce a diff nobody asked
for. Telling it what NOT to touch is as load-bearing as telling it what broke.

**Guarantees**

- Writes nothing into the working tree. Version-controlled files are byte-identical afterwards
  (SC-010), including if the process is interrupted.
- Every child process is invoked with an argument array, never a shell string. Refs, branch
  names and file paths come from pull-request metadata and are untrusted input; a
  shell-interpolated branch name would be a command-injection vector in a tool destined for CI.
  This is a review checkpoint, not a style preference.
- Reports rather than assumes: the selected mode per project, unmapped files, and discarded
  counts all appear in the report even on a passing run.
- Scoped to `core-web/` by default. A pull request touching only backend or docs is a no-op pass
  that costs ~0.3s: it never reads the project graph and never starts a compiler. CI additionally
  gates the job itself on the same path filter, so the usual case is that it does not run at all.

---

## `replay.mjs` — run the corpus

```
node tools/scripts/strict-gate/replay.mjs --pr <number>[,<number>...] [run.mjs options]
```

Resolves each pull request's merge commit `M` via `gh`, then invokes `run.mjs` with
`--base M^1 --head M` (D-005). Emits one report per pull request plus a summary table carrying
the detection result, the false-positive count, the discarded counts and the timings — the raw
material for the write-up.

| Option | Values | Default | Notes |
|---|---|---|---|
| `--pr` | comma-separated numbers | *required* | |
| `--matrix` | flag | off | Runs every combination of `--flags` and `--granularity` over the corpus, which is what FR-007 and FR-008 need in order to be compared on identical input |
| `--out` | directory | `./strict-gate-out` | Reports written outside the repository tree by default |

**Exit codes**: `0` when every case matched its pre-registered expectation, `1` on any mismatch,
`2` on a harness error. A mismatch is information, not a defect — the run still writes every
report so the adjudication required by SC-003 can proceed.
