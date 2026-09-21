# Feature Specification: Upgrade Spec-Kit to the latest upstream release and re-establish our customizations

**Feature Branch**: `nicobytes/upgrade-spec-kit-from-v0.12.4-to-the-latest-rele`

**Created**: 2026-09-21

**Status**: Draft

**Type**: New Feature (developer-tooling upgrade — re-generation of a vendored toolchain)

**Issue**: [#37649](https://github.com/dotCMS/core/issues/37649)

**Input**: User description: "https://github.com/dotCMS/core/issues/37649 — upgrade the repo's GitHub Spec-Kit installation from the pinned v0.12.4 to the latest available release, re-apply our two in-place script patches, verify our customizations survived, review the regenerated shipped skills diff by diff, evaluate retiring patch #5 in favour of upstream `--timestamp` numbering, assess the new `.specify/feature.json` state file, and update `.specify/CUSTOMIZATIONS.md`."

---

## Context

The repo's spec-driven workflow is a **vendored** copy of [GitHub Spec-Kit](https://github.com/github/spec-kit): the `specify` CLI writes its files into `.specify/` and `.claude/skills/`, and those files are then tracked in git. There is no project-level upgrade command — `specify self` only manages the CLI binary — so the only way to move versions is to re-generate the tree in place with `specify init --here --force` and then put our own modifications back.

State verified on this worktree at `ba7ab6f0a2` on 2026-09-21:

| Fact | Value |
|---|---|
| Pinned version in `.specify/init-options.json` | `0.12.4` |
| `specify` CLI actually installed on this machine | `1.0.1` |
| Latest upstream release | `v1.0.9`, published 2026-09-21 |
| Tracked files under `.specify/` | 24 |
| Tracked `speckit-*` skills under `.claude/skills/` | **13** — 10 shipped by upstream, 3 net-new and ours |
| `eval` of a produced string in `.specify/scripts/bash/` | none (patch #6 is currently applied) |
| `feature_numbering` | `sequential` |
| `.specify/feature.json` | does not exist today |

Two corrections to the issue text, both confirmed above:

- The issue says "two custom skills". There are **three**: `speckit-adr-context`, `speckit-specify-fix` and `speckit-docs-converge`. All three are net-new names upstream never ships, so all three are safe from `--force`, but the third must be verified too.
- The issue says "12 tracked `speckit-*` skills". There are **13**. The shipped count of 10 that must be diffed is correct.

The pin is 43 releases behind, and the installed CLI is already ahead of the tree it generated — so the repo is in a drifted state where a dev running any `specify` script locally is not necessarily running what is committed.

**This is not a semver break.** The upstream v1.0.0 notes state the major bump carries no migration guide and that the number is "just a number". The risk in this work is not adapting to breaking changes; it is that `--force` silently regenerates 10 skills that encode our two non-negotiable gates — the mandatory ADR consultation before planning, and the TDD Red gate — and a weakening of either would produce no error, no failing build, and no visible symptom. A green run after this upgrade proves nothing on its own; only a read of the diffs does.

---

## Clarifications

### C-001 — The numbering decision is made in the plan phase, not now

**Asked because**: patch #5 (branch-aware feature numbering) exists only because we use `sequential` numbering. Upstream v1.0.9 ships `--timestamp` numbering, which is collision-free by construction and would retire patch #5 permanently — one fewer patch to re-apply on every future upgrade — at the cost of unreadable branch and directory names. The issue asks for this to be evaluated, and the answer changes what gets built.

**Decision**: the specification requires the *outcome* (collision-free numbering across unmerged branches) and admits **either** route. Which route is taken is decided during `/speckit-plan`, once the real v1.0.9 `create-new-feature.sh` has been read, and the decision plus its rationale is recorded in `CUSTOMIZATIONS.md`. See FR-006.

### Session 2026-09-21

- Q: If an upstream diff weakens the ADR gate or the TDD gate, what response is admissible? → A: Re-impose it through an upgrade-safe mechanism (override, hook, or net-new skill); editing a shipped skill is not admissible; if no upgrade-safe route reaches it, halt the upgrade and escalate.
- Q: How far does the obligation to in-flight work and the 57 existing spec directories reach? → A: Verify feature-directory resolution against at least one pre-existing spec directory as well as a newly created one, read-only; delivering a migration procedure for other developers' open branches is out of scope.
- Q: Where must the evidence of the individual review of the 10 shipped skills live? → A: In a committed record in the feature directory, one row per shipped skill (what upstream changed, whether it touches a gate, verdict); the pull request links it and narrates only the gate-affecting entries.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The spec-driven flow still enforces our gates after the upgrade (Priority: P1)

A dotCMS developer starts a new piece of work with `/speckit-specify` — or an issue/bug resolution with `/speckit-specify-fix` — and moves to `/speckit-plan`, `/speckit-tasks` and `/speckit-implement`. Everything that made the flow ours before the upgrade is still there: the spec asks for Legacy Considerations, the plan carries Legacy Impact and ADR Alignment, the ADR consultation fires by itself before planning, and the task list still orders tests → approval → Red → implementation.

**Why this priority**: this is the whole point of the upgrade being safe. If the gates survive, a partially finished upgrade is still usable; if they do not, the upgrade has quietly removed the controls the constitution calls non-negotiable, and every subsequent feature is planned without them. Nothing fails loudly when this breaks, which is exactly why it is P1.

**Independent Test**: run the full flow end to end against a throwaway feature on a scratch branch and read the generated artifacts for the dotCMS-specific sections and the automatic ADR step. Delivers value on its own: it is the evidence that the regenerated tree is fit to use.

**Acceptance Scenarios**:

1. **Given** the tree has been regenerated from the target release and our customizations re-established, **When** a developer runs `/speckit-specify` for a throwaway feature, **Then** the produced `spec.md` contains the **Legacy Considerations** section that only our override template provides.
2. **Given** that spec exists, **When** the developer runs `/speckit-plan`, **Then** the ADR consultation step runs automatically without being asked for, and the produced `plan.md` contains both **Legacy Impact** and **ADR Alignment**.
3. **Given** the plan exists, **When** the developer runs `/speckit-tasks`, **Then** each user story is ordered tests → developer-approval gate → Red gate → implementation.
4. **Given** the task list exists, **When** the developer runs `/speckit-implement`, **Then** it halts at each gate rather than running through them, and on finishing recommends convergence without executing it.
5. **Given** a bug rather than a feature, **When** the developer runs `/speckit-specify-fix` and then `/speckit-plan`, **Then** the fix passes through the same ADR and legacy gates as a feature does.

---

### User Story 2 - No upstream change to our gates slips through unread (Priority: P1)

A reviewer opening the pull request can see, without re-doing the work, exactly what upstream changed in each regenerated shipped skill, and is told explicitly whether anything touched the ADR Alignment gate or the TDD gate.

**Why this priority**: equal-first with US1 because it is the only defence against a silent regression. US1 proves the gates *appear*; US2 proves nobody accepted an upstream behaviour change without seeing it. Accepting 10 regenerated skills blind is the single largest risk in this work, and it is a review problem, not a runtime one.

**Independent Test**: read the pull request description and the commit history; every shipped skill is accounted for with a reviewed diff, and gate-affecting changes are called out in prose. Testable by a reviewer with no access to the author's terminal.

**Acceptance Scenarios**:

1. **Given** the shipped skills have been regenerated, **When** the reviewer opens the committed review record, **Then** every shipped skill in the target release has its own entry — none is accepted as an unexamined bulk change, and an empty diff is recorded as an empty diff rather than omitted.
2. **Given** an upstream change alters wording or behaviour around the ADR Alignment gate or the TDD `[GATE]` steps, **When** the reviewer reads the pull request description, **Then** that change is called out explicitly in prose, not left to be inferred from the diff.
3. **Given** an upstream change would weaken either gate, **When** the author reaches that diff, **Then** the gate is re-imposed from a file we own — never by editing the shipped skill — and if no such route reaches it, the upgrade halts and the decision is escalated.

---

### User Story 3 - Feature numbering cannot collide across unmerged branches (Priority: P2)

Two developers each start a feature on their own branch before either has pushed or merged. Neither ends up claiming the same feature number as the other.

**Why this priority**: P2 because the failure is annoying and manual to untangle rather than dangerous, and because it only bites when two features start close together. It is below the gate work but above documentation, since regenerating the tree re-introduces the collision unless something is done about it.

**Independent Test**: create two features from two branches without pushing in between and confirm the identifiers differ — independent of the rest of the upgrade.

**Acceptance Scenarios**:

1. **Given** a branch exists that carries a feature identifier but has no local `specs/` directory for it, **When** a developer starts a new feature, **Then** the new identifier does not reuse the one that branch already claimed.
2. **Given** the numbering strategy chosen in the plan, **When** a reader opens `CUSTOMIZATIONS.md`, **Then** the choice and the reason for it are stated, and the re-apply table reflects whether patch #5 still exists.

---

### User Story 4 - The next upgrade is cheaper than this one (Priority: P3)

Six months from now another developer upgrades Spec-Kit again. `CUSTOMIZATIONS.md` tells them the truth about the current state: what version we are on, which patches still have to be re-applied, and what survives on its own.

**Why this priority**: P3 — it pays off later, not now, and the upgrade is functional without it. It is still in scope because an upgrade that leaves the documentation describing the previous version guarantees the next upgrade repeats this investigation from scratch.

**Independent Test**: read `CUSTOMIZATIONS.md` against the state of the tree and confirm they agree, with no reference to the old version left behind.

**Acceptance Scenarios**:

1. **Given** the upgrade is complete, **When** a reader follows the "Re-applying after a `specify` upgrade" table, **Then** every row matches what is actually in the tree, including the corrected count of net-new custom skills.
2. **Given** upstream's `bug` extension was re-checked, **When** a reader looks for why we still use `/speckit-specify-fix`, **Then** they find a current verdict rather than the verdict from the 0.12.4 era.
3. **Given** the next upgrade begins, **When** its author looks for what upstream last changed in the shipped skills, **Then** the committed review record gives them a baseline to diff against instead of an investigation to repeat.

---

### Edge Cases

- **A customization is quietly lost rather than visibly broken.** The template overrides, the automatic ADR step and the constitution all fail by *absence*: the flow keeps working and just stops asking for the dotCMS-specific content. Verification cannot be "it ran without error" — it has to be "the section is present in the output".
- **The re-generation changes how the feature directory is resolved.** Upstream now reads a `.specify/feature.json` state file that we do not have today. A tree that resolves feature directories differently could point the flow at the wrong directory, or at none, for work already in progress.
- **The regenerated tree disagrees with the installed CLI.** The machine running the upgrade has a CLI ahead of the pin; if the upgrade is performed with a CLI that is neither the old pin nor the target, the tree can end up matching neither.
- **Upstream has already fixed one of our patches.** If v1.0.9 resolved the `eval` or the numbering issue on its own, re-applying our patch on top would be redundant or conflicting — each patch must be confirmed still necessary against the real source before being re-applied.
- **The target release moves while the work is in progress.** Upstream published v1.0.9 the same day this spec was written; a newer release may appear mid-work.
- **A shipped skill changes in a way that is legitimate but incompatible with our overrides.** An upstream rewrite of how templates are resolved would not break anything visibly — our overrides would simply stop winning.
- **Two features are created simultaneously before either is pushed.** Depending on the numbering route chosen, this either remains a known residual risk or is eliminated by construction.

---

## Requirements *(mandatory)*

### Functional Requirements

**Upgrade applied**

- **FR-001**: The vendored Spec-Kit tree MUST be regenerated from the newest upstream release available when the work starts — v1.0.9 or later — rather than from a version chosen in advance.
- **FR-002**: The recorded version MUST be the version actually installed and used to regenerate, so that the pin, the tree and the CLI used agree with each other.

**Patches re-established**

- **FR-003**: Each in-place patch MUST be confirmed still necessary against the target release's own source before being re-applied; a patch upstream has since fixed MUST be retired rather than reapplied.
- **FR-004**: Feature-path resolution MUST NOT re-parse any produced string as code anywhere in the vendored scripts.
- **FR-005**: The security scan MUST report no command-injection finding in the vendored scripts.
- **FR-006**: Feature numbering MUST be collision-free across branches that have no local spec directory. This MAY be satisfied **either** by retaining sequential numbering with the branch-aware patch re-applied and proven, **or** by adopting upstream's timestamp numbering and retiring that patch. The route chosen MUST be decided during planning, after reading the target release's own numbering code, and MUST be recorded with its rationale.

**Customizations verified intact**

- **FR-007**: The three dotCMS template overrides MUST still take precedence over the shipped templates, demonstrated by the dotCMS-specific sections appearing in freshly generated artifacts — not by the override files merely still existing on disk.
- **FR-008**: The ADR consultation MUST still fire automatically at the start of planning, without the developer invoking it.
- **FR-009**: The constitution MUST be present and unreset, with the TDD principle and the "Spec-Kit never creates ADRs" guardrail intact.
- **FR-010**: All **three** net-new custom skills — the ADR-context skill, the fix-specification skill and the documentation-convergence skill — MUST survive regeneration and be *invocable*, each producing its expected output. Presence on disk is not sufficient evidence.
- **FR-011**: The net-new issue-spec template and the ADR-context script MUST be present after regeneration.
- **FR-012**: All three registered hooks — the mandatory pre-plan ADR hook, the optional post-implement convergence recommendation, and the mandatory post-converge documentation hook — MUST still match the names of the skills they dispatch.
- **FR-012a**: The repo's own skill-governance artifacts MUST be brought back into agreement with the regenerated tree: every `speckit-*` skill still exempted from the naming and ownership checks, the generated skill catalog regenerated, and the skill lint passing. A regenerated tree that leaves the governance metadata describing the previous skill set is not complete.

**Shipped skills reviewed, not accepted blind**

- **FR-013**: Every shipped skill present in the target release MUST be diffed and reviewed individually; bulk acceptance is not permitted. The number reviewed is whatever that release ships — 10 in v1.0.9, the baseline this specification was written against — not a fixed count, so a release that adds, removes or renames a shipped skill is still covered.
- **FR-013a**: The review MUST leave a **committed record** in the feature directory carrying one entry per shipped skill: what upstream changed, whether it touches the ADR gate or the TDD gate, and the verdict. A skill whose diff was empty is recorded as such — silence is not an acceptable entry, since it cannot be distinguished from an unreviewed one.
- **FR-014**: Any upstream change affecting the ADR Alignment gate or the TDD gate MUST be called out explicitly in the pull request description in prose. The pull request links the committed record and narrates the gate-affecting entries rather than reproducing every entry.
- **FR-015**: Where an upstream change would weaken either gate, the gate MUST be re-imposed through an upgrade-safe mechanism — a template override, a registered hook, or a net-new skill — so that it survives the next regeneration without a re-apply step. Editing the shipped skill is **not** an admissible response, and neither is accepting the weakened gate silently.
- **FR-015a**: Where no upgrade-safe mechanism can reach a weakened gate, the upgrade MUST halt and the decision be escalated to the team. Absorbing the loss, or shipping the upgrade with the gate weakened, requires an explicit human decision recorded in the pull request — it is never the default.

**New upstream surface assessed**

- **FR-016**: The behavioural effect of the new feature-directory state file MUST be assessed and documented, including confirmation that it is a no-op for us if that is the finding.
- **FR-016a**: Feature-directory resolution MUST be verified against at least one **pre-existing** spec directory, not only against a newly created one, so that a developer returning to work started before the upgrade still reaches their own directory. The verification is read-only and MUST NOT modify the spec directory it exercises.
- **FR-017**: Upstream's native `bug` extension MUST be re-checked for whether it has gained a planning step, and the current verdict recorded. Retiring our fix-specification flow is explicitly **out of scope** for this work even if the re-check is favourable.

**End-to-end**

- **FR-018**: The full feature flow — specify, plan, tasks, implement — MUST be exercised end to end against a throwaway feature and shown to work.
- **FR-019**: The fix flow into planning MUST be exercised and shown to pass through the same ADR and legacy gates.

**Documentation**

- **FR-020**: The customizations document MUST be updated so that the version, the re-apply table and the list of what survives all describe the post-upgrade tree, with no reference to the superseded version left behind.
- **FR-021**: The corrected count of net-new custom skills MUST be reflected wherever the customizations document enumerates them.

---

### Key Entities

- **Vendored Spec-Kit tree**: the generated files that make the flow work, tracked in git and regenerated wholesale by the upgrade. Split into files upstream owns and files we own.
- **In-place patch**: a modification to a file upstream owns. Destroyed by every regeneration and therefore carrying a permanent re-apply cost. There are two today; the numbering decision may reduce that to one.
- **Net-new customization**: a file under a name upstream never ships, or a mechanism upstream reads but does not own. Survives regeneration by construction — the reason most of our work is arranged this way.
- **Gate**: a mandatory stop in the flow — the ADR consultation before planning, the developer-approval and Red steps before implementation. Encoded partly in files upstream owns, which is why regeneration threatens them.
- **Numbering strategy**: how a new feature gets its identifier. Determines whether the branch-aware patch is needed at all.
- **Skill diff review record**: the committed table of what upstream changed in each shipped skill and what was decided about it. Serves this pull request as evidence and the next upgrade as the baseline to diff against.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer running the complete flow on a throwaway feature sees every dotCMS-specific section — Legacy Considerations, Legacy Impact, ADR Alignment — present in the generated artifacts, with zero missing.
- **SC-002**: The ADR consultation runs on 100% of planning invocations without being requested, for both the feature flow and the fix flow.
- **SC-003**: The security scan reports zero command-injection findings in the vendored scripts — the same result as before the upgrade, not a regression absorbed as acceptable.
- **SC-004**: Starting two features from two unpushed branches in the same clone, one after the other, produces two distinct identifiers, 100% of the time. Two cases sit outside what either route can guarantee and are accepted as residual risk rather than engineered away: creation concurrent enough to race the numbering read, and a branch that exists only in another developer's clone and so cannot be observed at all.
- **SC-005**: Every regenerated shipped skill in the target release is individually accounted for as reviewed — 10 of them where the target is v1.0.9; a reviewer can name any one of them and find its entry in the committed review record, including the ones whose diff turned out to be empty.
- **SC-006**: A reader can determine, from the customizations document alone and without inspecting the tree, which version we are on and what a future upgrade will have to re-apply — and every statement they read matches the tree.
- **SC-007**: The number of modifications to upstream-owned files does not increase as a result of this work; it stays at two or drops to one. This holds even where a gate had to be re-imposed — a re-imposed gate lives in a file we own, never in one upstream ships.
- **SC-008**: The recorded version, the regenerated tree and the CLI used to generate it all name the same release — the drift that exists today is gone.
- **SC-009**: A feature whose spec directory existed before the upgrade still resolves to that same directory after it — no in-flight work is orphaned.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the developer-facing spec-driven development tooling only — `.specify/` and the `speckit-*` skills. No product surface, no runtime code, no backend, no frontend, no API. Neither `com.dotcms.*` nor `com.dotmarketing.*` is involved, so the legacy-versus-modern package question does not arise. The affected "legacy" here is our own accumulated tooling debt: two in-place patches against a dependency 43 releases stale.

- **Backward-compatibility expectations**: the 57 existing feature directories under `specs/` must remain readable and usable — an in-flight feature must not be orphaned by a change in how feature directories are resolved. Everything a developer knows how to do today must keep working: the same commands, the same gates, the same two-PR flow. If the numbering strategy changes, it applies to features created from then on and must not retroactively invalidate existing directories. Note that the repo's real-world convention is issue-number-prefixed directories, which sits alongside rather than inside Spec-Kit's own numbering.

- **Known related decisions**: the constitution's Principle V (TDD, non-negotiable) and the guardrail that Spec-Kit must never create ADRs are both binding on this work and must come through unchanged. The earlier rejection of upstream's `bug` extension, on the grounds that it bypasses the ADR gate, is being re-checked but not reversed here. Planning will formally consult `dotCMS/platform-adrs`; this is tooling rather than platform architecture, so few if any ADRs are expected to apply — which the plan should state rather than leave implied.

---

## Assumptions

- **Target release**: v1.0.9 is the newest as of 2026-09-21. If upstream publishes a newer release before the work begins, that one is the target instead; the intent is to be current rather than to land on a specific number.
- **Not a semver break**: taken from upstream's own v1.0.0 notes, which state the major bump carries no migration guide. The work is treated as re-applying our modifications, not as adapting to breaking changes — if the target release turns out to break something, that is a finding to surface, not an assumption to keep.
- **Regeneration is destructive to upstream-owned files and only to those**: files under names upstream never ships are assumed safe, but "assumed safe" still means verified, not skipped.
- **Only the two scripts are patched in place**: shipped skills and core templates stay unedited, and our behaviour is layered on through overrides and hooks. This work does not change that policy.
- **The end-to-end validation uses a throwaway feature on a scratch branch**, removed afterwards; it is not committed as a real spec.
- **The `bug` extension re-check is investigation only.** Retiring our fix flow would be separate work needing its own decision, whatever the re-check finds.
- **The upgrade is performed and verified on one machine.** Other developers pick it up by pulling the regenerated tree. Delivering a migration procedure for their open branches is out of scope — FR-016a establishes that pre-existing directories still resolve, which is what makes a procedure unnecessary.
- **Scope is the 24 tracked files under `.specify/`, the 13 tracked `speckit-*` skills, and the two repo-owned skill-governance artifacts** (the skill config and the generated catalog) that enumerate them. No product code, no build files, no CI workflow changes.
- **The locally installed CLI matters only to whoever regenerates the tree.** The flow executes the committed scripts, not the CLI, so a developer whose CLI is a different version is not running mismatched code day to day. The required CLI version is therefore documented rather than enforced, and no version-check mechanism is built.
