# Feature Specification: Wire the diff-scoped strict typecheck gate into CI

**Feature Branch**: `nicobytes/37536-wire-the-diff-scoped-strict-typecheck-gate-into-ci-merged-on-main-but-never-executes`

**Created**: 2026-09-14

**Status**: Draft

**Type**: New Feature (activation of previously merged, never-executed capability)

**Issue**: [#37536](https://github.com/dotCMS/core/issues/37536)

**Input**: User description: "https://github.com/dotCMS/core/issues/37536 — the strict typecheck gate added in PR https://github.com/dotCMS/core/pull/37403 is merged on main but never executes in CI"

---

## Context

Spike [#37401](https://github.com/dotCMS/core/issues/37401) / PR [#37403](https://github.com/dotCMS/core/pull/37403) delivered a working diff-scoped strict typecheck harness and merged it to `main` on 2026-09-08. The harness was wired into nothing — no build hook, no local hook, no workflow. Since then every pull request has passed without it, and `main` has kept accumulating non-strict TypeScript exactly as before.

Re-verified on this worktree at `de342798f4`:

| Check | Result |
|---|---|
| References to the gate outside its own directory and spec directory | none |
| Build executions in the frontend module | `validate-dist-paths`, `pnpm-install`, `lint-test`, `format-test`, `build-test`, `build-analytics`, `unit-test`, `nx-reset`, `prod`, `do-nx-reset`, `validate`, `format`, `auto-format`, `auto-lint` — the gate is absent |
| Local pre-commit configuration | lint and format only — the gate is absent |
| Workflows mentioning the gate | none |

The capability is therefore dormant, not missing. This feature turns it on **as a blocking gate**: a change that adds a strict-mode violation on a line it wrote does not merge.

---

## Clarifications

Both were raised by inspecting the merged harness, not by reading the issue. Neither had a safe default, and both change what gets built.

### C-001 — The local check refuses the push; the pull request only reports *(SUPERSEDED TWICE — the local check was cut (US4), and the pull-request check now blocks too (FR-010). Kept as a record of how the decision moved.)*

**Asked because**: the issue specifies reporting-only for continuous integration and is silent on the local hook. Left to its defaults, a local hook aborts on findings — making local enforcement stricter than the pull request without anyone deciding so.

**Decided**: deliberately stricter locally. The pull request never blocks; the local push check does. Blocking at push is cheap to reverse (a developer-machine setting, not continuous-integration configuration), it is the only enforcement in this release, and it stops the debt before a reviewer ever sees it. The standard bypass remains available and documented, so a developer who genuinely needs to push failing work still can.

**Affects**: US4 (cut — see the story for what was kept).

### C-002 — The local check runs at push time, not commit time *(SUPERSEDED — the local check was cut; the finding behind it is still load-bearing, see US4)*

**Asked because**: the issue's acceptance list places the hook in the commit-time staged-files configuration. The harness as merged compares two committed points; it has no notion of staged-but-uncommitted content and rejects unrecognised options. A commit-time hook reusing that comparison would examine the *previous* commit and report "clean" on the very violation being committed — the exact case the hook exists to catch. Closing that would mean adding capability to a harness the spike declared finished.

**Decided**: run at push time instead. Pushing compares committed branch content against the trunk — precisely the comparison the harness already performs and the same one the pull request will make, so local and remote answers agree by construction. It costs one run per push rather than one per commit, and requires no new harness capability.

**Consequence — a deliberate deviation from the issue's acceptance list**: the commit-time staged-files configuration is *not* modified. The issue's related concern about non-overlapping glob keys racing on git's index lock disappears with it, since the check no longer shares that execution path. This deviation must be stated in the pull request description (FR-020).

**Affects**: US4 (cut), FR-017, Edge Cases.

### Session 2026-09-14

- Q: The gate runs inside a required check. What happens when the harness cannot run at all (unresolvable comparison point, unreadable project graph, unparseable configuration)? → A: Fail the check, always. A gate that reports "clean" without having looked is the one failure mode that defeats the whole feature; the harness already retries the shallow-checkout case itself, so the residual transient surface is small. Downgrading later is trivial; upgrading later means re-litigating the decision.
- Q: The frontend test suite runs on pull requests, in the merge queue, on trunk pushes and nightly. In which of those should the gate execute? → A: Pull requests only. Inline annotations can only render on a pull request, so anywhere else the gate is cost without value — and a merge-queue run would add 9–12 s plus the FR-011 merge-blocking failure mode to the most contention-sensitive part of the pipeline. Trunk and nightly would be near-free no-ops (the comparison yields an empty diff), but are excluded for the same reason: nothing reads the result.
- Q: *(superseded — the local check was cut)* How is the blocking pre-push check rolled out to developers? → A: On by default, individually disableable. It installs with the repository's hooks and blocks from day one, but a documented, permanent per-machine opt-out exists alongside the per-push bypass. Real enforcement without becoming something people tear out by hand — anyone who opts out still has the pull-request check as the backstop, and the spec already treats the local check as an accelerator rather than the primary mechanism.
- Q: Should the gate carry its own time limit in CI, and what happens when it is reached? → A: Yes, and exhausting it fails the check. An explicit cap, generous against the measured 9–12 s (on the order of a couple of minutes). Exhausting it is indistinguishable from "the harness could not run", so it follows the same rule as FR-011: fail, visibly. This bounds the worst case of a deep re-fetch without inventing error classification, and gives SC-005 a concrete number to compare the observed tail against.
- Q: How is each run's observed duration captured so SC-005 can be satisfied? → A: One duration line in the job summary the gate already writes. The harness already measures the run; it simply never prints it. This puts the number where a human already looks, with no artifact and no new step. It requires relaxing the "no harness changes" rule to permit OUTPUT-only changes — stated explicitly rather than left to slip through.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A frontend author sees strict violations on the lines they wrote (Priority: P1)

A developer opens a pull request that changes TypeScript under the frontend workspace. The pull request's checks run as usual. Where their changed lines would fail under the project's strict convention, the pull request shows those violations directly on the affected lines of the diff, and the run page carries a summary listing them. The pull request **does not merge** until those lines are fixed: the same check runs again in the merge queue, where a failure ejects it. The author sees the debt at the moment they add it, and cannot ship it.

**Why this priority**: This is the entire point of the feature. Without it the gate remains dead code and the strict-debt leak stays open. Every other story is a guard rail around this one.

**Independent Test**: Open a pull request that introduces a known strict violation on a changed line of frontend TypeScript; confirm the violation is reported on that line, appears in the run summary, and that the check fails.

**Acceptance Scenarios**:

1. **Given** a pull request whose diff adds a line of frontend TypeScript that violates the strict convention, **When** the frontend checks run, **Then** the violation is reported against that exact file and line on the pull request diff, and a summary of all violations appears on the run page.
2. **Given** a pull request whose frontend TypeScript changes are all strict-clean, **When** the frontend checks run, **Then** no violations are reported and no summary noise is produced.
3. **Given** a pull request that reports violations, **When** it is added to the merge queue, **Then** the queue run fails and the pull request is ejected — it does not merge until the violations are fixed.
4. **Given** a pull request that modifies an existing file whose surrounding, untouched lines already violate the strict convention, **When** the gate runs, **Then** only the lines this pull request changed are reported; the pre-existing violations on untouched lines are not attributed to the author.
5. **Given** a stale branch that has fallen many commits behind the trunk, **When** the gate runs, **Then** the reported violations are limited to what the branch itself changed and do not include changes the trunk made in the meantime.

---

### User Story 2 - A backend-only pull request is untouched (Priority: P1)

A developer opens a pull request that changes only backend, documentation, or build files. Nothing about the gate is visible to them: no added wait, no annotations, no summary, no new failure mode.

**Why this priority**: The repository is predominantly backend. If activation taxes every unrelated pull request, the change is a net regression regardless of how well it works on frontend pull requests. This must be verified, not assumed.

**Independent Test**: Open a pull request touching only backend files; confirm no gate output appears anywhere and no measurable time is added to the pull request's checks.

**Acceptance Scenarios**:

1. **Given** a pull request that changes no frontend files, **When** its checks run, **Then** the gate produces no annotations and no summary.
2. **Given** a pull request that changes no frontend files, **When** its checks run, **Then** no additional wall-clock time attributable to the gate is added.

---

### User Story 3 - A broken gate fails loudly instead of silently passing (Priority: P1)

The harness cannot run — the comparison point is unresolvable, the project graph is unreadable, or configuration cannot be parsed. Rather than reporting "clean" and letting the pull request through, the checks fail and say so.

**Why this priority**: A gate that reports nothing is indistinguishable from a clean pull request. This is the one failure mode that would quietly defeat the whole feature, and it stays dangerous even with blocking on: "the harness could not run" and "nothing was wrong" must never collapse into the same outcome.

**Independent Test**: Force a harness failure (for example, an unresolvable comparison point) and confirm the checks fail with a message identifying the harness as the cause — not a silent pass.

**Acceptance Scenarios**:

1. **Given** the harness cannot resolve its comparison point, **When** the checks run, **Then** the run fails and the reason is stated in the log.
2. **Given** the harness completes and finds violations, **When** the checks run, **Then** the run passes.
3. **Given** the harness completes and finds nothing, **When** the checks run, **Then** the run passes.
4. **Given** a run exceeds the gate's time limit, **When** the limit is reached, **Then** the gate is aborted and the check fails — an unfinished check is treated exactly like one that could not start.
5. **Given** a change moving through the merge queue, **When** the frontend validation runs there, **Then** the gate does not execute at all and therefore cannot fail the queue.

> The distinction between "found problems" and "could not look" must be explicit in the wiring, not an accident of which exit values happen to be tolerated. "Ran out of time" belongs on the "could not look" side.

---

### User Story 4 - A developer is stopped before pushing new strict debt — **CUT, deferred**

**Status: removed from this feature on 2026-09-14.** This change is continuous-integration
verification only; it adds no local git hook.

The story was specified, approved and built — a `pre-push` hook that ran the same
branch-versus-trunk comparison and refused the push on findings — and then cut before merge on
the decision that the feature should deliver CI verification alone.

Kept here rather than deleted because the work produced two findings a future local hook will hit
again, both recorded in Clarifications C-001 and C-002:

- **The harness compares two committed points.** A commit-time hook reusing that comparison
  examines the *previous* commit and reports "clean" on the very violation being committed. This
  was verified, not theorised — the harness reported `checked 0 project config(s)` against an
  uncommitted change. Any local hook must therefore sit at push time, or the harness must grow a
  notion of staged content.
- **Blocking locally while the pull request only reports** is a coherent position (C-001), but it
  is a separate decision from wiring the gate into CI and should be taken on its own.

Consequence for this release: **enforcement lives in CI, not on the developer's machine.** The
merge-queue run is what stops a violation from merging (see FR-006). A developer who wants the
answer before pushing runs the harness themselves; nothing does it for them automatically.

---

### User Story 5 - The team has measured evidence for the blocking decision (Priority: P2)

Once the gate is live, its real cost on real pull requests is recorded so the later decision to make it blocking rests on observed data rather than on the spike's synthetic corpus.

**Why this priority**: The gate now blocks, so its cost sits on the critical path of every frontend merge. The spike's numbers (8.4–9.4 s average, 12 s tail) were measured on a synthetic corpus; what matters now is the real distribution, because a gate that is both blocking and slow is the one that gets switched off.

**Independent Test**: After the gate is live, confirm that observed durations from at least five distinct real pull requests are recorded on the issue.

**Acceptance Scenarios**:

1. **Given** the gate has run on at least five real pull requests, **When** the evidence is gathered, **Then** each run's observed duration is recorded on the issue alongside the size of the diff it examined.
2. **Given** the recorded durations, **When** the blocking decision is revisited, **Then** both the typical and worst observed durations are available.

---

### Edge Cases

- **Annotations must actually reach the pull request.** Reported violations travel through the build tool's log. If that log decorates each line, the pull request may show nothing on the diff even though the gate ran correctly and the summary is fine. This must be confirmed on a real pull request before the work is considered done; a green run is not evidence that annotations rendered.
- **Shallow checkouts.** Test runners check out with minimal history. The harness deepens the checkout itself when the comparison point is missing, but a branch that has diverged far from the trunk can force a progressively deeper fetch — on a repository this size, far more expensive than the gate itself. This is the pathological case the time limit exists to catch: the run is aborted and the check fails, rather than the job being held hostage.
- **Renamed and deleted files.** A pull request that only deletes or renames frontend TypeScript has changed lines that no longer exist in a compilable form; this must be a clean pass, not an error.
- **A pull request touching frontend files that belong to no project.** The workspace has projects with nothing that compiles them; files outside any mapped project must be reported as unexamined rather than silently treated as clean.
- **Very large frontend pull requests.** A sweeping refactor touching hundreds of frontend files must not exceed the check's time budget or produce a summary so large it is unusable.
- **A long-lived branch's first run** is judged on the branch's combined effect against the trunk, not on its newest commit — so it can surface violations from work done days earlier. This is the same question the pull request asks and is intended, but it is the case most likely to surprise an author.

---

## Requirements *(mandatory)*

### Functional Requirements

**Activation**

- **FR-001**: The frontend build MUST invoke the diff-scoped strict typecheck as part of the same validation stage that already runs frontend lint and format checks, so that it executes on every pull request that runs frontend validation and on no other build.
- **FR-002**: The gate MUST compare the pull request's branch against the trunk and report only violations on lines the pull request added or modified.
- **FR-003**: The gate MUST be evaluated against the repository's established strict convention — the same standard the workspace-wide strict migration is moving toward — so that what it reports today is exactly what will be required tomorrow.
- **FR-004**: The gate MUST examine only frontend files. A change outside the frontend workspace MUST be a no-op pass.
- **FR-005**: No continuous-integration workflow definition changes are required; if the plan concludes otherwise, the reason MUST be stated explicitly rather than the change being made silently.
- **FR-006**: The gate MUST execute on pull requests **and in the merge queue**, and nowhere else. Trunk and scheduled runs MUST skip it: the diff there is empty and the run could only ever pass. Both contexts are required and they do different jobs — the pull-request run is where annotations render and the author reads them; the merge-queue run is what actually prevents a merge, because this repository declares no required status checks on `main`. This MUST be achieved without modifying workflow definitions (FR-005).

**Reporting semantics**

- **FR-007**: Violations MUST appear inline on the pull request diff, on the exact file and line reported.
- **FR-008**: Violations MUST additionally appear as a summary on the run page, readable without opening the raw log.
- **FR-009**: Reported output MUST state its own scope rule — that only changed lines were examined and that violations in dependencies were discarded deliberately — before listing violations, so that a reader (human or automated) does not respond by refactoring untouched code.
- **FR-010**: Findings MUST fail the check. A violation on a line the pull request wrote MUST prevent the change from merging, and the failure MUST name the file, the line and the rule so the author can act without opening the raw log.
- **FR-011**: A harness that could not complete MUST fail the check — **every time, with no exception for transient causes.** This is a separate requirement from FR-010 and must stay separate in the configuration: findings and "could not look" are different outcomes that happen to share a consequence today. A gate reporting "clean" without having looked is indistinguishable from a clean pull request, which defeats the feature entirely.
- **FR-012**: The wiring MUST NOT introduce retry, fallback, or failure-classification logic of its own. The harness's own recovery behavior is the only recovery; anything it surfaces as "could not run" reaches the build unaltered.
- **FR-013**: The gate MUST be bounded by an explicit time limit, set generously above the measured typical run so that it is reached only by a pathological case, never by a slow-but-healthy one. Reaching it MUST abort the gate and fail the check under the same rule as FR-011 — an unfinished check is not a passing one. The limit MUST be stated as a number the team can revisit against the evidence gathered under SC-005.

**Integrity of the existing harness**

- **FR-014**: The harness's existing test suite MUST still pass in full after this change.
- **FR-015**: The requirement that the harness's tests run serially MUST be documented wherever those tests are invoked, so that a parallel run's intermittent timeout is never mistaken for a real failure.
- **FR-016**: A full run of the gate MUST leave every version-controlled file byte-identical.
- **FR-017**: This feature MUST NOT change what the harness *decides*. It wires up what PR #37403 delivered; if the plan concludes a behavioral change — what is examined, what is reported as a violation, how the comparison is made — is unavoidable, that MUST be raised as a scope change rather than absorbed. **Changes to what the harness *prints* are permitted** and are the sole exception (see FR-018).
- **FR-018**: Every run MUST report its own elapsed time in the same summary it already writes, so the evidence FR-022 requires can be read off a run page rather than reconstructed from logs. This is an output-only change and is the only harness edit this feature allows.

**Documentation and issue hygiene**

- **FR-019**: The harness's own documentation MUST no longer state that the gate is wired into nothing; it MUST state where it is invoked from, that the pull-request check only reports, and that the local check refuses pushes.
- **FR-020**: The pull request description MUST state what is deliberately excluded: making the pull-request check blocking, checking templates, and the untried performance optimisations. It MUST also state that the local hook runs at push time rather than commit time, and why — see Clarifications, C-002.
- **FR-021**: The superseded predecessor issue MUST be confirmed closed and cross-referenced. *(Verified already closed as of 2026-09-14; confirm the cross-reference exists.)*

**Evidence**

- **FR-022**: Observed durations from at least five real pull requests MUST be recorded on the issue, each paired with the size of the diff examined, so the later blocking decision has a measured distribution rather than a single corpus figure.

### Key Entities

- **Diff scope**: the set of lines a pull request added or modified within the frontend workspace. Everything reported must fall inside it; everything outside it is discarded by design.
- **Finding**: one strict violation, identified by file, line and the rule it breaks.
- **Run outcome**: three distinct states that must never collapse into two — clean, violations found, harness could not run.
- **Observed duration**: the wall-clock cost of one gate run, paired with the diff size that produced it; the raw material for the blocking decision.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Within one week of activation, at least one real pull request has displayed a strict violation inline on its diff — demonstrating the mechanism works end to end on production traffic, not just in a test.
- **SC-002**: 100% of pull requests that change no frontend files complete with zero gate output and no measurable added time.
- **SC-003**: No change carrying a new strict-mode violation on a line it wrote reaches `main` after this ships. Measured by the absence of such violations in `main`'s history from the merge date onward.
- **SC-004**: 100% of runs in which the harness cannot complete result in a failed check, with the cause identifiable from the log alone.
- **SC-005**: Across at least five observed real pull requests, both the typical and the worst duration are recorded, and the worst is stated against the 10-second budget the spike set.
- **SC-006**: Every violation reported on a real pull request during the first week is adjudicated as genuine; any false positive is recorded and triaged before the blocking decision is revisited.
- **SC-007**: Zero merge-queue, trunk or scheduled runs execute the gate — verified by its absence from those runs' output, not assumed from configuration.
- **SC-008**: No gate run exceeds its stated time limit during the observation period; if one does, it is recorded as the pathological case the limit exists to catch, with its cause identified.
- **SC-009**: A full gate run leaves the working tree clean — zero modified version-controlled files, verified after the run.
- **SC-010**: The harness's documentation and the pull request description together let a reader unfamiliar with the spike answer, without opening the code: where the gate runs, what it checks, what it ignores, and what it will never do until a separate decision is taken.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The frontend validation stage of the build — the same stage that already carries lint and format. Nothing else: no local git hook, no workflow file. This is developer-facing build tooling, not product surface: no runtime behavior, no content, no API, no database. It sits adjacent to continuous-integration configuration, an area where a mistake is felt by every contributor at once rather than by one feature's users.
- **Backward-compatibility expectations**: Every existing check must behave exactly as it does today. The frontend validation job is a required check. The gate must never turn it red on *findings*; it deliberately can and will turn it red when the harness itself could not run or ran past its time limit, which is a real merge block and an accepted trade. It must not slow the job enough to matter. Contributors who change no frontend code must see no difference whatsoever, and no contributor's local workflow changes at all — `core-web/.husky/` is untouched, so what a commit or a push produces is exactly what it produces today.
- **Known related decisions**: The frontend merge-time reduction this repository already paid for is the reason the gate's runtime budget exists at all, and is why the blocking flip is deferred rather than taken now. The workspace-wide strict migration ([#37198](https://github.com/dotCMS/core/issues/37198)) is the eventual replacement for this gate — but only if it also gains a mechanism that actually type-checks the projects with no build target and the test files the build excludes, which it does not today. The harness carries its own decommission procedure describing what to remove once that condition is met. The plan phase will formally consult `dotCMS/platform-adrs`.

---

## Assumptions

- The harness merged in PR #37403 is correct as delivered. This feature activates it; it does not re-validate the spike's findings or redesign the check.
- The existing path filter that already restricts frontend jobs to frontend changes is sufficient to keep the gate off unrelated pull requests, and needs no modification.
- Test runners already obtain the trunk reference they need for the existing frontend checks; the gate reuses the same reference rather than introducing a new fetch.
- The evidence-gathering requirement (FR-022, SC-005) is satisfied after the change merges, by observing live pull requests. It is a post-merge obligation recorded on the issue, not a code deliverable that could gate the pull request itself.
- Making the pull-request check blocking, checking Angular templates, and the three untried performance optimisations are all deliberately excluded. Each is its own decision with its own evidence; none is a follow-on task implied by this one.
- Developers have the repository's local git hooks installed. Those who do not — and those who use the documented opt-out — simply lose Story 4; the pull-request check (Stories 1–3) is unaffected and remains the backstop. Opt-out rate is worth watching: if most of the team disables it, the local check is not earning its cost and should be reconsidered rather than tolerated.
- Excluding merge-queue, trunk and scheduled runs costs nothing in coverage: every change reaches the trunk through a pull request, where the gate does run.
- "Strict convention" means the standard already established for this repository and targeted by the workspace-wide migration — not the broader, stricter set the spike also measured and explicitly did not propose.
- Adjudicating reported violations (SC-006) is a human judgement made by the frontend team, not an automated check.

---

## Out of Scope

- **An escape hatch for a legitimate exception.** There is none. A frontend change that must add
  a strict violation — porting legacy code, an unavoidable third-party shape — cannot merge until
  the violation is fixed or the gate is turned off repository-wide. No label, no opt-out, no
  per-file suppression. This is a real gap, accepted knowingly: the spike measured 0 false
  positives across its corpus, so the expected frequency is low, and inventing a bypass before
  anyone needs one tends to produce the bypass everybody uses. Revisit if it bites.
- Checking Angular templates.
- Performance work on the harness.
- Changing what the harness decides — what it examines, what counts as a violation, how the comparison is made — including any notion of staged-but-uncommitted content (C-002). Adding the elapsed-time line to its output (FR-018) is the single permitted exception.
- Running the gate on trunk or scheduled builds: the diff there is empty, so it could only ever pass.
- Any local git hook — commit-time or push-time. `core-web/lint-staged.config.mjs` and
  `core-web/.husky/` are untouched. A push-time hook was built and then cut on 2026-09-14 when the
  feature was narrowed to continuous-integration verification only; what the work established is
  preserved in US4 and in C-001/C-002.
- Changes to workflow definitions, unless the plan phase demonstrates they are unavoidable and says why.
- Any change to the workspace-wide strict migration (#37198) or to the projects' own type configuration.
- Removing or relocating the harness. It remains where PR #37403 put it.
