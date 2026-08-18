# Spec-Kit Quick Start

Get from an issue to merged code using spec-driven development in this repo — two PRs: the
spec first, the implementation second. Sections 1-4 are the ones to actually read (~5 min);
the rest is reference you consult when you hit it.

> **See also:** [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)
> — the project law every `/speckit-*` command loads. If a command pushes back on you,
> it's almost always quoting this file.

---

## 1. What it is

**Your first decision is the only branch in the whole flow:** are you adding behavior that
doesn't exist yet, or fixing behavior that's broken? That picks your entry command. Everything
after it is the same for both.

```
   Adding NEW behavior           Fixing BROKEN behavior
    /speckit-specify              /speckit-specify-fix
            │                               │
            └───────────────┬───────────────┘
                            ▼
                      /speckit-plan     ← ADR + Legacy gates
                            │
                            ▼
                     /speckit-tasks
                            │
                            ▼
                   /speckit-implement   ← TDD gates, halts for you
```

You run these in Claude Code; each writes files into `specs/<your-feature>/` and hands off to
the next. Because both entry points converge at `/speckit-plan`, a bug fix gets exactly the
same ADR and legacy scrutiny as a feature.

---

## 2. Match the flow to the change

The full pipeline is heavy for tiny changes, and Spec-Kit is honest about that. Size the
process to the work. **When in doubt, size up** — and ask a teammate rather than guessing.

```
              A change to make
                      │
                      ▼
  ┌───────────────────────────────────────┐
  │ Does it change observable behavior?   │──── No ────→ TIER 0
  └───────────────────┬───────────────────┘
                      │ Yes
                      ▼
  ┌───────────────────────────────────────┐
  │ New/changed interface, cross-team,    │──── Yes ───→ TIER 2
  │ or security-sensitive?                │
  └───────────────────┬───────────────────┘
                      │ No
                      ▼
  ┌───────────────────────────────────────┐
  │ Bug fix or small change inside        │──── Yes ───→ TIER 1
  │ an existing interface?                │
  └───────────────────┬───────────────────┘
                      │ No
                      ▼
                   TIER 2
```

| Tier | Typical change | Flow |
|------|----------------|------|
| **0 — Trivial** | Typos, copy, config, true no-op refactors | No spec. Keep tests green. |
| **1 — Standard** | Most bug fixes; small changes inside an existing interface | Lean: `specify` → `plan` → `tasks` → `implement` |
| **2 — Significant** | New features, new or changed interfaces, data-model changes, cross-team or security-sensitive work | Full: add `clarify` + `checklist` before planning, `analyze` before implementing |

---

## 3. Two PRs: the spec is reviewed and merged first

**Spec-Kit work ships as two pull requests, not one.**

| | What's in it | Reviewed for | Then |
|---|---|---|---|
| **PR 1 — the spec** | `spec.md`, nothing else | Is this the right problem, scoped right, with measurable criteria? | Approved and **merged** before any planning starts |
| **PR 2 — the implementation** | Code, tests, and any durable design artifacts | Does it do what the merged spec said? | Merged as usual |

The sequence:

1. `/speckit-specify` (or `/speckit-specify-fix`) writes the spec on your feature branch.
2. Push it and open **PR 1 with the spec alone.** Ask for feedback, iterate on the wording,
   get it approved, and **merge it.**
3. Branch again from `main`. The approved spec now lives there, so you, your reviewer, and
   the agent all read the same contract.
4. `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`.
5. Open **PR 2** with the implementation, linking back to PR 1.

Why we work this way: reviewing a large diff means reconstructing intent from code — slow, and
it catches the wrong class of problem. Reviewing a spec means reading intent directly, in
natural language, while changing course is still cheap. We're deliberately moving the effort
upstream. **Planning is our bottleneck now, not PR review. Better input produces better
output.**

In practice:

- **Don't run `/speckit-plan` until PR 1 is merged.** The approval is a real gate — it's just
  a human one, enforced on the PR rather than by a script.
- **A spec that's hard to review isn't ready.** If a reviewer can't tell what "done" looks
  like from the acceptance criteria, that's the finding — fix the spec, not the review.
- **Review it like a spec, not like code.** Is the problem stated? Are the success criteria
  measurable? What's explicitly *out* of scope? Reviewers: push back on vagueness, not style.

> Branching again doesn't lose your place. Spec-Kit locates the spec through the local
> `.specify/feature.json` pointer (or `SPECIFY_FEATURE_DIRECTORY`), **not** the branch name,
> and that file is local and untracked — leave it out of both PRs. If a command can't find the
> feature, `export SPECIFY_FEATURE_DIRECTORY=specs/<your-dir>`.

Three different approvals happen in a Spec-Kit run — don't confuse them:

| Approval | Who gives it | Where |
|----------|--------------|-------|
| **The spec** | Another dev | On PR 1, before it merges |
| **The tests** | You | In-session, at the TDD gate (§7) |
| **ADR alignment** | Automated consult, you resolve conflicts | Inside `/speckit-plan` |

---

## 4. Before your first run

```bash
gh auth status   # must be authenticated AND have access to dotCMS/platform-adrs
```

The plan phase automatically looks up Architecture Decision Records in the **private**
`dotCMS/platform-adrs` repo. Without access the lookup returns nothing and planning
continues anyway — so a missing grant is silent. Check it now, not later.

> ⚠️ Your first run will prompt you to approve scripts under `.specify/scripts/bash/`.
> That's expected — they're the Spec-Kit helpers, and they're read-only at this stage.

---

## 5. A worked example — implementing a feature

One feature, all four commands, start to finish. Fixing a bug follows the same shape with a
different entry command — §6, next.

### `/speckit-specify` — write the contract

```
/speckit-specify Add a changelog panel to the site publish dialog so editors
can see what changed before pushing
```

Creates a branch and `specs/<n>-<short-name>/spec.md`, then interviews you about anything
ambiguous. The spec is **what and why** — user stories with priorities, acceptance
scenarios, success criteria, and a **Legacy Considerations** section. No implementation
detail.

Real example to read before your first run:
[`specs/36605-changelog-site-publish/spec.md`](../../specs/36605-changelog-site-publish/spec.md)
— note the directory is named for the **GitHub issue number**, not Spec-Kit's default
zero-padded `001-`. Working from an issue? Use its number, so the branch matches what
everyone else is reading (`create-new-feature.sh --help` for `--number` / `--short-name`).

**Stop here.** Open PR 1 with the spec alone and get it merged (§3). Everything below
happens after that.

### `/speckit-plan` — decide the approach

```
/speckit-plan
```

Reads your spec and produces `plan.md`: architecture, **Test Strategy**, **Constitution
Check**, **Legacy Impact**, and **ADR Alignment**. This is where the two automated gates live —
see §7. Takes optional guidance (`/speckit-plan use the existing WorkflowAPI rather than a new
service`).

### `/speckit-tasks` — break it down

```
/speckit-tasks
```

Produces `tasks.md`: dependency-ordered tasks grouped by user story. Every story is ordered
**Tests → `[GATE]` approval → `[GATE]` Red → Implementation**. Skim it before implementing —
this is the cheapest moment to catch a wrong decomposition.

### `/speckit-implement` — build it

```
/speckit-implement
```

Works through `tasks.md` in order and **halts at every `[GATE]`**. It writes tests first,
stops for your approval, stops again to show you they fail, and only then writes code.

---

## 6. A worked example — fixing a bug

Most bug fixes are Tier 1 — lean flow, no `clarify`/`checklist`/`analyze`.

```
/speckit-specify-fix Site publish dialog shows a stale changelog after a
second publish — repro in dotCMS/core#12345
```

1. **Reproduce it**, then run `/speckit-specify-fix` to write the defect spec: observed vs.
   expected behavior, and the repro.
2. **Open PR 1 with that spec alone**, get it approved, and merge it (§3).
3. **Branch from `main`, then write a failing regression test** that captures the expected
   behavior — it's your first acceptance criterion, and it's the Red gate from §7.
4. **Let the agent fix the code** until that test and the suite pass.
5. **Open PR 2 with the fix**, linking PR 1 (and any ADR, if the root cause touches a prior
   decision).

The spec is defect-framed rather than story-framed: **Problem Statement, Reproduction, Scope
of Investigation, Root-Cause Hypothesis, Fix Scope & Non-Goals, Regression Risk, Acceptance &
Verification**. *Fix Scope & Non-Goals* is the one that earns its keep — it's what stops a
bounded fix from becoming a legacy rewrite.

> **Urgent incident?** The order inverts, not the count. Ship the fix first, then send the
> short defect spec as its own PR right after. The goal is never to leave a behavior change
> with no record of intent — not to slow down an outage.

---

## 7. The two gates that stop the run

Both are deliberate halts. Neither is a bug.

### TDD (Constitution Principle V — non-negotiable)

No implementation code is written before three things happen:

1. **Tests are written** — layer-appropriate: unit, integration, Postman, Karate, or e2e.
2. **You approve them.** Explicitly. If a test type genuinely can't be written, *you* say
   which and why, and the reason gets recorded. Silence is not consent, and "no tests" is
   never the default.
3. **Tests are confirmed failing (Red)** for the right reason — not a compile error.

So when `/speckit-implement` appears to stall, read the last message: it's almost always
waiting on gate 2. Reply with your review — approve, or ask for different tests.

### ADR Alignment

`/speckit-plan` auto-runs `/speckit-adr-context` before planning (a mandatory `before_plan`
hook in [`.specify/extensions.yml`](../../.specify/extensions.yml)). It searches
`dotCMS/platform-adrs` and pulls relevant decisions into the plan as **binding** input. A
plan that conflicts with an **accepted** ADR must resolve the conflict or justify it.

> ⚠️ **Spec-Kit never creates ADRs.** It only *proposes* them, under "Proposed ADRs" in the
> plan. Real ADRs are authored in `dotCMS/platform-adrs` via its own `new-adr.sh`. Discuss
> in `#eng-adrs`.

To search ADRs by hand at any point:
`.specify/scripts/bash/adr-context.sh "workflow" "elasticsearch"`

---

## 8. What gets committed, and to which PR

One test: **durable reference vs. process artifact.** Would a reviewer or a future dev want
this *after* the PR merges, without re-reading the code?

| Artifact | Goes in | Why |
|----------|---------|-----|
| `spec.md` | **PR 1**, alone | the reviewed contract (FRs, user stories, success criteria) |
| `data-model.md` | **PR 2**, when it carries verified contracts | concrete entity→field/type, relationships, validation rules, real payload/DB shapes confirmed while building |
| `contracts/` | **PR 2**, same test as `data-model.md` | committed API specs are durable; scaffolding is not |
| `plan.md`, `research.md`, `tasks.md`, `quickstart.md`, `checklists/` | Neither — never committed | pure process — how / what-order / decisions-in-flight |

The never-commit set is enforced by [`.gitignore`](../../.gitignore). Those files still live
on disk and keep working — `/speckit-implement` and `/speckit-converge` read them normally.
If `git add tasks.md` seems to do nothing, this is why.

---

## 9. The other commands

| Command | Reach for it when |
|---------|-------------------|
| `/speckit-clarify` | Tier 2 work — resolve open questions in the spec before planning |
| `/speckit-checklist` | Tier 2 work — a domain-specific review checklist (security, a11y, performance) |
| `/speckit-analyze` | Tier 2 work — spec, plan, and tasks have drifted apart; cross-checks them, changes nothing |
| `/speckit-converge` | You've built ahead of the tasks (or by hand) and want the gap appended to `tasks.md` |
| `/speckit-taskstoissues` | The work needs to be split across people as GitHub issues |
| `/speckit-adr-context` | You want an ADR lookup outside the plan phase |
| `/speckit-constitution` | You're amending project law — rare, and it's a team decision |

---

## 10. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Run seems hung after `/speckit-implement` | It's parked at a `[GATE]` waiting on you | Read the last message; approve the tests or say what to change |
| ADR section says no ADRs found, always | No `gh` access to the private `platform-adrs` | `gh auth status`, then request access in `#eng-adrs` |
| `git add tasks.md` does nothing | Gitignored by design (§8) | Nothing to fix — it's a process artifact |
| "Feature directory already exists" | Number collision with an unmerged branch | Rerun with `--number N`, or `--timestamp` for a collision-free name |
| Implementation ignores the test order | `[GATE]` tasks were edited out of `tasks.md` | Regenerate with `/speckit-tasks`; never delete gate tasks |
| Plan skipped the ADR step | The `before_plan` hook didn't fire | Run `.specify/scripts/bash/adr-context.sh` yourself and fill in the ADR Alignment section |
| Commands can't find the feature after you branch again | `.specify/feature.json` is missing or stale | `export SPECIFY_FEATURE_DIRECTORY=specs/<your-dir>` — the pointer is local and untracked, never committed |
| Reviewer says the spec is too vague to approve | The spec is doing implementation, or the criteria aren't measurable | Rewrite the acceptance criteria as observable outcomes; `/speckit-clarify` helps |

---

## Reference

- Install, customizations, upgrade notes: [`.specify/CUSTOMIZATIONS.md`](../../.specify/CUSTOMIZATIONS.md)
- Project law: [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)
- Example spec: [`specs/36605-changelog-site-publish/spec.md`](../../specs/36605-changelog-site-publish/spec.md)
- Branching & PRs: [`GIT_WORKFLOWS.md`](GIT_WORKFLOWS.md)
- Testing: [`docs/testing/`](../testing/) — [`INTEGRATION_TESTS.md`](../testing/INTEGRATION_TESTS.md)
- Legacy vs modern: [`PROGRESSIVE_ENHANCEMENT.md`](PROGRESSIVE_ENHANCEMENT.md)

---

*Maintained by the Engineering Team. Spot something stale or wrong? Open a PR against this
file and ping `#eng`. ADR questions go to `#eng-adrs`.*
