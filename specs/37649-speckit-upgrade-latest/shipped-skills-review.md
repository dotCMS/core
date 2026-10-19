# Shipped skill review: Spec-Kit 0.12.4 → v1.0.9

**Feature**: [spec.md](./spec.md) — dotCMS/core#37649
**Reviewed**: 2026-09-22
**Diff source**: commit `83afcddb08`, which regenerates the vendored tree and contains nothing
else. Reproduce any row with `git show 83afcddb08 -- .claude/skills/speckit-<name>/`.

Upstream v1.0.9 ships **ten** skills, the same ten as 0.12.4 — none added, removed or renamed
(`templates/commands/` in the release source, corroborated by the ten hashes in
`.specify/integrations/claude.manifest.json`). Every one of the ten has a row below. None had an
empty diff, but the format requires a row either way: an omitted row and an unreviewed skill are
indistinguishable to a reviewer, which is why FR-013a forbids silence.

**Two gates are what this review is looking for**: the ADR Alignment gate (the mandatory
`before_plan` hook plus the plan template's ADR section) and the TDD gate (tests → developer
approval → Red → implementation). A change that weakens either must be re-imposed from a file we
own, never by editing the shipped skill (FR-015); where no such route exists the upgrade halts and
the decision is escalated (FR-015a).

**Verdict summary**: no shipped skill weakens either gate. One change strengthens the ADR gate,
one changes how our tasks-template override reaches the skill without changing which template
wins, and one — `speckit-constitution` — is a substantive rewrite that is safe as it stands but
opens a protection we do not currently take. Details in the rows, and the one item worth a
decision is called out under the table.

## The ten shipped skills

| Skill | Diff | What upstream changed | Touches the ADR gate or the TDD gate? | Verdict |
|---|---|---|---|---|
| `speckit-analyze` | +5 −5 | Adds `--require-spec` to the `check-prerequisites.sh` call, so the command refuses to run without a spec instead of proceeding on a half-built feature. Remainder is the shared hook-wording change described below the table. | No. It reads artifacts and reports; it enforces neither gate. | Accepted — stricter precondition, no behaviour we rely on changes. |
| `speckit-checklist` | +17 −7 | Adds an ownership and checkbox-lifecycle contract: generated checklists are reviewer-owned, `[x]` means "the reviewer judged this requirements-quality criterion satisfied" and explicitly **not** "implementation is complete", and the command must never tick its own items. Also resolves its template through `check-prerequisites.sh --template checklist-template` rather than reading the file path. | No — but adjacent to the TDD gate in a good way. It draws a line between a reviewer ticking a quality criterion and work being done, which is the confusion that could otherwise let a checklist stand in for a Red gate. | Accepted — strengthens the distinction between review state and implementation state. |
| `speckit-clarify` | +12 −6 | Adds question-writing rules: every question must be a full interrogative ending in `?`, never a bare topic label or requirement id, with a plain-language "why it matters" sentence and no undefined jargon. Narrows one deferral rule from "better deferred to planning" to implementation method, tech-stack comparison or task breakdown. | No. | Accepted — and it points the same way as the constitution's "Reporting to the Developer" rule: never let an identifier carry the meaning. |
| `speckit-constitution` | +47 −24 | The largest change, and a deliberate behaviour change upstream documents. (1) The consistency-propagation step is **removed**: the command no longer reads and edits `plan-template.md`, `spec-template.md`, `tasks-template.md` or the command files. Its description drops "ensuring all dependent templates stay in sync". (2) The constitution scaffold is now resolved through `resolve-template.sh constitution-template`, i.e. through the override → preset → extension → core stack, and the command must stop if resolution fails. (3) A new Scope Guard forbids it from writing application code or unrelated artifacts, deferring such requests to a `Next Actions` list. (4) The Sync Impact Report is redefined as temporary scratch expected to be removed before commit. | **Examined closely; neither gate is weakened.** The removed propagation only ever wrote to the *core* templates, which our overrides shadow — it could never reach `templates/overrides/`, so nothing we depend on was being kept in sync by it. Upstream's own reasoning matches ours: the live constitution is the runtime authority and the templates are scaffolds. Our TDD gate text lives in the constitution and in our tasks-template override, both untouched. | Accepted. See "One item that deserves a decision" below — the new scaffold resolution is an opportunity we are not currently taking, not a regression. |
| `speckit-converge` | +11 −5 | Adds `--require-spec` to its prerequisites. Adds a paragraph instructing it to include every existing task in the intent inventory "regardless of checkbox state or Convergence phase: completion claims are not evidence", to assess resulting behaviour for corrective task chains, and to flag implementation that contradicts or exceeds the stated intent. | No. | Accepted, with a documentation consequence: `CUSTOMIZATIONS.md` §8 records a "known asymmetry" that shipped converge lacks our dedupe. This change sharpens it — converge now *deliberately* re-examines checked tasks — so the asymmetry is upstream's intent rather than an oversight. The wording in that section is updated to say so. |
| `speckit-implement` | +22 −19 | Reframes checklist handling as a **read-only** gate: scan checkbox state, report, ask before proceeding, never modify a checklist file or marker. Restates the built-in `checklists/requirements.md` versus reviewer-owned custom checklists. Renames the status vocabulary from completed/incomplete to checked/unchecked throughout. Trailing-whitespace cleanup. | **No.** The `[GATE]` halting behaviour the TDD gate depends on comes from our `tasks-template` override plus the constitution, neither of which this touches; the `after_implement` hook dispatch that prints our convergence recommendation is unchanged. Verified behaviourally, not only by reading: the regenerated command halted at a throwaway feature's approval gate with the test written and no implementation on disk, and printed the convergence recommendation without executing it on reaching the end of the list. | Accepted. |
| `speckit-plan` | +6 −7 | Parses `FEATURE_DIR` instead of `SPECS_DIR` from `setup-plan.sh`. Drops the "Phase 1: Update agent context by running the agent script" step. Completion report now says the command ends after Phase 1 design rather than Phase 2 planning. Plus the shared hook-wording change. | **Yes — and it strengthens the ADR gate.** A malformed `.specify/extensions.yml` previously caused hook checking to be skipped *silently*, which would have disabled our mandatory ADR consultation with no symptom at all. v1.0.9 requires the command to say the file could not be read, name the parser error, and state that mandatory hooks were not checked. The `before_plan` dispatch contract is otherwise unchanged. | Accepted — this removes a silent-failure path in exactly the mechanism our ADR gate rides on. The dropped agent-context step is a no-op here: this tree ships no such script. |
| `speckit-specify` | +20 −20 | Step renumbering (the write and validation steps become 7 and 8) and trailing-whitespace cleanup, plus the shared hook-wording change. No instruction changed meaning. | No. | Accepted. Verified behaviourally that the dotCMS spec override still wins: a spec generated with the regenerated command carried `## Legacy Considerations *(dotCMS-specific — mandatory)*`. |
| `speckit-tasks` | +9 −8 | Now parses `TASKS_TEMPLATE_CONTENT` from `setup-tasks.sh` and uses that content as the structure, falling back to reading the `TASKS_TEMPLATE` path only for older setup scripts. Adds guidance to quote data-model field constraints verbatim into task descriptions. Whitespace. Plus the shared hook-wording change. | **Yes, mechanically — and our override still wins.** Which template reaches the command changed from "read this path" to "use this content", but both come from the same resolution stack, and the resolution now happens inside the script rather than in the agent. Confirmed live: `setup-tasks.sh --json` returns `TASKS_TEMPLATE_CONTENT` holding our dotCMS tasks template, gates included, and the throwaway task list generated afterwards ordered its story tests → `[GATE]` approval → `[GATE]` Red → implementation. | Accepted — arguably more robust, since the override is resolved by the script instead of depending on the agent reading the right path. |
| `speckit-taskstoissues` | +5 −5 | Corrects the task-id pattern used for issue deduplication from exactly three digits to **at least** three, because `/speckit-converge` assigns ids with `T{M+1:03d}`, a floor rather than a cap, so a file past 999 tasks has four-digit ids. Plus the shared hook-wording change. | No. | Accepted — a real off-by-format bug fix, though this repo has no feature anywhere near 999 tasks. |

### The change that appears in nine of the ten rows

Every skill that dispatches extension hooks got the same two edits, which is why they are described
once here rather than repeated ten times:

- **Hook parse failures are no longer silent.** Previously: "If the YAML cannot be parsed or is
  invalid, skip hook checking silently and continue normally." Now the command must tell the user
  the file could not be read, include the parser error, and state that no hooks were checked
  *including any mandatory ones registered there*. For this repo that is a direct improvement to
  the ADR gate, whose entire enforcement is one `optional: false` entry in that file.
- **"slash commands" → "command invocations"** when mapping a hook's dotted name to its command.
  Wording only; the dot-to-hyphen mapping our three hooks rely on is unchanged.

### One item that deserves a decision

`speckit-constitution` now resolves its scaffold through the override stack
(`resolve-template.sh constitution-template`) and applies it to the existing constitution,
"preserving information that is still applicable". We ship **no**
`.specify/templates/overrides/constitution-template.md`, so the scaffold it would apply is
upstream's generic one — meaning someone running `/speckit-constitution` could reshape our
authored constitution toward upstream's structure.

This is **not** a regression introduced by the upgrade, and nothing on disk changed: the risk of
running that command against an authored constitution existed at 0.12.4 too, where the command
described the constitution as "a TEMPLATE containing placeholder tokens" to be filled. What v1.0.9
adds is the *fix*: because the scaffold now comes through the override stack, an
`overrides/constitution-template.md` carrying our structure would make our own scaffold win — the
same upgrade-safe mechanism as our other three overrides.

Taking it is outside what FR-009 requires, which is that the constitution survive this
regeneration, and it did. Recorded here so the decision is made deliberately rather than
forgotten.

**Decision, 2026-09-22**: recorded, not taken. FR-009 requires the constitution to survive *this*
regeneration and it did, unmodified. Adding a fourth template override is scope the approved
specification does not ask for, and it deserves its own issue and its own review rather than
arriving inside an upgrade PR. A developer amending the constitution should be aware that
`/speckit-constitution` applies a resolved scaffold, and that the upgrade-safe fix — an
`overrides/constitution-template.md` — is now available whenever the team wants it.
