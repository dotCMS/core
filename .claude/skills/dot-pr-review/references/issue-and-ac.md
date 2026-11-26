# Stage 2: linked issue, acceptance criteria and the brief

Read this right after the PR data fetch (`pr-data.md`). Local mode runs steps 2 to 6 below from `issue=` (`local-mode.md`).

## Resolve the linked issue + acceptance criteria (best-effort)

The logic-reviewer (Stage 4) verifies the PR against the **intent** of the work, so the orchestrator must try to resolve the linked issue and extract its acceptance criteria. This is best-effort and **never aborts the pipeline**.

1. Find the issue number **and its repo** (`ISSUE_REPO`, default `dotCMS/core`), in this order:
   - `closingIssuesReferences` if you fetched it (`gh pr view <N> --repo dotCMS/core --json closingIssuesReferences` — covered by the `gh pr view` allow-rule). Each reference carries `repository.nameWithOwner`: use it.
   - Otherwise parse the PR body for `fixes|closes|resolves <owner>/<repo>#<n>` (cross-repo: `ISSUE_REPO = <owner>/<repo>`), then `fixes #<n>`, `closes #<n>`, `resolves #<n>`, or a bare `#<n>`. Never read the `#<n>` of an `owner/repo#<n>` reference as a core issue.
   - Otherwise parse the head branch name for `issue-<n>` / `<n>-`.
2. If an issue number is found, fetch it with `gh issue view` — it exposes the Issues-2.0 fields that the raw REST endpoint does not:

```bash
gh issue view <ISSUE_N> --repo "$ISSUE_REPO" --json number,title,body,labels,issueType,parent,subIssues
```

   **Private linked issue:** when `ISSUE_REPO` is `dotCMS/private-issues`, set `PRIVATE_LINKED = true`. The PR is a security fix in a public repo: the review still runs in full, but Stage 8 changes (see "Private linked issue" in `posting.md`).

   **Own PR on a private issue:** when `PRIVATE_LINKED` is true, also compare the PR author (`gh pr view <N> --repo dotCMS/core --json author --jq .author.login`) with the current user (`gh api user --jq .login`). If they match, set `SELF_TRIAGE = true`. The review is then the author's own check before merge: nothing is posted, and every finding feeds a local fix list. Stages 6 and 7 change; see "Own PR on a private issue" in `validation.md` and the triage table in `report-template.md`.

3. Extract the acceptance criteria from the issue body (look for an "Acceptance Criteria" / "AC" section; fall back to the full body as free-form intent). Store as `ACCEPTANCE_CRITERIA`, and write it (with the issue type and the parent line below) to `<RUN_DIR>/bundles/acceptance-criteria.md`, the file the AC-aware reviewers read.

   Then use the structural fields to set the AC bar correctly — the same PR is judged differently depending on what kind of work it is:

   - **`issueType.name`** (`Bug`, `Task`, `Spike`, `Epic`, …) — a `Bug` must show the defect is actually fixed, so a PR with no regression test is a real finding. A `Spike` is exploratory by definition: do NOT flag missing tests or incomplete ACs on one. Pass `issueType.name` into the logic-reviewer and ac-validator prompts.
   - **`parent`** — when the issue is a sub-issue, the parent's title is the wider goal. Include `parent.number` and `parent.title` in the AC block so the reviewer does not flag work as out-of-scope when it is scoped by the epic rather than by this issue.
   - **`subIssues`** — if the resolved issue is itself a parent with open children, this PR is expected to be **partial**. Record that, and do NOT let the ac-validator report MISSING for ACs that belong to sibling sub-issues.

   If `issueType` is `null` (older issues predate issue types), fall back to the labels as today.
4. If no issue is found or the fetch fails, set `ACCEPTANCE_CRITERIA = NO_LINKED_ISSUE` and continue. The logic-reviewer degrades gracefully (skips its AC-traceability pass).
5. Set the AC-validator flag: `RUN_AC_VALIDATOR = (ACCEPTANCE_CRITERIA != NO_LINKED_ISSUE)`. When a linked issue with ACs is resolved, the dedicated **`dotcms-pr-ac-validator`** runs in Stage 4 to produce a per-AC coverage table (independent of whether the PR touches logic). Also store `ISSUE_NUMBER` for the report.
6. Set the test-designer flag: `RUN_TEST_DESIGNER = RUN_AC_VALIDATOR && issueType != "Spike"`. The **`dotcms-pr-test-case-designer`** derives equivalence partitions and test cases from the ACs and maps them to existing tests. It runs whether or not the PR contains tests: a PR with no tests is where it matters most.

Each `gh` call is ONE Bash command — the Bash Command Rules in SKILL.md apply (no `cd`, no `head`, no stderr redirect).

## Brief the user on what the PR does (MUST — print before Stage 3)

Before any classification or agent work, write a **200-300 word plain-language brief** of what this PR is actually doing, and print it to the user immediately. The reviewer agents take minutes; the user should understand the change while they run.

Build it only from data already in hand: the PR title/body, the full diff, the per-file churn list, and the linked issue (title, body, ACs, `issueType`, `parent`). Do **not** launch an agent, run extra `gh` calls, or read repo files for it.

Print it in this shape:

```markdown
## What this PR does — #<N> <TITLE>

**Intent** — <1-2 sentences: the problem it solves, tied to issue #<ISSUE_NUMBER> and its type (Bug/Task/Spike…), or "no linked issue".>

**Change** — <3-5 sentences walking the actual mechanism: which components/stores/services/styles change and how behavior differs before vs after. Name the files that carry the real weight, not every touched path.>

**Scope** — <1-2 sentences: files changed, +additions/-deletions, what is production code vs test vs styling, and anything that looks like it exceeds the stated intent.>

**Watch items** — <1-2 sentences: the parts a reviewer should look hardest at (risky refactor, missing tests, wide blast radius). Observations only — no findings, no confidence scores.>
```

Rules:
- **200-300 words total.** Under 200 it is not a brief; over 300 the user stops reading. Count before printing.
- Describe the change, do **not** judge it. Verdicts belong in Stage 7 after the agents and confidence validation have run.
- Explain the mechanism in terms a reviewer who has never opened these files can follow — no bare file-path lists.
- Say so explicitly when the diff does not match the PR description or the linked issue; that mismatch is the single most useful thing an early brief can surface.

Store the brief as `PR_BRIEF` — Stage 7 reuses it verbatim as the report's Summary section.

