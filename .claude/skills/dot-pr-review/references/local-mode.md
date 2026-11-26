# Local mode

The "your own work" use: reviews the local branch, before or after its PR exists, with the same reviewers, standards and validation as a PR review, and writes the report to disk. Read this file once, right after parsing `local`; it lists everything that differs from PR mode, stage by stage, and the stage references apply wherever it does not replace them.

- **Read-only on GitHub:** the only `gh` calls are `gh issue view` for `issue=` and the historical reviewer's searches of past PRs. Nothing is commented, reviewed, labeled or edited, and nothing is asked.
- With a private issue (`dotCMS/private-issues#N`), the private-issue rules of this skill hold: the report stays local, and nothing describing the defect leaves the machine.
- `HEAD_DIR` is `CORE_REPO` itself: the working tree is what is reviewed, so no PR head is checked out or removed. `RUN_DIR` is `<scratchpad>/review-local-<BRANCH_SLUG>` (with `-2`, `-3` when it exists).

| Stage | In local mode |
|---|---|
| Preflight | unchanged |
| 1 Eligibility | skipped: there is no PR |
| 2 Fetch | replaced by "Local Stage 2" below: branch, base, HEAD, changed files and diff from local git, the issue from `issue=`; no PR threads, no existing-feedback digest. The brief is printed as in PR mode |
| 3 Classify | unchanged, on the local file list and diff. A CI change has no run yet: `CI_GATE_EVIDENCE` lists the triggers only |
| 4, 4b Reviewers, recovery | unchanged: same agents, bundles, sidecars and recovery; slices built from local git, prompts reworded (below) |
| 5, 6 Consolidate, validate | unchanged, except the head check (below) and `SELF_TRIAGE = PRIVATE_LINKED` |
| 7 Report | the same sections, with plain `path:line` references instead of GitHub links |
| 7b Verification | anchors checked against the working tree and the local diff; no existing comments, only the previous local report for the branch |
| 8 Posting | never posts, never asks: writes the report to `REPORT_PATH` and prints the summary table |
| 9 Simplifier | skipped: the caller decides the fixes |
| 10 Summary | the report path instead of the posting counts; the effective-POM base checkout is removed, the working tree never |

## Local Stage 2: branch, diff and issue

1. **HEAD and branch.**

   ```bash
   git rev-parse HEAD
   git rev-parse --short HEAD
   git branch --show-current
   ```

   Store `HEAD_SHA`, `SHORT_SHA` and `BRANCH` (`detached` when the last one prints nothing). `BRANCH_SLUG` is `BRANCH` with every character outside `A-Za-z0-9._-` replaced by `-`.

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

6. **The issue**, only with `issue=`. Resolve `ISSUE_REPO` and `ISSUE_NUMBER` from the token (a bare number is `dotCMS/core`), then run steps 2 to 6 of "Resolve the linked issue + acceptance criteria" (`issue-and-ac.md`) unchanged, with `gh issue view` only. Skip the "Own PR on a private issue" check and set `SELF_TRIAGE = PRIVATE_LINKED`. When `<OUT_DIR>/acceptance-criteria.md` exists and its first line starts with `Status: approved` (the ACs `/solve-issue` confirmed with the developer), it is `ACCEPTANCE_CRITERIA` instead of the issue body's. When the fetch fails but that approved file exists (the Security Pipeline's host cannot read `dotCMS/private-issues`), say so in one line and use the file all the same, with `PRIVATE_LINKED` set from the token. Without `issue=`, or when the fetch fails with no such file (say so in one line), `ACCEPTANCE_CRITERIA = NO_LINKED_ISSUE` and the report says why AC coverage was not evaluated.

7. **Output paths.** `OUT_DIR` is `out=`, else `local/security/<ISSUE_NUMBER>/` when `PRIVATE_LINKED`, else `local/code-review/`; create it with `mkdir -p`. `REPORT_PATH = <OUT_DIR>/local-<BRANCH_SLUG>-<SHORT_SHA>-report.md`. `PREVIOUS_REPORT` is the newest `local-<BRANCH_SLUG>-*-report.md` in `ls -t <OUT_DIR>`, if any (it may be `REPORT_PATH` itself when HEAD has not moved; read it now, before it is overwritten).

Then write and print the brief as `issue-and-ac.md` describes, headed `## What this change does — <BRANCH>`, from the commit messages, the diff, the file list and the issue.

## Stage 4: slices and prompts

- Build the slices from local git, never from a saved diff: `diff-slices.sh --out <RUN_DIR>/slices --expect <RUN_DIR>/changed-files.txt --git <BASE_SHA> <RUN_DIR>/slices/*.txt` (no `--head`: the working tree, untracked files included).
- There is no existing-feedback digest; drop that line from the Inputs block.
- In every template, read `PR #<N> '<TITLE>'` as `the local branch <BRANCH> '<TITLE>'`, `PR diff` as `Local diff`, and `linked issue #<ISSUE_NUMBER>` as `issue <ISSUE_REPO>#<ISSUE_NUMBER>`, and add one line: "This is a local review before any PR: the code under review is the working tree; there is no PR to look up."

## Stages 5 and 6

- **Head check:** instead of `gh pr view`, `git rev-parse HEAD`. If it moved (the developer committed during the review), set `HEAD_SHA` to it and rebuild the slices as above; the anchor check that follows runs on the working tree either way.
- **Anchors:** `verify-anchors.sh ... --worktree` instead of `--rev <HEAD_SHA>`.
- **`SELF_TRIAGE = PRIVATE_LINKED`.** The code under review is always the caller's own, so with a private issue the "Own PR on a private issue" rules of `validation.md` apply; otherwise validation and the filter run as in PR mode.
- `TEST_PLAN_PATH` is `<OUT_DIR>/local-<BRANCH_SLUG>-<SHORT_SHA>-test-plan.md`, next to the report, whatever the issue.

## Stage 7: the report

The same report as `report-template.md`, written to `REPORT_PATH` in Stage 8, with these changes:
- Title `# Local review: <BRANCH> — <TITLE>`. The `HEAD commit` line becomes `**Reviewed**: <BASE_SHA>..<HEAD_SHA>`, plus `, uncommitted changes included` when `git status --porcelain` prints anything. `**Linked issue**` names `<ISSUE_REPO>#<ISSUE_NUMBER>`, or "none (no `issue=` given): AC coverage and test design not run". The CI exercise line lists each changed workflow's triggers and reads "not exercised yet; check the PR's run".
- `**File**` rows are plain `path:line`: the commit may exist only locally, so a GitHub link would not resolve. Each finding adds `**Why it matters**` between `**Issue**` and `**Fix**`, and a finding also in `PREVIOUS_REPORT` gets ` [repeat]` after its title.
- The recommendation reads **Ready for the PR**, **Ready after the listed fixes** or **Fix first** in place of Approve, Approve with Comments and Request Changes, with the same gates.

## Stage 7b: verification against the working tree

There is no PR to refetch and nothing is posted, but every anchor is still checked. 7b.1 to 7b.4 of `anchors.md` become:

1. **HEAD.** `git rev-parse HEAD`. If it differs from `HEAD_SHA`, the branch moved during the review: set `HEAD_SHA` to it and say so in the gate line.
2. **Anchors**, with the 7b.3 table, against the working tree and the per-file local diff:

   ```bash
   bash <SKILL_DIR>/scripts/verify-anchors.sh --findings <RUN_DIR>/merged/anchored.jsonl --base <BASE_SHA> --worktree --root <CORE_REPO> --out <RUN_DIR>/merged/postable.jsonl
   ```

   It reads each file from the working tree and takes the changed ranges from `git diff --unified=0 <BASE_SHA> -- <path>`, one call per path; every line of an untracked file is changed. By hand: `sed -n '<line>p' <path>`, then `grep -n -F -- '<evidence>' <path>` when the line does not match.
3. **Repeats instead of duplicates.** There are no GitHub comments. A finding that matches one in `PREVIOUS_REPORT` (same path, within ±3 lines, same claim) is not dropped: it was found before and is still in the code, so it is still open. Keep it and mark it `[repeat]`.

Then print:

```
Local verification: <kept>/<total> findings anchored — <stale> stale anchors dropped, <reanchored> re-anchored, <repeats> repeats of <PREVIOUS_REPORT or "no previous report">, HEAD <unchanged|moved 1a2b3c4 → 5d6e7f8>.
```

## Stage 8: write the report, post nothing

No `AskUserQuestion`, and 8a does not run; the caller acts on the report.
- Write the Stage 7 report to `REPORT_PATH`. Every finding carries its severity, `path:line`, its origin (ADDED-BY-THIS-PR or PRE-EXISTING), what is wrong, why it matters and the suggested fix, with the reviewer confidence and the validator score. The AC coverage and test design sections carry the gaps 8a would have walked.
- Print the summary table of `posting.md`, headed `## Findings — local branch <BRANCH> (HEAD <SHORT_SHA>)`, then the "Not in report" line (stale anchors, below the confidence cut-off), the coverage gaps and `Report: <REPORT_PATH>`. Go to Stage 10.

## Stages 9 and 10

Stage 9 is skipped. In the Stage 10 summary, replace the `Comments posted`, `Comments edited` and `Skipped` lines with `**Report**: <REPORT_PATH>`, and print `Nothing was posted (local mode).` instead of the last line.

The caller acts on the report: every finding is fixed or deferred with the reason. Re-review the fixes with `base=<the HEAD_SHA the report names>`.
