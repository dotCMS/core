---
name: dot-pr-review
description: >
  Review a dotCMS/core change with specialist reviewer agents (Angular, Java, REST, persistence,
  security, tests, CI, Maven, docs, logic against the acceptance criteria, test design), each
  finding re-anchored to the code it cites. Two uses: someone else's PR ("review PR 34829", or its
  URL) gets the findings posted on it after one approval; your own work ("review my branch", the
  `local` mode, before or after its PR opens) gets a report on disk and nothing posted.
allowed-tools: Bash(gh --version:*), Bash(gh pr view:*), Bash(gh pr diff:*), Bash(gh pr list:*), Bash(gh pr checks:*), Bash(gh run view:*), Bash(gh issue view:*), Bash(gh api repos/dotCMS/core/pulls/*), Bash(gh api --paginate repos/dotCMS/core/*), Bash(gh api user:*), Bash(pwd), Bash(ls:*), Bash(cat:*), Bash(grep:*), Bash(sed -n:*), Bash(cmp:*), Bash(jq:*), Bash(mktemp:*), Bash(mkdir -p:*), Bash(bash .claude/skills/dot-pr-review/scripts/*), Bash(git -C:*), Bash(git fetch:*), Bash(git clone:*), Bash(git diff:*), Bash(git merge-base:*), Bash(git rev-parse:*), Bash(git symbolic-ref:*), Bash(git branch --show-current), Bash(git log:*), Bash(git ls-files:*), Bash(git status:*), Edit(local/code-review/**), Edit(local/agents/**), Edit(local/security/**)
owner: "@oidacra"
status: experimental
---

# dot-pr-review

## Two uses

Decide first which one the request is. `local` is your own work. A PR number or URL is your own work when you opened the PR, and someone else's PR otherwise: compare `gh pr view <N> --repo dotCMS/core --json author --jq .author.login` with `gh api user --jq .login` right after parsing, and stop with the error if either lookup fails.

| Use | Invocation | Reviews | Output |
|---|---|---|---|
| **Someone else's PR** | `/dot-pr-review <PR number or URL> [auto \| dry-run \| force]` | the PR head, checked out from GitHub, against the PR base | findings posted on the PR after one approval (`auto` posts without asking, `dry-run` posts nothing), then an optional verdict |
| **Your own work** | `/dot-pr-review local [base=<ref>] [issue=<ref>] [out=<dir>]` | the working tree of the current branch against its base, uncommitted changes included | a report and a test plan on disk, leaving out points already raised on the branch's open PR, if any; nothing posted, nothing asked. After fixing, rerun with `base=<the HEAD the report names>` to review only the fix |
| **Your own work, PR already open** | `/dot-pr-review <your PR number or URL> [out=<dir>]` | the PR head, checked out from GitHub, against the PR base, with the PR's existing comments | the same report and test plan, in the same place, as `local`; nothing posted, nothing asked |

All three run the same reviewers, standards, validation and anchor checks; `references/local-mode.md` says what changes for your own work, including your own open PR.

## Arguments

| Token | Meaning |
|---|---|
| `<PR_NUMBER>` / `<PR_URL>` | The PR in `dotCMS/core`; the number is taken from the URL. |
| `auto` | Stage 8 posts every finding that survived Stages 6 and 7b in one parallel batch, without asking. |
| `dry-run` | Runs the whole pipeline, Stage 7b included, prints the table and posts nothing. Never prompts. |
| `force` | Overrides a Stage 1 `SKIP` (merged, draft, bot author, already reviewed at this head) and a Stage 3 `reviewable == 0`. |
| `local` | First token: local mode. |
| `base=<ref>` | Local mode: the base of `git diff <base>`. Default `git merge-base HEAD origin/<default branch>`, after a fetch. |
| `issue=<ref>` | Local mode: the issue whose ACs the change is checked against (`38250`, `owner/repo#N` or a link; `dotCMS/private-issues#N` is private mode). Without it the ac-validator and the test-case designer do not run. |
| `out=<dir>` | Your own work: where the report and the test plan go. Default `local/code-review/`, or `local/security/<N>/` for a private issue. |

Parse once. `LOCAL` when the first token is `local`, then read its `key=value` tokens. `OWN_PR` when the PR is yours (above). For either, read `references/local-mode.md` now: it lists what changes at every stage, and `auto`, `dry-run` and `force` do not apply (print `Ignoring <token>: your own work is reviewed into a local report.` for each one given). Otherwise set `AUTO_POST`, `DRY_RUN` and `FORCE` from their tokens. `DRY_RUN` beats `AUTO_POST` (a dry run never posts), and `DRY_RUN` and `FORCE` both bypass Stage 1.

## Command rules

Every Bash call in this skill follows these; the permission rules and hooks of a core checkout reject the alternatives mid-run.

- One command per call, run in place from the repo root, with repo-relative paths in commands and in agent prompts alike. The only absolute paths are the run directory (`RUN_DIR`, and `HEAD_DIR` inside it) and the skill's own directory (`SKILL_DIR`, given when it loads).
- `--repo dotCMS/core` on every `gh pr` and `gh api` call. `gh` output and errors reach you whole: saving stdout to a file in `RUN_DIR` is fine, piping it through `head` or `tail` or silencing stderr hides auth and rate-limit errors.
- Comments are fetched as a JSON field (`--json comments`). gh 2.99+ rejects `--comments` next to `--json`.
- Mechanical steps are the scripts in `scripts/`, run from the repo root exactly as `bash .claude/skills/dot-pr-review/scripts/<name>.sh`: that is the one form `allowed-tools` pre-approves (each prints its usage with `--help`). References are read as `<SKILL_DIR>/references/<name>.md`. Hooks can reject a heredoc or an inline `python`/`node` script, and `rm -rf` on an absolute path, so a run writes no such script and wipes nothing: a new run gets a new directory.

## Preflight (before Stage 1, both uses)

Run `pwd`, then `ls docs/frontend`. If `docs/frontend/` is missing, stop with:

> This skill must be run from a clone of `dotCMS/core`. Current working directory is `<pwd output>`. Please `cd` into your `dotCMS/core` checkout and retry.

A worktree passes like a clone. Store the directory as `CORE_REPO`: the reviewers read from it, and Stage 7b re-slices the diff from it.

Then gate on the `gh` version. Stages 2 and 3 need `changeType` from `gh pr view --json files`, `gh pr diff --exclude`, and the `issueType` and `parent` issue fields. Run:

```bash
gh --version
```

Its first line reads `gh version <major>.<minor>.<patch> (<date>)`. Compare major, then minor, as numbers: `2.100` is newer than `2.99`, although it sorts below it as text.

| Result | Action |
|---|---|
| 2.99 or newer | proceed |
| older than 2.99 | **abort**: "This skill requires GitHub CLI 2.99.0 or newer (found `<version>`). Upgrade with `brew upgrade gh` or `mise up gh@latest`, then retry." |
| command not found, or no version on the first line | **abort**: "GitHub CLI (`gh`) is not installed or not on PATH. Install it from https://cli.github.com and run `gh auth login`." |

A later `gh` call failing with `unknown flag:` or `Unknown JSON field:` means an older `gh` is first on PATH: abort and report the failing command verbatim. Every flag and field stays as written; the older forms they replaced give a worse review that still looks complete.

## Run directory (before Stage 1)

Create a fresh, empty directory in the session scratchpad: `<scratchpad>/review<N>`, or `<scratchpad>/review-local-<BRANCH_SLUG>` in local mode (right after Local Stage 2 step 1), adding `-2`, `-3` when the name exists; `mktemp -d` when the session has no scratchpad. Store it as `RUN_DIR`, create its `sidecars/`, `slices/`, `bundles/` and `merged/` folders with one `mkdir -p`, and set `SIDECAR_DIR = <RUN_DIR>/sidecars`.

Everything the run writes for itself (diffs, slices, bundles, sidecars, merged findings, the PR head checkout) goes there, so two reviews never share a file. What outlives the session (reports, test plans) goes to `local/`.

## Pipeline

Read each stage's reference when you reach that stage, not before.

| # | Stage | What it does | Done when | Read |
|---|---|---|---|---|
| 1 | Eligibility | Haiku checks state, draft, bot author, a review of yours at this head; on `SKIP` print the reason and stop, unless `FORCE` or `DRY_RUN` (then warn and go on) | `ELIGIBLE`, or a `SKIP` that `force` / `dry-run` overrides | `pr-data.md` |
| 2 | Fetch | metadata, base and head, diff, file list; PR head checked out; existing-feedback digest; linked issue and ACs; the brief | brief printed, `HEAD_DIR` ready | `pr-data.md`, `issue-and-ac.md` |
| 3 | Classify | buckets (code, CI, Maven, docs), stack gate, specialist triggers, CI exercise and vendored-import checks | `STACK` and every `RUN_*` flag recorded | `classify.md` |
| 4 | Launch | standards bundles, diff slices, roster, every active reviewer in parallel | `diff-slices.sh` prints `OK`; every active agent launched | `agents.md`, `sidecar-and-budget.md`, `agent-prompts.md` |
| 4b | Recovery | sidecar check, one resume per silent agent, report-only findings saved | every instance `OK` or `SIDECAR_ONLY`; `COVERAGE_GAPS` recorded | `recovery.md` |
| 5 | Consolidate | head check, merge, anchors re-derived, origin, dedupe and fold, AC reconciliation, test plan file | one anchored, deduped list | `validation.md` |
| 6 | Validate | Haiku per finding, Sonnet for logic, CI, Maven and docs, MISSING and contested ACs, at most 20 at once; keep scores of 80 or more (every finding under `SELF_TRIAGE`) | every finding scored and filtered | `validation.md` |
| 7 | Report | the structured report, SHA-anchored | report printed | `report-template.md` |
| 7b | Verify | head and comments refetched, anchors re-checked, duplicates dropped, overlaps become replies; runs in every mode, nothing reaches GitHub without it | gate line printed | `anchors.md` |
| 8 | Post | AC walk-through as actions; a card per finding with a recommendation, the table and one posting decision; the verdict asked apart | posted, or declined | `posting.md` |
| 9 | Simplifier | optional proposals, never edits | answered or skipped | `posting.md` |
| 10 | Summary | summary, PR head checkout removed | summary printed | `posting.md` |

Stages 1-6 are automatic; 8-10 are interactive unless `auto` or `dry-run`. For your own work (`LOCAL` or `OWN_PR`), `local-mode.md` changes Stages 2, 7, 7b, 8 and 10, and Stages 1 and 9 are skipped.
