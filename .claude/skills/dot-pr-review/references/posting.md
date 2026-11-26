# Stages 8 to 10: posting, simplifier, summary

Local mode never posts: its Stage 8 writes the report instead (`local-mode.md`), skips Stage 9, and changes the Stage 10 summary.

## Stage 8: Comment Posting

Behavior depends on the `AUTO_POST` flag parsed from the arguments:

- **`DRY_RUN = true`** → print the summary table, post nothing, ask nothing (overrides everything below)
- **`AUTO_POST = false`** (default) → summary table + one approval (this section)
- **`AUTO_POST = true`**  → non-interactive batch post (see **Auto-Post Mode** at the end of this stage)

Stage 7b must have run. Never post a finding that did not pass its anchor and duplicate gate.

**Private linked issue (`PRIVATE_LINKED = true`):** `dotCMS/core` is public and the linked issue is
a security report, so findings must not describe the defect on the PR. `auto` is ignored and 8a is
not posted.
- Write the full report to `local/security/<ISSUE_N>/code-review.md` (gitignored), not to the PR.
- The summary table is a local report; the default answer is "Post nothing". The question also offers "Post the neutral drafts": every postable finding rewritten as below, grepped, and shown in full before anything is sent.
- A finding the user chooses to post is first rewritten as a neutral WHAT-question or suggestion
  ("should this also reject X?"), with no defect class, exploit wording or private reference, and
  grepped for the banned words in the `dot-security-issue` skill's `references/disclosure.md`. Show
  each rewritten comment and post only with the user's OK.
- Findings that need the full context go to the private issue, as a comment the user approves.

### Stage 8a — AC coverage walk-through (before findings; no posting)

Run this block first, only when `RUN_AC_VALIDATOR` was true and `AUTO_POST = false`. Its purpose is to settle completeness and scope with the user — **it posts nothing itself**: an action queued here is sent only with the findings, under their one posting decision. If `AUTO_POST = true`, skip 8a entirely; auto mode only posts line-level findings.

Walk through, one item at a time via `AskUserQuestion`, in the order below. **Every item is an action, never a question for the user to classify a gap.** Before showing an item, decide what happens if nobody acts, who has to act, and draft that action: a comment to the author written as a question, an edit to the issue, or a follow-up issue. Gaps with one cause are one item (three ACs missing because one endpoint is missing is one item). A gap with no action anyone could take stays in the report and is not walked.

Every item uses this shape:

```
{Kind} {id} — [{status}] {where}

> {the AC text, the change, or the case}

**If nobody acts:** {what ships broken, untested or undecided}
**Who acts:** {the PR author | the issue owner | a follow-up issue}
**Drafted action:** {the comment as a question to the author ("Should X also ...?"), or the issue edit, or the follow-up issue's title and body}

Options:
  1. Queue the drafted action (sent with the findings, under the one posting decision)
  2. Edit the draft first
  3. Leave it in the report only
```

In order:

1. **Every `MISSING` (CONFIRMED_MISSING) AC**, then **every `PARTIAL` AC**: kind `AC`, status `MISSING|PARTIAL`, where `issue #<ISSUE_NUMBER>`. The consequence names the behavior that ships without it; the gap cites where you looked or the partial evidence.

2. **Every out-of-scope item bucketed `SCOPE_CREEP`, `HIDDEN_REQUIREMENT`, or `RISK`** (skip `ENABLER` — it needs no decision): kind `Out-of-scope`, where `{file}:{location}`. The drafted action follows the bucket: SCOPE_CREEP → a question asking the author to confirm intent or split the PR; HIDDEN_REQUIREMENT → the AC to add to the issue; RISK → the test to add, or the removal.

3. **Every test-design gap with Risk `Critical` or `High`** (NOT COVERED or PARTIAL), then **every UNSPECIFIED case**, only when `RUN_TEST_DESIGNER` was true. Medium/Low gaps are not walked; they stay in the report and the plan file. Kind `Test case {TC-###}`, status `{NOT COVERED|PARTIAL|UNSPECIFIED} {Risk} · {Level}`; the consequence is what breaks unnoticed if the case regresses, and an UNSPECIFIED case drafts the question the ACs leave open.

**Rules for 8a:**
- One item, then stop and wait for the answer — same one-by-one discipline as the findings loop.
- **Never call the `gh api ... /comments` POST** in this block, and no issue edit either. A queued action joins the findings table below as one more row and is sent only under its one posting decision; record every answer for the Stage 10 summary.
- If the user answers that an AC is in fact covered (and where) or out of scope for this PR, update `AC_COVERAGE` (or `TEST_DESIGN`, and the plan file) and recompute the Stage 7 recommendation gate before moving on.
- After 8a completes, proceed to the line-level findings loop below.
- If there were 0 MISSING/PARTIAL ACs, 0 actionable out-of-scope items, and 0 Critical/High test gaps or UNSPECIFIED cases, print "AC coverage: all criteria satisfied, no scope concerns, no critical test gaps." and skip straight to the findings loop.

### Summary table + one approval (default)

This is the default whenever `AUTO_POST` is false. The old per-finding interrogation is gone: asking N times for N findings turns a 12-finding review into 12 prompts, and the user loses the shape of the review while answering them.

Instead, present **everything at once, then ask exactly once.**

Print the full table of postable findings (post-Stage 7b):

```markdown
## Findings ready to post — PR #<N> (HEAD `<SHORT_SHA>`)

| # | Severity | File:line | Domain | Claim | Origin | Conf | Anchor |
|---|---|---|---|---|---|---|---|
| 1 | Critical | `path/to/file.ts:42` | Logic | <one-line claim> | ADDED-BY-THIS-PR | 96 | ✅ |
| 2 | Important | `path/to/other.ts:118` | Angular | <one-line claim> | PRE-EXISTING | 88 | ↷ re-anchored from 115 |
| 3 | Important | `path/to/other.ts:140` | Logic | <the additive part only> | ADDED-BY-THIS-PR | 85 | ↪ reply to @user1 |

**Not posting <M>:** <X> stale anchors dropped · <Y> duplicates (already raised by @user1, @sonarcloud) · <Z> below confidence 80

**Coverage gaps:** <COVERAGE_GAPS from Stage 4b, or "none — all assigned files reviewed">
```

Then **one** `AskUserQuestion`:

```
Post these <kept> comments on PR #<N>?

Options:
  1. Post all <kept>
  2. Post a subset — I'll give you the numbers
  3. Walk through them one at a time
  4. Post nothing (report only)
```

Rules:
- **Ask once.** Do not re-prompt per finding, do not ask for confirmation after the answer. The table gave the user everything needed to decide. When the user has already said to post (in the request, or in answer to this table), that is the one confirmation: post every surviving finding and every queued 8a action, without asking again.
- A PRE-EXISTING finding is posted with its origin in the first words ("Not introduced here, but ..."), so the author knows it is context, not a request against this change.
- Option 2 → the user replies with numbers (`1,3,7`); post exactly those, in one parallel batch.
- Option 3 → fall back to the legacy one-at-a-time loop (**Per-finding walk-through** below) for users who want it. This is now opt-in, never the default.
- Option 4 → post nothing, keep the report, go to Stage 10.
- If there are 0 postable findings, print the table's "Not posting" line, say so, and skip to Stage 10 without asking anything.
- **`DRY_RUN = true`** → print the table, print `DRY RUN — no comments posted.`, and skip straight to Stage 10. Never call `AskUserQuestion`, never POST.

### The formal verdict (asked separately)

After the posting answer (also when nothing was posted), ask **one more** `AskUserQuestion`, never folded into the posting question:

```
Submit a review verdict on PR #<N>? (Stage 7 recommends: <recommendation>)

Options:
  1. Approve
  2. Request changes
  3. Comment only
  4. No verdict
```

Send it with `gh api repos/dotCMS/core/pulls/<N>/reviews -X POST -f commit_id="<HEAD_SHA>" -f event="APPROVE|REQUEST_CHANGES|COMMENT" -f body="<two or three sentences: the recommendation and its main reason>"`. On a PR that fixes a private issue the body is neutral, like every other comment there. `auto` and `dry-run` never submit a verdict, and neither does a review of your own PR (the author is the current user): GitHub does not accept a verdict from a PR's author.

### Per-finding walk-through (opt-in — only when the user picks option 3)

Present findings strictly one at a time, highest severity first. For **each**:

```
Finding {N}/{total} — [{Severity}] `{file}:{line}` [{Domain}]

> {Issue title — what is wrong}

**Why it matters:** {Plain-language explanation tied to the actual code. What breaks, what the user/dev experiences, what standard or acceptance criterion it violates.}

**Solution:** {The concrete fix — the change to make, with a short code snippet when it helps.}

**Suggested PR comment:**
> {The comment that would be posted, written to the PR author — concise, actionable, respectful.}

Options:
  1. Post this comment on the PR
  2. Edit comment before posting (I'll provide the text)
  3. Skip this finding
```

- Present **one** finding, then **stop and wait**. Do NOT advance until answered.
- Every finding shows all four parts: issue, **Why it matters**, **Solution**, **Suggested PR comment**.

### Option 1 — Post comment

Post an inline review comment using the GitHub API. Use `HEAD_SHA` for `commit_id`:

```bash
gh api repos/dotCMS/core/pulls/<N>/comments -X POST \
  -f body="<comment_text>" \
  -f commit_id="<HEAD_SHA>" \
  -f path="<file_path>" \
  -F line=<line_number> \
  -f side="RIGHT"
```

Rules:
- `side` is `"RIGHT"` for additions/modifications, `"LEFT"` for deletions
- `line` must be a line number that appears as **added or modified** in the diff (`+` lines). If the finding targets an unchanged line, use the nearest changed line above and mention the target line in the comment body
- On failure, show the error and offer retry or skip

### Reply to an existing thread

A finding Stage 7b marked `REPLY — adds to @<user>` is posted in that comment's thread, with only the part the existing comment lacks:

```bash
gh api repos/dotCMS/core/pulls/<N>/comments/<comment_id>/replies -X POST -f body="<the additive part>"
```

### Option 2 — Edit before posting

1. Ask the user for the edited comment text via `AskUserQuestion`
2. Post using the same API call as Option 1

### Option 3 — Skip

Move to the next finding.

### Auto-Post Mode (`AUTO_POST = true`)

When the skill was invoked with the `auto` token, Stage 8 becomes non-interactive:

1. **Do NOT call `AskUserQuestion`** — no per-finding prompts.
2. Build one `gh api repos/dotCMS/core/pulls/<N>/comments` POST call per surviving finding, using the same parameters documented in **Option 1 — Post comment** above (`HEAD_SHA` as `commit_id`, `RIGHT`/`LEFT` side, `line` on a changed line).
3. **Fire all POSTs in parallel** — emit every `gh api ...` call as a separate `Bash` tool call in a single message.
4. Collect each result:
   - Success → increment `posted_count`
   - Failure → record the error with the finding index; do NOT retry. The run continues.
5. After all POSTs complete, emit a one-line `Auto-post summary: posted <X>/<total> — <Y> failed.` and list any failures with their reason. Then proceed to Stage 9 (which is itself opt-in; in auto mode, skip straight to Stage 10).
6. `edited_count` is always 0 in auto mode; `skipped_count` is 0 unless POSTs failed.

Rules that still apply in auto mode:
- The Stage 6 confidence filter (≥ 80, Haiku or Sonnet) is the gate — only those findings reach here.
- Line-not-in-diff failures are logged as failures (no "nearest line" fallback in auto mode — that would require human judgement).
- If `HEAD_SHA` fell back to `main` in Stage 2, **abort before posting**: print a warning and ask the user to re-run interactively. Anchored comments with a stale SHA are worse than no comments.

## Stage 9: Code-Simplifier (Opt-In)

**Only runs if the user explicitly opts in.**

If `AUTO_POST = true`, skip this stage entirely and proceed directly to Stage 10 — auto mode is a hands-off run and must not prompt the user.

After all comments are processed (Stage 8), if there are any findings with **Quality severity** (75-84) that touched the same file twice or more, ask:

```
I noticed {N} Quality-level findings across {M} files. Want me to propose simplifications for those files?

This will NOT edit any code — only show suggestions for you to review.

Options:
  1. Yes, show simplification proposals
  2. No, skip
```

### If the user says yes

For each file with 2+ Quality findings, run an **inline analysis** (no agent spawn):

1. Read the file
2. Write a block with proposed simplifications following this format:

    ## Simplification Proposal — `<file>`

    ### Suggestion 1: <short title>
    **Location**: `<file>:L<start>-L<end>`
    **Current**: <code fence with the code as it is today>
    **Proposed**: <code fence with the simplified version>
    **Why**: One-line rationale tied to a project standard (ANGULAR_STANDARDS / TYPESCRIPT_STANDARDS / STYLING_STANDARDS / FORM_STRUCTURE).

### Rules for Stage 9

- **Never edit code directly** — always proposals
- **Preserve functionality** — simplifications must not change behavior
- **Cite a standard** — every proposal references a specific rule from the standards docs
- **No speculation** — if you can't cite a standard, don't propose
- **Skip if the user says no** — don't nag

## Stage 10: Summary

After all findings and (optional) simplifications are processed:

```markdown
## Review Summary

- **Findings total**: {total after confidence filter}
- **Comments posted**: {posted_count}
- **Comments edited**: {edited_count}
- **Skipped**: {skipped_count}
- **Simplifications proposed**: {count or "not requested"}

**AC coverage** (issue #<ISSUE_NUMBER>, or "no linked issue"):
- {COVERED}/{total} COVERED · {PARTIAL} PARTIAL · {MISSING} MISSING · {UNTESTABLE} UNTESTABLE
- Out-of-scope items: {count by bucket, or "none"}
- Walk-through decisions: {e.g. "AC3 acknowledged as missing; extra retry logic flagged for separate PR"} (or "not discussed")

**Agents run**:
- code-reviewer: {N instances} — {N findings}
- test-reviewer: {N instances} — {N findings}
- historical-reviewer: {N findings}
- silent-failure-hunter: {activated/skipped} — {N findings}
- comment-analyzer: {activated/skipped} — {N findings}
- logic-reviewer: {activated/skipped} — {N instances} — {N findings}
- ac-validator: {activated/skipped} — {COVERED}/{total} ACs covered
- test-case-designer: {activated/skipped} — {total} cases, {NOT COVERED} not covered, {UNSPECIFIED} unspecified — plan at `<TEST_PLAN_PATH>`
- ci-reviewer / maven-reviewer / docs-reviewer: {activated/skipped each} — {N findings each}

**Confidence filter**: {raw_count} findings → {surviving_count} after ≥80 cutoff (Haiku for most domains, Sonnet for Logic, CI, Maven and Docs)

{If any comments were posted: "Comments are live on PR #<N> at commit <SHORT_SHA>."}
```

Then remove the PR head checkout, also when the run stopped early: `git -C <CORE_REPO> worktree remove --force <HEAD_DIR>` (never in local mode, where `HEAD_DIR` is the checkout itself). When `<RUN_DIR>/base` exists, remove it too, in both modes: `git -C <CORE_REPO> worktree remove --force <RUN_DIR>/base`. The run directory stays; it is in the session scratchpad.
