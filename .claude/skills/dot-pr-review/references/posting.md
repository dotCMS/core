# Stages 8 to 10: posting, simplifier, summary

Your own work (`LOCAL` or `OWN_PR`) never posts: its Stage 8 writes the report instead (`local-mode.md`), skips Stage 9, and changes the Stage 10 summary.

## Stage 8: Comment Posting

- **`DRY_RUN`** → print the summary table and `DRY RUN — no comments posted.`, post nothing, ask nothing, go to Stage 10. This overrides everything below.
- **`AUTO_POST` false** (default) → 8a, then the finding cards, the table and one approval.
- **`AUTO_POST` true** → skip 8a and post non-interactively (**Auto-Post Mode**, at the end of this stage).

Only findings that passed Stage 7b are posted.

**Private linked issue (`PRIVATE_LINKED = true`):** `dotCMS/core` is public and the linked issue is a security report, so the PR carries nothing that describes the defect. `auto` is ignored and 8a is not posted.
- Write the full report to `local/security/<ISSUE_N>/code-review.md` (gitignored), not to the PR, and the findings file to `local/security/<ISSUE_N>/pr-<N>-findings.md` instead of `local/code-review/`: the cards never leave that folder, and each card's proposed comment is its neutral draft, or "withheld" when the banned-word list is unreachable.
- The neutral drafts need the banned-word list in the `dot-security-issue` plugin skill's `references/disclosure.md`. Core has no copy of it: find the installed plugin's file and read it. When it cannot be read, offer no neutral drafts and post nothing, and the report says the drafts were withheld because the banned-word list was unreachable.
- The summary table is a local report and the default answer is "Post nothing". With the list read, the question also offers "Post the neutral drafts": every postable finding rewritten as below, grepped, and shown in full before anything is sent.
- A finding the user chooses to post is first rewritten as a neutral WHAT-question or suggestion ("should this also reject X?"), with no defect class, exploit wording or private reference, and grepped for every word of that list. Show each rewritten comment and post only with the user's OK.
- Findings that need the full context go to the private issue, as a comment the user approves.

### Stage 8a — AC coverage walk-through (before findings; posts nothing itself)

Runs only when `RUN_AC_VALIDATOR` was true and `AUTO_POST` is false. It settles completeness and scope with the user; an action queued here is sent with the findings, under their one posting decision.

Walk the items one at a time with `AskUserQuestion`, in the order below. **Every item is a drafted action**, never a question asking the user to classify a gap: before showing it, decide what happens if nobody acts, who has to act, and draft that action (a comment to the author written as a question, an edit to the issue, or a follow-up issue). Gaps with one cause are one item (three ACs missing because one endpoint is missing is one item). A gap no one could act on stays in the report and is not walked.

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

2. **Every out-of-scope item bucketed `SCOPE_CREEP`, `HIDDEN_REQUIREMENT`, or `RISK`** (`ENABLER` needs no decision): kind `Out-of-scope`, where `{file}:{location}`. The drafted action follows the bucket: SCOPE_CREEP → a question asking the author to confirm intent or split the PR; HIDDEN_REQUIREMENT → the AC to add to the issue; RISK → the test to add, or the removal.

3. **Every test-design gap with Risk `Critical` or `High`** (NOT COVERED or PARTIAL), then **every UNSPECIFIED case**, only when `RUN_TEST_DESIGNER` was true. Medium/Low gaps stay in the report and the plan file. Kind `Test case {TC-###}`, status `{NOT COVERED|PARTIAL|UNSPECIFIED} {Risk} · {Level}`; the consequence is what breaks unnoticed if the case regresses, and an UNSPECIFIED case drafts the question the ACs leave open.

**Rules for 8a:**
- One item, then wait for the answer.
- 8a sends nothing: no `gh api ... /comments` POST, no issue edit. A queued action joins the findings table below as one more row. Record every answer for the Stage 10 summary.
- When the user answers that an AC is covered (and where) or out of scope for this PR, update `AC_COVERAGE` (or `TEST_DESIGN` and the plan file) and recompute the Stage 7 recommendation gate before the next item.
- With 0 MISSING/PARTIAL ACs, 0 actionable out-of-scope items, and 0 Critical/High test gaps or UNSPECIFIED cases, print "AC coverage: all criteria satisfied, no scope concerns, no critical test gaps." and go to the findings.

### Finding cards, the table and one approval (default)

This is the default when someone else's PR is reviewed with no flag. `auto` posts without cards (Auto-Post Mode), `dry-run` prints the table only, and your own work writes its report instead (`local-mode.md`).

Before the one question, the user gets what each decision needs: a **card** per finding, the **Not posting** list, and the table. All findings share one `#n` numbering, the recommended ones first. Queued 8a actions join as rows, recommended POST.

**The findings file.** Write every card and the Not posting section to `local/code-review/pr-<N>-findings.md` (`mkdir -p local/code-review` first); nothing of it goes to the PR. The chat shows the table, the question and the file path, and the cards too when there are 5 findings or fewer.

**The card** is the one shape a finding is shown in, here and when walking them one at a time:

```markdown
### #<n> · <Severity> · <Domain> · [`<path>:<line>`](https://github.com/dotCMS/core/blob/<HEAD_SHA>/<path>#L<line-1>-L<line+1>)

**What this code does:** <1-2 sentences on what the changed code does in context, from reading the file at <HEAD_DIR>>
**Problem:** <the claim>
**Evidence:** `<the literal line(s) at the anchor>` — <what was checked to confirm it, e.g. "no permission check up the call chain">
**Why it matters:** <the concrete consequence: what breaks, who sees it> — <the rule, standard section or AC it violates>
**Origin:** <ADDED-BY-THIS-PR | PRE-EXISTING>
**Confidence:** reviewer <conf> · validator <score> — <the validator's REASON line>
**Recommendation:** <POST | POST AS REPLY to @user | DON'T POST> — <one-line reason>
**Proposed comment:**
> <the exact text that would be posted, written to the author as a question or suggestion>

**Suggested fix:** <a short snippet, when it helps>
```

**The recommendation**, per finding:
- **POST**: added by this PR, validated at 80 or more, raised by nobody yet. A pre-existing finding this PR makes reachable or worse is POST too, and its comment opens with its origin ("Not introduced here, but ...").
- **POST AS REPLY to @user**: Stage 7b found an existing comment it adds to; the proposed comment holds only the additive part.
- **DON'T POST**, with the reason: pre-existing and outside what this PR changes or makes reachable; already said (by whom, where); a style point the linter already catches; below 80; a stale anchor.

**Not posting** lists every finding left out under the same `#n`, each with its reason: below 80 (both scores and the validator's reason), a duplicate (`@user at path:line`), a stale anchor. In the findings file each one also gets its card.

**The table** stays the overview:

```markdown
## Findings — PR #<N> (HEAD `<SHORT_SHA>`)

| # | Severity | File:line | Domain | Claim | Origin | Conf / Val | Anchor | Recommendation |
|---|---|---|---|---|---|---|---|---|
| 1 | Critical | `path/to/file.ts:42` | Logic | <one-line claim> | ADDED-BY-THIS-PR | 96 / 90 | ✅ | POST |
| 2 | Important | `path/to/other.ts:140` | Logic | <the additive part only> | ADDED-BY-THIS-PR | 85 / 82 | ↪ reply to @user1 | POST AS REPLY to @user1 |
| 3 | Important | `path/to/other.ts:118` | Angular | <one-line claim> | PRE-EXISTING | 88 / 85 | ↷ re-anchored from 115 | DON'T POST — pre-existing, out of scope |

**Not posting:** #4 below 80 (75 / 60) · #5 duplicate of @user1 at `path:120` · #6 stale anchor

**Coverage gaps:** <COVERAGE_GAPS from Stage 4b, or "none — all assigned files reviewed">

**Findings file:** `local/code-review/pr-<N>-findings.md`
```

Then **one** `AskUserQuestion`, the recommendation first:

```
Post on PR #<N>? <k> recommended (#1, #2). Cards: local/code-review/pr-<N>-findings.md

Options:
  1. Post the recommended ones (Recommended)
  2. Pick numbers — any #n, Not posting ones included
  3. Walk through them one at a time
  4. Post nothing (report only)
```

Rules:
- **Ask once.** When the user has already said to post (in the request, or in answer to this question), that is the one confirmation: post the recommended findings and every queued 8a action without asking again.
- Option 1 → every POST and POST AS REPLY finding and every queued 8a action, in one parallel batch.
- Option 2 → the user replies with numbers (`1,3,7`). The recommended ones among them go in one parallel batch; each DON'T POST number shows its card again and is posted only when the user confirms that one.
- Option 3 → the cards one at a time, highest severity first, each waiting for its answer: `1. Post this comment` · `2. Edit the comment first (I'll provide the text)` · `3. Skip`. An edit is asked with `AskUserQuestion`, then posted the same way.
- Option 4 → post nothing, keep the report and the findings file, go to Stage 10.
- With no finding at all, say so and go to Stage 10 without asking. With none recommended but some under Not posting, the table and the question still come.

### The formal verdict (asked separately)

After the posting answer (also when nothing was posted), ask **one more** `AskUserQuestion`, separate from the posting question:

```
Submit a review verdict on PR #<N>? (Stage 7 recommends: <recommendation>)

Options:
  1. Approve
  2. Request changes
  3. Comment only
  4. No verdict
```

Send it with `gh api repos/dotCMS/core/pulls/<N>/reviews -X POST -f commit_id="<HEAD_SHA>" -f event="APPROVE|REQUEST_CHANGES|COMMENT" -f body="<two or three sentences: the recommendation and its main reason>"`. On a PR that fixes a private issue the body is neutral, like every other comment there. `auto` and `dry-run` never submit a verdict.

### Posting a comment

An inline review comment, with `HEAD_SHA` as `commit_id`:

```bash
gh api repos/dotCMS/core/pulls/<N>/comments -X POST \
  -f body="<comment_text>" \
  -f commit_id="<HEAD_SHA>" \
  -f path="<file_path>" \
  -F line=<line_number> \
  -f side="RIGHT"
```

- `side` is `"RIGHT"` for additions/modifications, `"LEFT"` for deletions.
- `line` must be an **added or modified** line of the diff (`+` lines). For a finding on an unchanged line, use the nearest changed line above and name the target line in the body.
- On failure, show the error and offer to retry or skip.

### Reply to an existing thread

A finding Stage 7b marked `REPLY — adds to @<user>` is posted in that comment's thread, with only the part the existing comment lacks:

```bash
gh api repos/dotCMS/core/pulls/<N>/comments/<comment_id>/replies -X POST -f body="<the additive part>"
```

### Auto-Post Mode (`AUTO_POST = true`)

1. No `AskUserQuestion`.
2. One `gh api repos/dotCMS/core/pulls/<N>/comments` POST per surviving finding, with the parameters of **Posting a comment** above.
3. **All POSTs in parallel**: every `gh api ...` call as a separate `Bash` tool call in a single message.
4. Collect each result: success → `posted_count`; failure → record the error with the finding index, no retry, the run continues.
5. Then print `Auto-post summary: posted <X>/<total> — <Y> failed.` with each failure's reason, and go to Stage 10 (Stage 9 is skipped).
6. `edited_count` is 0; `skipped_count` is 0 unless POSTs failed.

The Stage 6 filter (80 or more) is the gate. A line-not-in-diff failure is logged as a failure, with no "nearest line" fallback (that takes human judgement). If `HEAD_SHA` fell back to `main` in Stage 2, **abort before posting** and ask the user to re-run interactively: comments anchored to a stale SHA are worse than none.

## Stage 9: Code-Simplifier (Opt-In)

Skipped under `AUTO_POST`. Otherwise, when **Quality**-severity findings touch the same file twice or more, ask:

```
I noticed {N} Quality-level findings across {M} files. Want me to propose simplifications for those files?

This will NOT edit any code — only show suggestions for you to review.

Options:
  1. Yes, show simplification proposals
  2. No, skip
```

On yes, for each file with 2+ Quality findings, read it and write proposals inline (no agent), in this shape:

    ## Simplification Proposal — `<file>`

    ### Suggestion 1: <short title>
    **Location**: `<file>:L<start>-L<end>`
    **Current**: <code fence with the code as it is today>
    **Proposed**: <code fence with the simplified version>
    **Why**: One-line rationale tied to a project standard (ANGULAR_STANDARDS / TYPESCRIPT_STANDARDS / STYLING_STANDARDS / FORM_STRUCTURE).

Proposals only, never edits; each keeps behavior the same and cites a specific rule from the standards docs, and a change no rule backs is not proposed. On no, move on.

## Stage 10: Summary

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

Then remove the PR head checkout, also when the run stopped early: `git -C <CORE_REPO> worktree remove --force <HEAD_DIR>` (never in local mode, where `HEAD_DIR` is the checkout itself). When `<RUN_DIR>/base` exists, remove it too, in both modes: `git -C <CORE_REPO> worktree remove --force <RUN_DIR>/base`. The run directory stays in the session scratchpad.
