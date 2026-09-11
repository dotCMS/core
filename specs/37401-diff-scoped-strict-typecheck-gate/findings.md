# Findings: diff-scoped strict typecheck gate

**Issue**: dotCMS/core#37401 · **Spec**: [spec.md](./spec.md) · **Date**: 2026-09-07
**Status**: complete — all four user stories reported. US1/US2 (mechanism, corpus, adjudication),
US3 (decision matrix, §6), US4 (template arm, §7). SC-005's runtime budget is the one criterion
not met, accepted as a deviation and deferred to #37448 (§13).

Every number below was produced by `core-web/tools/scripts/strict-gate/`, replaying real merged
pull requests. Nothing here is estimated.

---

## 1. The research question is answered: yes

A diff-scoped strict typecheck **does** block new non-strict TypeScript without the dependency
libraries being strict first. The mechanism works, and the margin is not close.

`libs/portlets/dot-locales/portlet`, checked under the repo's strict convention:

| Origin of diagnostic | Count |
|---|---|
| `libs/ui/src` | 111 |
| `libs/dotcms-js/src` | 38 |
| `libs/data-access/src` | 36 |
| `libs/utils/src` | 32 |
| **the portlet itself** | **2** |
| **Total** | **219** |

**217 of 219 discarded — 99.1%.** The dependency's errors do not need to be fixed; they need to
stop counting. That is the whole hypothesis, and it holds.

---

## 2. The corpus

Pre-registration rule, fixed before any case ran: a pull request is `clean` if **every** project
it touches already declares the convention the gate enforces. Structural, derivable from
tsconfigs and the diff, never from a gate result.

Run at `--flags strict --granularity line`:

| PR | Pre-registered | Findings | Discarded | Wall clock | Outcome |
|---|---|---|---|---|---|
| #37264 | debt | 3 | 3 | 6.3s | as predicted |
| #37415 | debt | 2 | 2 736 | 11.4s | as predicted |
| #37372 | debt | 3 | 2 064 | 12.0s | as predicted |
| #37405 | clean | 1 | 991 | 6.9s | **prediction wrong** |
| #37339 | clean | 2 | 990 | 7.1s | **prediction wrong** |

---

## 3. Adjudication — all 11 findings (SC-003)

| PR | Code | Location | Verdict |
|---|---|---|---|
| #37264 | TS4111 | `sdk/create-app/src/index.ts:294` | **real** — confirmed with `tsc` before the harness existed |
| #37264 | TS2345 | `create-app/src/utils/readiness.spec.ts:263` | **real** |
| #37264 | TS2345 | `create-app/src/utils/readiness.spec.ts:271` | **real** |
| #37415 | TS2531 | `dot-relationship-field.component.ts:394` | **real** — `strictNullChecks` |
| #37415 | TS18047 | `dot-relationship-field.component.spec.ts:332` | **real** — `strictNullChecks` |
| #37372 | TS18047 | `dot-content-drive-shell.component.spec.ts:1293` | **real** |
| #37372 | TS2769 | `dot-content-drive-shell.component.spec.ts:2894` | **real** — cascade, see below |
| #37372 | TS2345 | `dot-content-drive-shell.component.spec.ts:2894` | **real** — same defect as the row above |
| #37405 | TS2571 | `dot-auth-config.component.ts:144` | **real** — `useUnknownInCatchVariables` |
| #37339 | TS7006 | `dot-auth-oidc-connection.component.spec.ts:44` | **real** — `noImplicitAny` |
| #37339 | TS7006 | `dot-auth-oidc-connection.component.spec.ts:59` | **real** — `noImplicitAny` |

**11 findings, 11 real, 0 false positives.** Every one sits on a line its pull request wrote.

Two observations that qualify the count:

- **One cascade.** The TS2769 and TS2345 at `:2894` are one defect reported twice — TypeScript
  emits the overload failure and the specific argument mismatch separately. It inflates the count
  without being wrong. A production gate should collapse diagnostics that share a file and line.
- **One false positive was found and eliminated during adjudication**: TS2307
  *Cannot find module `@openng/spectator/jest`* on #37339. It appears under plain `tsc` with no
  flags forced — a module-resolution failure, never a strictness violation. The harness now
  discards `TS2307`, `TS2688` and `TS6053` as infrastructure, counted but never reported. Without
  that exclusion the false-positive rate would have been 1 in 12.

---

## 4. The pre-registration rule was wrong, and why that matters

Both "clean" predictions failed. `libs/portlets/dot-auth` declares the full convention — `strict`,
all four extras, and `strictTemplates` — and still contains real type errors on lines a merged
pull request wrote.

**Declaring `strict: true` does not mean anything compiles it.** The `typecheck` target exists on
**3 of 57 projects**. Everything else is covered by lint only, and lint does not type-check. A
project can carry the strictest configuration in the workspace and accumulate type errors
indefinitely with nothing to notice.

This enlarges the gate's value beyond the issue's framing. It is not only a ratchet against new
debt in non-strict libraries — **it is the first thing in CI that type-checks 54 of 57 projects
at all.**

It also means SC-002 cannot be measured as written. "At least 3 merged pull requests that
introduced no strict debt" presumes such pull requests are identifiable in advance; in this
workspace they are close to nonexistent. Of 42 recent frontend pull requests:

| Projects touched | PRs |
|---|---|
| all declare the full convention | 2 |
| all `strict: true`, none of the four extras | 1 |
| **at least one not strict at all** | **39 (93 %)** |

Waiting for per-project opt-in covers 7 % of pull requests. The diff filter covers the rest.
That is the strongest argument for this approach that the spike produced, and it was not
anticipated in the issue.

**The honest false-positive measure is therefore per finding, not per case: 0 of 11.**

---

## 5. Runtime — SC-005 is not met

| PR | Wall clock | Within 10s budget |
|---|---|---|
| #37264 | 6.3s | yes |
| #37339 | 7.1s | yes |
| #37405 | 6.9s | yes |
| #37415 | 11.4s | **no** |
| #37372 | 12.0s | **no** |

Two of five exceed the budget SC-005 set to protect what ADR-0013 bought (frontend merge time
cut from ~45 min to ~15 min). Both overruns are pull requests touching `libs/ui` or
`libs/edit-content` — large programs whose dependency closure is recompiled in full.

The cost is visible in the discard counts: 2 736 and 2 064 diagnostics computed and thrown away.
The gate currently pays to typecheck every dependency source in order to ignore it. Obvious
optimisations exist and are untried: reusing one program across a project's configurations,
skipping projects that already declare everything the flag set forces, and caching the dependency
closure between targets. None was attempted — measuring came first.

---

## 6. The three decisions, settled with measurements (SC-007)

Full matrix: 3 flag sets x 2 granularities x 5 pull requests, compiled once per
(pull request, flag set) and filtered twice — granularity only affects the filter.

### Findings per combination

| PR | null-checks<br>file / line | strict (8+4)<br>file / line | strict-max<br>file / line |
|---|---|---|---|
| #37264 | 2 / 2 | 5 / 3 | 13 / 8 |
| #37415 | 22 / 2 | 22 / 2 | 39 / 2 |
| #37372 | 17 / 3 | 23 / 3 | 71 / 4 |
| #37405 | 10 / 1 | 10 / 1 | 22 / 9 |
| #37339 | 3 / 2 | 3 / 2 | 14 / 2 |
| **Total** | **54 / 10** | **63 / 11** | **159 / 25** |

Average wall clock: 8.4s (null-checks), 9.4s (strict), 9.0s (strict-max). **The flag set barely
affects cost** — the expense is building the program, not the rules applied to it.

### Decision 1 — Granularity: **line-level**. Not close.

| Flag set | whole-file | line-level | inherited from untouched lines |
|---|---|---|---|
| null-checks | 54 | 10 | **44 (81 %)** |
| strict | 63 | 11 | **52 (83 %)** |
| strict-max | 159 | 25 | **134 (84 %)** |

Under whole-file, **83 % of what the gate reports is debt the author did not write**. #37415 goes
from 2 findings to 22, #37372 from 3 to 23, #37405 from 1 to 10 — touching one line of a file
makes you inherit roughly ten times your own work. No team adopts that; it converts every small
fix into an unbounded cleanup.

Line-level costs nothing in coverage that matters: an added file has every line changed, so new
code is still held to the full bar. It only ever forgives pre-existing lines in modified files.

### Decision 2 — Flag set: **the repo convention (8 + 4)**.

At line granularity the whole corpus separates the candidates by **one finding**: 11 versus 10.
The four extra flags are, in practice, free.

| | line-level findings | vs narrow |
|---|---|---|
| null-checks (`strictNullChecks` + `noImplicitAny`) | 10 | — |
| **strict (repo convention)** | **11** | +1 |
| strict-max | 25 | +15 |

The narrow set is not meaningfully quieter, and it misses the TS4111 class outright — the very
violation the issue used as its reproducible case. Meanwhile `strict` matches `tsconfig.base.json`
on PR #37198 exactly, so the gate measures with the same yardstick as the destination. Choosing
the narrow set would buy one fewer finding across five pull requests at the cost of letting
through debt the migration must later fix by hand.

`strict-max` more than doubles findings at line granularity (25 vs 11) and adds 134 inherited
findings whole-file. It exceeds what any project in the workspace has ever met and what #37198
targets. **Measured and recorded for a future ratchet; not the blocking set.**

### Decision 3 — Runtime: **8.4–9.4s average, and SC-005 is not met at the tail**

Two of five corpus cases exceed the 10s budget (11.4s and 12.0s), both touching `libs/ui` or
`libs/edit-content`. Since the flag set barely moves the number, the cost is structural: the gate
compiles a project's entire dependency closure in order to discard it — 2 736 and 2 064 discarded
diagnostics on exactly those two runs.

**Recommended gate invocation:**

```
--flags strict --granularity line
```

## 7. The template arm (SC-011 / SC-012 / SC-013)

Case: **PR #37248**, one template file in `apps/dotcms-ui` — one of the four applications carrying
`TODO(#35930): re-enable strictTemplates once Angular 22 template errors are fixed per app`.

| | TypeScript-only | Template-aware |
|---|---|---|
| Mode selected | `tsconfig.app.json` [typescript] | `tsconfig.app.json` [template-aware] |
| Source diagnostics discarded | 2 631 | 2 637 |
| **Template diagnostics discarded** | **0** | **547** |
| Findings reported | 0 | 0 |
| Compiler time | 7.4s | **16.3s** |
| Total wall clock | 12.7s | **21.2s** |

**SC-011 — met.** Template strictness is forced on an application whose configuration sets
`strictTemplates: false`, with no version-controlled file edited. The Angular settings are supplied
through `readConfiguration(project, existingOptions)`, whose `existingOptions` outrank everything
in the extends chain — the same in-memory approach as the TypeScript arm, for the same reason:
an overlay file would survive a crash and break SC-010.

**SC-012 — met, and the number is the point.** 547 template diagnostics were discarded and **zero**
reported. Those 547 are the `TODO(#35930)` backlog. A gate that reported them would be unusable
on day one; a gate that counted none would mean the filter did nothing. Discarding 547 to report 0
is exactly the behaviour that lets a diff-scoped gate coexist with an application-wide opt-out.

**SC-013 — the cost, and the recommendation: NO-GO for day-one blocking.**

Template-aware checking costs **2.2× the compiler time** (16.3s vs 7.4s) and **1.7× wall clock**
(21.2s vs 12.7s) on the largest application. The TypeScript arm already breaches SC-005's 10s
budget at the tail; the template arm puts the worst case at over 20s. Measured against what
ADR-0013 bought — frontend merge time cut from ~45 min to ~15 min — that is not a cost to add
before the optimisations in §5 are done.

The recommendation is **not** that templates are unsuitable. The mechanism works, the filter works,
and the four applications with no template gate at all are where the most user-facing code lives.
It is that the template arm should ship **after** the TypeScript arm, once the dependency-closure
cost is addressed — or immediately as a **non-blocking, advisory** run, which costs a reader
nothing and starts producing the data.

### Three silent-failure bugs the spike exposed

All three were invisible in the same way: the run reported a plausible result and a plausible exit
code. None would have been found by checking that the harness ran without error — only by checking
*which configuration it chose* and *what range it compared*.

1. **Entry-point configs are invisible to file-list matching.** `apps/dotcms-ui/tsconfig.app.json`
   declares `"files": ["src/main.ts", "src/polyfills.ts"]`. Its resolved file list is two entries —
   every component arrives through the import graph. Selecting "the configuration whose resolved
   list contains this file" therefore never picks it, and the app's sources and templates fell
   through to `tsconfig.editor.json`, an IDE-only config Nx generates that carries **no**
   `angularCompilerOptions`. Files were still checked, which is precisely why it hid.

2. **Colocation makes the spec config look like the build config.** Angular puts
   `x.component.ts`, `x.component.html` and `x.component.spec.ts` in one directory, so a rule of
   "the configuration that owns TypeScript in this directory" matches `tsconfig.spec.json` as
   readily as the build config — and the spec config has no Angular settings either.

3. **A tree diff instead of a merge-base diff.** The harness compared `base..head` — two trees —
   where a pull request means `base...head`, everything since the two diverged. The issue's own
   acceptance criteria specify three dots; I used two. Invisible while a branch is fresh, wrong once
   it is stale: every file the BASE modified is reported as changed, and the author is blamed for
   violations someone else merged. Found by running the documented quickstart command against this
   very branch, which reported **50 findings**, essentially none of them its own. After the fix:
   PASS, 0 targets — correct, since this branch adds only `.mjs` files. The report now cites the
   merge base rather than the base tip, which is also what makes a re-run months later reproduce
   the same numbers.

Selection now ranks candidates (`app` > `lib` > `json` > `spec` > `editor`) instead of taking the
first match, and an IDE-only configuration is ignored outright whenever a real build configuration
exists. Both rules are pinned by tests that reproduce the `apps/dotcms-ui` shape specifically — a
lib-shaped fixture passed by accident, because alphabetical ordering happened to put the right
answer first.

---

## 8. Corrections to the issue's premises

| Issue says | Verified |
|---|---|
| "PR #37262" | #37262 is an **issue**. The pull request is **#37264**, merge commit `788795e915` |
| "3 strict errors" | **5** under the repo convention (2 under bare `--strict`) |
| "`src/index.ts` (TS4111)" ×1 | **two** TS4111, at lines 294 and 515 |
| — | plus a TS7030 in `src/utils/index.ts:41` the issue did not list |
| TS4111 is a strict error | It is **not** — `noPropertyAccessFromIndexSignature` is outside `--strict` |

The last row is the load-bearing one: it is why the flag-set decision could be settled with
evidence before the harness existed.

---

## 9. Edge cases (SC-006)

All ten exercised. None crashed; none skipped silently.

| # | Edge case | Observed |
|---|---|---|
| 1 | Pull request with no TypeScript at all | exit 0, 0 targets, **132 ms** — no project graph read, no compiler started |
| 2 | Deleted and renamed files in the diff | 28 files resolved from a real merge, statuses `M`/`A`; deletions excluded, renames at their new path |
| 3 | Shared config touched (`tsconfig.base.json`, `nx.json`) | **1 target of 57 projects** — no fan-out, exactly as FR-010 requires |
| 4 | Project with no `tsconfig.lib.json` (apps, entry-point configs) | Resolved to `tsconfig.app.json` via ranked selection — and this is where two silent-failure bugs were found (§7) |
| 5 | Changed file matching no project | Reported as unmapped with a reason, never dropped |
| 6 | Shallow checkout / `merge_group` | Base ref fetched; an unresolvable base **throws** rather than reporting an empty diff |
| 7 | One file claimed by two configs | Both targets produced, diagnostics deduplicated by file/line/code — hit for real on `src/utils/index.ts` (TS7030) |
| 8 | Inline template | Diagnostic attributed to the component source, not to a nonexistent template path |
| 9 | Template-only pull request | Still resolves a project and checks it (§7); the config-selection work exists because of this case |
| 10 | Framework upgrade adding diagnostics | `extendedDiagnostics` deliberately excluded — promoting a whole category to errors lets a future minor fail pull requests for code they did not change (FR-018) |

Edge case 6 deserves emphasis: an unresolvable base ref is treated as a **harness failure (exit 2)**,
never as a clean run. A gate that reports "no changes" because it could not find its base would
pass every pull request in CI while looking perfectly healthy.

---

## 10. Recommendation — go / no-go on blocking merges (SC-008)

### **GO**, for the TypeScript arm, with one precondition.

| Criterion | Result |
|---|---|
| Detects real debt | 11 of 11 findings real, on lines their pull requests wrote |
| False positives | **0** of 11, after excluding three infrastructure diagnostic codes |
| Discards dependency noise | 217 of 219 on a representative portlet — 99.1 % |
| Needs no config change | Confirmed; every version-controlled file byte-identical after full corpus runs |
| Adds no dependency | Confirmed |
| **Runtime** | **8.4–9.4 s average, 12 s at the tail — SC-005's 10 s budget breached on 2 of 5 cases** |

Precision is not the problem — it is better than the spec asked for. **Runtime is the only thing
standing between this and a day-one blocking gate**, and it is a solved kind of problem: the cost
is entirely dependency-closure recompilation (2 736 and 2 064 diagnostics computed and discarded on
the two slow cases). Untried optimisations, in the order I would try them: reuse one program across
a project's configurations, skip projects whose configuration already declares everything the flag
set forces, and cache the dependency closure between targets.

**Recommended posture:**

1. **Ship non-blocking first.** Same invocation, reporting only. Costs no one a merge, and produces
   the data to set a realistic budget.
2. **Optimise the closure cost**, then flip to blocking. If the tail lands under ~10 s, blocking is
   justified against ADR-0013's cost model; if it does not, blocking is not worth what ADR-0013 bought.
3. **Configuration:** `--flags strict --granularity line --scope core-web`.
4. **Templates: no-go for now** (§7). Ship advisory alongside, or defer to the follow-up.

**Fallback if blocking proves untenable:** keep it non-blocking and surface findings as pull-request
annotations. Even advisory, it is the only thing type-checking 54 of 57 projects.

### The argument that changed during the spike

The issue framed this as a ratchet against new debt in non-strict libraries. It is that — but the
larger finding is that **`strict: true` does not mean anything compiles it**: the `typecheck` target
exists on 3 of 57 projects. Both pre-registered "clean" pull requests contained real type errors, in
a project declaring the strictest configuration in the workspace. The gate's value is bigger than
the issue assumed, and it does not depend on PR #37198 landing.

---

### Operational note on the test suite

93 tests, all passing — but only with `--test-concurrency=1`. `node --test` parallelises files by
default, and each of these builds real TypeScript or Angular programs; under that pressure one
acceptance case intermittently timed out and reported a failure that did not reproduce in
isolation. Serial run: 93/93 in ~120s. Recorded rather than papered over, since an intermittently
red suite is one people stop running.

---

## 11. Timebox (SC-009)

The issue set 4 hours; the template arm was expected to add ~2. **Both were exceeded**, and the
overrun is worth recording because of where it went — not into the mechanism, which worked early,
but into four things the plan did not anticipate:

1. **Configuration selection.** Selecting by resolved file list rather than filename convention
   (research D-003) turned out to be load-bearing twice over: it is why the anchor case's spec-file
   violations were found at all, and it is where both silent-failure bugs in §7 lived.
2. **Adjudicating every finding by hand** (SC-003). This is what turned an apparent
   false-positive rate of 1.0 into a measured 0 of 11, and what surfaced the TS2307 class.
3. **The pre-registration rule being refuted**, which produced the spike's most valuable finding
   and was not on anyone's list.
4. **Two rounds of test correction** — including one test that passed for the wrong reason and had
   to be rewritten against the real `apps/dotcms-ui` shape before it would fail.

None of that is waste; a 4-hour version would have reported the mechanism works and missed every
one of these. But the estimate was wrong and the write-up says so.

---

## 12. Follow-up

**This gate is scaffolding with an expiry date.** Whatever the follow-up builds is removed when
#37198 merges and the workspace baseline turns strict — the removal procedure, the full inventory,
and the precondition that #37198 adds no mechanism which actually runs a type-check are in
[DECOMMISSION.md](./DECOMMISSION.md).

**Recommendation: build it.** The follow-up task covers:

- The durable script, promoted from `core-web/tools/scripts/strict-gate/`.
- The closure-cost optimisations in §10, with a measured tail before any blocking flip.
- The CI hook in `core-web/pom.xml` under the `-Pvalidate` profile, beside the existing
  `lint-test` / `format-test` executions. `cicd_comp_test-phase.yml` already fetches `origin/main`,
  and `.github/filters.yaml`'s `frontend` filter already gates the job on `core-web/**` — **no
  workflow change is needed**.
- The local hook in `core-web/lint-staged.config.mjs`.
- **Non-blocking first**, blocking only once the tail is measured under budget.
- Templates advisory or deferred (§7).

Two items this spike deliberately did not touch:

- **`devEngines` for Node provisioning.** Verified working with pnpm 12.1.0, but it writes a
  runtime entry into `pnpm-lock.yaml` and would sit alongside `core-web/.nvmrc`, which three CI
  workflows read — `^22.0.0` resolves to 22.23.2 while `.nvmrc` pins 22.22.3, so the two drift on
  day one. Its own change, pinned exactly, retiring `.nvmrc` and updating those workflows.
- **The `strict-max` ratchet.** Measured (§6) and recorded; not proposed.

## 13. Accepted deviations and scope additions

`/speckit-converge` compared the built code against the approved spec. Nothing in the specified
scope was missing, but eight gaps surfaced. All are resolved below — six by conscious acceptance,
two by correcting the artifact.

### Deviations accepted, not fixed

**SC-005 — the 10s budget is not met (deferred to #37448).** Two of five corpus cases run at
11.4s and 12.0s. The three untried optimisations are named in §5 and §10 and are scope for the
follow-up task, which explicitly gates the blocking flip on a measured tail under budget. Fixing
it here would mean optimising before the decision to build the production gate has been taken.

**SC-002 — the criterion as written is unachievable in this workspace, and that is the finding.**
It asks for zero findings across at least three pull requests carrying no strict debt. Only two
structurally-clean pull requests exist across 42, and both report findings — every one adjudicated
real (§3), because declaring `strict: true` does not mean anything compiles the project (§4). The
precision guarantee that replaces it is measured **per finding, not per case: 0 false positives of
11**, and it is pinned by tests that assert no finding is an infrastructure diagnostic and that
every finding sits on a line its pull request wrote.

### Additions that outran the approved spec

Four behaviours were built that no functional requirement authorises. Each is defensible and each
is kept — but the spec was **approved on PR 1 before they existed**, so they are recorded here
rather than back-annotated into `spec.md`. Per the two-PR flow, spec changes after sign-off need
re-approval; silently editing an approved spec to match what was built inverts the point of the
gate.

| Addition | Why it exists | Beyond |
|---|---|---|
| **Infrastructure-code exclusion** (`TS2307`, `TS2688`, `TS6053` in `lib/filter.mjs`) | Adjudication found one such diagnostic reported on a clean pull request; it appears under plain `tsc` too. Without the exclusion the false-positive rate would have been 1 in 12 | FR-004 |
| **Output formatters** (`lib/format.mjs`) — four formats, per-code fix hints, GitHub annotations, job summary | Requested during implementation. Coding agents read CI output and act on it; the text and github formats lead with the scope rule so an agent does not refactor an entire legacy file | FR-006, which asks only for file, line and code |
| **`--scope core-web`** | Requested during implementation. Makes a backend-only pull request a 132 ms no-op. FR-010 governs project fan-out, a different concern | — |
| **Third flag set `strict-max`** | Measured so a future ratchet arrives with its cost already known rather than blind. Never the blocking set | FR-007, which specifies two |

### Artifacts corrected

**`data-model.md`** now lists `infrastructure` among the `Diagnostic.origin` values and
`strict` / `null-checks` / `strict-max` for `RunReport.flagSet`, matching
`contracts/report.schema.json` and `lib/filter.mjs`. It had drifted while the schema and the code
moved together.

**`quickstart.md`** now documents `--format` and `--scope`, which the recommended invocation uses
and the validation guide did not name.

---

## 14. Still open

Every user story in the spec is reported. What remains is work this spike deliberately did not do,
carried into the follow-up (§12) rather than left unanswered here.

- **SC-005 — the runtime budget.** 11.4s and 12.0s at the tail against a 10s budget. The three
  untried optimisations are named in §5 and §10; deferred to #37448, which gates the blocking flip
  on a measured tail. Accepted as a deviation in §13, not an open question.
- **SC-008 — the blocking flip, not the recommendation.** The recommendation is settled in §10:
  **GO for the TypeScript arm, non-blocking first.** Detection and precision support day-one
  blocking; runtime does not, so the flip waits on the line above.
- **Templates — advisory or deferred.** §7 measures the cost and recommends NO-GO for day-one
  blocking; which of the two postures ships is a call for the follow-up.
- **#37086** (`libs/sdk/angular`: `strict: true`, none of the extras) — the intermediate tier,
  deliberately excluded from the corpus so it could not contaminate the false-positive denominator.
