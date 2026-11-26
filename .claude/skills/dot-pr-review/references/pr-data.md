# Stages 1 and 2: eligibility and PR data

Local mode skips Stage 1 and replaces this Stage 2 with "Local Stage 2" in `local-mode.md`.

## Stage 1: Eligibility Check

Before spending any tokens on analysis, verify the PR is worth reviewing. Launch a **Haiku** agent with this task:

```
Fetch PR #<N> metadata via `gh pr view <N> --repo dotCMS/core --json state,isDraft,author,reviews,title,body,headRefOid`.

Note the field name: in `gh pr view --json reviews` each review carries `commit.oid` (a nested object). It is **not** `commit_id` — that spelling only exists on the REST endpoint `gh api repos/dotCMS/core/pulls/<N>/reviews`. Using the wrong one yields null, which compares unequal to every SHA and makes this check silently pass every time.

Return one of:
- ELIGIBLE
- SKIP — <reason>

Skip the PR if any of these are true:
- state is CLOSED or MERGED
- isDraft is true
- author.login matches bot pattern: dependabot, renovate, github-actions, gradle[bot]
- reviews[] contains a review by the current user (gh api user → login) **on the current head SHA** — compare `reviews[].commit.oid` against `headRefOid`, NOT the review's timestamp
- title starts with "chore(deps)" or "[WIP]"

Keep the response under 3 lines.
```

If the Haiku returns `SKIP`, print the reason and stop. Do NOT proceed to Stage 2 — **unless `FORCE` or `DRY_RUN` is set**, in which case print the reason as a warning and continue.

**Why the SHA and not a time window.** This check used to skip any PR the user had reviewed "in the last 24h". That is wrong in both directions: a PR reviewed 20 hours ago and force-pushed twice since then is a *new* PR and gets skipped, while one reviewed 25 hours ago with no new commits gets a full duplicate review. Comparing `commit.oid` to `headRefOid` asks the question that actually matters — has this exact code already been reviewed by me?

When a previous review exists on an **older** SHA, proceed. Do not treat the earlier comments as a reason to stay quiet: Stage 7b refetches them and drops anything that would duplicate, which is a far more precise filter than skipping the whole run.

## Stage 2: Fetch PR Data

With eligibility confirmed, fetch everything the pipeline needs.

```bash
# Metadata
gh pr view <N> --repo dotCMS/core --json title,body,headRefName,baseRefName,additions,deletions,changedFiles,headRefOid

# Base commit of the diff
gh api repos/dotCMS/core/pulls/<N> --jq '.base.sha'

# Full diff, minus noise that no reviewer should ever read; stdout saved for the slicer
gh pr diff <N> --repo dotCMS/core --exclude 'pnpm-lock.yaml' --exclude 'yarn.lock' --exclude 'package-lock.json' --exclude '**/*.snap' --exclude '**/dist/**' > <RUN_DIR>/pr.diff

# File list WITH per-file churn and change type (one call, replaces `gh pr diff --name-only`)
gh pr view <N> --repo dotCMS/core --json files --jq '.files[] | {path, changeType, additions, deletions}'
```

`--exclude` is repeatable and glob-based; it keeps lock files and generated output out of the diff the reviewers read. Do **not** exclude anything a reviewer might legitimately need to judge — only lock/generated/snapshot artifacts. Saving stdout to a file is allowed; stderr still reaches you.

`gh pr view --json files` returns `changeType` per file (`ADDED` / `MODIFIED` / `DELETED` / `RENAMED`) along with `additions` and `deletions`. Use this as the single source of truth for the file list — it gives Stage 3 both the change type and the real churn in one request. Write its paths, one per line, to `<RUN_DIR>/changed-files.txt`, leaving out the lock, snapshot and `dist` files the diff excludes: the slicer checks every slice against it.

Each `gh` call is ONE Bash command. Do not combine them with `&&`, do not pipe through `head`, and do not redirect stderr. If `gh` fails, surface the full error to the user.

**Capture the HEAD SHA** from `headRefOid` — it's used throughout for SHA-anchored links (Stage 7) and for posting comments (Stage 8). Store it as `HEAD_SHA`, the base as `BASE_SHA`, and `changedFiles` as `CHANGED_FILES`.

### Large PRs: build the diff from local git

`gh pr diff` fails with HTTP 406 above 20,000 diff lines and writes nothing, and `gh pr view --json files` stops at 100 files. When either happens (`CHANGED_FILES` > 100, or the 406):

- File list: `gh api --paginate repos/dotCMS/core/pulls/<N>/files --jq '.[] | {path: .filename, changeType: .status, additions, deletions}'` (`status` is `added`, `modified`, `removed` or `renamed`; map `removed` to `DELETED` and the others to their upper case).
- Diff: from local git, one `git diff <BASE_SHA> <HEAD_SHA> -- <path>` per path, with the paths read from a file (`diff-slices.sh --git <BASE_SHA> --head <HEAD_SHA>`, see `agents.md`). Never paste a path list into a command line.
- A huge generated or JSON slice (a Postman collection, a generated client) is not handed to an agent. Precompute what a reviewer would count, base against head (requests, tests, endpoints, with `jq`), and give the agent those numbers instead.

### Materialize the PR head

Reviewers told to read full files read the checkout they run in, which is usually `main`, not the PR. Check the head out once, detached, in the run directory:

```bash
git -C <CORE_REPO> cat-file -e <HEAD_SHA>^{commit}
git -C <CORE_REPO> fetch origin pull/<N>/head
git -C <CORE_REPO> worktree add --detach <RUN_DIR>/pr<N> <HEAD_SHA>
```

(fetch only when `cat-file` errors). Store it as `HEAD_DIR`. Every reviewer reads code from `<HEAD_DIR>/<path>` and reports repo-relative `file:line`; the historical reviewer runs `git -C <HEAD_DIR>` for blame and log. Stage 10 removes it with `git -C <CORE_REPO> worktree remove --force <HEAD_DIR>`, also when the run stops early.

### Existing feedback digest

Right after the fetches above, read what has already been said on the PR, so no reviewer spends turns re-finding it:

```bash
gh api --paginate repos/dotCMS/core/pulls/<N>/comments --jq '.[] | {id, path, line, original_line, user: .user.login, body}'
gh api --paginate repos/dotCMS/core/issues/<N>/comments --jq '.[] | {id, user: .user.login, body}'
```

Write `<RUN_DIR>/bundles/existing-feedback.md`: one line per inline comment (`path:line @user: <first 200 characters>`) and one per PR-level comment (`@user: <first 300 characters>`), bots included. Every reviewer gets its path (`agent-prompts.md`). Bots edit their comments after the fact (a `claude[bot]` "working..." comment becomes a full review minutes later), so Stage 7b fetches both lists again.
