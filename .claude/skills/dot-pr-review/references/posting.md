# Stages 8 to 10: posting, simplifier, summary

Your own work (`LOCAL` or `OWN_PR`) never posts: its Stage 8 writes the report instead (`local-mode.md`), skips Stage 9, and changes the Stage 10 summary.

## Stage 8: Comment Posting

- **`DRY_RUN`** → print the summary table and `DRY RUN — no comments posted.`, post nothing, ask nothing, go to Stage 10. This overrides everything below.
- **`AUTO_POST` false** (default) → 8a, then the summary table and one approval.
- **`AUTO_POST` true** → skip 8a and post non-interactively (**Auto-Post Mode**, at the end of this stage).

Only findings that passed Stage 7b are posted.

**Private linked issue (`PRIVATE_LINKED = true`):** `dotCMS/core` is public and the linked issue is a security report, so the PR carries nothing that describes the defect. `auto` is ignored and 8a is not posted.
- Write the full report to `local/security/<ISSUE_N>/code-review.md` (gitignored), not to the PR.
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

### Summary table + one approval (default)

Present every postable finding at once, then ask exactly once: asking per finding turns a 12-finding review into 12 prompts, and the user loses the shape of the review.

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
- **Ask once.** When the user has already said to post (in the request, or in answer to this table), that is the one confirmation: post every surviving finding and every queued 8a action without asking again.
- A PRE-EXISTING finding opens with its origin ("Not introduced here, but ..."), so the author reads it as context, not as a request against this change.
- Option 2 → the user replies with numbers (`1,3,7`); post exactly those, in one parallel batch.
- Option 3 → the **Per-finding walk-through** below.
- Option 4 → post nothing, keep the report, go to Stage 10.
- With 0 postable findings, print the table's "Not posting" line, say so, and go to Stage 10 without asking.

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

### Per-finding walk-through (only when the user picks option 3)

One finding at a time, highest severity first; wait for the answer before the next. Each shows all four parts:

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

Option 1 posts it (below); option 2 asks for the edited text with `AskUserQuestion`, then posts it the same way; option 3 moves to the next finding.

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
