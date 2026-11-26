# Stage 2: linked issue, acceptance criteria and the brief

Read this right after the PR data fetch (`pr-data.md`). Local mode runs steps 2 to 6 below from `issue=` (`local-mode.md`).

## Resolve the linked issue + acceptance criteria (best-effort, never aborts)

The logic reviewer, the ac-validator and the test-case designer judge the PR against the intent of the work, which the linked issue states.

1. Find the issue number **and its repo** (`ISSUE_REPO`, default `dotCMS/core`), in this order:
   - `closingIssuesReferences` (`gh pr view <N> --repo dotCMS/core --json closingIssuesReferences`); each reference carries `repository.nameWithOwner`: use it.
   - Otherwise the PR body: `fixes|closes|resolves <owner>/<repo>#<n>` (cross-repo: `ISSUE_REPO = <owner>/<repo>`), then `fixes #<n>`, `closes #<n>`, `resolves #<n>`, or a bare `#<n>`. The `#<n>` of an `owner/repo#<n>` reference belongs to that repo, not to core.
   - Otherwise the head branch name: `issue-<n>` / `<n>-`.
2. Fetch it with `gh issue view`, which exposes the Issues-2.0 fields the REST endpoint lacks:

```bash
gh issue view <ISSUE_N> --repo "$ISSUE_REPO" --json number,title,body,labels,issueType,parent,subIssues
```

   **Private linked issue:** when `ISSUE_REPO` is `dotCMS/private-issues`, set `PRIVATE_LINKED = true`. The PR is a security fix in a public repo: the review runs in full, and Stage 8 changes ("Private linked issue" in `posting.md`).

   **Own PR on a private issue:** when `PRIVATE_LINKED` is true, compare the PR author (`gh pr view <N> --repo dotCMS/core --json author --jq .author.login`) with the current user (`gh api user --jq .login`). If they match, set `SELF_TRIAGE = true`: the review is the author's own check before merge, nothing is posted, and every finding feeds a local fix list ("Own PR on a private issue" in `validation.md`, the triage table in `report-template.md`).

3. Extract the acceptance criteria from the issue body (an "Acceptance Criteria" / "AC" section; else the full body as free-form intent). Store them as `ACCEPTANCE_CRITERIA` and write them, with the issue type and the parent line below, to `<RUN_DIR>/bundles/acceptance-criteria.md`, the file the AC-aware reviewers read. The structural fields set the bar:

   - **`issueType.name`** (`Bug`, `Task`, `Spike`, `Epic`, …): a `Bug` must show the defect is fixed, so a PR with no regression test is a real finding. A `Spike` is exploratory: missing tests and incomplete ACs are not findings on one. Pass `issueType.name` into the logic-reviewer and ac-validator prompts. When `issueType` is `null` (older issues), use the labels.
   - **`parent`**: on a sub-issue, the parent's title is the wider goal. Include `parent.number` and `parent.title` in the AC block, so work the epic scopes is not flagged as out of scope.
   - **`subIssues`**: an issue with open children expects a **partial** PR. Record that; the ac-validator reports ACs that belong to sibling sub-issues as out of this PR, never as MISSING.
4. With no issue found, or a failed fetch, set `ACCEPTANCE_CRITERIA = NO_LINKED_ISSUE` and continue; the logic reviewer then skips its AC-traceability pass.
5. `RUN_AC_VALIDATOR = (ACCEPTANCE_CRITERIA != NO_LINKED_ISSUE)`. Store `ISSUE_NUMBER` for the report.
6. `RUN_TEST_DESIGNER = RUN_AC_VALIDATOR && issueType != "Spike"`, whether or not the PR contains tests: a PR with no tests is where the test plan matters most.

## Brief the user on what the PR does (print before Stage 3)

Write a **200-300 word plain-language brief** of what the PR does and print it right away; the reviewers take minutes, and the user reads it meanwhile. Build it from data already in hand (PR title and body, diff, churn list, linked issue), with no agent, extra `gh` call or file read.

```markdown
## What this PR does — #<N> <TITLE>

**Intent** — <1-2 sentences: the problem it solves, tied to issue #<ISSUE_NUMBER> and its type (Bug/Task/Spike…), or "no linked issue".>

**Change** — <3-5 sentences walking the actual mechanism: which components/stores/services/styles change and how behavior differs before vs after. Name the files that carry the real weight, not every touched path.>

**Scope** — <1-2 sentences: files changed, +additions/-deletions, what is production code vs test vs styling, and anything that looks like it exceeds the stated intent.>

**Watch items** — <1-2 sentences: the parts a reviewer should look hardest at (risky refactor, missing tests, wide blast radius). Observations only — no findings, no confidence scores.>
```

- 200-300 words: count before printing.
- Describe the change; the verdict waits for Stage 7.
- Explain the mechanism so a reviewer who never opened these files can follow it, in sentences rather than path lists.
- Say so explicitly when the diff does not match the PR description or the linked issue: that mismatch is the most useful thing the brief can surface.

Store it as `PR_BRIEF`; Stage 7 reuses it verbatim as the report's Summary.
