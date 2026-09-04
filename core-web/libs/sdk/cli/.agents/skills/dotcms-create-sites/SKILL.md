---
name: dotcms-create-sites
description: Plans and builds a complete dotCMS site in two phases. Phase 1 interviews the user and writes PLAN.md (purpose, sitemap, data model, every fact tagged) and DESIGN.md (visual identity tokens). Phase 2 hands off to the dotcms-best-practices skill, which owns the build sequence, and holds the verify-and-fix loop until every page type renders. Stops invented facts shipping as real. Supports both VTL-rendered and headless (Next.js) delivery. Use when someone wants to build, scaffold, or create a new dotCMS site or microsite.
---

# dotCMS Create Sites

Two phases: **plan**, then **build**. You do both.

- **Phase 1 — Plan.** Interview the user and write two artifacts:
  - **`PLAN.md`** — everything non-visual: purpose, audience, hostname, sitemap, sections, data model, and every fact tagged confirmed / placeholder / n-a.
  - **`DESIGN.md`** — the visual identity (YAML tokens + prose rationale). **Colors are never invented** — sample them from the user's logo/assets if provided, or ask.
- **Phase 2 — Build.** Hand off to **`dotcms-best-practices`**, which owns the build sequence.

`PLAN.md` and `DESIGN.md` belong in the project directory you're building — not wherever you
happen to be. A `PLAN.md` describing something other than a dotCMS site is not yours.

## Phase 1 — Plan

Copy this checklist and track progress. References are in `reference/plan/`.

```
Plan Progress:
- [ ] 1. If a site PLAN.md/DESIGN.md already exists for THIS project, read it, confirm it,
        and interview only on gaps. Otherwise start fresh — you are writing them.
- [ ] 2. Run the interview (reference/plan/interview.md) — infer from assets, confirm the
        drafted plan in ONE batch; ask upfront only what can't be inferred (delivery mode,
        transaction model, real-vs-placeholder facts)
- [ ] 3. Draft prose in the agreed voice; collect or defer every fact; source colors from
        logo/assets or ask
- [ ] 4. Write DESIGN.md (visual identity) from reference/plan/design-template.md
- [ ] 5. Write PLAN.md (structure + tagged facts) from reference/plan/plan-template.md
- [ ] 6. Confirm tagging is complete — every FACT field tagged, every STRUCTURE section
        carrying a heading tag — and PLAN.md's top checklist lists all gaps
- [ ] 7. Confirm the plan with the user before building
```

Phase 1 **cannot be skipped** unless both files already exist for this project and the user
confirms them. Don't start Phase 2 until the plan is confirmed.

## Phase 2 — Hand off, then hold the loop

**Load `dotcms-best-practices` and work through `reference/README.md`** — the 11 steps in
dependency order, the delivery-mode fork, and the file for each step.

What you carry into it:

| From PLAN.md | Used for |
|---|---|
| §2 hostname | creating the site |
| §3b delivery mode | which branch of the sequence applies — VTL or headless |
| data model | the content types to create |
| sitemap and sections | the pages, and what gets placed on them |
| DESIGN.md tokens | the theme's CSS (VTL), or the app's styling (headless) |

**The tagging rules survive the build:** `[PLACEHOLDER]` renders as a visible TODO, never an
invented value; `[n/a]` is removed, not flagged; `[ai-draft]` needs sign-off before launch.

### Hold the loop

The sequence ends in a verify-and-fix loop. **Your job is not to let it stop at one pass** — a
first failure is normal. Do not report the build done until every page type verifies.
`dotcms-best-practices` has a symptom index for classifying failures.

## References — Phase 1 only

- **How to interview + how to tag answers** → [reference/plan/interview.md](reference/plan/interview.md)
- **DESIGN.md output format** (visual identity) → [reference/plan/design-template.md](reference/plan/design-template.md)
- **PLAN.md output format** (structure + facts) → [reference/plan/plan-template.md](reference/plan/plan-template.md)

## Tagging (the core rule)

Every field carries exactly one of `[confirmed]` · `[PLACEHOLDER — needs human]` ·
`[n/a — intentionally omitted]` · `[ai-draft — approve]`. Definitions and how to apply them:
[reference/plan/interview.md](reference/plan/interview.md). **Never assume a missing fact is a
placeholder — ask which applies.**

## Done when

`PLAN.md` and `DESIGN.md` exist and are fully tagged — every FACT field, every STRUCTURE section.
The site is built and published. **Every page type has passed its verify step.** No
`[PLACEHOLDER]` renders as a real value, every `[n/a]` was removed rather than flagged, and
`[ai-draft]` copy is flagged for approval.
