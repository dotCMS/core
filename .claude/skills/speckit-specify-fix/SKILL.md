---
name: "speckit-specify-fix"
description: "Create an issue/bug-resolution specification (defect-framed) from a bug report or issue description. dotCMS variant of /speckit-specify for fixes."
argument-hint: "Describe the bug or paste the issue (title + repro)"
compatibility: "Requires spec-kit project structure with .specify/ directory"
metadata:
  author: "dotcms"
  source: "dotcms customization (see .specify/CUSTOMIZATIONS.md)"
user-invocable: true
disable-model-invocation: false
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Purpose

This is the **issue / bug-resolution** counterpart to `/speckit-specify`. It produces a
defect-framed specification (problem, reproduction, root-cause hypothesis, fix scope,
regression risk, acceptance) using the dotCMS issue template, and then feeds the normal
`/speckit-plan` → `/speckit-tasks` → `/speckit-implement` flow — so the plan phase's
**Legacy Impact** and **ADR Alignment** gates still apply.

Use `/speckit-specify` instead when the work is a NEW FEATURE rather than fixing existing
behavior.

## Outline

1. **Create the feature directory** (reuse the standard Spec-Kit script — do NOT reimplement
   numbering):

   ```bash
   .specify/scripts/bash/create-new-feature.sh --json "<short issue description>"
   ```

   Parse the JSON for `BRANCH_NAME` and `SPEC_FILE`. If the user explicitly provided a
   feature directory or branch name, honor it.

2. **Swap in the issue-resolution template**. The script above seeds `SPEC_FILE` with the
   feature template; overwrite it with the issue template (respecting the override stack):

   - If `.specify/templates/overrides/spec-issue-template.md` exists, copy that over `SPEC_FILE`.
   - Otherwise copy `.specify/templates/spec-issue-template.md` over `SPEC_FILE`.

3. **Load `.specify/memory/constitution.md`** for project principles (legacy-awareness, ADR
   rules) — they inform how you scope the fix.

4. **Fill the issue spec** at `SPEC_FILE`, preserving section order and headings, using the
   bug report / issue text in the user input:
   1. Parse the report. If empty: ERROR "No issue description provided".
   2. **Problem Statement** + severity/impact.
   3. **Reproduction**: environment, numbered steps, expected vs actual, reproducibility.
      If the report lacks repro detail, mark with `[NEEDS CLARIFICATION: ...]` (max 3 total).
   4. **Scope of Investigation**: name the affected dotCMS area; note suspected surface
      (modern `com.dotcms.*` vs legacy `com.dotmarketing.*`) as a best guess — the plan
      confirms it. Do NOT do code-level root-causing here beyond a hypothesis.
   5. **Root-Cause Hypothesis**: current best theory (may be refined/replaced in planning).
   6. **Fix Scope & Non-Goals**: keep the fix bounded; list explicit non-goals to avoid
      scope creep and unintended legacy rewrites.
   7. **Regression Risk**: blast radius, backward compatibility, data considerations.
   8. **Acceptance & Verification**: measurable ACs tied to the reproduction, plus the
      specific test(s) to add/run.

5. **Quality validation**. Create a checklist at
   `<feature-dir>/checklists/requirements.md` and validate the spec against it:

   ```markdown
   # Issue Spec Quality Checklist: [ISSUE TITLE]

   ## Content Quality
   - [ ] Problem and impact are clear to a non-author
   - [ ] Reproduction steps are concrete and ordered
   - [ ] Expected vs actual behavior are unambiguous

   ## Completeness
   - [ ] Affected area / suspected surface identified
   - [ ] Fix scope bounded with explicit non-goals
   - [ ] Regression risk (blast radius, back-compat, data) assessed
   - [ ] Acceptance criteria are measurable and tied to the repro
   - [ ] Verification method (specific test/steps) named
   - [ ] No unresolved [NEEDS CLARIFICATION] markers (or ≤3, most critical only)

   ## Readiness
   - [ ] Ready for /speckit-plan (Legacy Impact + ADR Alignment will run there)
   ```

   If items fail (excluding `[NEEDS CLARIFICATION]`), update the spec and re-validate (max 3
   iterations). If `[NEEDS CLARIFICATION]` markers remain, present up to 3 numbered questions
   with suggested-answer tables and wait for the user's response.

6. **Report** the branch, the issue `SPEC_FILE` path, and any open clarifications. Recommend
   next step: `/speckit-plan` (which will consult ADRs and assess legacy impact).

## Guardrails

- This skill scopes a fix; it does not write code. `/speckit-implement` does that later.
- **Never create, edit, or commit an ADR** (here or in `dotCMS/platform-adrs`). If the fix
  implies a new architectural decision, it will be captured as a *proposal* in the plan's
  "Proposed ADRs" section. ADRs are authored only in `dotCMS/platform-adrs` via `new-adr.sh`.
