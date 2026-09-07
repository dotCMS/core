# Issue Writing Style — Human-Readable Bodies

How to write an issue body that a human can act on in 30 seconds and an agent can still
implement from. Read this before writing any issue body (CREATE Step 8).

**Contents:** [The 30-second test](#the-30-second-test) · [The lead](#the-lead) · [Length budget](#length-budget) · [What to fold](#what-to-fold-and-what-never-folds) · [Acceptance criteria](#acceptance-criteria) · [Body shapes by template](#body-shapes-by-template) · [Worked examples](#worked-examples) · [Pre-publish checklist](#pre-publish-checklist)

---

## The 30-second test

Issues are read by people who did not live the investigation: a triager sorting a backlog,
a PM deciding priority, a developer picking up work three sprints later. They read the first
screen and decide whether to keep reading.

**The test:** a reader who stops after the first 10 lines can say what is broken (or what is
being asked for), who it affects, and what it costs. If they cannot, the body fails — rewrite
the top, do not add more detail lower down.

Detail is not the problem. Detail *above* the summary is. Everything technical still belongs
in the issue; it belongs below the fold.

---

## The lead

The first line of the body is a lead sentence. It is the single most important line in the issue.

**Rules:**

- One sentence, two at most. Hard cap 40 words.
- Plain language. No class names, file paths, line numbers, stack frames, config keys, or
  internal acronyms. If a term needs the codebase to understand, it is not lead material.
- Say the consequence, not the mechanism. "Content disappears from the site" beats "the read
  path bypasses `PhaseRouter`".
- Never open with background, design-doc context, or "As part of the X migration…". Open with
  what is wrong or what is needed.

| Bad lead | Why | Rewrite |
|---|---|---|
| "The ES→OpenSearch migration design promises an automatic read fallback in Phase 2… That fallback exists and is correctly implemented in `PhaseRouter`, but the content read path never goes through `PhaseRouter`." | Opens with design context; mechanism before consequence; identifiers in the first line | "In Phase 2, if OpenSearch goes down, content searches return **nothing** instead of falling back to Elasticsearch — pages render empty with no error anywhere." |
| "This issue tracks the work required to improve the developer onboarding experience." | Says nothing. Tautological. | "Connecting an AI coding agent to a dotCMS instance takes four manual steps and only works for two editors." |
| "`AssetPickerComponent.onSelect()` iterates `files[0]` only." | Implementation detail as the headline | "Selecting multiple files in the Asset Picker uploads only the first one." |

For non-defect templates the lead answers "what is missing and why does it matter", not "what
is broken":

- **Task:** "X takes N manual steps today; this collapses it into one."
- **Spike:** "We do not know whether X is viable; without an answer we cannot plan Y."
- **Feature:** "Users cannot do X, so they work around it by Y."

---

## Length budget

Enforceable numbers. Exceeding one is the signal to fold, not to keep writing.

| Part | Budget |
|---|---|
| Lead sentence | ≤ 40 words |
| Visible prose in the problem/description section (lead included, before the first fold) | ≤ 150 words |
| Impact bullets | ≤ 4, one line each |
| Acceptance criteria | 3–7 checkboxes typical; above 7, group under sub-headings; hard cap 12 |
| Each acceptance criterion | one line, ≤ 25 words |
| Any code block, log, or table in a visible section | ≤ 10 lines — beyond that, fold it |

The whole visible body should read in under a minute. Folded content has no budget: put
everything an implementer needs in there.

---

## What to fold, and what never folds

Fold with `<details>`. GitHub collapses it for humans; the API returns the full text, so
agents and downstream tooling lose nothing.

```markdown
<details>
<summary>Root cause and call-site analysis</summary>

...stack traces, code, line references, design-doc citations...

</details>
```

**Always fold when present:**

- Stack traces and log excerpts
- Code blocks over 10 lines
- Line-number inventories ("`:1352`, `:1607`, `:1616`, …")
- Design-doc or spec citation tables
- Enumerations of every affected call site
- Alternatives considered, and why they were rejected
- Environment dumps, full request/response payloads

**Never fold** — a reviewer or QA acts directly on these:

- The lead and impact bullets
- Steps to Reproduce
- Acceptance Criteria
- Version, severity, priority, links

Give each `<summary>` a specific label — "Root cause: content read path bypasses the router",
not "Details" or "More info". The summary line is what a skimmer reads to decide whether to open it.

---

## Acceptance criteria

The refinement loop ([issue-refinement.md](issue-refinement.md)) produces rigorous criteria.
Rigour is about *precision*, not *volume* — compress before publishing.

- **One checkbox per observable outcome, not per implementation site.** "All five call sites
  route through `PhaseRouter`" is a task list, not a criterion. What a tester can observe is:
  "With OpenSearch down in Phase 2, a content search returns the same results Elasticsearch holds."
- **Merge criteria that a single test would cover.** Three checkboxes verified by one test
  are one checkbox.
- **Move rationale out of the criterion.** A checkbox is a pass/fail statement. Its
  justification, if it needs one, goes in a folded note.
- **Above 7 criteria, group under sub-headings** (`**Behaviour**`, `**Regression**`, `**Tests**`)
  so the list is scannable rather than a wall.
- Test-coverage criteria count toward the budget. Two ("the fix is covered", "the regression is
  covered") is usually enough.

---

## Body shapes by template

The GitHub template supplies the section headings — read it fresh and match it. These shapes
describe what goes *inside* each section.

**Defect**

```markdown
### Problem Statement

<lead: one sentence, plain language, consequence first>

<impact: 2-4 bullets — who is affected, how often, is there a workaround>

<details>
<summary>Root cause: <specific one-line summary></summary>

<code, stack traces, line references, design-doc citations>

</details>

### Steps to Reproduce

<numbered, minimal, no folding>

### Acceptance Criteria

<3-7 checkboxes>

### dotCMS Version / Severity / Links
```

**Task**

```markdown
### Description

<lead: what is missing or manual today, and the cost>

<3-6 lines of what this adds — a usage example if one exists>

<details>
<summary>Design notes and constraints</summary>
...
</details>

### Acceptance Criteria
### Priority
### Additional Context
```

**Spike**

Lead states the unknown and what is blocked by it. Deliverables go in acceptance criteria
(decision doc, PoC, benchmark, recommendation). Fold any prior investigation.

---

## Worked examples

### Example 1 — Defect (real issue, before and after)

**Before** — the visible body opened with roughly 450 words, two stack traces, two code
blocks, and a design-doc citation table before the reader learned what actually breaks:

```markdown
### Problem Statement

The ES→OpenSearch migration design promises an automatic **read fallback to Elasticsearch in
Phase 2**: if OpenSearch throws on a read, the error is logged at `ERROR` and the read is
retried against ES, which is still active. That fallback exists and is correctly implemented
in `PhaseRouter`, but **the content read path never goes through `PhaseRouter`**, so it never
fires.

With OpenSearch unavailable in Phase 2 (ES up and healthy), `POST /api/content/_search` does
not fall back. It returns either: ...

### Root cause

`dotCMS/src/main/java/com/dotcms/content/elasticsearch/business/ESContentFactoryImpl.java:273`
selects the provider with a bare ternary: ...
```

**After** — same information, reordered and folded:

```markdown
### Problem Statement

In Phase 2 of the OpenSearch migration, if OpenSearch goes down, content searches return
nothing instead of falling back to Elasticsearch. Pages render empty with no error anywhere.

- Affects any customer running Phase 2 — the documented safety net does not exist.
- Two failure modes: `HTTP 200` with zero results (silent — monitoring on 5xx misses it), or `HTTP 500`.
- Elasticsearch holds complete, in-sync copies the whole time. The content is not lost, just unreachable.
- No workaround short of rolling back to Phase 1.

<details>
<summary>Root cause: the content read path never reaches PhaseRouter</summary>

`ESContentFactoryImpl.java:273` picks the provider with a bare ternary, and its five call
sites invoke it directly, so `PhaseRouter.read` is never reached:

[code, stack traces, call-site list, design-doc citation table]

</details>

<details>
<summary>Second layer: OpenSearch failures are converted into valid empty results</summary>

[ERROR_HIT analysis]

</details>
```

The lead is 33 words. A triager now knows the severity from line one; the root-cause analysis
is intact for whoever picks it up.

### Example 2 — Acceptance criteria compression

**Before** (10 criteria, implementation-flavoured, rationale inline):

```markdown
- [ ] All five read call sites of `ESContentFactoryImpl.indexOperationsDelegate()` (`:1352`,
      `:1607`, `:1616`, `:1634`, `:1669`) route through `PhaseRouter.read` / `readChecked`
      rather than invoking the selected provider directly.
- [ ] The `OpenSearchException` branch in `ContentFactoryIndexOperationsOS` (`:109-121`) no
      longer converts an OS read failure into a valid empty result while in Phase 2, so the
      router has an exception to act on.
- [ ] Changing that branch does not alter behaviour for Elasticsearch
      (`ContentFactoryIndexOperationsES.java:151-161`) nor for Phase 3, where the design
      specifies no fallback and failures must propagate. Callers that currently rely on
      receiving an empty result instead of an exception are enumerated and confirmed unaffected.
...
```

**After** (7 criteria, grouped, outcomes only):

```markdown
**Behaviour**
- [ ] In Phase 2 with OpenSearch down, a content search returns the same results Elasticsearch holds — no empty result, no 500
- [ ] A missing OpenSearch index in Phase 2 is served from Elasticsearch
- [ ] Each fallback logs at `ERROR` naming the failing operation and cause

**No regression**
- [ ] Phase 0, 1 and 3 read behaviour is unchanged; Phase 3 still propagates failures
- [ ] Elasticsearch-only paths behave exactly as before

**Tests**
- [ ] Integration test: Phase 2 with the OpenSearch provider throwing returns the ES result set
- [ ] Integration test: the same failure in Phase 3 propagates instead of falling back
```

Implementation specifics (the five call sites, the `ERROR_HIT` branch, the caller audit) move
into a folded "Implementation notes" block — an implementer still gets them, a reviewer is not
made to read them to approve the issue.

---

## Pre-publish checklist

Run this against the drafted body before creating the issue. If any answer is no, fix it and
re-check.

```
- [ ] First sentence is ≤ 40 words and names the consequence, not the mechanism
- [ ] First sentence contains no file path, class name, line number, or stack frame
- [ ] Reading only the first 10 lines tells you what it is and why it matters
- [ ] Visible prose before the first fold is ≤ 150 words
- [ ] Every stack trace, long code block, and line-number inventory is inside <details>
- [ ] Every <summary> says what is inside it, specifically
- [ ] Acceptance criteria: ≤ 7, or grouped under sub-headings
- [ ] Every acceptance criterion is one line and states an observable outcome
- [ ] Steps to Reproduce and Acceptance Criteria are not folded
```
