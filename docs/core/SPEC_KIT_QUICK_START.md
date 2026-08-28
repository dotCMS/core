# Spec-Kit Quick Start

Get from an issue to merged code using spec-driven development in this repo — two PRs: the
spec is approved first, the implementation follows. Sections 1-4 are the ones to actually read (~5 min);
the rest is reference you consult when you hit it.

> 🎥 **New to this?** Watch the [walkthrough video](https://drive.google.com/file/d/1XhQBgbME2XejZ7PHc7-xNdFtaXUFOULL/view?usp=sharing) first — it covers the same
> ground in a few minutes. This doc is what you come back to afterward.

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
| **1 — Standard** | Most bug fixes; small changes inside an existing interface | **Lean** — `specify` / `specify-fix` → `plan` → `tasks` → `implement` |
| **2 — Significant** | New features, new or changed interfaces, data-model changes, cross-team or security-sensitive work | **Full** — `specify` / `specify-fix` → (`clarify`) → (`checklist`) → `plan` → `tasks` → (`analyze`) → `implement` |

The entry command is a separate question from the tier: `specify` for new behavior,
`specify-fix` for broken behavior (§1). **Tier decides how much process; new-vs-broken decides
where you start.** A bug can be Tier 2 — §6's example is one — and a small non-bug change
inside an existing interface is Tier 1 but still starts at `specify`.

**Steps in parentheses are optional.** They are judgment calls, not required stages — nothing
blocks or complains if you skip them. The four unparenthesized commands are the flow; the rest
are tools you pick up when they help:

- `clarify` — the spec still has open questions you'd rather resolve before planning
- `checklist` — the change is security-, privacy-, or accessibility-sensitive and deserves a
  domain review pass
- `analyze` — spec, plan, and tasks may have drifted apart, and you want that checked before
  writing code

§9 covers what each of them actually does.

---

## 3. Two PRs: the spec is approved before you build

**Spec-Kit work ships as two pull requests, not one.** The gate is **approval, not merge** —
once a reviewer approves the spec, start planning. Don't wait on the merge queue.

| | What's in it | Reviewed for | Then |
|---|---|---|---|
| **PR 1 — the spec** | `spec.md`, nothing else | Is this the right problem, scoped right, with measurable criteria? | **Approved** → you start planning. It merges whenever the queue gets to it. |
| **PR 2 — the implementation** | Code, tests, and any durable design artifacts | Does it do what the approved spec said? | Merged as usual |

The sequence:

1. `/speckit-specify` (or `/speckit-specify-fix`) writes the spec on your feature branch.
2. Push it and open **PR 1 with the spec alone.** Ask for feedback, iterate on the wording,
   get it **approved.** Leave it in the queue — you're not blocked on it merging.
3. Branch off **your spec branch** (not `main` — the spec isn't there yet) and keep working.
4. `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`.
5. Open **PR 2** with the implementation, linking back to PR 1.

> Until PR 1 merges, PR 2's diff also shows the spec commit — it's the shared ancestor, not a
> duplicate. Once PR 1 lands, PR 2 collapses to just the implementation on its own.

Why we work this way: reviewing a large diff means reconstructing intent from code — slow, and
it catches the wrong class of problem. Reviewing a spec means reading intent directly, in
natural language, while changing course is still cheap. We're deliberately moving the effort
upstream. **Planning is our bottleneck now, not PR review. Better input produces better
output** — and gating on approval rather than merge keeps the queue from becoming the new
bottleneck.

In practice:

- **Don't run `/speckit-plan` until PR 1 is approved.** The approval is a real gate — it's
  just a human one, enforced on the PR rather than by a script.
- **A spec that's hard to review isn't ready.** If a reviewer can't tell what "done" looks
  like from the acceptance criteria, that's the finding — fix the spec, not the review.
- **Review it like a spec, not like code.** Is the problem stated? Are the success criteria
  measurable? What's explicitly *out* of scope? Reviewers: push back on vagueness, not style.
- **If the spec changes after approval**, say so on PR 1 and get a re-approval. The whole
  point is that the contract everyone read is the contract you built.

> Branching again doesn't lose your place. Spec-Kit locates the spec through the local
> `.specify/feature.json` pointer (or `SPECIFY_FEATURE_DIRECTORY`), **not** the branch name,
> and that file is local and untracked — leave it out of both PRs. If a command can't find the
> feature, `export SPECIFY_FEATURE_DIRECTORY=specs/<your-dir>`.

Three different approvals happen in a Spec-Kit run — don't confuse them:

| Approval | Who gives it | Where |
|----------|--------------|-------|
| **The spec** | Another dev | On PR 1, before you start planning |
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

Running example: [**#37070**](https://github.com/dotCMS/core/issues/37070) — a new Roles API
endpoint returning the users directly granted a role. It adds a public REST contract, so by the
ladder in §2 it's **Tier 2**. Fixing a bug follows the same shape with a different entry
command — §6, next.

### `/speckit-specify` — write the contract

```
/speckit-specify Add GET /v1/roles/{roleid}/users returning the users directly
granted a role — paginated, filterable, and carrying emailAddress. Reuse the
User shape that /v1/users/filter already returns. Inheritance is out of scope:
clients walk the ancestor chain themselves. Details in dotCMS/core#37070
```

Creates a branch and `specs/<n>-<short-name>/spec.md`, then interviews you about anything
ambiguous. The spec is **what and why** — user stories with priorities, acceptance scenarios,
success criteria, and a **Legacy Considerations** section.

Keep implementation out of it. *"Returns the users granted this role, with their email"* belongs
in the spec; *`RoleResource#loadUsersByRoleId`* does not. The bar: a reviewer should be able to
disagree with the spec without reading any code.

Name the directory for the **GitHub issue number** — `37070-roles-users-endpoint`, not Spec-Kit's
default zero-padded `001-` — so the branch matches the issue everyone else is reading
(`create-new-feature.sh --help` for `--number` / `--short-name`).

**Stop here.** Open PR 1 with the spec alone and get it **approved** (§3). Everything below
happens after that — you don't need to wait for it to merge.

### `/speckit-plan` — decide the approach

```
/speckit-plan
```

Reads your spec and produces `plan.md`: architecture, **Test Strategy**, **Constitution
Check**, **Legacy Impact**, and **ADR Alignment**. This is where the two automated gates live —
see §7. Takes optional steering:

```
/speckit-plan reuse the paginator behind /v1/users/filter rather than adding
a second one, and gate the endpoint on requiredPortlet("roles") — the payload
carries user PII
```

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

Running example: [**#36958**](https://github.com/dotCMS/core/issues/36958) — the
`Languagevariable` content type is created with `system = false`, so nothing stops anyone
deleting it, and losing it breaks i18n site-wide.

Most bug fixes are Tier 1. **This one isn't.** The fix ships a startup task that writes to the
database, which is rollback-unsafe — so by the ladder in §2 it sizes up to Tier 2. *"It's a bug"
doesn't decide the tier; what the fix touches does.*

```
/speckit-specify-fix The Languagevariable content type is created with
system=false, so the delete guard in ContentTypeFactoryImpl never fires and
the type can be removed. Losing it breaks i18n site-wide and degrades
quietly — language variables fall back to emitting the raw key. Full
analysis, repro and proposed fix in dotCMS/core#36958
```

1. **Reproduce it**, then run `/speckit-specify-fix` to write the defect spec: observed vs.
   expected behavior, and the repro.
2. **Open PR 1 with that spec alone** and get it approved (§3) — no need to wait for the merge.
3. **Branch off your spec branch, then write a failing regression test** that captures the expected
   behavior — it's your first acceptance criterion, and it's the Red gate from §7.
4. **Let the agent fix the code** until that test and the suite pass.
5. **Open PR 2 with the fix**, linking PR 1 (and any ADR, if the root cause touches a prior
   decision).

The spec is defect-framed rather than story-framed: **Problem Statement, Reproduction, Scope
of Investigation, Root-Cause Hypothesis, Fix Scope & Non-Goals, Regression Risk, Acceptance &
Verification**. *Fix Scope & Non-Goals* is the one that earns its keep — it's what stops a
bounded fix from becoming a legacy rewrite.

*Regression Risk* is where #36958 gets interesting:
`Task240306MigrateLegacyLanguageVariablesTest` **deliberately deletes this content type** to
prove the upgrade task recreates it. Protect the type and that test starts failing. Surfacing
that in the spec is what keeps it from being a surprise in PR 2 — and it's exactly the kind of
thing a reviewer catches in natural language but misses in a diff.

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
| `/speckit-analyze` | After `/speckit-tasks` — audit spec, plan and tasks against each other |
| `/speckit-converge` | Code has run ahead of `tasks.md` and you want to know what's left |
| `/speckit-taskstoissues` | The work needs splitting across people as GitHub issues |
| `/speckit-adr-context` | An ADR lookup outside the plan phase — usually automatic |
| `/speckit-constitution` | You're amending project law — rare, and a team decision |

The two spec helpers take plain English. Continuing the Roles API example from §5:

```
/speckit-clarify pagination defaults, whether filter searches email as well
as name, and what happens when roleId doesn't exist

/speckit-checklist security: PII exposure, authorization gates, and what an
unauthenticated or under-privileged caller sees
```

The other four are worth explaining properly.

### `/speckit-analyze` — audit the artifacts against each other

Reads `spec.md`, `plan.md`, `tasks.md` and the constitution, then runs six detection passes:
duplication, ambiguity, underspecification, constitution alignment, **coverage gaps**, and
inconsistency. Coverage is the one that earns its keep — it maps every task back to a
requirement and tells you which `FR-###` has no task covering it at all.

Output is a severity-graded findings list (CRITICAL / HIGH / MEDIUM / LOW, capped at 50).
**Strictly read-only** — it never edits a file. It will offer a remediation plan, but you have
to approve that before anything acts on it.

Needs `tasks.md` to exist, so it slots between `/speckit-tasks` and `/speckit-implement`.

```
/speckit-analyze
```

### `/speckit-converge` — find what the code still doesn't do

Reads the same three artifacts *and the actual codebase*, then classifies every gap it finds:

| Class | Meaning |
|-------|---------|
| `missing` | the required work is absent from the code entirely |
| `partial` | it exists but doesn't yet satisfy the requirement |
| `contradicts` | the code conflicts with stated intent or a constitution MUST |
| `unrequested` | code that nothing in the spec, plan, or tasks asked for |

It shows you the graded findings first and writes nothing. Then it **appends** a new
`## Phase N: Convergence` section to the end of `tasks.md`, numbering from the highest existing
task ID and putting constitution violations first. Append-only: it never rewrites or deletes
existing tasks, and it never deletes code — even `unrequested` findings are just surfaced.

Reach for it when the code and the task list have drifted apart: you hand-wrote a chunk, or
picked up someone else's half-finished branch, and want the gap turned back into tasks
`/speckit-implement` can finish.

```
/speckit-converge
```

### `/speckit-taskstoissues` — one GitHub issue per task

Reads `tasks.md`, checks `git remote`, and refuses outright if the remote isn't GitHub. For each
task it opens an issue titled `T001: <description>`. Re-running is safe — it scans existing issue
titles for `T###` and skips any already filed.

> ⚠️ **This needs the GitHub MCP server, which is not configured in this repo.** It calls the MCP
> `list_issues` / `create_issue` tools, not the `gh` CLI, so today it will not run. Weigh the
> output before wiring it up, too: *one issue per task* can mean dozens of issues for a single
> Tier 2 feature. It suits work genuinely being split across people, not routine features.

### `/speckit-adr-context` — look up the binding decisions

You rarely run this yourself: `/speckit-plan` fires it automatically as a `before_plan` hook
(§7). Run it manually when you want the decisions in hand *before* committing to an approach.

With no arguments it derives keywords from the current spec; with arguments it uses yours. It
queries `dotCMS/platform-adrs` through `gh`, treats **accepted** ADRs as binding and *proposed*
ones as directional, and hands `/speckit-plan` a summary for the ADR Alignment gate. Read-only,
and it never creates, edits, or commits an ADR.

```
/speckit-adr-context rest pagination user PII
```

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

- Walkthrough video: [Spec-Kit quick start (Google Drive)](https://drive.google.com/file/d/1XhQBgbME2XejZ7PHc7-xNdFtaXUFOULL/view?usp=sharing)
- Install, customizations, upgrade notes: [`.specify/CUSTOMIZATIONS.md`](../../.specify/CUSTOMIZATIONS.md)
- Project law: [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)
- Worked examples: [#37070](https://github.com/dotCMS/core/issues/37070) (feature, §5) and [#36958](https://github.com/dotCMS/core/issues/36958) (bug, §6)
- Branching & PRs: [`GIT_WORKFLOWS.md`](GIT_WORKFLOWS.md)
- Testing: [`docs/testing/`](../testing/) — [`INTEGRATION_TESTS.md`](../testing/INTEGRATION_TESTS.md)
- Legacy vs modern: [`PROGRESSIVE_ENHANCEMENT.md`](PROGRESSIVE_ENHANCEMENT.md)

---

*Maintained by the Engineering Team. Spot something stale or wrong? Open a PR against this
file and ping `#eng`. ADR questions go to `#eng-adrs`.*
