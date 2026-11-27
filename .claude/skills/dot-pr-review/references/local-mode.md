# Your own work: local mode and your own open PR

The "your own work" use, entered two ways:
- **`LOCAL`** (`/dot-pr-review local`): reviews the working tree of the current branch, before or after its PR exists.
- **`OWN_PR`** (`/dot-pr-review <N>` on a PR you opened): reviews the PR head, checked out from GitHub, against the PR base.

Both run the same reviewers, standards and validation as a PR review, and write the report to disk. Read this file once, right after parsing; it lists everything that differs from reviewing someone else's PR, stage by stage, and the stage references apply wherever it does not replace them.

- **Read-only on GitHub:** nothing is commented, reviewed, labeled or edited, and nothing is asked. The `gh` calls are reads: the PR's data, comments and reviews (`OWN_PR`, or the branch's open PR under `LOCAL`), `gh issue view` for the issue, and the historical reviewer's searches of past PRs.
- With a private issue (`dotCMS/private-issues#N`), the private-issue rules of this skill hold: the report stays local, and nothing describing the defect leaves the machine.
- `SELF_TRIAGE = PRIVATE_LINKED`: the code under review is always the caller's own, so with a private issue the "Own PR on a private issue" rules of `validation.md` apply; otherwise validation and the filter run as for someone else's PR.

| Stage | `LOCAL` | `OWN_PR` |
|---|---|---|
| Preflight | unchanged | unchanged |
| 1 Eligibility | skipped | skipped: a draft or a PR you already reviewed is still worth reviewing for yourself |
| 2 Fetch | "Local Stage 2" below: branch, base, HEAD, files and diff from local git, the issue from `issue=`, and the existing-feedback digest when the branch has an open PR | `pr-data.md` Stage 2 and `issue-and-ac.md` as written (PR head checked out as `HEAD_DIR`, existing-feedback digest, linked issue), then "Output paths" below |
| 3 Classify | on the local file list and diff; `CI_GATE_EVIDENCE` lists the triggers only (no run yet) | unchanged |
| 4, 4b Reviewers, recovery | same agents, bundles, sidecars and recovery; slices and prompts as "Stage 4" below | unchanged |
| 5, 6 Consolidate, validate | head check and anchors on the working tree ("Stages 5 and 6" below) | unchanged |
| 7 Report | the local report shape ("Stage 7" below) | the local report shape |
| 7b Verification | anchors on the working tree; duplicates of the open PR's comments dropped; repeats of the previous local report | `anchors.md` 7b.1 to 7b.4, then repeats of the previous local report ("Stage 7b" below) |
| 8 Posting | writes the report to `REPORT_PATH`, prints the summary table, posts nothing, asks nothing | same |
| 9 Simplifier | skipped: the caller decides the fixes | skipped |
| 10 Summary | the report path instead of the posting counts; the effective-POM base checkout is removed, the working tree never | the same summary; `HEAD_DIR` and the base checkout are removed as in `posting.md` |

`RUN_DIR` is `<scratchpad>/review-local-<BRANCH_SLUG>` for `LOCAL` (with `-2`, `-3` when it exists) and `<scratchpad>/review<N>` for `OWN_PR`. For `LOCAL`, `HEAD_DIR` is `CORE_REPO` itself: the working tree is what is reviewed, so no PR head is checked out or removed.

## Local Stage 2: branch, diff and issue

1. **HEAD and branch.**

   ```bash
   git rev-parse HEAD
   git rev-parse --short HEAD
   git branch --show-current
   ```

   Store `HEAD_SHA`, `SHORT_SHA` and `BRANCH` (`detached` when the last one prints nothing). `BRANCH_SLUG` is `BRANCH` with every character outside `A-Za-z0-9._-` replaced by `-`.

   **An open PR for the branch.** Reviewers may already have commented on it, and the review must not re-raise their points:

   ```bash
   gh pr list --repo dotCMS/core --head <BRANCH> --state open --json number --jq '.[].number'
   ```

   When it prints exactly one number, store it as `LOCAL_PR`, and right after step 4 write `<RUN_DIR>/bundles/existing-feedback.md` from that PR as "Existing feedback digest" in `pr-data.md` describes, adding one line per non-empty review body (`@user (<state>): <first 300 characters>`) from `gh api --paginate repos/dotCMS/core/pulls/<LOCAL_PR>/reviews --jq '.[] | select(.body != "") | {user: .user.login, state, body}'`. No number (or several): no digest.

2. **Base.** With `base=<ref>`, `git rev-parse --verify '<ref>^{commit}'`. Without it:

   ```bash
   git symbolic-ref --short refs/remotes/origin/HEAD
   git fetch origin <default branch>
   git merge-base HEAD origin/<default branch>
   ```

   The default branch is the first output without `origin/` (`main` if the command fails). Store the result as `BASE_SHA`. If it does not resolve, abort: "Cannot resolve the base `<ref>`."

3. **Changed files**, with change type and churn:

   ```bash
   git diff --name-status -M <BASE_SHA>
   git diff --numstat -M <BASE_SHA>
   git ls-files --others --exclude-standard
   ```

   Map `A` to `ADDED`, `M` to `MODIFIED`, `D` to `DELETED` and `R<score>` to `RENAMED` (the new path), and take `additions` / `deletions` from `--numstat`. `git diff` does not show untracked files, so add each path the third command prints as `ADDED`, with its line count as `additions`. Leave out lock files, `**/*.snap` and `**/dist/**`, as PR mode's `--exclude` does. This list replaces the `gh pr view --json files` output everywhere; write its paths to `<RUN_DIR>/changed-files.txt`.

4. **The diff.**

   ```bash
   git diff -M <BASE_SHA> -- . ':(exclude,glob)**/pnpm-lock.yaml' ':(exclude,glob)**/yarn.lock' ':(exclude,glob)**/package-lock.json' ':(exclude,glob)**/*.snap' ':(exclude,glob)**/dist/**'
   ```

   For each untracked file, also `git diff --no-index -- /dev/null <path>` (exit code 1 is its normal result when the file has content). Together they are `FULL_DIFF`, which replaces the `gh pr diff` output everywhere (Stage 3's trigger grep, the brief). If the file list is empty, print `Nothing to review: no changes between <BASE_SHA> and the working tree.` and stop.

5. **Title and intent.** `git log --format='%s%n%b' <BASE_SHA>..HEAD` gives the commit messages, which stand in for the PR title and body. `TITLE` is the issue title when `issue=` is given, else the first commit subject, else `BRANCH`.

6. **The issue**, only with `issue=`. Resolve `ISSUE_REPO` and `ISSUE_NUMBER` from the token (a bare number is `dotCMS/core`), then run steps 2 to 6 of "Resolve the linked issue + acceptance criteria" (`issue-and-ac.md`) unchanged, with `gh issue view` only. When `<OUT_DIR>/acceptance-criteria.md` exists and its first line starts with `Status: approved` (the ACs `/solve-issue` confirmed with the developer), those ACs are the AC index instead of the issue body's, numbered `AC<n>` as `issue-and-ac.md` step 3 writes them. When the fetch fails but that approved file exists (the Security Pipeline's host cannot read `dotCMS/private-issues`), say so in one line and use the file all the same, with `PRIVATE_LINKED` set from the token. Without `issue=`, or when the fetch fails with no such file (say so in one line), `ACCEPTANCE_CRITERIA = NO_LINKED_ISSUE` and the report says why AC coverage was not evaluated.

7. **Output paths** (both entries). `OUT_DIR` is `out=`, else `local/security/<ISSUE_NUMBER>/` when `PRIVATE_LINKED`, else `local/code-review/`; create it with `mkdir -p`. `REPORT_PATH = <OUT_DIR>/local-<BRANCH_SLUG>-<SHORT_SHA>-report.md`. `PREVIOUS_REPORT` is the newest `local-<BRANCH_SLUG>-*-report.md` in `ls -t <OUT_DIR>`, if any (it may be `REPORT_PATH` itself when HEAD has not moved; read it now, before it is overwritten). For `OWN_PR`, `BRANCH` is the PR's `headRefName` and `SHORT_SHA` is `git -C <CORE_REPO> rev-parse --short <HEAD_SHA>`.

Then write and print the brief as `issue-and-ac.md` describes, headed `## What this change does — <BRANCH>`, from the commit messages, the diff, the file list and the issue. `OWN_PR` prints the PR-mode brief.

## Stage 4: slices and prompts (`LOCAL`)

- Build the slices from local git, never from a saved diff: `diff-slices.sh --out <RUN_DIR>/slices --expect <RUN_DIR>/changed-files.txt --git <BASE_SHA> <RUN_DIR>/slices/*.txt` (no `--head`: the working tree, untracked files included).
- Without `LOCAL_PR` there is no existing-feedback digest; drop that line from the Inputs block.
- In every template, read `PR #<N> '<TITLE>'` as `the local branch <BRANCH> '<TITLE>'`, `PR diff` as `Local diff`, and `linked issue #<ISSUE_NUMBER>` as `issue <ISSUE_REPO>#<ISSUE_NUMBER>`, and add one line: "This is a local review before any PR: the code under review is the working tree; there is no PR to look up."

## Stages 5 and 6 (`LOCAL`)

- **Head check:** instead of `gh pr view`, `git rev-parse HEAD`. If it moved (the developer committed during the review), set `HEAD_SHA` to it and rebuild the slices as above; the anchor check that follows runs on the working tree either way.
- **Anchors:** `verify-anchors.sh ... --worktree` instead of `--rev <HEAD_SHA>`.

Both entries: `TEST_PLAN_PATH` is `<OUT_DIR>/local-<BRANCH_SLUG>-<SHORT_SHA>-test-plan.md`, next to the report, whatever the issue.

## Stage 7: the report (both entries)

The same report as `report-template.md`, written to `REPORT_PATH` in Stage 8, with these changes:
- Title `# Local review: <BRANCH> — <TITLE>` (`OWN_PR`: `# Local review: <BRANCH> (PR #<N>) — <TITLE>`). The `HEAD commit` line becomes `**Reviewed**: <BASE_SHA>..<HEAD_SHA>`, plus `, uncommitted changes included` when `LOCAL` and `git status --porcelain` prints anything. `**Linked issue**` names `<ISSUE_REPO>#<ISSUE_NUMBER>`, or "none (no `issue=` given): AC coverage and test design not run". For `LOCAL` the CI exercise line lists each changed workflow's triggers and reads "not exercised yet; check the PR's run".
- `**File**` rows are plain `path:line`: the commit may exist only locally, so a GitHub link would not resolve. Each finding adds `**Why it matters**` between `**Issue**` and `**Fix**`, and a finding also in `PREVIOUS_REPORT` gets ` [repeat]` after its title.
- The recommendation reads **Ready for the PR**, **Ready after the listed fixes** or **Fix first** in place of Approve, Approve with Comments and Request Changes, with the same gates.

## Stage 7b: verification

Nothing is posted, but every anchor is still checked.

**`LOCAL`**, against the working tree. 7b.1 to 7b.4 of `anchors.md` become:

1. **HEAD.** `git rev-parse HEAD`. If it differs from `HEAD_SHA`, the branch moved during the review: set `HEAD_SHA` to it and say so in the gate line.
2. **Anchors**, with the 7b.3 table, against the working tree and the per-file local diff:

   ```bash
   bash .claude/skills/dot-pr-review/scripts/verify-anchors.sh --findings <RUN_DIR>/merged/anchored.jsonl --base <BASE_SHA> --worktree --root <CORE_REPO> --out <RUN_DIR>/merged/postable.jsonl
   ```

   It reads each file from the working tree and takes the changed ranges from `git diff --unified=0 <BASE_SHA> -- <path>`, one call per path; every line of an untracked file is changed. By hand: `sed -n '<line>p' <path>`, then `grep -n -F -- '<evidence>' <path>` when the line does not match.
3. **Duplicates, with `LOCAL_PR`.** Refetch its inline comments, review bodies and PR-level comments with the three `gh api` calls of `anchors.md` 7b.1 (on `LOCAL_PR`), and drop the findings 7b.4 calls duplicates. The comments' lines refer to the PR head, which may trail the working tree, so judge by the claim and the code around it more than by the line number. An overlap stays in the report with its additive part, marked "adds to @<user>".

**`OWN_PR`**: 7b.1 to 7b.4 of `anchors.md` as written (head and comments refetched, anchors re-checked at the head, duplicates of existing comments dropped). An overlap stays in the report with its additive part, marked "adds to @<user>", since nothing is replied.

**Both: repeats.** A finding that matches one in `PREVIOUS_REPORT` (same path, within ±3 lines, same claim) is not dropped: it was found before and is still in the code, so it is still open. Keep it and mark it `[repeat]`.

Then print:

```
Local verification: <kept>/<total> findings anchored — <stale> stale anchors dropped, <reanchored> re-anchored, <dupes> duplicates of existing comments, <repeats> repeats of <PREVIOUS_REPORT or "no previous report">, HEAD <unchanged|moved 1a2b3c4 → 5d6e7f8>.
```

## Stage 8: write the report, post nothing (both entries)

No `AskUserQuestion`, and 8a does not run; the caller acts on the report.
- Write the Stage 7 report to `REPORT_PATH`. Every finding carries its severity, `path:line`, its origin (ADDED-BY-THIS-PR or PRE-EXISTING), what is wrong, why it matters and the suggested fix, with the reviewer confidence and the validator score. The AC coverage and test design sections carry the gaps 8a would have walked.
- Print the table of `posting.md` without its Recommendation column, headed `## Findings — <BRANCH> (HEAD <SHORT_SHA>)`, then the "Not in report" line (stale anchors, duplicates, below the confidence cut-off), the coverage gaps and `Report: <REPORT_PATH>`. Go to Stage 10.

## Stages 9 and 10

Stage 9 is skipped. In the Stage 10 summary, replace the `Comments posted`, `Comments edited` and `Skipped` lines with `**Report**: <REPORT_PATH>`, and print `Nothing was posted (your own work).` instead of the last line.

The caller acts on the report: every finding is fixed or deferred with the reason. Re-review the fixes with `local base=<the HEAD_SHA the report names>`.
